#!/usr/bin/env bash
# Build, install and open Money Assistant. Use --demo to install the side-by-side variant.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
source toolchains/activate.sh
if [[ "${1:-}" == '--demo' ]]; then
    # Remove generated Firebase resources from a preceding full-app build.
    bash gradlew --no-daemon --max-workers=2 :app:clean :app:assembleDebug -PdemoSideBySide=true
    PACKAGE=com.example.myapplication.preview
else
    bash gradlew --no-daemon --max-workers=2 :app:clean :app:assembleDebug
    PACKAGE=com.example.myapplication
fi
bash toolchains/start-emulator.sh
ADB="$ANDROID_HOME/platform-tools/adb"
"$ADB" wait-for-device
for attempt in $(seq 1 120); do
    if [[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then break; fi
    if [[ "$attempt" == 120 ]]; then echo 'Emulator did not finish booting; see /tmp/opencode/money-assistant-emulator.log' >&2; exit 1; fi
    sleep 2
done
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" shell am start -n "$PACKAGE/com.example.myapplication.MainActivity"
echo "Money Assistant is running in the emulator ($PACKAGE)."
