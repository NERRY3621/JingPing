#!/data/data/com.dsharnessmobile.shell/files/usr/bin/bash
# 净屏 · 手工构建脚本（无需 Gradle）
# 依赖：aapt(v1) / d8.jar / apksigner.jar / zipalign / OpenJDK 17 / android.jar(API 34)
set -euo pipefail

BASE="$(cd "$(dirname "$0")" && pwd)"
BUILD="$BASE/build"
SDK_JAR="${SDK_JAR:-/data/data/com.dsharnessmobile.shell/files/home/toolchain/sdk/android.jar}"
JDK="${JAVA_HOME:-/data/data/com.dsharnessmobile.shell/files/usr/lib/jvm/java-17-openjdk}"
JAVAC="$JDK/bin/javac"
JAVA="$JDK/bin/java"
KEYTOOL="$JDK/bin/keytool"
D8_JAR="${D8_JAR:-/data/data/com.dsharnessmobile.shell/files/usr/share/java/d8.jar}"
APKSIGNER_JAR="${APKSIGNER_JAR:-/data/data/com.dsharnessmobile.shell/files/usr/share/java/apksigner.jar}"
AAPT="${AAPT:-/data/data/com.dsharnessmobile.shell/files/usr/bin/aapt}"
ZIPALIGN="${ZIPALIGN:-/data/data/com.dsharnessmobile.shell/files/usr/bin/zipalign}"
VERSION_NAME="1.3.0"
OUT="$BASE/dist/JingPing-$VERSION_NAME.apk"

for f in "$SDK_JAR" "$D8_JAR" "$APKSIGNER_JAR" "$AAPT" "$ZIPALIGN" "$JAVAC"; do
    if [ ! -e "$f" ]; then
        echo "缺少构建文件：$f" >&2
        exit 1
    fi
done

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/stub-classes" "$BUILD/dex" "$BASE/dist"

echo "[1/7] 生成 R.java"
"$AAPT" package -f -m \
    -J "$BUILD/gen" \
    -M "$BASE/AndroidManifest.xml" \
    -S "$BASE/res" \
    -I "$SDK_JAR"

echo "[2/7] 编译 Xposed API 桩"
mapfile -t STUB_SRC < <(find "$BASE/stubs" -name '*.java')
"$JAVAC" --release 8 -encoding UTF-8 \
    -cp "$SDK_JAR" \
    -d "$BUILD/stub-classes" \
    "${STUB_SRC[@]}"

echo "[3/7] 编译应用与模块"
CP="$SDK_JAR:$BUILD/stub-classes"
for j in "$BASE"/libs/*.jar; do
    CP="$CP:$j"
done
mapfile -t SRC < <(find "$BASE/src" -name '*.java')
"$JAVAC" --release 8 -encoding UTF-8 \
    -cp "$CP" \
    -d "$BUILD/classes" \
    "${SRC[@]}" \
    "$BUILD/gen/com/jingping/adshield/R.java"

echo "[4/7] 打包资源 / assets"
"$AAPT" package -f \
    -M "$BASE/AndroidManifest.xml" \
    -S "$BASE/res" \
    -A "$BASE/assets" \
    -I "$SDK_JAR" \
    -F "$BUILD/unsigned.apk"

echo "[5/7] dex"
mapfile -t CLASSES < <(find "$BUILD/classes" -name '*.class')
"$JAVA" -cp "$D8_JAR" com.android.tools.r8.D8 \
    --min-api 26 \
    --lib "$SDK_JAR" \
    --output "$BUILD/dex" \
    "${CLASSES[@]}" \
    "$BASE"/libs/*.jar

# classes.dex / classes2.dex 必须以根目录名放入 APK
for f in "$BUILD"/dex/*.dex; do
    ( cd "$BUILD/dex" && "$AAPT" add "$BUILD/unsigned.apk" "$(basename "$f")" )
done

echo "[6/7] zipalign"
"$ZIPALIGN" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

echo "[7/7] 签名"
if [ ! -f "$BASE/release.jks" ]; then
    "$KEYTOOL" -genkeypair -noprompt \
        -keystore "$BASE/release.jks" \
        -alias jingping \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass jingping123 -keypass jingping123 \
        -dname "CN=JingPing, OU=DSH, O=LocalBuild, L=Local, C=CN" >/dev/null 2>&1
fi
"$JAVA" -jar "$APKSIGNER_JAR" sign \
    --ks "$BASE/release.jks" \
    --ks-key-alias jingping \
    --ks-pass pass:jingping123 \
    --key-pass pass:jingping123 \
    --min-sdk-version 26 \
    --out "$OUT" \
    "$BUILD/aligned.apk"

"$JAVA" -jar "$APKSIGNER_JAR" verify --print-certs "$OUT"
ls -lh "$OUT"
echo "完成：$OUT"
