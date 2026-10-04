# 项目现状总表（接手先读这份）

## 一、一句话现状

主线已完成两批工作：**自学习算法**（核心 + 调参器 + 离线回放，单测与判据都过，**尚未接进两端**）与
**桌面端补齐 Android 功能的第一批**（10 个提交，已提版本号与 CHANGELOG）。
**1.9.153 已打包并发布**（9 个附件，线上大小与本地逐项一致；预构建 pre-release）。

## 二、项目结构与环境（关键，容易踩）

| 事实 | 说明 |
| --- | --- |
| 模块 | `app/`（Android 主项目）、`shared/`（两端共用数据层）、`desktop/`（Compose Multiplatform 桌面端） |
| **desktop 是独立 Gradle 构建** | 根项目只 `include(":app")` 与 `include(":shared")`；desktop 用 `kotlin.srcDir("../shared/src/main/kotlin")` **直接编译 shared 源码** → 所以放 shared 的代码两端自动共用 |
| 构建必须进 chroot | `/data/data/com.termux/files/home/jmc/.work/enter.sh`（JDK 与 Gradle 在容器里；容器内需 `JAVA_HOME=/opt/jdk21-linux`、`LD_LIBRARY_PATH=/opt/jdk21-linux/lib`） |
| 桌面构建脚本 | `.work/build_guarded.sh`（含温度守卫：≥105 度等待；成功才更新 `/root/jmnext-new/`；失败自动重试一次） |
| 温度 | 构建前看 `/sys/class/thermal/thermal_zone*/temp`，超过 105 度不开工；一律 `nice -n 19`；完成后 `pkill -f "[K]otlinCompileDaemon"` |
| `/tmp` 不可写 | Termux 侧 `/tmp` 不可写，临时文件放 `.work/` |
| 读不了图 | 本环境 `read_image` 坏（sharp 在 android-arm64 上装不上）→ **界面观感只能由人看** |

## 三、构建与核对命令（照抄即可）

```bash
# 桌面端编译
.work/enter.sh 'export JAVA_HOME=/opt/jdk21-linux LD_LIBRARY_PATH=/opt/jdk21-linux/lib; cd desktop && nice -n 19 /opt/gradle-9.8.0/bin/gradle --console=plain --no-daemon compileKotlin 2>&1 | grep -E "^e: |BUILD "'

# Android 端核验（任务名必须写全，否则报 Ambiguous matches）
:app:compileFullDebugKotlin  :app:compileLiteDebugKotlin

# shared 单测
:shared:test --tests "com.jmcomic_next.lyqs.selftune.*"
```

**看报错**：只用 `grep -E "^e: "`（Kotlin 错误固定这样开头），**绝不要加 `tail`** —— 报错在输出前部、
帮助文本在尾部，`tail` 会把真正原因截掉（本项目为此白跑过一轮）。

## 四、发布流程（每版）

1. 版本号：`desktop/.../Version.kt`、`desktop/build.gradle.kts`、三个 `desktop/scripts/package-*.sh`、`README.md`；
2. CHANGELOG 顶部加索引行 + 新版小节（**必须写明未验证项**）；
3. 打包 9 个产物：`scripts/package-linux.sh dist`、`package-linux-x64.sh dist-x64`、
   `package-windows-x64.sh dist-win64`、`package-windows-arm64-x.sh dist-win`；
4. **核对**：与上一版逐项比体积（超 10% 人工查）、Windows ZIP 内必须有 `skiko-windows-*.jar`、
   deb 内必须有两个图标条目；
5. 发布：`gh release create vX.Y.Z --prerelease --notes-file …` 一次带上 9 个附件，
   然后 `gh release view --json assets` 核对线上大小与本地一致；
6. 通知：`.work/notify.sh "标题" "正文"`（必须在 Termux 里跑；chroot 内没有 am 广播）。
   **通知只在构建与提交都成功后才发**（曾把没做成的改动说成做成了）。

## 五、已验证 / 未验证（边界要守住）

**已验证**：键盘翻页生效（真机日志 12 行）；Windows x64 免装包经群友确认真机可运行；
Android full/lite 两个 flavor 编译通过；算法核心 7 个单测、调参器 18 个单测；
离线回放方向判据通过（狂点场景深度 7、慢读场景深度 3）；壁纸图源实测
（Bing 返回 url 与 copyright；尺寸段改写成 1080x1920 后 HTTP 200 / image/jpeg / 336420 字节；
三个二次元源 200 且 image/webp，最终地址与请求地址不同）。

**未验证（不得说成生效）**：预取 v2 是否降低翻页等待；取消误报修复是否让日志不再出现
`The coroutine scope left the composition`；issue #2 两条修复是否真好了（待报告者复测）；
界面观感与图标观感；**1.9.153 这一批 10 个改动全部只过编译、没有跑过界面**；
所有写操作只到"接口返回什么就显示什么"这一层，账号内是否生效未确认。

## 六、未完成清单（按建议顺序）

1. 4a 首页分区「更多」页 + 连载更新表（类型 + 星期）——接口 `promoteList(id,page)`、
   `weekIssues()`/`weekList(issueId,type,page)` 都已具备；Android 页面在 `ui/screens/more/MoreListScreen.kt`，
   路由 `more/{id}?title=…`；桌面导航在 `Main.kt` 的 `Screen.Page(route)`；
2. 中等缺口 8 项（随机浮钮、签到日历与历史、详情标签可点可屏蔽、移入收藏夹、创作者作品内容、
   随机页版式切换、分类分组标签、阅读默认形态进设置页）；
3. 下载（共享层有 `albumDownload`）；
4. 连载更新提醒（Android 有 `SerialNotify`）；
5. **标签级屏蔽整条链路**（`TagBlockResolver` + `TagCache` + 「允许一次」）——桌面端完全没有，
   只加按钮没有意义（没有东西被标签挡住）；
6. 算法接进桌面端与 Android 端（预取深度/并发）→ 发 **1.9.443**（+300）→ 文档整理 →
   **改名 JMNeXt**（包名去 lyqs、产物名、窗口标题、安装路径、日志名，同步改 `.work/verify.sh`）→
   **2.0.0**（发布前一刻叫用户改仓库名，用户确认后再发正式版）。

## 七、约定与教训（这些是血换来的，别重犯）

**沟通**：回复里**不要出现对号（U+2713）与叉号（U+2717）**，也不要用其它符号打勾打叉，写字；
中文回复、术语保留英文；少堆符号、直接说结论；**不要用 Python 做文本处理**（用 sed/awk/perl）。

**改代码**：
- 改前**断言目标行的实际内容**，改后**按内容 grep 计数核对**（编译通过 ≠ 改动到位：
  曾出现 `isTracked` 根本没插进去，只因 `awk` 取行号时带了冒号）；
- **整行替换或行间插入**，绝不在单行内部插换行（曾把 `.clickable { … }` 切成两半）；
- 多处改动**自下而上**做，避免行号漂移；
- 一次只改一处逻辑，改完就编译；**宁可拆成两步**也不要在同文件里一次改四处；
- 缺 import/缺声明**只认编译器**（手写清单与"提取大写标识符"两种预检查都被证明不可靠）。

**验证**：
- 先量后改；改动要能说清"为什么慢、省了什么"；
- 判据要**对症**（例如判断包内有没有原生库，要看 jar 里有没有 `.dll/.so`，不要数类名）；
- 构建/测试失败时看**整段**输出，不要用关键词列表猜；
- 产物要真验（deb 内路径与图标条目、ZIP 内 skiko jar、线上大小与本地逐项对比）。

## 八、已发布版本（本会话内）

| 版本 | 内容 | 线上核对 |
| --- | --- | --- |
| 1.9.153 | 桌面端补齐一批 Android 已有功能（在线壁纸、详情点赞与追更初始态、通知已读、标签收藏增删、注册与找回密码、搜索整套筛选与历史/热门/随机推荐、首页分页刷新与「更新」角标） | 9 个附件，线上大小与本地逐项一致 |

发布前逐项核对结果（可复用的判据）：体积与上一版相差 +0.2% 到 +0.3%（阈值 10%）；两个 Windows ZIP 各含
1 个 `skiko-windows-*.jar`、`runtime/bin/java` 条目 5 个；deb 内图标条目 2 个。

## 九、本轮新增完成项（会话内）

| 项 | 状态 | 提交 |
| --- | --- | --- |
| 首页分区「更多」页（新建 `MoreListScreen.kt` + `Main.kt` 路由 `more/<id>?title=…` + `HomeSections.kt` 的「更多」按钮） | **完成**，桌面 `compileKotlin` 通过（BUILD SUCCESSFUL in 2m 6s） | `787ac93` |
| README 重写为开源项目门面（12 节，含"现状：这里说的话都算数"一节，如实标注已验证与未验证） | 完成（260 行；无假 badge/CI/截图） | `08e790a` |

**仍未做**：连载更新表（类型 + 星期，用 `weekIssues()`/`weekList(issueId, type, page)`）—— 同属 4a 的第二部分，
已明确停在这里（不糊假表）。下一步按顺序：第 5 项中等 8 项 → 第 6 项下载（实情是小件）→ 第 7 项连载提醒
（桌面只能用 SystemTray，且仅程序运行时有效）→ 标签级屏蔽整条链路 → 第 8 项算法接线与 1.9.443 → 改名 → 2.0.0。

## 十、两处环境事实（子代理实测发现，我已按事实修正本文档）

1. **`.work/enter.sh` 不接受 `-c`**：它把收到的参数整行写进容器脚本，所以 `enter.sh -c '命令'` 会让第一行
   变成 `-c …` 并报 `-c: command not found`。**正确用法是把整条命令作为一个参数**：
   `.work/enter.sh 'export JAVA_HOME=…; cd … && gradle … 2>&1 | grep -E "^e: |BUILD "'`。
   本文档上一版写的就是 `-c` 形式，是错的，已修正。
2. **`write` 工具在本环境会 EACCES**（它用 hardlink 落到目标目录，而这台设备的文件系统不允许）：
   新建文件要用 **bash heredoc**；覆盖已有文件有时可行，但不可依赖。这不是权限问题（目录属主就是当前用户），
   是文件系统能力问题。

## 十一、分页下标：两套接口不一样（容易接错）

| 接口 | page 起算 | 说明 |
| --- | --- | --- |
| `promoteList(id, page)` | **0** 起算 | 分区「更多」页用；「刷新」重拉第 0 页 |
| `weeklyUpdate(type, date, page)` | **1** 起算 | 连载更新表（类型 + 星期）用；`total` 恒为 0，靠空页判断到底 |
| `latest(page)` 等 | 1 起算 | 首页用 |

另记一条已确认的限制：`weeklyUpdate` 在共享层**没有**走 `blockFiltered`，所以那张表的 `hidden` 恒为 0，
`BlockedNotice` 对它不会显示内容 —— 这是共享层的既有实现，不是桌面端漏做。

## 十二、运行时验证能做到什么程度（本环境实测，含我自己的调用错误）

**结论：这个 chroot 里做不了 GUI 运行时验证。**

实测过程（都发生在 1.9.153 的构建产物 `/root/jmnext-new/` 上）：

1. `/root/jmnext-new/bin/jmcomic-next` 是 **ELF 原生启动器**（不是脚本）。用 `bash -x` 跑它会报
   `cannot execute binary file`（退出码 126）；包内也**没有** `lib/runtime/bin/java`（这个 app-image 不带内置 JRE），
   所以原生启动器在无 JRE 环境下会立刻退出（退出码 1、无输出）。
2. 直接用 jar 跑主类可以起来：
   `java -cp "lib/app/*" com.jmcomic_next.desktop.MainKt`，日志写入
   `[启动] 渲染后端：软件渲染` 与 `版本 1.9.153，构建时间 2026-10-04 17:41` —— **说明包里带的是这一版**。
3. 随后崩在 `Main.kt:109` 的 `painterResource`（窗口图标）：
   加载 PNG 会初始化 Skiko 的 `Image`，而 **Skiko 原生库在本 chroot 里加载不了**（`ExceptionInInitializerError`，
   栈顶 `org.jetbrains.skiko.Library.load`）。包内 skiko jar 是齐的
   （`skiko-awt-0.150.1-*.jar`、`skiko-awt-runtime-linux-arm64-0.150.1-*.jar` 都在），所以不是打包漏文件。
4. 曾怀疑是 `/tmp` 不可写导致 Skiko 解压原生库失败：改用 `-Djava.io.tmpdir=<可写目录>` 后**仍然失败**，
   该假设不成立。
5. 冒烟模式**不是环境变量开关**：它是 `desktop/.../Smoke.kt` 里另一个 `main`，用 Gradle 的 `smoke` 任务跑，
   需要 `JM_USER`/`JM_PASS` 登录凭据 —— 本环境没有凭据，因此**数据层的运行时验证也做不了**。

**我自己的调用错误（三次，记下来避免重犯）**：用不可写的 `/tmp` 接输出；用 `bash -x` 跑 ELF 启动器；
把冒烟模式当成环境变量。三次都是"没先确认对象的形态就下手"。

**因此，本环境能给出的最高证据是：编译通过 + 启动到写出启动日志**。
界面、交互、写操作是否生效、自动轮换等，只能靠用户或群友在真机上看。

## 十三、改名 JMNeXt 的命名语义（第 8 步要落实）

用户先提出 X 可读作 enchanted（着迷），随后撤回，改提 ex- 打头表"增强"的词；**最终定为 extended**。

定稿读法：**X = cross（跨平台）+ extended（扩展、加强）**。两个都是真词，ex- 前缀名副其实
（extended 的 ex- 表"向外"），不需要解释，也不怕被人挑；README 只写这两个读法，不再堆更多。

落地注意：这层语义**不影响任何改名工作** —— 包名、产物名、仓库名一律 `jmnext`（小写），
`JMNeXt` 这个大小写形式只用于展示（窗口标题、README、发布说明）。

第 8 步要做的事：
README 的写法建议：只写两个读法（X = cross 跨平台，加上最终选定的那一个），贪多会显得硬凑。
**这层语义不影响任何改名工作** —— 包名、产物名、仓库名一律 `jmnext`，X 只出现在展示用文本里。

第 8 步要做的事：
1. 包根 `com.jmcomic_next.desktop` 改为 `com.jmnext.desktop`，`com.jmcomic_next.lyqs.*` 改为 `com.jmnext.*`（去掉 lyqs）；
2. 窗口标题、`/opt` 安装路径、包名、脚本名、jar 名、产物文件名改为 jmnext（展示处写 JMNeXt）；
3. 日志文件名如果改，要同步改 `.work/verify.sh`；
4. **Android applicationId 本次不动**（保老用户能升级），留到 2.0.0 之后另开一版；
5. README 与 2.0.0 发布说明里写一句命名语义

## 十四、第 5 项（中等缺口）进展

| 小件 | 状态 | 提交 |
| --- | --- | --- |
| 详情标签可点、可屏蔽、可收藏标签 | 完成（编译通过；`updateFavoriteTags` 2 处、`addTag` 1 处） | `948e432` |
| 创作者库作品内容（`creatorWorkContent`） | 完成 | `948e432` |
| 随机页版式切换（网格/列表，选择存本地） | 完成 | `948e432` |
| 分类页分组标签 + 热门标签兜底 | **未落盘**（子代理在做） | —— |
| 首页随机/签到浮钮、通知未读角标进导航、阅读默认形态进设置页 | **未落盘**（另一个子代理在做） | —— |

**证据等级**：这一批与前面各批一样，只有编译证据，界面未跑过（本环境 Skiko 原生库加载不了，
做不了 GUI 运行时验证，详见第十二节）。

## 十五、第 5 项（中等缺口）第二批量与遗留事项

已提交 `756a4e1`：

| 小件 | 落地方式 | 依据 |
| --- | --- | --- |
| 分类页分组标签 + 兜底 | `CategoryScreen.kt` 补分组标签，缺数据用 `hotTags()` 兜底，并接上点标签搜索（局部函数 `searchTag`） | Android `CategoryScreen.kt:499/545` |
| 首页随机/签到浮钮 | `Main.kt` 的 `RandomFab`（单击随机一本、长按进随机列表）与 `DailyQuickFab`（未登录不渲染、点击 `dailyCheck`） | 共享层 `randomRecommend()`:479、`daily(uid)`:698、`dailyCheck(uid,dailyId)`:719 |
| 通知未读角标进导航 | `SideNav.kt` 的「通知（N）」，>99 显示 99+；`Main.kt` 以 `screen` 为 key 拉 `notificationsUnread().total` | Android 的未读数实际在 `ProfileScreen.kt:267-276`（不是 JmNavHost），按它的语义：未登录不请求、只镜像服务端数量 |
| 阅读默认形态进设置页 | 新增 `ReaderModePref`（`PreferencesKeyValueStore("jm_reader_mode")`），阅读页读它、设置页写它 | Android `AppPrefs.readerMode`（默认 Scroll）、设置项在 `ProfileScreen.kt:923-941`；`ReaderMode` 枚举在共享层 |

**遗留事项（记下来，别丢）**：

1. **`Daily` 逻辑两端各写了一份**：Android 的 `Daily` 纯逻辑对象在 `app/.../data/Daily.kt`（**不在 shared**），
   桌面端看不到，于是照 `isSignedToday` 与 `isAlreadyChecked` 写了三个等价本地函数
   （`dailySignedToday` / `dailyDayNumberOf` / `dailyAlreadyChecked`）。**这是会漂移的重复逻辑**，
   建议将来把 `Daily` 移进 `shared`，两端共用一份 —— 属于"减少两头分叉"的清理项，不是缺陷。
2. 有意未移植：Android `DailyQuickFab` 的 `MAX_ALREADY_PROMPTS` 兜底计数（桌面只用"今天已签"禁用按钮）；
   桌面端没有引入 material-icons 依赖，浮钮与图标按钮一律用文字（「随机」「签到」），代码注释里写明了。
3. **证据等级**：这批同样只有编译证据。界面观感、浮钮是否遮挡内容、鼠标长按是否触发、
   随机跳详情、签到写操作真实结果、未读数字是否与服务端一致、标记已读后角标是否刷新、
   设置页选完进阅读页是否真按所选形态打开 —— **全部未核实**。

## 十六、第 6 项（下载）已完成，第 7 项方案已定

**第 6 项下载**：桌面端在 `DetailScreen.kt` 接上「下载整部作品」入口（简介之后、标签收藏之前），
动作与文案照 Android（`app/.../detail/DetailScreen.kt:1051-1083` 与 `:343-371`）：未登录提示、
异常提示、`!isOk` 用 `payload.msg ?: "这个作品暂时不能下载"`、成功提示「已开始下载：title（fileSize）」。
三处有意偏离已记在提交信息里（用系统浏览器、未登录不跳登录页、打开失败如实报错）。
**范围正确**：Android 端也没有下载管理器，这一步不是做下载系统。

**第 7 项连载更新提醒（桌面端形态，用户已定）**：**两者都做 —— 系统托盘通知为主，窗口内也显示**。
必须写明的限制：桌面端**只能在程序运行时提醒**（Android 靠 AlarmManager 可在后台唤醒，桌面不行）；
部分 Linux 桌面没有系统托盘，此时必须优雅降级到窗口内提示，**不能假装托盘可用**。

**两条遗留（记下来）**：
1. 点标签搜索目前是"就地搜索"（`CategoryScreen`/`DetailScreen` 各留了 `onSearch` / `onOpenTag` 钩子，
   带默认值，不影响既有调用）。要变成"跳搜索页"的正式路由，需要改 `Main.kt`，并给 `SearchScreen`
   加初始关键词参数（它现在没有这个参数，`TagsScreen` 的 `onSearch` 也是把 query 丢掉的，属既有缺口）。
2. 未登录时点击下载只就地提示；若要跳登录页，需要改 `Main.kt` 的调用点。

## 十七、第 7 项（连载更新提醒）状态：未完成（已中断代理）

**事实**：为第 7 项派出的子代理在连续多轮内**没有产生任何文件改动**（`git status` 始终干净，
`desktop/.../SerialReminder.kt` 未创建），因此该代理已被主动中断，避免留下一个随时可能写文件的悬挂任务、
与下一个会话产生冲突。

**方案已定、尚未实现**（接手直接照做即可）：

- 桌面端形态（用户已定）：**系统托盘通知为主（`java.awt.SystemTray` + `TrayIcon.displayMessage`，零新依赖），
  窗口内也显示**；`SystemTray.isSupported()` 为假或安装图标失败时必须**降级到窗口内提示**，
  不可假装托盘可用、不可崩溃；
- **必须在代码注释与文档里写明**：桌面端**只能在程序运行时提醒**（Android 靠 AlarmManager 可后台唤醒，桌面不行）；
- 数据与间隔照 Android 的 `app/.../data/SerialNotify.kt`（先读它取准接口名与间隔、以及 `AppPrefs` 里的开关项名），
  共享层调用要先用 grep 确认，不要猜；未登录不做无意义轮询；网络失败只记日志、保留上次状态、不弹错误托盘；
- 允许改：`desktop/.../Main.kt`、`AppearanceScreen.kt`，并新建 `desktop/.../SerialReminder.kt`；不要改 `shared/`。

接手顺序：第 7 项 → 标签级屏蔽整条链路 → 第 8 步（算法接线 → 1.9.443 → 改名 JMNeXt → 2.0.0）。
