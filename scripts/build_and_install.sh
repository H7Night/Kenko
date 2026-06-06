#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Building and Installing Debug APK..."
"$SCRIPT_DIR/../gradlew" installDebug

echo
echo "Success! The app has been installed and is ready to run."
