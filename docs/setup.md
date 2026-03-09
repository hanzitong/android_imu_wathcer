# 環境構築手順

このドキュメントは、このプロジェクトをゼロから動かせる状態にするために実行した
コマンドを順番通りに記録している。

**前提条件:**
- Ubuntu（sudo なし / ホームディレクトリへの書き込みのみ）
- インターネット接続
- curl, unzip, tar が利用可能

---

## 1. JDK 17 のインストール

Android SDK・Gradle 8 系はいずれも JDK 17 以上を必要とする。
`apt` でのインストールは sudo が必要なため、Adoptium（Eclipse Temurin）のバイナリを
ユーザーホームに展開する。

```bash
# 保存先ディレクトリを作成
mkdir -p ~/.local/jdk17

# Temurin JDK 17.0.18 をダウンロード（約 200MB）
curl -L -o /tmp/jdk17.tar.gz \
  "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.18%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.18_8.tar.gz"

# 展開して所定の場所に移動
tar -xzf /tmp/jdk17.tar.gz -C /tmp/
mv /tmp/jdk-17.0.18+8 ~/.local/jdk17/

# 動作確認
~/.local/jdk17/jdk-17.0.18+8/bin/java -version
# 出力例: openjdk version "17.0.18" 2025-01-21
```

---

## 2. Android SDK のインストール

Android コマンドラインツール（`sdkmanager`）をダウンロードし、
必要な SDK コンポーネントをインストールする。

```bash
# SDK ディレクトリを作成
mkdir -p ~/Android/Sdk/cmdline-tools

# Android コマンドラインツールをダウンロード（約 300MB）
curl -L -o /tmp/cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

# 展開（解凍後のディレクトリ名は "cmdline-tools" なので latest に移動）
unzip -q /tmp/cmdline-tools.zip -d /tmp/
mv /tmp/cmdline-tools ~/Android/Sdk/cmdline-tools/latest

# sdkmanager の動作確認
JAVA_HOME=~/.local/jdk17/jdk-17.0.18+8 \
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --version
```

```bash
# ライセンスに同意（すべて "y" で承認）
JAVA_HOME=~/.local/jdk17/jdk-17.0.18+8 \
yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses

# 必要なコンポーネントをインストール
#   platforms;android-35   : Android API 35 のフレームワーク
#   build-tools;35.0.0     : APK のビルドに必要なツール群
#   platform-tools         : adb などのデバッグツール
JAVA_HOME=~/.local/jdk17/jdk-17.0.18+8 \
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# インストール確認
JAVA_HOME=~/.local/jdk17/jdk-17.0.18+8 \
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --list_installed
```

---

## 3. 環境変数の設定

`~/.bashrc` に追記し、新しいターミナルで自動で有効になるようにする。

```bash
# ~/.bashrc に追記
cat >> ~/.bashrc << 'EOF'

# Android 開発環境
export JAVA_HOME=/home/nachi/.local/jdk17/jdk-17.0.18+8
export ANDROID_HOME=/home/nachi/Android/Sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
EOF

# 現在のセッションに反映
source ~/.bashrc

# 確認
java -version
echo $ANDROID_HOME
```

設定後の確認コマンド:

```bash
java -version          # openjdk 17.0.18
sdkmanager --version   # パスが通っているか
adb --version          # platform-tools が入っているか
```

---

## 4. Gradle ラッパーの生成

プロジェクトリポジトリに Gradle ラッパー（`gradlew`）が含まれていなかったため、
Gradle 本体を一時的にダウンロードしてラッパーを生成する。

```bash
# Gradle 8.11.1 をダウンロード
curl -L -o /tmp/gradle-8.11.1-bin.zip \
  "https://services.gradle.org/distributions/gradle-8.11.1-bin.zip"

# 展開
unzip -q /tmp/gradle-8.11.1-bin.zip -d /tmp/

# プロジェクトルートに移動してラッパーを生成
cd /home/nachi/android_imu_wathcer
JAVA_HOME=~/.local/jdk17/jdk-17.0.18+8 \
  /tmp/gradle-8.11.1/bin/gradle wrapper --gradle-version 8.11.1

# 生成されたファイルの確認
ls gradle/wrapper/
# gradlew  gradlew.bat  gradle/wrapper/gradle-wrapper.jar  gradle/wrapper/gradle-wrapper.properties
```

生成された `gradle/wrapper/gradle-wrapper.properties` の内容:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## 5. プロジェクト設定ファイルの作成

### `local.properties` — SDK パスの指定

Gradle が Android SDK を見つけるために必要。`.gitignore` 対象（マシン固有の設定）。

```bash
cat > /home/nachi/android_imu_wathcer/local.properties << 'EOF'
sdk.dir=/home/nachi/Android/Sdk
EOF
```

### `gradle.properties` — ビルド設定

```bash
cat > /home/nachi/android_imu_wathcer/gradle.properties << 'EOF'
android.useAndroidX=true
android.enableJetifier=false
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
EOF
```

各設定の意味:

| プロパティ | 値 | 意味 |
|---|---|---|
| `android.useAndroidX` | `true` | AndroidX ライブラリを使う（必須） |
| `android.enableJetifier` | `false` | AndroidX 非対応の古いライブラリはないので無効化 |
| `org.gradle.jvmargs` | `-Xmx2048m ...` | Gradle デーモンのヒープサイズ上限を 2GB に設定 |

---

## 6. Android リソースファイルの作成

`AndroidManifest.xml` から参照されるリソースが存在しないとビルドエラーになるため、
最低限必要なファイルを作成する。

```bash
# リソースディレクトリを作成
mkdir -p /home/nachi/android_imu_wathcer/app/src/main/res/values
mkdir -p /home/nachi/android_imu_wathcer/app/src/main/res/drawable
mkdir -p /home/nachi/android_imu_wathcer/app/src/main/res/mipmap-anydpi-v26
```

### `res/values/themes.xml` — アプリのテーマ

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.SensorViewer" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

### `res/drawable/ic_launcher_background.xml` — アイコン背景

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#4CAF50" />
</shape>
```

### `res/drawable/ic_launcher_foreground.xml` — アイコン前景

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M54,30 L54,78 M30,54 L78,54"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="6"
        android:strokeLineCap="round" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M54,30 L48,42 L60,42 Z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M78,54 L66,48 L66,60 Z" />
</vector>
```

### `res/mipmap-anydpi-v26/ic_launcher.xml` — アダプティブアイコン

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

### `res/mipmap-anydpi-v26/ic_launcher_round.xml` — 丸型アイコン

内容は `ic_launcher.xml` と同じ（丸型端末用）。

---

## 7. 初回ビルドと動作確認

```bash
cd /home/nachi/android_imu_wathcer

# APK ビルド（初回は依存ライブラリのダウンロードが走るため数分かかる）
JAVA_HOME=~/.local/jdk17/jdk-17.0.18+8 ./gradlew assembleDebug

# 出力確認
ls -lh app/build/outputs/apk/debug/app-debug.apk
# -rw-r--r-- 1 nachi nachi 9.5M ... app-debug.apk

# JVM ユニットテスト実行（実機不要）
JAVA_HOME=~/.local/jdk17/jdk-17.0.18+8 ./gradlew testDebugUnitTest
```

`~/.bashrc` を `source` した新しいターミナルなら `JAVA_HOME=...` のプレフィックスは不要:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

---

## 8. トラブルシューティング

### `JAVA_HOME` が正しく設定されない

`sdkmanager` が古い Java を使って起動するとエラーになる。
コマンドの前に明示的に `JAVA_HOME` を指定する:

```bash
JAVA_HOME=~/.local/jdk17/jdk-17.0.18+8 sdkmanager ...
```

### `Configuration contains AndroidX dependencies but android.useAndroidX is not enabled`

`gradle.properties` に `android.useAndroidX=true` が設定されていない。
[セクション 5](#5-プロジェクト設定ファイルの作成) を参照。

### `Could not find 'mipmap/ic_launcher'` などのリソースエラー

`AndroidManifest.xml` で参照されているリソースが存在しない。
[セクション 6](#6-android-リソースファイルの作成) のファイルを作成する。

### `gradlew: Permission denied`

Gradle ラッパースクリプトに実行権限がない:

```bash
chmod +x gradlew
```

---

## インストール済みコンポーネント一覧

| コンポーネント | バージョン | パス |
|---|---|---|
| JDK | 17.0.18+8 (Temurin) | `~/.local/jdk17/jdk-17.0.18+8` |
| Android SDK | — | `~/Android/Sdk` |
| cmdline-tools | latest (11076708) | `~/Android/Sdk/cmdline-tools/latest` |
| platforms | android-35 (API 35) | `~/Android/Sdk/platforms/android-35` |
| build-tools | 35.0.0 | `~/Android/Sdk/build-tools/35.0.0` |
| platform-tools | latest | `~/Android/Sdk/platform-tools` |
| Gradle (wrapper) | 8.11.1 | `~/.gradle/wrapper/dists/gradle-8.11.1-bin/` |
