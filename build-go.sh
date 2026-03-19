#!/bin/sh
set -e

export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.0.12077973"
export PATH="$PATH:/opt/homebrew/bin:$HOME/go/bin"

# Check deps
command -v go >/dev/null || { echo "go not found"; exit 1; }
command -v gomobile >/dev/null || { echo "gomobile not found, run: go install golang.org/x/mobile/cmd/gomobile@latest"; exit 1; }
command -v gobind >/dev/null || { echo "gobind not found, run: go install golang.org/x/mobile/cmd/gobind@latest"; exit 1; }
[ -d "$ANDROID_NDK_HOME" ] || { echo "NDK not found at $ANDROID_NDK_HOME"; exit 1; }

cd "$(dirname "$0")/relay"

echo "Building gomobile .aar..."
gomobile bind -v -target=android -androidapi 23 -o mobile.aar ./mobile/ 2>&1

echo "Copying .aar to app/libs..."
mkdir -p ../app/app/libs
cp mobile.aar ../app/app/libs/mobile.aar

echo "Copying hook.js to assets..."
mkdir -p ../app/app/src/main/assets
cp ../hook.js ../app/app/src/main/assets/hook.js

echo "Done. .aar size: $(du -h mobile.aar | cut -f1)"
