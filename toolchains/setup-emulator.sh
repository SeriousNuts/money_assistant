#!/usr/bin/env bash
# Android 11 x86_64 emulator. User-local setup, no Android Studio or sudo required.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/.local/android-sdk}"
JDK21="$(find "$HOME/.local/jdks" -maxdepth 1 -type d -name 'jdk-21*' | sort -V | tail -n 1)"
if [[ -z "$JDK21" ]]; then echo 'Install the toolchain with bash toolchains/setup.sh first' >&2; exit 1; fi
export JAVA_HOME="$JDK21"
export PATH="$JAVA_HOME/bin:$PATH"
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" emulator \
    'system-images;android-30;google_apis;x86_64'
if [[ ! -f "$HOME/.android/avd/MoneyAssistant_API30.ini" ]]; then
    printf 'no\n' | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd \
        --name MoneyAssistant_API30 --package 'system-images;android-30;google_apis;x86_64' \
        --device pixel_3a
fi
if [[ ! -r /dev/kvm || ! -w /dev/kvm ]]; then
    echo 'Warning: /dev/kvm is unavailable. Enable virtualization/KVM for usable speed.' >&2
else
    "$SDK/emulator/emulator" -accel-check
fi
echo 'Ready. Run: bash toolchains/run-on-emulator.sh'
