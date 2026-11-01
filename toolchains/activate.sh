#!/usr/bin/env bash
# Source this file (do not execute it) before building with Gradle 8.13.
_money_jdk21="$(find "$HOME/.local/jdks" -maxdepth 1 -type d -name 'jdk-21*' | sort -V | tail -n 1)"
export JAVA_HOME="${MONEY_JDK21:-$_money_jdk21}"
unset _money_jdk21
export ANDROID_HOME="${ANDROID_HOME:-$HOME/.local/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
