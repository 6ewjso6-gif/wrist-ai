#!/system/bin/sh
set -e
JAVA_HOME=/data/user/0/app.dsh.mobile/files/engine/extensions/android-buildtools/lib/jvm/java-21-openjdk
AJ=/data/user/0/app.dsh.mobile/cache/android/plat/android-11/android.jar
BT=/data/user/0/app.dsh.mobile/cache/android/bt/android-14
AAPT=/data/user/0/app.dsh.mobile/files/engine/extensions/android-buildtools/bin/aapt
ZIPALIGN=/data/user/0/app.dsh.mobile/files/engine/extensions/android-buildtools/bin/zipalign
export ANDROID_DATA=/data ANDROID_ROOT=/system ANDROID_ASSETS=/system/app ANDROID_STORAGE=/storage

echo "[1/5] javac"
rm -rf out && mkdir -p out
"$JAVA_HOME/bin/javac" -source 8 -target 8 -nowarn -encoding UTF-8 \
  -bootclasspath "$AJ" -classpath libs/xms-wearable.jar \
  -d out $(find src -name "*.java") > ./javac.log 2>&1
if [ $? -ne 0 ]; then echo "❌ javac 失败"; grep -E "error:" ./javac.log | head -8; exit 1; fi

echo "[2/5] d8"
rm -f classes.dex
"$JAVA_HOME/bin/java" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
  --min-api 21 --lib "$AJ" --output . \
  $(find out -name "*.class") libs/xms-wearable.jar 2>&1 | head -4
[ -f classes.dex ] || { echo "d8 失败"; exit 1; }
echo "      classes.dex $(stat -c%s classes.dex) bytes"

echo "[3/5] aapt"
rm -f unsigned.apk aligned.apk wrist-ai-config.apk
"$AAPT" package -f -M AndroidManifest.xml -S res -I "$AJ" -F unsigned.apk 2>&1 | grep -vE "ResourceType" || true
"$AAPT" add unsigned.apk classes.dex 2>&1 | grep -vE "ResourceType" || true

echo "[4/5] zipalign"
"$ZIPALIGN" -f 4 unsigned.apk aligned.apk

echo "[5/5] apksigner"
"$JAVA_HOME/bin/java" -jar "$BT/lib/apksigner.jar" sign \
  --ks wristai.p12 --ks-pass pass:wristai2026 --ks-key-alias wristai --key-pass pass:wristai2026 \
  --min-sdk-version 21 --v1-signing-enabled true --v2-signing-enabled true \
  --out wrist-ai-config.apk aligned.apk
echo "✅ wrist-ai-config.apk $(stat -c%s wrist-ai-config.apk) bytes"
