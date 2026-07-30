#!/bin/bash
# Copyright 2026 Luca Azhrael
# Ravencore APK Compilation Script (Root Execution)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

echo "Building Ravencore native Compose App via Gradle..."

# Ensure changelog asset is synced
mkdir -p overlay_src/app/src/main/assets
cp -f changelog.md overlay_src/app/src/main/assets/changelog.md 2>/dev/null

cd overlay_src || exit 1
./gradlew assembleRelease

if [ $? -eq 0 ]; then
    echo "APK Compilation Successful: raven_engine.apk generated!"
    cp -f app/build/outputs/apk/release/app-release.apk ../raven_engine.apk
    cd ..
else
    echo "APK Compilation FAILED!"
    cd ..
    exit 1
fi
