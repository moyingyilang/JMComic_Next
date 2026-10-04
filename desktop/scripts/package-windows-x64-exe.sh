#!/bin/bash
# Windows x64 **单体 exe**（便携、免安装、双击即运行）。
#
# 为什么要它：此前的交付物是一个 ZIP（里面是 jmnext.bat + runtime/ + jar），第一次拿到的人
# 不知道该怎么用。单体 exe 的目标是"双击就行"。
#
# 做法：用 NSIS 把 stage（应用 jar + Skiko 原生库 + 免装 JRE）打进一个静默自解压 exe，
# 解压到 %LOCALAPPDATA%\JMNeXt 后直接启动（javaw，不弹控制台）。首次运行解压、之后直接启动。
#
# 用法：scripts/package-windows-x64-exe.sh [输出目录]      默认 dist-win64
set -euo pipefail
OUT_DIR="${1:-dist-win64}"
VERSION=$(grep -oE '[0-9]+\.[0-9]+\.[0-9]+' src/main/kotlin/com/jmnext/desktop/Version.kt | tail -1)
W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
STAGE="$W/stage"; mkdir -p "$STAGE"

echo "== 1/4 胖 jar（windows-x64）=="
/opt/gradle-9.8.0/bin/gradle --console=plain --no-daemon fatJar -Ptarget=windows-x64 >/dev/null
JAR="build/libs/jmnext-windows-x64.jar"
[ -f "$JAR" ] || { echo "  找不到 $JAR"; exit 1; }
cp "$JAR" "$STAGE/jmnext.jar"

echo "== 2/4 Skiko Windows x64 原生库（缺了会 ExceptionInInitializerError）=="
SK="$W/skiko.jar"
curl -sL --max-time 300 -o "$SK" \
  "https://repo1.maven.org/maven2/org/jetbrains/skiko/skiko-awt-runtime-windows-x64/0.150.1/skiko-awt-runtime-windows-x64-0.150.1.jar"
unzip -l "$SK" | grep -q "skiko-windows-x64.dll" || { echo "  该 jar 里没有 skiko-windows-x64.dll，拒绝出包"; exit 1; }
cp "$SK" "$STAGE/skiko-windows-x64.jar"

echo "== 3/4 Temurin 21 Windows x64 运行时（优先用缓存 $HOME/.cache/win-jre-x64.zip）=="
JREZ="$W/jre.zip"; CACHE="${JRE_CACHE:-$HOME/.cache/win-jre-x64.zip}"
if [ -s "$CACHE" ]; then cp "$CACHE" "$JREZ"; echo "  用缓存：$(stat -c %s "$JREZ") 字节"
else
  for base in \
    "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jre/x64/windows" \
    "https://mirror.nju.edu.cn/Adoptium/21/jre/x64/windows" \
    "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1" ; do
    curl -sL --max-time 600 -o "$JREZ" "$base/OpenJDK21U-jre_x64_windows_hotspot_21.0.12.1_1.zip" || true
    [ -s "$JREZ" ] && [ "$(stat -c %s "$JREZ")" -gt 1000000 ] && break
  done
  [ -s "$JREZ" ] || { echo "  运行时下载失败"; exit 1; }
  mkdir -p "$(dirname "$CACHE")"; cp "$JREZ" "$CACHE"; echo "  已缓存到 $CACHE"
fi
unzip -q "$JREZ" -d "$W/jre" && mv "$W"/jre/*/ "$STAGE/runtime"
[ -f "$STAGE/runtime/bin/java.exe" ] || { echo "  runtime/bin/java.exe 不在，拒绝出包"; exit 1; }

echo "== 4/4 NSIS 打成单体 exe =="
OUT_EXE="$VERSION-$$.exe"
cat > "$W/app.nsi" <<NSI
Unicode true
Name "JMNeXt"
OutFile "$OUT_EXE"
SilentInstall silent
RequestExecutionLevel user
AutoCloseWindow true
Section
  ; 解压到固定目录：首次稍慢，之后直接启动
  SetOutPath "\$LOCALAPPDATA\\JMNeXt"
  File /r "stage\\*"
  ; 与 zip 版一致：Windows 上默认用 GL 渲染，绕开部分环境的 OpenGL 上下文问题
  System::Call 'kernel32::SetEnvironmentVariable(t "JMCOMIC_RENDER", t "GL")'
  ; javaw：不弹控制台窗口
  Exec '"\\\$LOCALAPPDATA\\JMNeXt\\runtime\\bin\\javaw.exe" -cp "\\\$LOCALAPPDATA\\JMNeXt\\jmnext.jar;\\\$LOCALAPPDATA\\JMNeXt\\skiko-windows-x64.jar" com.jmnext.desktop.MainKt'
SectionEnd
NSI
( cd "$W" && makensis -V2 "app.nsi" >/dev/null )
mkdir -p "$OUT_DIR"
cp "$W/$OUT_EXE" "$OUT_DIR/Windows-x64-$VERSION.exe"
EXE="$OUT_DIR/Windows-x64-$VERSION.exe"

echo "== 验收 =="
printf "  %s  %s 字节\n" "$(basename "$EXE")" "$(stat -c %s "$EXE")"
[ "$(head -c2 "$EXE")" = "MZ" ] && echo "  PE 头：MZ（Windows 可执行）" || { echo "  不是 PE 文件，拒绝出包"; exit 1; }
for m in runtime/bin/java.exe skiko-windows-x64.jar jmnext.jar; do
  if 7z l "$EXE" 2>/dev/null | grep -q "$(echo "$m" | tr '/' '\\')"; then echo "  内含 $m"; else echo "  包内缺 $m"; exit 1; fi
done
echo "单体 exe 完成：$EXE"
