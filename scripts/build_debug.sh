#!/usr/bin/env bash
set -euo pipefail

echo "Starting to build Debug APK..."
"$(dirname "$0")/../gradlew" assembleDebug

echo
echo "Build Successful!"
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
