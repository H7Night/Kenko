#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLEW="$SCRIPT_DIR/../gradlew"

if [[ ! -x "$GRADLEW" ]]; then
    echo "Error: gradlew not found or not executable at $GRADLEW" >&2
    exit 1
fi

echo "Building and Installing Debug APK..."
"$GRADLEW" installDebug "$@"

echo
echo "Success! The app has been installed and is ready to run."
