# JMNeXt

用 Kotlin 与 Compose 写成的 JMComic 客户端。**Android 是主项目**，桌面端（Linux / Windows）由
Android 端移植而来，两端共用同一个 `shared` 数据层。这是一个**只读的阅读客户端**：能浏览、搜索、
阅读、登录、收藏、追更、看评论；不做发帖、投票、购买，不含官方广告或任何埋点。

原则沿用原来那句话 ——「Stability and simplicity are paramount」：**能少不写多，能显式不隐式。**

[下载](#下载) · [快速开始](#快速开始) · [功能](#功能) · [现状](#现状这里说的话都算数) ·
[自己构建](#自己构建) · [路线图](#路线图)

> 本项目是对 JMComic 客户端的**第三方重新实现**，出发点是对客户端本身的技术研究：跨平台（Compose
> Multiplatform）、数据层在两端共用、以及性能与体验上的工程实践。与官方无关；禁漫、JMComic
> 及相关内容的权利归其权利人所有。

---

## 下载

发布页：[github.com/moyingyilang/JMNeXt/releases](https://github.com/moyingyilang/JMNeXt/releases)。
桌面端当前版本 **2.1.7**（正式版），与 Android 端共用同一份数据层与自学习算法；下载见上方表格，历史预构建版本保留在 Releases 列表里。
每个版本的附件都按上表命名：桌面端 9 个（Linux aarch64/x86_64、Windows x64/arm64）加 Android 2 个 APK（full / lite）。

| 平台 | 架构 | 格式 | 附件名 | 状态 |
| --- | --- | --- | --- | --- |
| Linux | aarch64 | 便携 tar.gz | `Linux-aarch64-2.0.0.tar.gz` | 可用 |
| Linux | aarch64 | deb | `Linux-aarch64-2.0.0.deb` | 可用 |
| Linux | aarch64 | rpm | `Linux-aarch64-2.0.0.rpm` | 可用 |
| Linux | aarch64 | AppImage | `Linux-aarch64-2.0.0.AppImage` | 可用 |
| Linux | x86_64 | 便携 tar.gz | `Linux-x86_64-2.0.0.tar.gz` | 未在真机跑过 |
| Linux | x86_64 | deb | `Linux-x86_64-2.0.0.deb` | 同左 |
| Linux | x86_64 | AppImage | `Linux-x86_64-2.0.0.AppImage` | 同左 |
| Linux | x86_64 | rpm | 无 | **出不来**，见[已知限制](#已知限制) |
| Windows | x64 | 免装 Java 的 ZIP | `Windows-x64-2.0.0.zip` | 可用（用户真机确认） |
| Windows | arm64 | 免装 Java 的 ZIP | `Windows-arm64-2.0.0.zip` | 真机未验证 |
| Android | arm64 / armv7 | APK（`full` / `lite`） | [Releases](https://github.com/moyingyilang/JMNeXt/releases/latest)：`Android-full-2.0.0.apk`（full）与 `Android-lite-2.0.0.apk`（lite） | 已发布至 2.0.0；**applicationId 已改为 `com.jmnext`，老版本不能覆盖安装、需重装**（旧包数据不会自动迁移） |

两个 Windows ZIP 都自带 Temurin JRE 21 与对应架构的 Skiko 原生库，解压后双击 `jmcomic-next.bat`，
不需要另装 Java；包里另有 `diag.bat`，启动不了时收集系统信息与运行日志。Android 的 `full` 与 `lite`
是同一个应用的两种裁剪，`applicationId` 不同，可以同时安装。Linux 只支持 64 位：渲染层 Skiko 不发布
32 位 Linux 原生库（armv7a / i686 均为 404），32 位 JDK 也没有 —— 这是上游依赖缺失，不是打包问题。
macOS 与 iOS 不再发布。
---

## 快速开始

Linux 便携包（解压后运行 `./bin/jmcomic-next`）：

```bash
tar xzf jmnext-2.0.0-linux-aarch64-portable.tar.gz && ./bin/jmcomic-next
```

deb（x86_64 换成 `_amd64.deb`）、rpm、AppImage：

```bash
sudo dpkg -i jmnext_2.0.0_arm64.deb && jmcomic-next
sudo rpm -i jmnext-2.0.0-1.aarch64.rpm && jmcomic-next
chmod +x jmnext-2.0.0-aarch64.AppImage && ./jmnext-2.0.0-aarch64.AppImage
```

系统缺 `libfuse.so.2` 时，AppImage 可以不挂载直接跑：加 `--appimage-extract-and-run`。运行时需要
图形环境（X11 / Wayland）与 OpenGL；纯无头环境会在创建窗口时失败，这是 Compose Desktop 的既有
行为，不是本项目的缺陷。Windows 解压 ZIP 后双击 `jmcomic-next.bat`；Android 装 APK 即可，
登录是可选的，不登录也能浏览、搜索、阅读。

---

## 功能

两端共用的数据层带来这些能力（桌面端在列表形态上另有取舍：**左侧常驻导航**、
**阅读页纵向连续滚动**、**顶栏常驻账号状态**、**方向键 / PageUp / PageDown 翻页**）：

| 能力 | 说明 |
| --- | --- |
| 阅读 | 阅读页、章节选择、图片反切片还原、按字节上限的图片缓存、预取 |
| 浏览 | 首页（分区 + 最新）、分类、周刊、随机、标签、画师与作品库 |
| 搜索 | 关键词搜索；桌面端另有排序与检索字段筛选 |
| 账号 | 登录、注册、找回密码 |
| 收藏与历史 | 收藏夹增删改、收藏列表、历史、追更 |
| 互动 | 评论列表、点赞、通知 |
| 本地屏蔽 | 关键词 / 标签 / 分类三类名单，命中即从列表隐藏，并在页面顶部提示"挡掉多少条" |
| 外观 | 五套风格可切换：WindowGlass / Translucent / FlatBlur / Miuix / Material；背景可选内置渐变或壁纸。换的不只是配色 —— 圆角尺度、表面工艺、字重与按压手感一起换 |

**桌面端尚未追平 Android**：首页分区「更多」页与连载更新表、下载、连载更新提醒、标签级屏蔽整条
链路，以及随机浮钮、签到日历与历史、详情标签可点可屏蔽、移入收藏夹、创作者作品内容、随机页版式
切换、分类分组标签、阅读默认形态进设置页等中等缺口。逐条代码证据见 [docs/PARITY-AUDIT.md](docs/PARITY-AUDIT.md)。

---

## 现状：这里说的话都算数

这个项目把「**已验证**」与「**未验证**」严格分开写，下面两份表是判断"能不能用"的准绳。

**已经验证过的**：

| 项 | 证据 |
| --- | --- |
| 键盘翻页生效 | 真机日志 12 行 |
| Windows x64 免装包能跑 | 用户真机确认 |
| Android `full` / `lite` 编译通过 | `:app:compileFullDebugKotlin`、`:app:compileLiteDebugKotlin` |
| 联网能力（登录 + 拉列表） | 无界面冒烟任务 |
| 打包产物自检 | 应用 jar 唯一、启动类为全限定名、deb 内含图标条目、Windows ZIP 内含 skiko 原生库 |
| 发布附件完整、算法核心、壁纸图源 | 每版 9 个附件线上大小与本地一致（波动 0.2% 到 0.3%，阈值 10%）；shared 单测与离线回放判据通过；Bing 与三个二次元源实测 200 |

**明确未验证的**（不要当成已生效）：

| 项 | 现在到哪一步 |
| --- | --- |
| 界面观感与图标观感 | 开发环境渲染不了 Compose，只能由人过目 |
| **2.0.0 这批 10 个改动** | **只过编译，界面一次没跑过** |
| 所有写操作（点赞、标记已读、标签增删、注册、找回密码等） | 只到「接口返回什么就显示什么」，**账号内是否生效未确认** |
| 预取 v2 与取消误报修复 | 是否降低翻页等待、是否不再出现 `The coroutine scope left the composition`，均未验证 |
| issue #2 的两条修复 | 待报告者复测 |
| 在线壁纸的界面与自动轮换，Windows arm64 与 Linux x86_64 包 | 图源与产物实测过，真机跑起来没验证 |

### 已知限制

- **x86_64 的 rpm 出不来。** rpm 4.18 拒绝跨架构构建（`No compatible architectures found for build`，
  八种 define 组合全部失败）；改用 qemu 运行 `rpm:amd64` 后，真实 spec 的 `%install` 仍会失败 ——
  rpm 每个脚本段都要 `exec /bin/sh`，而环境里 binfmt_misc 不可用。需要真正的 x86_64 机器才能补上。
- **x86_64 产物未在真实机器上运行过。** 只验证到 qemu 用户态模拟下"创建窗口"这一步。
- 桌面端的半成品与未做页：标签、画师与作品库是半成品，评论未做。

---

## 自己构建

### 项目结构与环境要求

| 模块 | 是什么 | 构建方式 |
| --- | --- | --- |
| `app/` | Android 主项目，`full` 与 `lite` 两个变体 | 根 Gradle 构建 |
| `shared/` | 两端共用的数据层（网络、DTO、prefs、屏蔽规则、算法） | 根 Gradle 构建，Android 侧使用 |
| `desktop/` | Compose Multiplatform 桌面端（打包脚本在 `desktop/scripts/`） | **独立 Gradle 构建** |

根 `settings.gradle.kts` 只 include `:app` 与 `:shared`；`desktop/` 有自己的 `settings.gradle.kts`，用
`kotlin.srcDir("../shared/src/main/kotlin")` **直接编译 shared 源码**，因此桌面构建完全不需要 Android
SDK；代价是 shared 会被编译两次。好处是它能在纯 JVM 环境里编译通过 —— 这本身证明了数据层与 Android
无关。

| 组件 | 版本 |
| --- | --- |
| JDK | 21（桌面端 jlink 运行时与 Windows 自带 JRE 都是 Temurin 21） |
| Gradle | 9.x（仓库根有 wrapper；桌面构建另用系统 `gradle`） |
| Android SDK | `compileSdk 37` / `minSdk 24` / `targetSdk 36`，含 build-tools 与 platform-tools |
| 打包工具 | deb / rpm / AppImage 各自需要对应工具链，必须在目标平台或有交叉环境的机器上执行 |

在 aarch64 设备上构建 Android 端还需一条规避：`android.aapt2FromMavenOverride` 指向本机
build-tools 里的 arm64 `aapt2`（AGP 默认拉的是 x86_64 版，跑不了）。已写在根 `gradle.properties`。

### 桌面端

桌面端**不通过根构建**，要在 `desktop/` 目录里单独编（`cd desktop` 后用系统 `gradle`，
也可从仓库根用 wrapper：`./gradlew -p desktop <任务>`）：

```bash
cd desktop
gradle createDistributable     # 产出可运行镜像
gradle run                     # 直接跑（需要图形环境）
gradle smoke                   # 无界面冒烟：登录 + 拉列表，凭据取 JM_USER / JM_PASS
gradle compileKotlin           # 只做编译检查
```

打包（会先做**产物自检**，任一项不过就拒绝出包）：

```bash
scripts/package-linux.sh         dist         # 本机架构（aarch64）：portable / deb / rpm / AppImage
scripts/package-linux-x64.sh     dist-x64     # 交叉产出 x86_64：portable / deb / AppImage
scripts/package-windows-x64.sh   dist-win64   # x64 免装 Java ZIP
scripts/package-windows-arm64-x.sh dist-win   # arm64 免装 Java ZIP
```

自检内容：应用 jar 只能有一个（改代码后 Compose 会生成新的哈希文件名，旧 jar 残留会让旧类抢先加载）、
启动类必须是全限定名、Windows ZIP 里必须真有 `skiko-windows-*.dll`、按字节核对新旧标记
（`strings` 默认只输出 ASCII，用它查中文等于没查）。

跨架构不需要交叉编译器 —— 桌面端是纯 JVM、没有自写原生代码，只需换三样东西：目标架构的 JDK
（用它的 jmods 跑 jlink）、目标架构的 Skiko 原生库（**删掉**宿主那份，两份并存会加载错的一份），
以及启动器（jpackage 不能跨平台生成启动器，Linux 上改用等价的 shell 脚本）。细节见
[docs/engineering-notes.md](docs/engineering-notes.md)。

### Android 端与测试

```bash
./gradlew :app:assembleFullDebug      # full 变体
./gradlew :app:assembleLiteDebug      # lite 变体
./gradlew :app:assembleFullRelease    # 发布包（需要签名配置）
```

任务名要写全：写 `:app:compileDebugKotlin` 会报 Ambiguous matches。

`shared` 有单元测试（图片反切片、动作形状、壁纸核心语义、自学习算法核心与离线回放），
桌面端没有单元测试、只有上面那个无界面冒烟任务：

```bash
./gradlew :shared:test
./gradlew :shared:test --tests "com.jmcomic_next.lyqs.selftune.*"
```

---

## 路线图

### 1.9.x：跨平台预构建线

`1.9.00x` 到 `1.9.xxx` 是**跨架构支持与界面移植的预构建序列**，**每个版本的 CHANGELOG 都会写明
「无主项目更新」** —— 不含 Android 端的任何改动。版本步长按改动量取：**小功能 +001、大功能 +010、
特大可行性验证 +100**（例：1.9.129 → 大功能 1.9.139）。两端正式版各走各的主线。

### 2.0.0：改名 JMNeXt

2.0.0 将在项目改名为 **JMNeXt** 之后发布（大写 X 既表示 cross / 跨平台，也表示 extended / 扩展与加强），原因：与另一个项目重名。
改名会**深改、改彻底**：包名去掉 `lyqs` 段、产物名、窗口标题、安装路径、日志名一起换。唯有一处
不动 —— **Android 的 applicationId 这次不变**（保老用户能升级），2.0.0 之后另开一版再换。
在这之前还要做完：首页分区「更多」页与连载更新表、几项中等缺口、下载、连载更新提醒、标签级屏蔽
整条链路，以及把自学习算法接进两端（那一版走 +300）。

---

## 工程与验证纪律

每个版本都按同一条纪律走，写在 [CHANGELOG.md](CHANGELOG.md) 里：改动能说清「为什么慢 / 省了什么」，
不靠感觉；**已验证**与**未验证**分开写，不许把"编译通过"说成"功能可用"；**已知未修**的明确记档，
不含糊带过；判据要对症、改完要验证**产物**而不是只看配置；构建产物（APK / deb / rpm / AppImage）
不进源码仓库，发版时作为 Release 附件。

## 界面风格

设计令牌取自 [moyingyilang.github.io](https://moyingyilang.github.io) 的
**Fluent（Windows 11 Acrylic / Mica）× MIUI 毛玻璃** 设计语言，两端观感统一：

| 令牌 | 浅色 | 深色 |
| --- | --- | --- |
| 强调色 | `#0f6cbd` | `#60cdff` |
| 正文 | `#16181d` | `#f3f4f7` |
| 次级 / 三级文字 | `#4a4f5a` / `#767c88` | 白 72% / 白 50% |
| 描边 | `rgba(15,23,42,0.09)` | 白 10% |
| 圆角 | 8 / 12 / 18 px | 同左 |

深色不是浅色的反相，而是另一套值。桌面端跟随系统深浅色。

## 深入阅读

- [docs/STATE.md](docs/STATE.md) —— 现状总表：环境、发布流程、已验证与未验证边界、未完成清单
- [docs/PARITY-AUDIT.md](docs/PARITY-AUDIT.md) —— 桌面端与 Android 的功能对齐审计，逐条带代码行号
- [docs/requirements.md](docs/requirements.md) —— 用户全部要求的归并表与落实状态
- [desktop/SELFTUNE.md](desktop/SELFTUNE.md) —— 自学习算法的设计、判据与离线回放实测
- [docs/engineering-notes.md](docs/engineering-notes.md) —— 工程笔记：构建坑、静默失败坑、判断依据
- [CHANGELOG.md](CHANGELOG.md) —— 每个版本改了什么，含未验证项声明

## 致谢

- 屏蔽功能的设计参考了 [haka_comic](https://github.com/raoxwup/haka_comic)（GPL-3.0）：它按
  「标签命中 / 分类黑名单 / 标题含关键词」三条规则过滤列表。本项目是 Kotlin / Compose，
  与它的 Flutter / Dart 实现**没有共用代码**（见 [shared/.../data/BlockRules.kt](shared/src/main/kotlin/com/jmnext/data/BlockRules.kt)）。
- 界面设计语言参考 [moyingyilang.github.io](https://moyingyilang.github.io)。

## 许可

本项目以 **AGPL-3.0-only** 授权，见 [LICENSE](LICENSE)。
禁漫、JMComic 及相关内容的权利归其权利人所有；本应用与官方无关，仅提供客户端实现。

## 相关项目

- **[JMNeXt4QtDesktop](https://github.com/moyingyilang/JMNeXt4QtDesktop)** —— 桌面端的**原生重实现**（C++ / Qt），
  与主项目**并行开发**，定位不同：

  | 项目 | 技术 | 适用场景 |
  | --- | --- | --- |
  | 本仓库 `JMNeXt` | Kotlin + Compose Multiplatform | Android（移植为主）与桌面（JVM，**稳定线**） |
  | `JMNeXt4QtDesktop` | C++ + Qt | 桌面**原生线**：桌面没有 ART 这类限制，原生版可做到约 5–20MB 体积、约 50ms 启动、**无需 JVM**（因而摆脱 jlink/jpackage/自解压 exe 那一整套打包链） |

  后者目前处于**早期阶段**（骨架 + 核心算法已就位、界面尚未开始），功能对齐的判据写在它的
  `docs/PORT-SPEC.md`（移植规格清单）。**在逐项对齐完成之前，桌面端请优先使用本仓库的发布包**。
