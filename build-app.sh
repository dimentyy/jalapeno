#!/bin/sh
set -e

ROOT="$(dirname "$0")"
cd "$ROOT/app"

[ -f "./gradlew" ] || { echo "gradlew not found"; exit 1; }

echo "Building APK..."
./gradlew assembleDebug 2>&1 | tail -5

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
    cp "$APK" "$ROOT/whitelist-bypass.apk"
    echo "APK ready: whitelist-bypass.apk ($(du -h "$ROOT/whitelist-bypass.apk" | cut -f1))"
else
    echo "Build failed, APK not found"
    exit 1
fi
