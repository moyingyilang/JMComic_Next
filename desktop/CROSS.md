# 交叉打包方案：aarch64 主机 → x86_64 Linux（2.0.0）

## 为什么这里叫"打包"而不是"编译"

桌面端是**纯 JVM，没有自己写的原生代码**。所谓反向交叉，实际只需换三样东西再打包：

1. **JVM 运行时**：取 x86_64 的 JDK，用它的 jmods 做 jlink（jlink 不关心目标架构，
   给 Windows ARM 做运行时就是同一手法，已验证可行）。
2. **Skiko 原生库**：换成 `org.jetbrains.skiko:skiko-awt-runtime-linux-x64`。
   Compose 的 Gradle 插件为此提供显式平台限定符（`compose.desktop.linux_x64`），
   不能用 `currentOs` —— 后者解析的是**宿主**平台。
3. **启动器**：jpackage 不能跨平台生成启动器，Linux 上改用 shell 脚本
   （`exec java -cp ... com.jmnext.desktop.MainKt`）完全够用。

`dpkg-deb` 与 `rpmbuild` 只是打包文件、不做编译，指定目标架构即可
（`Architecture: amd64` / `--target x86_64`）。

## 四项前提已实测（2026-10-04）

| 检查 | 结果 |
| --- | --- |
| x86_64 JDK（清华 Adoptium 镜像） | 有：OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz |
| x86_64 Skiko（Maven Central） | 有：skiko-awt-runtime-linux-x64:0.150.1 返回 200 |
| x86_64 AppImage runtime（GitHub 代理） | 有：193728 字节 |
| 容器内 qemu-user-static | 可装（1:7.2+dfsg-7+deb12u18+b3） |

## 产出与验证

四类产物：便携 tar.gz、deb(amd64)、rpm(x86_64)、AppImage(x86_64)。

**验证分两级，必须如实标注**：
- 结构验证（一定能做）：包内运行时确实是 x86_64（读 ELF 头 Machine 字段）、
  Skiko 是 x86_64 原生库、启动脚本路径正确、deb/rpm 元数据结构正确。
- 运行验证（取决于 qemu）：装 qemu-user-static 并注册 binfmt 后在容器里跑。
  若 Android 内核不允许 binfmt_misc，则**只能做到结构验证**，
  发布说明里要写明"未在真实 x86_64 上运行过"。

## 同一套手法可复用到

Windows x64（换 windows-x64 运行时与 Skiko，启动器用 .cmd）、
以及 Phase 1 已备好但未组装的 **Windows on ARM**。
