#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK="$SCRIPT_DIR/../app/build/outputs/apk/debug/app-debug.apk"

if ! command -v adb >/dev/null 2>&1; then
    echo "Error: adb not found. Install Android SDK platform-tools and add it to PATH." >&2
    exit 1
fi

if [[ ! -f "$APK" ]]; then
    echo "Error: APK not found at $APK. Run build_debug.sh first." >&2
    exit 1
fi

echo "Installing Debug APK..."
adb install -r "$APK"

echo
echo "Install Successful!"
