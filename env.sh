#!/usr/bin/env bash
# Android Sensor Viewer 開発用環境変数
# 使い方: source .env.sh

export JAVA_HOME=$HOME/.local/jdk17/jdk-17.0.18+8
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# local.properties を現在の環境に合わせて生成
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "sdk.dir=$ANDROID_HOME" > "$SCRIPT_DIR/local.properties"
