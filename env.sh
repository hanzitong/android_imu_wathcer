#!/usr/bin/env bash
# Android Sensor Viewer 開発用環境変数
# 使い方: source .env.sh

export JAVA_HOME=/home/nachi/.local/jdk17/jdk-17.0.18+8
export ANDROID_HOME=/home/nachi/Android/Sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
