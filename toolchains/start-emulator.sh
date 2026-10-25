#!/usr/bin/env bash
# Launch the AVD in its own graphical window, without installing the app.
set -euo pipefail
SDK="${ANDROID_HOME:-$HOME/.local/android-sdk}"
AVD=MoneyAssistant_API30
if [[ ! -f "$HOME/.android/avd/$AVD.ini" ]]; then
    echo 'AVD missing: run bash toolchains/setup-emulator.sh first' >&2; exit 1
fi
if "$SDK/platform-tools/adb" devices | grep -Eq '^emulator-[0-9]+[[:space:]]+(device|offline)$'; then
    echo 'An Android emulator is already running.'
    exit 0
fi
mkdir -p /tmp/opencode
# nohup keeps the emulator alive after this terminal command has returned.
nohup "$SDK/emulator/emulator" -avd "$AVD" -memory 2048 \
    -gpu swiftshader_indirect -no-audio -no-boot-anim -no-snapshot-save \
    </dev/null >/tmp/opencode/money-assistant-emulator.log 2>&1 &
echo "Starting $AVD (PID $!). Log: /tmp/opencode/money-assistant-emulator.log"
echo "To close it: $SDK/platform-tools/adb emu kill"
