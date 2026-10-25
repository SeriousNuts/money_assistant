#!/usr/bin/env bash
# Install a user-local Android toolchain, without sudo or any credentials.
set -euo pipefail
SDK="$HOME/.local/android-sdk"
JDKS="$HOME/.local/jdks"
mkdir -p "$SDK/cmdline-tools" "$JDKS" /tmp/opencode
if ! find "$JDKS" -maxdepth 3 -path '*/jdk-11*/bin/java' | grep -q .; then
    curl -fL --retry 3 'https://api.adoptium.net/v3/binary/latest/11/ga/linux/x64/jdk/hotspot/normal/eclipse' -o /tmp/opencode/money-jdk11.tar.gz
    tar -xzf /tmp/opencode/money-jdk11.tar.gz -C "$JDKS"
fi
if ! find "$JDKS" -maxdepth 3 -path '*/jdk-21*/bin/java' | grep -q .; then
    curl -fL --retry 3 'https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse' -o /tmp/opencode/money-jdk21.tar.gz
    tar -xzf /tmp/opencode/money-jdk21.tar.gz -C "$JDKS"
fi
if [[ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]]; then
    curl -fL --retry 3 'https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip' -o /tmp/opencode/money-android-cli.zip
    unzip -oq /tmp/opencode/money-android-cli.zip -d /tmp/opencode/money-android-cli
    mkdir -p "$SDK/cmdline-tools/latest"
    cp -a /tmp/opencode/money-android-cli/cmdline-tools/. "$SDK/cmdline-tools/latest/"
fi
JAVA_HOME="$(find "$JDKS" -maxdepth 1 -type d -name 'jdk-21*' | sort -V | tail -n 1)"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" \
    'platforms;android-30' 'build-tools;30.0.2' 'platform-tools'
printf 'sdk.dir=%s\n' "$SDK" > local.properties
echo 'Run: source toolchains/activate.sh'
