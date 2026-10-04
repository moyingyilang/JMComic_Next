#!/usr/bin/env bash
#
# 交叉打包：aarch64 主机 → x86_64 Linux。
#
# 桌面端是纯 JVM，没有自写原生代码，所以"交叉"只需换三样东西：
#   1. JVM 运行时换成 x86_64 的（jlink 用 x86_64 的 jmods 生成，见 /root/x64-runtime）
#   2. Skiko 原生库换成 libskiko-linux-x64.so（Arm 版那个 .so 要删掉）
#   3. 启动器从 jpackage 的 ELF 换成 shell 脚本（jpackage 不能跨平台生成启动器）
# 应用自己的 jar 是平台无关的，直接用 aarch64 构建出的那份。
#
# 用法：scripts/package-linux-x64.sh [输出目录]   默认 dist-x64
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=${1:-dist-x64}
APP=build/compose/binaries/main/app/jmcomic-next
X64_RUNTIME=${X64_RUNTIME:-/root/x64-runtime}
SKIKO_JAR=${SKIKO_JAR:-/root/skiko-win-none.jar}     # 名字起错了，内容是 linux-x64 的
[ -f "$SKIKO_JAR" ] || { echo "缺少 x86_64 的 Skiko jar"; exit 1; }
[ -d "$X64_RUNTIME" ] || { echo "缺少 x86_64 运行时：$X64_RUNTIME"; exit 1; }

echo "== 1/5 组装 x86_64 应用目录 =="
rm -rf "$OUT" && mkdir -p "$OUT/stage"
STAGE="$OUT/stage/jmcomic-next"
mkdir -p "$STAGE/bin" "$STAGE/lib"
cp -a "$APP/lib/app" "$STAGE/lib/app"
cp -a "$X64_RUNTIME" "$STAGE/lib/runtime"

# 换原生库：删掉 Arm 版，解出 x86_64 版放到与原来相同的位置
rm -f "$STAGE"/lib/app/libskiko-linux-arm64.so "$STAGE"/lib/app/libskiko-linux-arm64.so.sha256
( cd "$STAGE/lib/app" && unzip -o -q "$SKIKO_JAR" 'libskiko-linux-x64.so' 'libskiko-linux-x64.so.sha256' )

# 启动器：脚本代替 ELF
cat > "$STAGE/bin/jmcomic-next" <<'LAUNCH'
#!/bin/sh
# JMComic_Next 桌面版启动器（x86_64）
# jpackage 不能跨平台生成启动器，所以这里用脚本；行为与它的 ELF 等价：
# 用自带的运行时跑应用的 jar，并把 $APPDIR 传给 Skiko 找原生库。
DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec "$DIR/lib/runtime/bin/java" \
  -Dskiko.library.path="$DIR/lib/app" \
  -cp "$DIR/lib/app/*" \
  com.jmcomic_next.desktop.MainKt "$@"
LAUNCH
chmod 755 "$STAGE/bin/jmcomic-next"
cp $(cd "$(dirname "$0")/.." && pwd)/src/main/resources/icon.png "$STAGE/jmcomic-next.png" 2>/dev/null || true
cp $(cd "$(dirname "$0")/.." && pwd)/src/main/resources/icon.png "$OUT/jmcomic-next.png" 2>/dev/null || true

echo "== 2/5 架构自检（不通过就拒绝出包）=="
file "$STAGE/lib/runtime/bin/java" | grep -q "x86-64" || { echo "  失败：运行时不是 x86-64"; exit 1; }
[ -f "$STAGE/lib/app/libskiko-linux-x64.so" ] || { echo "  失败：缺 libskiko-linux-x64.so"; exit 1; }
[ ! -f "$STAGE/lib/app/libskiko-linux-arm64.so" ] || { echo "  失败：Arm 的 .so 没删干净"; exit 1; }
echo "  运行时 x86-64、Skiko x86_64、Arm 原生库已清"

echo "== 3/5 便携包 =="
tar czf "$OUT/jmcomic-next-1.9.142-linux-x86_64-portable.tar.gz" -C "$STAGE" .

echo "== 4/5 deb(amd64) 与 rpm(x86_64) =="
D=$(mktemp -d)/jmcomic-next
mkdir -p "$D/DEBIAN" "$D/opt/jmcomic-next" "$D/usr/bin" "$D/usr/share/icons/hicolor/256x256/apps"
cp -a "$STAGE/." "$D/opt/jmcomic-next/"
printf '#!/bin/sh\nexec /opt/jmcomic-next/bin/jmcomic-next "$@"\n' > "$D/usr/bin/jmcomic-next"
chmod 755 "$D/usr/bin/jmcomic-next"
cp "$OUT/jmcomic-next.png" "$D/usr/share/icons/hicolor/256x256/apps/jmcomic-next.png" 2>/dev/null || true
cat > "$D/DEBIAN/control" <<'CTL'
Package: jmcomic-next
Version: 1.9.142
Architecture: amd64
Maintainer: moyingyilang
Depends: libc6
Section: utils
Priority: optional
Description: JMComic_Next desktop client (x86_64)
 A third-party JMComic client built with Kotlin and Compose Desktop.
CTL
dpkg-deb --build --root-owner-group "$D" "$OUT/jmcomic-next_1.9.142_amd64.deb" >/dev/null

TOP=$(mktemp -d)
mkdir -p "$TOP"/{BUILD,RPMS,SOURCES,SPECS,SRPMS}
cp -a "$STAGE" "$TOP/SOURCES/jmcomic-next-app"
cat > "$TOP/SPECS/jmcomic-next.spec" <<'SPEC'
Name:           jmcomic-next
Version:        1.9.142
Release:        1
Summary:        JMComic_Next desktop client (x86_64)
License:        AGPL-3.0-only
BuildArch:      x86_64
Requires:       glibc
%description
A third-party JMComic client built with Kotlin and Compose Desktop.
%prep
%build
%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/opt/jmcomic-next %{buildroot}/usr/bin
cp -a %{_sourcedir}/jmcomic-next-app/. %{buildroot}/opt/jmcomic-next/
printf '#!/bin/sh\nexec /opt/jmcomic-next/bin/jmcomic-next "$@"\n' > %{buildroot}/usr/bin/jmcomic-next
chmod 755 %{buildroot}/usr/bin/jmcomic-next
%files
/opt/jmcomic-next
/usr/bin/jmcomic-next
SPEC
# rpm：本机 rpmbuild（aarch64）会以 "No compatible architectures found for build"
# 拒绝跨架构构建（试过八种 define 组合均无效）。绕法是**让 rpmbuild 自己就是 x86_64**：
# 装 rpm:amd64（多架构），再用 qemu 运行它 —— 它看到的宿主就是 x86_64，
# 于是这条构建路径与在真机上完全一致，而不是伪造架构标记。
RPM_OK=0
if rpmbuild -bb --define "_topdir $TOP" --define "_target_cpu x86_64" "$TOP/SPECS/jmcomic-next.spec" 2>/dev/null; then
  RPM_OK=1
elif [ -x /usr/bin/qemu-x86_64-static ] && [ -x /usr/bin/rpmbuild ]; then
  echo "  本机 rpmbuild 拒绝跨架构，改用 qemu 运行 x86_64 的 rpmbuild"
  if /usr/bin/qemu-x86_64-static /usr/bin/rpmbuild -bb --define "_topdir $TOP" \
       "$TOP/SPECS/jmcomic-next.spec" 2>&1 | tail -2; then
    RPM_OK=1
  fi
fi
if [ "$RPM_OK" = 1 ]; then
  find "$TOP/RPMS" -name "*.rpm" -exec cp {} "$OUT/" \;
  echo "  rpm 成功"
else
  echo "  rpm 失败：需在 x86_64 环境构建（已记档，不影响其它三类产物）"
fi

echo "== 5/5 AppImage(x86_64) =="
if [ -f /root/runtime-x86_64 ]; then
  AD=$(mktemp -d)/AppDir
  mkdir -p "$AD/usr"
  cp -a "$STAGE/." "$AD/usr/"
  printf '#!/bin/sh\nexec "$(dirname "$0")/usr/bin/jmcomic-next" "$@"\n' > "$AD/AppRun"
  chmod 755 "$AD/AppRun"
  cp "$OUT/jmcomic-next.png" "$AD/jmcomic-next.png" 2>/dev/null || true
  printf '[Desktop Entry]\nType=Application\nName=JMComic_Next\nExec=jmcomic-next\nIcon=jmcomic-next\nTerminal=false\nCategories=Utility;Graphics;\n' > "$AD/jmcomic-next.desktop"
  SQ=$(mktemp -u).squashfs
  mksquashfs "$AD" "$SQ" -root-owned -noappend -comp gzip -quiet
  cat /root/runtime-x86_64 "$SQ" > "$OUT/jmcomic-next-1.9.142-x86_64.AppImage"
  chmod 755 "$OUT/jmcomic-next-1.9.142-x86_64.AppImage"
  rm -f "$SQ"
else
  echo "  跳过：缺少 x86_64 的 AppImage runtime"
fi
rm -rf "$OUT/stage"
echo; echo "== 成品 =="; ls -lh "$OUT" | tail -6
