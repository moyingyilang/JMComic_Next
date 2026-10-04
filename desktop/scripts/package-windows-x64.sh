#!/bin/bash
# Windows x64 免装 Java 的 ZIP。
#
# 为什么要单独写：`gradle fatJar` 打出的 jar **不含 Skiko 的原生库**（.dll），
# 而 Compose 桌面端必须有它，否则启动即 java.lang.ExceptionInInitializerError
# （报错位置 androidx.compose.ui.scene.skia.SurfaceSkiaLayerComponent.<init>）。
# 群友实测：只放 fat jar 会闪退；补上 skiko-windows-x64.jar 后正常启动（2026-10-04 确认）。
#
# 用法：在容器内运行 scripts/package-windows-x64.sh [输出目录]
set -euo pipefail
OUT_DIR="${1:-dist-win64}"
VERSION=$(grep -oE '[0-9]+\.[0-9]+\.[0-9]+' src/main/kotlin/com/jmnext/desktop/Version.kt | tail -1)
W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
STAGE="$W/stage"; mkdir -p "$STAGE"

echo "== 1/5 胖 jar（windows-x64）=="
/opt/gradle-9.8.0/bin/gradle --console=plain --no-daemon fatJar -Ptarget=windows-x64 >/dev/null
JAR="build/libs/jmnext-windows-x64.jar"
[ -f "$JAR" ] || { echo "  找不到 $JAR"; exit 1; }
cp "$JAR" "$STAGE/jmnext.jar"

echo "== 2/5 Skiko Windows x64 原生库（关键，缺了会 ExceptionInInitializerError）=="
SK="$W/skiko.jar"
curl -sL --max-time 300 -o "$SK" \
  "https://repo1.maven.org/maven2/org/jetbrains/skiko/skiko-awt-runtime-windows-x64/0.150.1/skiko-awt-runtime-windows-x64-0.150.1.jar"
unzip -l "$SK" | grep -q "skiko-windows-x64.dll" || { echo "  该 jar 里没有 skiko-windows-x64.dll，拒绝出包"; exit 1; }
cp "$SK" "$STAGE/skiko-windows-x64.jar"

echo "== 3/5 Temurin 21 Windows x64 运行时（国内镜像优先）=="
JREZ="$W/jre.zip"
for base in \
  "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jre/x64/windows" \
  "https://mirror.nju.edu.cn/Adoptium/21/jre/x64/windows" \
  "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1" ; do
  curl -sL --max-time 600 -o "$JREZ" "$base/OpenJDK21U-jre_x64_windows_hotspot_21.0.12.1_1.zip" || true
  [ -s "$JREZ" ] && [ "$(stat -c %s "$JREZ")" -gt 1000000 ] && break
done
[ -s "$JREZ" ] || { echo "  运行时下载失败"; exit 1; }
unzip -q "$JREZ" -d "$W/jre" && mv "$W"/jre/*/ "$STAGE/runtime"
[ -f "$STAGE/runtime/bin/java.exe" ] || { echo "  runtime/bin/java.exe 不在，拒绝出包"; exit 1; }

echo "== 4/5 启动脚本（英文，避免 cmd 代码页乱码）=="
printf '@echo off\r\nset JMCOMIC_RENDER=GL\r\ncd /d "%%~dp0"\r\necho === JMNeXt starting ===\r\nruntime\\bin\\java.exe -version\r\necho === running ===\r\nruntime\\bin\\java.exe -cp "jmnext.jar;skiko-windows-x64.jar" com.jmnext.desktop.MainKt\r\necho === exited with code %%ERRORLEVEL%% ===\r\npause\r\n' > "$STAGE/jmnext.bat"
printf '@echo off\r\ncd /d "%%~dp0"\r\nset OUT=diag.txt\r\necho ==== JMNeXt diagnostics ==== > "%%OUT%%"\r\nsysteminfo | findstr /B /C:"OS Name" /C:"OS Version" /C:"System Type" >> "%%OUT%%" 2>&1\r\nruntime\\bin\\java.exe -version >> "%%OUT%%" 2>&1\r\necho ---- run app ---- >> "%%OUT%%"\r\nruntime\\bin\\java.exe -cp "jmnext.jar;skiko-windows-x64.jar" com.jmnext.desktop.MainKt >> "%%OUT%%" 2>&1\r\necho ---- exit code %%ERRORLEVEL%% ---- >> "%%OUT%%"\r\ntype "%%OUT%%"\r\npause\r\n' > "$STAGE/diag.bat"

echo "== 5/5 打包 + 验收 =="
mkdir -p "$OUT_DIR"
ABS="$(cd "$OUT_DIR" && pwd)/jmnext-$VERSION-windows-x64.zip"
Z="$OUT_DIR/jmnext-$VERSION-windows-x64.zip"
rm -f "$Z"; (cd "$STAGE" && zip -qr "$(cd "$OLDPWD" && pwd)/$Z" . 2>/dev/null) || (cd "$STAGE" && zip -qr "$ABS" .)
for must in runtime/bin/java.exe skiko-windows-x64.jar jmnext.jar jmnext.bat diag.bat; do
  unzip -l "$Z" > "$W/list.txt" 2>/dev/null; grep -q "$must" "$W/list.txt" || { echo "  验收失败：缺 $must"; exit 1; }
done
echo "  通过：$(stat -c %s "$Z") 字节 -> $Z"
