#!/usr/bin/env bash
set -euo pipefail

APK="$(dirname "$0")/../app/build/outputs/apk/debug/app-debug.apk"

echo "Installing Debug APK..."
adb install -r "$APK"

echo
echo "Install Successful!"
