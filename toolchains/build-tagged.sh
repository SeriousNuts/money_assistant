#!/usr/bin/env bash
# The canonical verification build: one GitHub tag for a complete, successful pair of APKs.
set -euo pipefail
cd "$(dirname "$0")/.."

[[ $# -eq 0 || ($# -eq 1 && $1 == '--plan') ]] || { echo 'Usage: toolchains/build-tagged.sh [--plan]' >&2; exit 2; }
[[ -z "$(git status --porcelain --untracked-files=normal)" ]] || {
    echo 'Commit your source changes before creating a tagged build.' >&2; exit 1;
}
git fetch --quiet origin master --tags
[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/master)" ]] || {
    echo 'Push the current master commit to origin before building.' >&2; exit 1;
}
remote_tags=$(git ls-remote --tags origin 'refs/tags/1.4.2-*') || exit 1
max=0
while read -r hash ref; do
    if [[ $ref =~ ^refs/tags/1\.4\.2-([0-9]+)$ ]]; then
        number=$((10#${BASH_REMATCH[1]}))
        (( number > max )) && max=$number
    fi
done <<< "$remote_tags"
number=$((max + 1))
tag="1.4.2-$number"
code=$((12 + number))
(( code <= 2100000000 )) || { echo 'Android versionCode limit reached.' >&2; exit 1; }
if [[ ${1:-} == '--plan' ]]; then
    echo "Next tag: $tag, Android versionCode: $code (nothing built or pushed)"
    exit 0
fi
git rev-parse --verify "refs/tags/$tag" >/dev/null 2>&1 && {
    echo "Local tag $tag already exists; resolve it before retrying." >&2; exit 1;
}
source toolchains/activate.sh
mkdir -p build/verification
bash gradlew --no-daemon --max-workers=2 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug \
    -PbuildNumber="$number" -PdemoSideBySide=true
cp app/build/outputs/apk/debug/app-debug.apk "build/verification/money-assistant-$tag-demo.apk"
bash gradlew --no-daemon --max-workers=2 :app:assembleDebug -PbuildNumber="$number"
cp app/build/outputs/apk/debug/app-debug.apk "build/verification/money-assistant-$tag-full.apk"
for variant in demo full; do
    apk="build/verification/money-assistant-$tag-$variant.apk"
    "$ANDROID_HOME/build-tools/30.0.2/apksigner" verify "$apk"
done
git tag -a "$tag" -m "Verified Money Assistant $tag (versionCode $code)"
if ! git push origin "refs/tags/$tag"; then
    echo "Build succeeded but tag push failed. Local tag $tag remains; check GitHub before retrying." >&2
    exit 1
fi
cp "build/verification/money-assistant-$tag-demo.apk" \
   "build/verification/money-assistant-$tag-full.apk" "$(dirname "$PWD")/"
echo "Published $tag and copied both APKs to $(dirname "$PWD")/"
