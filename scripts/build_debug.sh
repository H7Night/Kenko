#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLEW="$SCRIPT_DIR/../gradlew"

if [[ ! -x "$GRADLEW" ]]; then
    echo "Error: gradlew not found or not executable at $GRADLEW" >&2
    exit 1
fi

echo "Starting to build Debug APK..."
"$GRADLEW" assembleDebug "$@"

echo
echo "Build Successful!"
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
