#!/usr/bin/env bash
# The canonical verification build: one GitHub tag for a complete, successful pair of APKs.
set -euo pipefail
cd "$(dirname "$0")/.."

[[ $# -eq 0 || ($# -eq 1 && ($1 == '--plan' || $1 == '--store')) ]] || {
    echo 'Usage: toolchains/build-tagged.sh [--plan|--store]' >&2; exit 2;
}
[[ -z "$(git status --porcelain --untracked-files=normal)" ]] || {
    echo 'Commit your source changes before creating a tagged build.' >&2; exit 1;
}
branch=$(git symbolic-ref --short HEAD)
[[ $branch == release/1.4.2 ]] || {
    echo 'Verification tags must be built from release/1.4.2, not master.' >&2; exit 1;
}
git fetch --quiet origin "$branch" --tags
[[ "$(git rev-parse HEAD)" == "$(git rev-parse "origin/$branch")" ]] || {
    echo "Push the current $branch commit to origin before building." >&2; exit 1;
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
if [[ ${1:-} == '--store' ]]; then
    for name in STORE_FILE STORE_PASSWORD KEY_ALIAS KEY_PASSWORD; do
        variable="MONEY_RELEASE_$name"
        [[ -n ${!variable:-} ]] || { echo "$variable must be set for a store release." >&2; exit 1; }
    done
    [[ $MONEY_RELEASE_STORE_FILE == /* && -f $MONEY_RELEASE_STORE_FILE ]] || {
        echo 'MONEY_RELEASE_STORE_FILE must point to an existing absolute file path.' >&2; exit 1;
    }
    [[ ${MONEY_PRIVACY_POLICY_URL:-} =~ ^https://[^[:space:]]+$ ]] || {
        echo 'Provide a public HTTPS MONEY_PRIVACY_POLICY_URL.' >&2; exit 1;
    }
    if grep -REq '\[(ФИО|УКАЗАТЬ|LEGAL NAME|PROVIDE|SPECIFY)' app/src/modern/res/raw*/{privacy,terms}.txt; then
        echo 'Replace and review legal placeholders before a store release.' >&2; exit 1
    fi
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
    "$ANDROID_HOME/build-tools/35.0.0/apksigner" verify "$apk"
done
if [[ ${1:-} == '--store' ]]; then
    # Both artifacts use the same package/version/signing config, but only RuStore
    # defaults the optional online OCR choice to Cloud.ru. Copy between builds.
    bash gradlew --no-daemon --max-workers=2 :app:bundleRelease \
        -PbuildNumber="$number" -PreleaseSigning=true
    grep -Fq '>gemini</string>' app/build/generated/res/resValues/release/values/gradleResValues.xml || {
        echo 'Play AAB must default to Gemini; no tag was created.' >&2; exit 1;
    }
    aab="build/verification/money-assistant-$tag-play.aab"
    cp app/build/outputs/bundle/release/app-release.aab "$aab"
    bash gradlew --no-daemon --max-workers=2 :app:assembleRelease \
        -PbuildNumber="$number" -PreleaseSigning=true -PrustoreDistribution=true
    grep -Fq '>cloud</string>' app/build/generated/res/resValues/release/values/gradleResValues.xml || {
        echo 'RuStore APK must default to Cloud.ru; no tag was created.' >&2; exit 1;
    }
    apk="build/verification/money-assistant-$tag-rustore.apk"
    cp app/build/outputs/apk/release/app-release.apk "$apk"
    bash toolchains/verify-store-artifacts.sh "$apk" "$aab"
fi
git tag -a "$tag" -m "Verified Money Assistant $tag (versionCode $code)"
if ! git push origin "refs/tags/$tag"; then
    echo "Build succeeded but tag push failed. Local tag $tag remains; check GitHub before retrying." >&2
    exit 1
fi
cp "build/verification/money-assistant-$tag-demo.apk" \
   "build/verification/money-assistant-$tag-full.apk" "$(dirname "$PWD")/"
if [[ ${1:-} == '--store' ]]; then
    cp "$apk" "$aab" "$(dirname "$PWD")/"
    echo 'Compare the Play App signing key certificate with the RuStore APK certificate.'
fi
echo "Published $tag; copied verification APKs to $(dirname "$PWD")/"
