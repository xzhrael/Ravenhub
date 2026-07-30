#!/bin/bash
# ==============================================================================
# RavenHub Development Environment Setup Script
# ==============================================================================

set -e

echo "🚀 Setting up RavenHub development environment..."

# 1. Ensure Executable Permissions on Gradle Wrappers and Scripts
echo "🔧 Setting executable permissions on Gradle wrappers..."
chmod +x gradlew 2>/dev/null || true
chmod +x compile_apk.sh 2>/dev/null || true
if [ -d "apps_src" ]; then
    chmod +x apps_src/gradlew 2>/dev/null || true
    chmod +x apps_src/compile_apk.sh 2>/dev/null || true
fi
if [ -d "overlay_src" ]; then
    chmod +x overlay_src/gradlew 2>/dev/null || true
fi

# 2. Check & Install Rust Toolchain
if ! command -v rustc &> /dev/null; then
    echo "📦 Rust not found. Installing Rust toolchain via rustup..."
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    source "$HOME/.cargo/env"
else
    echo "✅ Rust toolchain is installed ($(rustc --version))"
fi

# 3. Add Android Cross-Compilation Targets for Rust (UniFFI / Native Bridge)
echo "🎯 Adding Rust target architectures for Android..."
rustup target add aarch64-linux-android || true
rustup target add armv7-linux-androideabi || true
rustup target add x86_64-linux-android || true

# 4. Auto-detect Android SDK and generate local.properties if missing
SDK_PATH=""
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    SDK_PATH="$ANDROID_HOME"
elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
    SDK_PATH="$ANDROID_SDK_ROOT"
elif [ -d "$HOME/Android/Sdk" ]; then
    SDK_PATH="$HOME/Android/Sdk"
fi

if [ -n "$SDK_PATH" ]; then
    echo "📱 Found Android SDK at: $SDK_PATH"
    
    if [ -d "apps_src" ] && [ ! -f "apps_src/local.properties" ]; then
        echo "sdk.dir=$SDK_PATH" > apps_src/local.properties
        echo "  Created apps_src/local.properties"
    fi
    
    if [ -d "overlay_src" ] && [ ! -f "overlay_src/local.properties" ]; then
        echo "sdk.dir=$SDK_PATH" > overlay_src/local.properties
        echo "  Created overlay_src/local.properties"
    fi
else
    echo "⚠️ Warning: Android SDK not automatically detected."
    echo "   Please set ANDROID_HOME environment variable or ensure SDK is installed at ~/Android/Sdk"
fi

# 5. Check Java JDK
if command -v java &> /dev/null; then
    echo "✅ Java JDK is available ($(java -version 2>&1 | head -n 1))"
else
    echo "⚠️ Warning: Java (JDK 17 or higher) was not found in PATH."
    echo "   Please install OpenJDK 17/21 (e.g. via: sudo apt install openjdk-17-jdk)"
fi

echo ""
echo "🎉 Setup complete! You are ready to develop and build RavenHub."
echo "   To build release APKs, simply run:"
echo "   cd apps_src && ./gradlew assembleRelease"
