#!/usr/bin/env bash
# Build a sideloadable debug APK of LightOS IV on your own Mac (needs internet).
# Run from anywhere:   bash ~/developer/LightOS-IV/build-apk.sh
set -euo pipefail

PROJ="$HOME/developer/LightOS-IV"
cd "$PROJ"

echo "== 1/6  JDK 21 (use IntelliJ's bundled runtime if present) =="
JBR="/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home"
if [ -d "$JBR" ]; then
  export JAVA_HOME="$JBR"
else
  echo "IntelliJ JBR not found; installing Temurin 21 via Homebrew."
  brew install --cask temurin@21
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
echo "JAVA_HOME=$JAVA_HOME"
"$JAVA_HOME/bin/java" -version

echo "== 2/6  Android SDK command-line tools (direct, no Homebrew) =="
export ANDROID_HOME="$HOME/Android/sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  # If this URL 404s, grab the current macOS link from
  # https://developer.android.com/studio#command-line-tools-only
  URL="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
  curl -L -o /tmp/cmdline-tools.zip "$URL"
  unzip -q -o /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses >/dev/null
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

echo "== 3/6  Gradle 8.11.1 (matches the wrapper; AGP 8.7.3 needs 8.9-8.11) =="
GR="$HOME/gradle-8.11.1"
if [ ! -d "$GR" ]; then
  curl -L -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.11.1-bin.zip
  unzip -q -o /tmp/gradle.zip -d "$HOME"
fi

echo "== 4/6  Point Gradle at the SDK =="
echo "sdk.dir=$ANDROID_HOME" > local.properties

echo "== 5/6  Generate the wrapper + build a debug APK =="
"$GR/bin/gradle" wrapper --gradle-version 8.11.1 --distribution-type bin
./gradlew :app:assembleDebug

echo "== 6/6  Copy the APK somewhere obvious =="
cp app/build/outputs/apk/debug/app-debug.apk "$PROJ/LightOS-IV-debug.apk"
echo
echo "DONE. APK: $PROJ/LightOS-IV-debug.apk"
ls -lh "$PROJ/LightOS-IV-debug.apk"
echo
echo "To install over USB (with USB debugging on):"
echo "  $ANDROID_HOME/platform-tools/adb install -r \"$PROJ/LightOS-IV-debug.apk\""
echo "Or copy LightOS-IV-debug.apk to your phone and tap it (enable Install unknown apps)."
