#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GRADLEW="$ROOT/gradlew"
LOG="$ROOT/build/build_and_install.log"

if [[ ! -x "$GRADLEW" ]]; then
    echo "Error: gradlew not found or not executable at $GRADLEW" >&2
    exit 1
fi

mkdir -p "$ROOT/build"

echo "Building and Installing Debug APK..."
# Redirect Gradle output to a file so the long-lived Gradle daemon inherits the
# file handle instead of the console pipe, which would otherwise keep waiting.
status=0
"$GRADLEW" installDebug "$@" > "$LOG" 2>&1 || status=$?

cat "$LOG"

if [[ "$status" -eq 0 ]]; then
    echo
    echo "Success! The app has been installed and is ready to run."
else
    echo
    echo "Operation Failed! Please check the error messages above."
fi

exit "$status"
