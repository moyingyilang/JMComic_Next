#!/bin/bash
# Windows ARM64 免装 Java 包（与 x64 同理：fat jar 缺 Skiko 原生库，必须补）
set -euo pipefail
OUT_DIR="${1:-dist-win}"; VERSION=$(grep -oE '[0-9]+\.[0-9]+\.[0-9]+' src/main/kotlin/com/jmcomic_next/desktop/Version.kt | tail -1)
W=$(mktemp -d); trap 'rm -rf "$W"' EXIT; STAGE="$W/stage"; mkdir -p "$STAGE"
echo "== 1/4 fatJar(arm64) + Skiko 原生库 =="
/opt/gradle-9.8.0/bin/gradle --console=plain --no-daemon fatJar -Ptarget=windows-arm64 >/dev/null
cp build/libs/jmcomic-next-windows-arm64.jar "$STAGE/jmcomic-next.jar"
curl -sL --max-time 300 -o "$W/skiko.jar" "https://repo1.maven.org/maven2/org/jetbrains/skiko/skiko-awt-runtime-windows-arm64/0.150.1/skiko-awt-runtime-windows-arm64-0.150.1.jar"
unzip -l "$W/skiko.jar" | grep -q "skiko-windows-arm64.dll" || { echo "  缺 skiko-windows-arm64.dll，拒绝出包"; exit 1; }
cp "$W/skiko.jar" "$STAGE/skiko-windows-arm64.jar"
echo "== 2/4 Temurin 21 Windows aarch64 =="
for base in "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jre/aarch64/windows" "https://mirror.nju.edu.cn/Adoptium/21/jre/aarch64/windows"; do
  curl -sL --max-time 600 -o "$W/jre.zip" "$base/OpenJDK21U-jre_aarch64_windows_hotspot_21.0.12.1_1.zip" || true
  [ -s "$W/jre.zip" ] && [ "$(stat -c %s "$W/jre.zip")" -gt 1000000 ] && break
done
unzip -q "$W/jre.zip" -d "$W/jre" && mv "$W"/jre/*/ "$STAGE/runtime"
[ -f "$STAGE/runtime/bin/java.exe" ] || { echo "  缺 runtime/bin/java.exe"; exit 1; }
echo "== 3/4 启动脚本 =="
printf '@echo off\r\nset JMCOMIC_RENDER=GL\r\ncd /d "%%~dp0"\r\nruntime\\bin\\java.exe -cp "jmcomic-next.jar;skiko-windows-arm64.jar" com.jmcomic_next.desktop.MainKt\r\npause\r\n' > "$STAGE/jmcomic-next.bat"
echo "== 4/4 打包 + 验收 =="
Z="$OUT_DIR/jmcomic-next-$VERSION-windows-arm64.zip"; rm -f "$Z"; (cd "$STAGE" && zip -qr "$Z" .)
for m in runtime/bin/java.exe skiko-windows-arm64.jar jmcomic-next.jar jmcomic-next.bat; do unzip -l "$Z" | grep -q "$m" || { echo "  验收失败：缺 $m"; exit 1; }; done
echo "  通过：$(stat -c %s "$Z") 字节"
