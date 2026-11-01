#!/usr/bin/env bash
# Verify the certificate of the APK RuStore installs matches the AAB uploaded to Play.
set -euo pipefail
[[ $# -eq 2 ]] || { echo 'Usage: verify-store-artifacts.sh RUSTORE.apk PLAY.aab' >&2; exit 2; }
: "${ANDROID_HOME:?Source toolchains/activate.sh first}"
[[ -s $1 && -s $2 ]] || { echo 'Missing APK or AAB' >&2; exit 1; }
apk_cert=$("$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --print-certs "$1" |
    sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -n 1)
[[ -n $apk_cert ]] || { echo 'APK has no verified signing certificate' >&2; exit 1; }
# Self-signed owner keys are expected, so -strict would incorrectly reject them.
verification=$(jarsigner -verify "$2")
[[ $verification == *'jar verified.'* ]] || { echo 'AAB JAR signature failed' >&2; exit 1; }
aab_cert=$(keytool -J-Duser.language=en -printcert -jarfile "$2" |
    sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' |
    tr -d ':' | tr '[:upper:]' '[:lower:]' | head -n 1)
[[ -n $aab_cert && $apk_cert == "$aab_cert" ]] || {
    echo 'APK and AAB signing certificates differ' >&2; exit 1;
}
echo "Matching APK/AAB signing certificate (SHA-256): $apk_cert"
