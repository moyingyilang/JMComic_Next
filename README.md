# JMComic_Next

用 Kotlin 与 Compose 重写的 JMComic 客户端，分 **Android** 与 **桌面（Linux）** 两端，
共用同一份跨平台数据层。视觉系统移植自 [moyingyilang.github.io](https://moyingyilang.github.io)
的 **Fluent（Windows 11 Acrylic / Mica）× MIUI 毛玻璃** 设计语言。

原则沿用了原来的那句话 ——「Stability and simplicity are paramount」：
**能少不写多，能显式不隐式。**

> 这是一个**只读的阅读客户端**：能浏览、搜索、阅读、登录、收藏、看评论；
> 不做发帖、投票、购买，也不包含官方的任何广告或埋点。

---

## 目录

- [下载](#下载)
- [平台与架构支持](#平台与架构支持)
- [桌面端](#桌面端)
  - [运行](#运行)
  - [自己构建](#自己构建)
  - [跨架构是怎么做的](#跨架构是怎么做的)
  - [已知限制](#桌面端的已知限制)
- [Android 端](#android-端)
- [界面风格](#界面风格)
- [工程纪律](#工程纪律)
- [许可](#许可)

---

## 下载

发布页在 [Releases](https://github.com/moyingyilang/JMComic_Next/releases)。

| 平台 | 架构 | 格式 | 状态 |
| --- | --- | --- | --- |
| Android | arm64-v8a / armeabi-v7a | APK（full / lite 两种变体） | 稳定，已发布至 1.8.2 |
| Linux 桌面 | aarch64 (arm64) | 便携 tar.gz / deb / rpm / AppImage | 可用 |
| Linux 桌面 | x86_64 (amd64) | 便携 tar.gz / deb / AppImage | 可用（**未经真实 x86_64 机器测试**） |
| Windows 桌面 | arm64 (ARM64) | 免装 Java 的便携 ZIP | 产物已产出（**真机未验证**） |
| Linux 桌面 | armv7a (arm32) | — | **上游不支持**，见下 |
| Linux 桌面 | i686 (x86-32) | — | **上游不支持**，见下 |

> `1.9.00x` 至 `1.9.xxx` 这一串版本号是**跨架构支持测试**专用的预构建序列，
> 不含 Android 端的任何改动；两个平台的正式版本仍按各自的主线推进。

## 平台与架构支持

桌面端建立在 Compose Desktop 之上，渲染层是 **Skiko**。Skiko 只发布 64 位 Linux 原生库，
所以桌面端**只能覆盖 arm64 与 x86_64**：

| 目标 | Skiko 原生库 | 32 位 JDK（Temurin 21） | 结论 |
| --- | --- | --- | --- |
| linux-x64 | 有 | 有 | 可做 |
| linux-arm64 | 有 | 有 | 可做 |
| linux-arm（armv7a） | **无（404）** | **无** | 做不了 |
| linux-x86 / ia32（i686） | **无（404）** | **无** | 做不了 |

这不是打包问题，而是**上游依赖缺失**：要支持 32 位，得自己从源码编 Skiko 的 arm32/x86-32
原生层，属于另一个量级的工程。所以桌面端现实可行的是 **arm64 与 x86_64** 两条线。

---

## 桌面端

形态上有意与手机端不同：**左侧常驻导航**（桌面上比底部标签栏更合适）、
**阅读页纵向连续滚动**（鼠标滚轮纵向翻比左右翻自然）、**顶栏常驻账号状态**。

### 运行

便携包解压后：

```bash
./bin/jmcomic-next
```

deb 与 rpm 安装后（rpm 见[已知限制](#桌面端的已知限制)）：

```bash
sudo dpkg -i jmcomic-next_1.9.003_arm64.deb     # 或 _amd64.deb
jmcomic-next
```

AppImage：

```bash
chmod +x jmcomic-next-1.9.003-aarch64.AppImage
./jmcomic-next-1.9.003-aarch64.AppImage
```

若系统缺 `libfuse.so.2`，AppImage 可用官方提供的方式直接运行（不挂载）：

```bash
./jmcomic-next-1.9.003-aarch64.AppImage --appimage-extract-and-run
```

运行时需要图形环境（X11/Wayland）与 OpenGL；纯无头环境会在创建窗口时失败，
这是 Compose Desktop 的既有行为，不是本项目的缺陷。

### 自己构建

桌面端是**独立的 Gradle 构建**（`desktop/` 有自己的 `settings.gradle.kts`），
它按路径直接编译 `:shared` 的源码，**不依赖 Android 的 SDK**，也顺带证明了数据层与 Android 无关。

```bash
cd desktop
gradle createDistributable     # 产出可运行镜像
gradle run                     # 直接跑（需要图形环境）
gradle smoke                   # 无界面冒烟：登录 + 拉列表，凭据取 JM_USER / JM_PASS
```

打包（必须在目标平台或已配好交叉环境的机器上执行）：

```bash
scripts/package-linux.sh      dist      # 本机架构：便携 tar.gz / deb / rpm / AppImage
scripts/package-linux-x64.sh  dist-x64  # 交叉产出 x86_64（见下）
```

两个脚本都会先做**产物自检**，任一项不过就拒绝出包：应用 jar 只能有一个
（改代码后 Compose 会生成新的哈希文件名，旧 jar 残留会让旧类抢先加载）、
启动类必须是全限定名、以及按字节 grep 核对新旧标记
（`strings` 默认只输出 ASCII，**用它查中文等于没查**）。

### 跨架构是怎么做的

桌面端是纯 JVM、没有自写原生代码，所以"交叉"不需要交叉编译器，只需换三样东西：

1. **运行时** —— 取目标架构的 JDK，用它的 jmods 做 jlink。
   `jlink` 由本机 JDK 提供也能生成目标架构的运行时（`bin/java` 的 ELF 头会变成目标架构）。
   注意 `--strip-debug` 会调用 `objcopy`，在异构主机上可能认不出目标格式而报错，
   后果只是调试符号没剥掉，不影响正确性。
2. **Skiko 原生库** —— 换成目标架构的
   `org.jetbrains.skiko:skiko-awt-runtime-linux-<arch>`，并**删掉**原来那份
   （两份并存会加载错的那一份）。
3. **启动器** —— jpackage 不能跨平台生成启动器，Linux 上改用 shell 脚本，
   行为等价：用自带运行时 + `-cp "lib/app/*"` 启动主类，
   并用 `-Dskiko.library.path` 指向原生库所在目录。

`dpkg-deb` 只需写对 `Architecture:` 字段；**rpm 则不行**，原因见下。

### 1.9.x 预构建线（与主板本的关系）

1.9.001 与 1.9.003 是**桌面端的预构建**，用于跨架构打包与界面移植的验证，
**不含主项目（Android 端）的任何更新**。桌面端的功能线仍在 2.0.0。

版本策略：1.9.003、1.9.003 这样正常递增（不用三位数小版本）；积累到一批修复就可以发一次预构建。
每次发布的附件见 Releases 页面。

发布包的验证边界（每次发布都会在 Release 说明里重复）：界面观感、以及所有**写操作**
（发表/回复/删除评论、删除历史、取消收藏、追更切换、收藏夹增删改）**均未经验证** ——
开发环境无法渲染 Compose；写操作使用的是用户账号，开发期间没有点过。

### 桌面端的已知限制

- **x86_64 的 rpm 出不来。** rpm 4.18 直接拒绝跨架构构建
  （`No compatible architectures found for build`，试过八种 define 组合全部失败）。
  改成用 qemu 运行 `rpm:amd64` 后，最简 spec 能构建，但真实 spec 的 `%install` 会失败 ——
  rpm 每个脚本段都要 `exec /bin/sh`，而构建环境里 **binfmt_misc 不可用**，
  qemu 只能转译被直接指定的那一个二进制，子进程不会跟着转译。
  需要一台真正的 x86_64 机器（或允许 binfmt_misc 的容器）才能补上这个格式。
- **x86_64 产物未在真实机器上运行过。** 目前只在 qemu 用户态模拟下验证到
  "创建窗口"这一步（55 个 jar 加载成功、Compose 初始化、随后因无头环境抛
  `HeadlessException`）。
- **界面尚无可视化确认。** 开发容器无法渲染 Compose，界面需要人工过目。
- 空壳页与未做页：标签、画师与作品库（空壳），评论（未做）。

---

## Android 端

- **两种变体**：`full` 与 `lite`（精简掉部分依赖与功能），各自可独立安装。
- **五套界面风格**可切换：WindowGlass / Translucent / FlatBlur / Miuix / Material。
  换的不只是配色 —— 圆角尺度、表面工艺、字重与按压手感一起换；背景可选内置渐变或壁纸。
- **主要内容页**：首页（分区 + 最新）、分区更多、周刊、随机本子、标签、分类、搜索、
  收藏、历史、追更、通知、评论、画师与作品库、屏蔽设置、关于、我的。
- **本地屏蔽**：关键词 / 标签 / 分类三类名单，命中即从所有列表隐藏，
  并在页面顶部提示"被挡掉多少条"，不静默少几条。

---

## 界面风格

桌面端直接采用 [moyingyilang.github.io](https://moyingyilang.github.io) 的设计令牌
（取自该站 `src/styles/global.css`），所以两端观感统一：

| 令牌 | 浅色 | 深色 |
| --- | --- | --- |
| 强调色 | `#0f6cbd` | `#60cdff` |
| 正文 | `#16181d` | `#f3f4f7` |
| 次级文字 | `#4a4f5a` | 白 72% |
| 三级文字 | `#767c88` | 白 50% |
| 描边 | `rgba(15,23,42,0.09)` | 白 10% |
| 圆角 | 8 / 12 / 18 px | 同左 |

深色不是浅色的反相，而是另一套值（深底上强调色要更亮才看得清）。桌面端跟随系统深浅色。

## 工程纪律

每个版本遵循同一条纪律，写在 [CHANGELOG](CHANGELOG.md) 里：

- 改动能说清「为什么慢 / 省了什么」；
- **已验证**与**未验证**分开写；
- **已知未修**的明确记档，不含糊带过；
- 构建产物（APK / deb / rpm / AppImage）不进源码仓库，发版时作为 Release 附件。

开发过程中踩过的坑与判断依据记录在 [docs/engineering-notes.md](docs/engineering-notes.md)，
桌面端的功能对齐清单在 [desktop/PARITY.md](desktop/PARITY.md)，
跨架构打包的实测前提在 [desktop/CROSS.md](desktop/CROSS.md)。

## 许可

本项目以 **AGPL-3.0-only** 授权。禁漫、JMComic 及相关内容的权利归其权利人所有；
本应用与官方无关，仅提供客户端实现。
