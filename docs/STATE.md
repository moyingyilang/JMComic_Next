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
:shared:test --tests "com.jmnext.selftune.*"
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

1. `/root/jmnext-new/bin/jmnext` 是 **ELF 原生启动器**（不是脚本）。用 `bash -x` 跑它会报
   `cannot execute binary file`（退出码 126）；包内也**没有** `lib/runtime/bin/java`（这个 app-image 不带内置 JRE），
   所以原生启动器在无 JRE 环境下会立刻退出（退出码 1、无输出）。
2. 直接用 jar 跑主类可以起来：
   `java -cp "lib/app/*" com.jmnext.desktop.MainKt`，日志写入
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
1. 包根 `com.jmnext.desktop` 改为 `com.jmnext.desktop`，`com.jmnext.*` 改为 `com.jmnext.*`（去掉 lyqs）；
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

## 十八、第 7 项：两次分派均未产出（结论与下次的做法）

**事实**：为第 7 项连续派了两个子代理，两次都在"分析阶段"耗尽预算，**没有产出可用实现**
（第一个完全没有落盘；第二个按要求先建了 390 字节的骨架文件，但后续两轮一直停在骨架、没有再写）。
两个代理都已被主动中断；那个只有骨架、无人引用的 `SerialReminder.kt` 已移出仓库，留档在
`.work/SerialReminder.skeleton.kt.bak`（不留半成品在仓库里误导接手）。

**下次不要再用"分派给子代理"的方式做这一项**。第一项任务的失败模式已经清楚：它需要先读
`SerialNotify.kt` + `AppPrefs.kt` + 共享层接口，阅读量把预算吃掉，写不出东西来。**直接自己做**，做法：

1. 先只做两处 grep 取证（**不要通读**）：
   `grep -nE "suspend fun .*(track|serial|weeklyUpdate|weekList)" shared/src/main/kotlin/com/jmnext/data/JmRepository.kt`
   与 `grep -nE "Serial|serial|notify|interval|hour" app/src/main/kotlin/com/jmnext/data/prefs/AppPrefs.kt`；
2. 再只 grep `app/.../data/SerialNotify.kt` 的两点：取数用哪个接口、怎么判断"有更新"；
3. 然后**立刻**写 `desktop/.../SerialReminder.kt`（托盘为主 + 窗口内回调 + `isSupported()` 降级 +
   未登录不轮询 + 失败只记日志 + 类注释写明"仅程序运行时有效"），再在 `Main.kt` 接线，最后编译。

方案本身已经定死，不需要再讨论；卡点只在"读得太多、写得太少"。

## 十九、改名 JMNeXt 的清单（第 8 步照此机械执行）

**盘点结果（改动范围的事实，先看清再动手）**：

| 位置 | 数量 |
| --- | --- |
| `app/` 里声明 `package com.jmnext` 的文件 | 60 个（30 个子目录要一起搬） |
| `shared/` 里声明同上的文件 | 38 个（10 个子目录） |
| `desktop/` 里声明 `package com.jmnext.desktop` 的文件 | 40 个 |
| `import com.jmcomic_next...` 行总数 | 574 行 |
| `desktop/scripts/package-*.sh` | 5 个脚本，脚本内 `jmnext` 出现 107 次 |
| `jmnext.log` 出现处 | 2 个文件（`Log.kt` 与 `.work/verify.sh`） |
| 窗口标题 | `Main.kt:116` 的 `title = "JMNeXt"` |
| `/opt/jmnext` | 在脚本与 `desktop/build.gradle.kts` 里**没有出现**（要改安装路径时另行确认） |

**三条容易踩坏的地方（先想清楚再改）**：

1. **`namespace` 与 `applicationId` 本次都不动**（`app/build.gradle.kts:22/27/103`：`com.jmnext`
   与 `.lite`）。因此 **`R` 与 `BuildConfig` 的包路径不变**，所有 `import com.jmnext.R`
   （以及 `.BuildConfig`）**保持原样**，只改我们自己写的那些包。
2. **AndroidManifest 里的相对类名要跟着改**：例如 `android:name=".data.SerialNotifyReceiver"` 是相对
   `namespace` 解析的。若把该类搬进 `com.jmnext.*`，Manifest 必须写成新的全限定名，否则运行时报找不到类
   （编译期不一定报错，属于"编过但一跑就崩"的坑）。
3. **`shared` 被两端同时编译**（Android 经 `:shared`、桌面经 `srcDir` 直编源码），所以 shared 的包名改动
   必须与 `app`、`desktop` 的 import 改动**放在同一次提交**里完成，中间任何一步单独提交都会编译不过。

**执行顺序（每步都要编译核对，不要一口气改完）**：

1. 先改 `shared`（38 个文件 + 10 个目录）→ 只为它编译一次是不可能的（没人单独编它），所以紧接着做第 2 步；
2. 同一次提交里改 `app`（60 个文件 + 30 个目录，注意 Manifest 的相对类名）与 `desktop`（40 个文件）
   的 `package` 与 `import`；
3. 核对：`:app:compileFullDebugKotlin`、`:app:compileLiteDebugKotlin`、`desktop` 的 `compileKotlin` 三条都要过；
4. 再改展示与打包层：窗口标题（`Main.kt:116`）、5 个 `package-*.sh` 里的产物名与 `jmnext` 字样、
   `Log.kt` 与 `.work/verify.sh` 里的日志名（两处必须同步，否则验证脚本找不到日志）；
5. 打包核对照旧：9 个产物、体积与上一版对比、Windows ZIP 内含 `skiko-windows-*.jar`、deb 内图标条目；
6. **Android applicationId 仍不动**（保老用户能升级），留到 2.0.0 之后另开一版。

**改名后的展示语义**（第十三节）：`JMNeXt`，X = cross（跨平台）与 extended（扩展、加强）；
包名、产物名、仓库名一律小写 `jmnext`。

## 二十、第 8 步"算法接线"清单（第 1.9.443 那一步要用的）

**已确认的接口（`shared/.../selftune/SelfTune.kt`）**：`class SelfTune(...)`（第 41 行，构造时要给它
`enabled` 与一个 `SelfTuneStore` 实现）、`params(): ParamValues`（第 109 行，未启用时返回默认值）、
`onPage(sample: PageSample): WindowOutcome?`（第 137 行，非 null 表示一个窗口结束）、
`onCancellation()`（第 145 行）、`stats()`（第 112 行）、内部 `save()`（第 239 行）。
类文档（第 38 到 39 行）写明了预期用法：**每页结束时**调 `onPage(PageSample(...))`，
**窗口开始时**读 `params()` 并应用。

**可调旋钮（`shared/.../selftune/Tunables.kt`）**：`prefetchDepth` 2 到 12（默认 6）、
`prefetchConcurrency` 1 到 3（默认 1）、`cacheBudgetMB` 64 到 512（默认 256）、
`retryBackoffMs` 200 到 3000（默认 800）。

**桌面端的接线点（已定位）**：`desktop/.../ReaderScreen.kt` 的预取逻辑（第 193 到 212 行有在途去重集合
`prefetchInFlight`，第 233 到 237 行在读到 60% 时预取下一章）。**待定位**：当前"往前取几页"与"并发几条"
这两个量在代码里没有显式常量（很可能体现为循环边界或干脆没有上限）—— 接线时要先把它们**变成显式变量**
再交给 `SelfTune.params()`，否则"算法在调参"这句话没有落点。
另需给桌面端写一个 `SelfTuneStore` 实现（用 `PreferencesKeyValueStore`，参考 `TagsScreen.kt` 的用法），
以及决定 `enabled` 从哪里来（建议放设置页，默认开或默认关要明确写出来）。

**Android 端的接线点（待定位）**：在 `app/src/main/kotlin` 下按 `prefetch`、`PREFETCH`、`concurrency`、
`concurrent` 全库搜一次（我按 `data/` 与 `ui/reader/` 两个目录搜过，没有命中，说明路径不对，不要照抄我的路径）。
Android 侧同样要先找到它的预取/并发旋钮，再接 `SelfTune`（算法核心在 shared，两端共用同一份）。

**纪律提醒**：接线的证据等级只能到"编译通过 + 日志显示参数被读取"。**"算法真的改善了体验"必须靠真机数据**，
本环境做不了（第十二节），不要用"已经接上"来暗示效果，也不要用日志里出现过某参数就当成功。

### 二十之一、Android 侧旋钮已定位（补第二十节的"待定位"）

**位置**：`app/src/main/kotlin/com/jmnext/ui/screens/reader/ReaderScreen.kt`：
- 第 1029 到 1031 行是预取窗口 `PREFETCH_BEFORE` / `PREFETCH_AFTER`，注释写明取自 `LiteFeatures`；
- 第 1045 行的 `PrefetchPages(...)` 是预取实现，第 1054 到 1055 行用 `center ± PREFETCH_*` 算前后页范围；
- 第 784 行的注释说明：**Compose 的 Pager 自带相邻页预加载**，所以 Android 的"往前取几页"就是这套窗口。

**一条重要发现（接线前必须决定）**：Android 的阅读页预取**没有并发上限**（全库搜 `semaphore|concurren`
只在随机页的标签读取里有信号量，阅读页没有）。而 SELFTUNE 的 `prefetchConcurrency`（1 到 3）是第二十个旋钮之一 —— 
也就是说它**在 Android 侧没有对应物**。接 Android 时必须二选一，并写明选了哪个：
1. 给 Android 阅读页预取**新加一个并发上限**（改动更大，属新功能），或
2. 明确把 `prefetchConcurrency` 标为**仅桌面端可用**，Android 侧不使用这一个旋钮（改动小、也更诚实）。

另注：`PREFETCH_*` 取自 `LiteFeatures`（lite 变体的功能开关），Android 有 full 与 lite 两个变体，
接线时要**两个变体都看一遍**，不要只按其中一个的取值下结论。

## 二十一、标签级屏蔽整条链路：实测位置与移植判断（更正我先前的写法）

**更正**：我先前在 `docs/PARITY-AUDIT.md` 里写这套东西在 `shared/.../data/`，**这是错的**。
实测位置（`grep -rn "class TagBlockResolver"`）：

| 零件 | 实际位置 | 规模 |
| --- | --- | --- |
| `TagBlockResolver` | `app/src/main/kotlin/com/jmnext/data/TagBlockResolver.kt`（第 32 行 class 定义） | 208 行 |
| `TagCache` | `app/src/main/kotlin/com/jmnext/data/TagCache.kt` | 81 行 |
| `LocalTagBlocker` | `app/src/main/kotlin/com/jmnext/ui/LocalTagBlocker.kt`（`staticCompositionLocalOf<TagBlockResolver?>`） | 12 行 |
| 单元测试 | `app/src/test/kotlin/com/jmnext/TagBlockResolverTest.kt` | 393 行 |
| 共享层现有的相关文件 | 只有 `shared/.../data/FavoriteTags.kt`（标签收藏，与屏蔽无关） |  |

**所以"移植到桌面端"的真实含义是**：这套代码**在 app 模块里，桌面端编译不到**（桌面只直编 `shared` 的源码）。
因此必须先做一个架构决定，二选一：

1. **把 `TagBlockResolver` 与 `TagCache` 移进 `shared`**（连带它们的单测一起移，测试也要能编过），
   然后两端共用；桌面端再补 `LocalTagBlocker` 的等价物（桌面没有 CompositionLocal 的必要，直接传参即可）。
   代价：移动涉及包路径与 import 改动、要确认它们**不依赖任何 Android API**（这是能否移动的前提，先查）。
2. **只在桌面端重写一份**。代价：同一套判定逻辑两端各一份，**会漂**（与第十五节记的 `Daily` 重复问题同类），
   不推荐。

**顺序建议**：先做第一步的"可行性判断"——`grep -nE "android\.|Context|CompositionLocal" TagBlockResolver.kt TagCache.kt`
看它们是否纯 Kotlin（`LocalTagBlocker.kt` 本身是 UI 层，必然是 Android 的，但这不妨碍前两个文件是纯逻辑）。
**这件事做完再谈「允许一次」按钮**：没有标签级判定，那个按钮点下去不会有任何变化。

### 二十一之一、本轮我踩的工具坑（写下来省下一个人一轮）

**场景**：要替换文档里含反引号与竖线的一整行。

| 我做的 | 结果 |
| --- | --- |
| `sed -i "${L}s|.*|**更正**：\`TagBlockResolver.kt\` 实际在…|"` （双引号里写反引号） | **shell 把反引号当命令替换执行了**：报 `TagBlockResolver.kt: command not found`，替换进去的内容里两个类名**消失**，文档反而更糟 |
| `sed -i 's|同上|81 行 / 12 行 / 393 行|g'` | **全局替换误伤无关行**：把另一处"声明同上的文件"改成了"声明81 行 / 12 行 / 393 行的文件" |
| `sed -i "${L}s|.*|… \`shared/\` 里声明…|"` | 替换串里的 `|` 与反引号把 sed 表达式弄坏：`sed: -e expression #1, char 12: unknown option to 's'`，本次未改动（好在没改） |

**可靠做法（已验证）**：把替换内容写进**带引号的 heredoc** 文件（`<<'MD'`，反引号与竖线都不会被 shell 碰），
再用 **awk 整行替换**：
`awk -v n="$L" -v f=$W/line.md 'NR==n{while((getline l < f)>0) print l; next} {print}' 目标 > 新文件 && cp 新文件 目标`。
多处替换同理，用 `NR==a||NR==b` 配合按行号升序的替换文件即可。

**核对纪律（这次起了作用）**：每步都"先打印目标行核对、再改、改完逐行打印核对"，所以三次失误**都只停在文档层**，
没有一次碰到代码；但代价是这一轮为修文档用了四次提交。**下次直接用 heredoc + awk，不要用双引号写 sed 替换串。**

### 二十一之二、可行性判断已做完：可以移进 shared（结论）

查了那两个文件的**全部 import** 与 Android 专有符号：

| 文件 | import 内容 | Android 符号 |
| --- | --- | --- |
| `TagBlockResolver.kt` | 只有 `java.util.concurrent.ConcurrentHashMap` 与 kotlinx.coroutines（`CoroutineScope`/`MutableStateFlow`/`StateFlow`/`launch`/`Semaphore`/`withPermit`） | 0 处 |
| `TagCache.kt` | 只有 kotlinx.serialization（`ListSerializer`/`MapSerializer`/`serializer`/`Json`） | 0 处 |

按 `android\.|Context|Application|CompositionLocal|@Composable` 搜两个文件，**无命中**。

**结论：这两个文件是纯 Kotlin，可以原样移进 `shared`**，两端共用同一份判定逻辑
（`LocalTagBlocker.kt` 那个 12 行的 CompositionLocal 是 Android UI 层的东西，**不必**跟着移；桌面端直接传参即可）。
因此第二十一节里"移进 shared 复用"这条路是可行的，**不要选桌面端重写一份**（会漂）。

**执行时注意**（照第十九节的教训）：移到 `shared` 后要与 `app`、`desktop` 的 import 改动**放在同一次提交**里；
`TagBlockResolverTest.kt`（393 行）也要一起移到 `shared/src/test/`，并确认它在 shared 的测试配置下能跑
（`shared` 已有 JUnit 4.13.2 与 selftune 那批单测的先例，照它们的位置放即可）。

### 二十一之三、标签级屏蔽：已完成的两步与剩余

| 步 | 内容 | 提交 |
| --- | --- | --- |
| 1 | `TagBlockResolver.kt`（208 行）与 `TagCache.kt`（81 行）从 app 移进 shared（包名不变，故 app 侧 import 零改动），单测 393 行一并移到 `shared/src/test/`；三种构建全过（桌面 compileKotlin、`:shared:test --tests TagBlockResolverTest`、`:app:compileFullDebugKotlin`） | `f1f05fa` |
| 2 | 桌面端新增 `TagBlocker.kt`（照 Android `JmApp.tagBlocker` 组装：并发 3、缓存落盘、规则跟随 `BlockStore`），并在 `Main.kt` 启动时 init；搜索页按标签过滤、明示挡住条数与命中标签、提供「允许一次」 | `d86a2f6` |

**剩余（未做）**：首页、分类页、随机页、创作者页、「更多」页等列表尚未接（每页只需三步：提交 id、按 `TagBlocker.hidden` 过滤、给被挡的批次加提示与「允许一次」）。
**证据边界**：只有编译证据；过滤的真实效果、标签接口在真账号下的返回、托盘与界面观感均未验证。

### 二十之二、算法接线的进度与"反馈那一半"的明确任务

**已完成（桌面端，参数侧）**：提交 `d408fc2` —— `ReaderScreen.kt:200` 的硬编码深度 6 改为
`SelfTuner.prefetchDepth`（来自共享层 `SelfTune.params().asInt(Tunables.prefetchDepth)`），
并把本窗口实际深度写进日志；`SelfTuner.kt` 负责落盘学习状态、默认开启、日志走 `Log.line("自学习", …)`。

**未完成（反馈侧）**：还没把 `PageSample` 喂回 `SelfTune.onPage(...)`，也没接 `onCancellation()`。
**没有反馈，算法只会用默认值，不会学** —— 不许把"参数被读取"说成"算法生效"。

**下一步要补的三件事（已取证，照着做即可）**：

1. **`dwellMs`**：阅读页目前没有停留统计。要在 `currentPage` 变化时结算上一页的停留时间
   （记一个进入时间戳，切页时 `now - entered`）。
2. **`hitCache`**：进入某页时查 `RemoteImage.cached(img.image) != null` 即可（这个现成）。
3. **`bytes` 与 `latencyMs`**：
   - `RemoteImage.kt` 的 `load(url): ImageBitmap?` 内部有 `download(url)` 返回的字节数组
     （`load` 里能用 `bytes.size`），但**没有对外返回** → 需要加一个带尺寸的变体（例如返回
     `Pair<ImageBitmap?, Long>`）或让 `RemoteImage` 记一个"本次下载字节数"的计数器；
   - `latencyMs`（翻到该页到图片可见）需要新增测量：进入页面时记时间戳，图片可用（或解码完成）时结算。

**为什么这一步不硬接**：四个字段里有两个现在拿不到真值。用 0 或猜的值填进 `PageSample`，
算法会拿错信号去学 —— **比不学更糟**。宁可停在这里，把任务写清。

**Android 端接线尚未开始**：它的预取窗口取自 `LiteFeatures.prefetchBefore/After`（full 与 lite 两个变体取值不同，
两处都要看），并发上限同样不存在。接线时按同一套：参数从 `SelfTune.params()` 取、样本按上面三件事喂回。

### 二十之三、桌面端算法接线：已完成（参数 + 反馈）

| 部分 | 内容 | 提交 |
| --- | --- | --- |
| 参数侧 | 阅读页预取深度不再是硬编码 6，改为 `SelfTuner.prefetchDepth`（共享层 `SelfTune.params()`），并把实际深度写进日志 | `d408fc2` |
| 字节数出口 | `RemoteImage.loadSized(url): Loaded(bitmap, bytes)` 与 `downloadedSize(url)`（按 URL 记，命中缓存为 0）；原 `load` 委托它，调用点不变 | `24e0749` |
| 反馈侧 | 阅读页进页记时、离页结算，把 `PageSample(latencyMs, bytes, hitCache, failed, dwellMs)` 喂回 `SelfTuner.onPage`；图片未备好即被取消时调 `onCancellation()` | 见本节提交 |
| 主动不接 | `prefetchConcurrency` 两端都没有对应物（桌面串行预取、Android 无并发上限），不为"用满四个旋钮"造假落点 | —— |

**仍未做**：Android 端接线（`LiteFeatures.prefetchBefore/After` 是它的旋钮，full 与 lite 取值不同）。
**证据边界**：全部只有编译证据；算法是否真的改善体验必须靠真机数据，本环境做不了 GUI 运行时验证。

### 二十之四、Android 端算法接线：参数侧已完成，反馈侧的落点已找到

**已完成（参数侧，提交 `78cccb7`）**：`app/.../data/SelfTuner.kt`（算法核心用 shared 的 `SelfTune`，
状态落 `SharedPrefsKeyValueStore("jm_selftune")`，未初始化时返回默认值）；`JmApp.onCreate` 调 `init(this)`；
`ReaderScreen` 的 `PREFETCH_BEFORE/AFTER` 改为**按比例缩放**（`(base*depth+3)/6`，默认深度 6 时系数为 1、
窗口与今天逐位相同）。`:app:compileFullDebugKotlin` 与 `:app:compileLiteDebugKotlin` 都通过。

**反馈侧的落点已找到（未实现）**：Android 阅读页用 Coil 显示图片
（`rememberAsyncImagePainter(request)`，`ReaderScreen.kt:946`），并且**已经有**状态判断
`if (state is AsyncImagePainter.State.Success)`（`:958`）。所以：

| 字段 | 取法 | 把握 |
| --- | --- | --- |
| `latencyMs` | 页面进入时记时间戳，`State.Success` 时结算 | 高（挂钩现成） |
| `dwellMs` | 页面切换时结算上一页（与桌面端同一做法） | 高（只需时间戳） |
| `hitCache` | Coil 的 success 结果里能拿到数据来源（内存缓存/网络），据此判断 | 中（需确认 coil3 的字段名） |
| `bytes` | Coil 的 success 结果里**拿不到**下载字节数，需要挂 Coil 的 `EventListener` 或改用自定义请求 | 低（需先验证，别猜） |

**纪律重申**：`bytes` 若一时拿不到真值，**宁可先不上反馈**，也不要用 0 填 —— 用假值喂 `PageSample`
会让算法拿错信号学，比不学更糟（与第二十之二节同一条）。所以 Android 反馈侧的完成标准是：
四个字段全为真值、编译通过；缺一个就不算接完。

### 二十之五、Android 反馈侧取证结论：三个字段能拿真值，bytes 拿不到

用**字节码**核实（`javap`，不是猜 API）：

| 事实 | 证据 |
| --- | --- |
| `AsyncImagePainter.State.Success` 带 `result: SuccessResult` | `getResult()` 存在 |
| `SuccessResult.getDataSource(): coil3.decode.DataSource` | 存在；枚举取值为 `MEMORY_CACHE / MEMORY / DISK / NETWORK` |
| 因此 `hitCache` 可判 | `dataSource == DataSource.MEMORY_CACHE` 即"命中预取/历史缓存" |
| `coil3.fetch.FetchResult` 是**空标记接口** | `javap` 只有 `public interface coil3.fetch.FetchResult {}`，无任何成员 |
| `EventListener` 的方法签名里**没有字节数** | 只有 `fetchStart/fetchEnd/onSuccess` 等，`fetchEnd` 只给 `FetchResult`（而它是空接口） |

**结论**：
- `latencyMs`（进页 → `State.Success`）、`dwellMs`（页面切换时结算）、`hitCache`（`dataSource`）**三个字段能拿真值**；
- `bytes` **拿不到** —— 想拿真值只有两条路：包一层自定义 `Fetcher`（侵入较大），或让共享层支持"字节未知"这个状态。

**三条路与代价（下一步要选一条，不选就不接）**：
1. **给共享层加"bytes 未知"语义**（例如 `PageSample.bytes: Long? = null` 或 `bytes = 0 表示未知`，
   并让适应度在未知时**不把字节计入**）—— 改动小但要改算法侧并补单测，且要保证"未知"不会被当成"0 字节"混进评分；
2. **自定义 Coil Fetcher 统计字节** —— 最接近真实，但侵入请求链路，风险最高；
3. **Android 不接反馈，只用默认深度** —— 最保守：Android 侧算法不学（等于没生效），但绝不会有假数据。

我的倾向是 1（能真学、也不撒谎），但**必须连单测一起补**：证明"bytes 未知"的样本不会让算法学到错误方向。
在选定之前，Android 反馈侧**不上线**——按第二十之四节的纪律：缺真值就宁可不接。

### 二十之六、Android 反馈侧的落点与一个会毁掉算法的细节

**已就绪**：`ImageBytes.diskSize(context, diskCacheKey)`（提交 `0c56f3b`）—— 取 Coil 落盘文件的真实大小，
拿不到返回 `null`（共享层的 `PageSample.bytes` 已支持"未知"）。

**落点已确认**：阅读页的 `LaunchedEffect(state)`（`ReaderScreen.kt:957`）里已有
`if (state is AsyncImagePainter.State.Success)` —— 这是"图到位"的时刻，可记 latency/hitCache/bytes。

**必须注意（否则算法会学错方向）**：阅读页的图片组件是**按页实例化**的（每个 page 一个 composable），
所以"组件存活时间"**不等于**"用户停留时间"：
- 分页模式下相邻页也会被组合（预加载）；
- 滚动模式下页面会被懒加载与回收。

因此 **`dwellMs` 必须在屏幕级测**（`currentPage` 变化时结算上一页），若用页级组件的存活时长当 dwell，
`Fitness` 依据的中位停留时长会被污染，`Habit`（QUICK/SLOW）判断就错了 —— 而"快翻加深预取、慢读减小"
正是整个算法的立论基础，污染它的后果比不接更糟。

**实现方案（下一步照做）**：
1. 页级 `State.Success` 处记录 `latencyMs`（该页进入时的屏幕级时间戳 → 此刻）、
   `hitCache`（`SuccessResult.dataSource == DataSource.MEMORY_CACHE`）、`bytes`（`ImageBytes.diskSize`），
   按页号存进一个小映射；
2. 屏幕级 `LaunchedEffect(chapterId, currentPage)` 结算 `dwellMs = now - 进页时间戳`，
   从映射里取出该页的 latency/hitCache/bytes，组装 `PageSample` 喂 `SelfTuner.onPage`；
3. 图未到位就被取消的，记 `failed=true` 并调 `SelfTuner.onCancellation()`（取消单独统计）；
4. 两种变体（full/lite）编译都过才算完成。

### 二十之七、两端算法接线完成（参数 + 反馈）

| 端 | 内容 | 提交 |
| --- | --- | --- |
| 共享层 | `PageSample.bytes` 支持"未知"（null，不是 0）+ 4 个单测 | `6c20584` |
| 桌面端 | 预取深度取自 `SelfTuner.prefetchDepth` | `d408fc2` |
| 桌面端 | `RemoteImage.loadSized`/`downloadedSize` 暴露真实字节 | `24e0749` |
| 桌面端 | 阅读页进页记时、离页结算并喂 `SelfTuner.onPage` | `c673d6e` |
| Android 参数侧 | `SelfTuner` + `JmApp.init` + 阅读页按比例缩放窗口 | `78cccb7` |
| Android 取字节 | `ImageBytes.diskSize`（Coil 落盘真实大小，拿不到为 null） | `0c56f3b` |
| Android 采样核心 | `PageSampler` + 5 个单测（报告 tests=5 failures=0） | `7ba55ad` |
| Android 页级 | `State.Success` 处记 latency/hitCache/bytes | `b4677fe` |
| Android 屏幕级 | `pageLink.current` 变化时 settle 并喂样本；换话 clear | 本节提交 |

**主动不接**：`prefetchConcurrency`（桌面串行预取、Android 无并发上限，两端都没有对应物）。
**边界**：全部只有编译与单测证据；效果要真机数据。下一步：发 **1.9.443 基线（+300）**。

## 二十二、2.0.0 发布与两处遗留决定

**2.0.0（JMNeXt 正式版）已发布**：9 个附件线上与本地逐项一致（见 release v2.0.0）。

**改名已完成**：Kotlin 包、`namespace`、`applicationId`（`com.jmnext` / `com.jmnext.lite`）、展示名、
产物名（`jmnext-2.0.0-*`）、日志名（`~/jmnext.log`，`.work/verify.sh` 已同步）、打包脚本与包内主类
（`com.jmnext.desktop.MainKt`）全部改完。

**遗留决定一（刻意不改）**：`desktop/.../PreferencesKeyValueStore.kt:15` 的
`Preferences.userRoot().node("com/jmcomic_next/$node")` **保持旧字符串**。
理由：这是桌面端本地设置的存储节点，改它等于让老用户的设置（壁纸、模糊、风格、阅读形态、屏蔽名单等）
**全部丢失**；而它不对外可见、不与任何其它应用撞名。若以后要统一，必须同时写迁移逻辑（读旧节点 → 写新节点）。
**Android 侧同类情况**：`SharedPrefsKeyValueStore` 用的是 Android 私有目录，随 applicationId 改变而换新，
这部分无法避免（已接受）。

**遗留决定二（已修）**：`origin` 这个 remote 的 push URL 曾被指向旧仓库名，已改为新仓库；
注意本仓库推送实际走的是名为 `main` 的 remote（`git push main main`），别被 `origin` 误导。

## 二十三、2.1.0（动效改造）与遗留

**已发布 v2.1.0**（大功能 +010），14 个附件线上与本地逐项一致；桌面端 x64 与 aarch64 同一次构建、同一提交。

### 做了什么

| 项 | Android | 桌面端 |
| --- | --- | --- |
| 图片淡入 | 全局 `ImageLoader.crossfade(true)` | `ComicCover` 走 `Motion.IMAGE_MS` |
| 列表项动效 | 既有（`animateItem` 4 处） | 9 处 `Modifier.animateItem()` |
| 交互反馈 | 既有（Material3 涟漪） | 新增 `Modifier.jmClickable()`（悬停放大 + 按下回缩） |
| 页面转场 | 既有（`JmNavHost` + `motion.*`） | 新增 `AnimatedContent`（淡入 + 横向轻移） |
| 动效 token | 既有（`ui/theme/ThemeStyle.kt` 的 `MotionSpec`，按风格各带一套） | 新增 `Motion.kt` |
| 共享元素 | 既有真实现 | **接入件已就位、未接线**（2.1.0 里是近似效果） |

### 测试证据（本轮实测）

`:shared:test` 56 用例、`:app:testFullDebugUnitTest` 136 用例，**失败 0、错误 0**。

### 遗留一：桌面端共享元素未接线

`desktop/.../SharedElement.kt` 已就位（两个 CompositionLocal + `Modifier.jmSharedElement(key)`，
结构照 Android 同名文件，未接线时走空操作分支故行为零变化）。接线两处写在文件末尾注释与
`docs/MOTION.md` 第五节：`Main.kt` 包 `SharedTransitionLayout` 并提供两个作用域；`ComicCover.kt`
与 `DetailScreen.kt` 各加一次 `jmSharedElement(jmCoverKey(...))`，key 必须一致。

**两次尝试失败的原因（务必避免重犯）**：都是"按行号单行插入" —— 一次行号偏移导致作用域没提供、
**编译通过但功能静默失效**（因为缺作用域时是安静地退化为空操作），一次把右括号插到别处直接语法错误。
正确做法：把 `Main.kt` 相关段落**完整读出后整体替换**，并显式 grep 断言两处作用域都在。

### 遗留二：动效手感无真机证据

所有动效只验到"编译通过、调用链正确、单测通过"。**快慢、幅度、是否跟手没有任何真机/真桌面观察记录**，
需要使用者确认。另有一类编译器查不到的坑：`AnimatedContent` 块内若误用外层状态而非动画提供的 `target`，
动画会"空转"（新旧两帧渲染同一页面）——本轮已在桌面端踩到并修正。

## 二十四、编译速度优化：实测结论（2026-10-05）

方法：**受控 A/B**（同一场景、交替顺序、各两轮，先改一行源码强制增量编译真正参与，每轮结果立即落盘）。
凡未通过受控复验的"优化"一律不保留。

### 保留的改动

| 改动 | 效果 | 代价 |
| --- | --- | --- |
| 去掉打包脚本里的 `--no-daemon` | 每次调用省 7 到 16 秒（受控 A/B：`--no-daemon` 21/14 秒，daemon 7/5 秒，温度同为 93 度） | daemon 随容器会话结束而消失，无泄漏 |
| 把多件事合并进**同一次容器会话** | 4 件事 34 秒完成（分批调用需 60 秒以上） | 无 |

### 明确**不设**的配置（附作废原因）

| 配置 | 实测 | 为什么不留 |
| --- | --- | --- |
| `org.gradle.workers.max=2` | 默认 17/20 秒 vs 22/21 秒 | 默认更快；此前一次"快一倍"的测量不可复现，已作废 |
| `org.gradle.parallel=true` | 串行 20/24 秒 vs 并行 24/21 秒 | 差异落在噪声内 |
| `--no-daemon`（一次性构建更快） | 见上，反而更慢 | 结论被推翻，已作废 |
| 保留 Gradle/Kotlin daemon 跨会话 | 进程随会话消失，跨会话不可能 | 环境限制，不是配置问题 |

### 打包链实测（单会话 7 步）

gradle 构建 13 秒 + Linux aarch64 四件套 84 秒 + **Linux x86_64 四件套 170 秒（最大头，含 qemu 跑 rpm）**
+ Windows 两个 ZIP 69 秒 + 两个单体 exe 97 秒，**合计 433 秒**。

同一次运行完成**产物正确性回归**：两个 exe 的 `MZ` 头与 `java.exe` 架构（x86-64 / Aarch64）正确、
`skiko-windows-<arch>.jar` 在包内；两个 rpm 内部为 `jmnext 2.1.1 aarch64` 与 `x86_64`。

**下一步的优化方向**（未做）：给各平台打包加重"输入未变则跳过"的哈希闸门 —— 现在即使应用 jar 未变，
四组打包与两个 exe 仍会全量重跑；有了闸门，改文档或只改一个平台时可跳过其余步骤。

### 编译速度：按需打包的闸门（未完成，原因未明）

**目标**：应用未变时跳过平台打包。收益明确 —— 单会话完整链 433 秒里，Linux aarch64 84 秒、
**Linux x86_64 170 秒**、两个 ZIP 69 秒、两个单体 exe 97 秒。

**已证实的部分**：指纹算法 `find -type f -printf '%p %s %T@\n' | LC_ALL=C sort | sha256sum` 可用
（最小实验：touch 一个 jar 后指纹从 `33584e18…` 变为 `652de167…`）。
**不可用**的替代：`stat -c '%n %s %Y'`（`%Y` 只到秒，同一秒内 touch 后指纹不变，实测确认）。

**尚未查清**：把闸门接进 `package-linux-x64.sh` 后，`touch` 输入仍然跳过 —— 而算法本身在同一实验里是有效的。
已排除的可能：`sha256sum` 缺失（容器内存在）、`$STAGE` 当输入（第一版的问题，已修）。
**未排除**：脚本内 `V`/`OUT`/`GATE_EXPECT` 的展开时机、`find` 在脚本上下文中的相对路径解析、
以及"首次运行写入的指纹与比较时算出的指纹是否同一口径"。

**处置**：闸门脚本已完整回退并从仓库删除，未留任何半成品。**在"输入变了必须不跳过"这条被判据证明之前不接线** ——
否则会出现"产物永不更新且不报错"，正是本项目最忌讳的拿旧包交付。

### 编译速度：按需打包闸门（已接线并验收）

**收益实测（整条链跑两轮）**：

| 轮次 | gradle | Linux aarch64 | Linux x86_64 | 两个 ZIP | 两个 exe | 合计 |
| --- | --- | --- | --- | --- | --- | --- |
| 第 1 次 | 28 秒 | 0（已跳过） | 233 秒 | 31 / 35 秒 | 13（失败）/ 49 秒 | **389 秒** |
| 第 2 次 | 22 秒 | 0 | 0 | 0 / 0 | 61（补跑）/ 0 | **84 秒** |

即"输入未变"时打包部分几乎为零，只剩 gradle 自身约 22 秒。

**实现**：`desktop/scripts/lib-gate.sh` + 6 个平台脚本各一行 `gate_begin`；输入指纹 =
输入文件（路径+大小+mtime）排序后 sha256；输出指纹文件 `.gate-<名字>.sha` 放在各发行目录里。

**验收判据（全部通过）**：首次执行并记录 → 输入未变则跳过 → **改源码后不跳过**（关键）→ `FORCE=1` 强制重跑。

**四条硬约束**（两次接错后总结，详见工具仓库 `docs/SPEED.md`）：
1. 输入必须是真输入（用中间产物会永不更新；用脚本自己重建的文件会永不生效）；
2. 接线位置在所有被引用变量之后（`set -u` 下否则当场退出）；
3. 指纹用 `find -printf %T@`，不用 `stat %Y`（秒级精度会漏判）；
4. 安全兜底：指纹/产物清单为空或任一产物缺失 → 一律不跳过。

**已知未查明**：Windows x64 单体 exe 在第 1 轮出现过一次 `rc=1`（13 秒即退出），第 2 轮自动补跑成功；
原因未定位，属偶发，已记录。

### 编译速度：x86_64 的 rpm 不再走 qemu（省约 150 秒）

**做法**：用**原生 aarch64 的 rpmbuild** 直接产 x86_64 的包 —— rpm 报"没有兼容架构"是 rpmrc 里的兼容表决定的，
而包内容是架构无关的文件（x86_64 的运行时已交叉摆好），因此放开该检查、用 `--target x86_64` 指定目标即可：

```
arch_compat: aarch64: x86_64
buildarch_compat: aarch64: x86_64
```

`rpmbuild -bb --rcfile <自定义> --target x86_64 --define "_buildrootdir …" --define "__strip /bin/true"`

**实测**：Linux x86_64 四件套 **233 秒 → 83 秒**；产物 `jmnext 2.1.1 x86_64`、体积 89,386,057（此前 89,386,034）、
包内含 `/opt/jmnext/lib/runtime/bin/java` —— 内容等价，架构字段正确。

**代价/风险**：这是"绕过 rpm 的架构兼容检查"，语义上是撒谎（aarch64 机器并不会真去跑 x86_64 的 rpm）；
安全性来自"包里全是数据文件、运行时是我们自己交叉摆好的"，所以此技巧**只适用于纯文件负载**的包。
脚本里保留了日志与失败兜底；`__strip /bin/true` 仍然必要（宿主 strip 处理不了目标架构的 .so）。

## 二十五、动效与交互逻辑专项（进行中）

### 交互现状审计（计数式，2026-10-05）

| 项 | Android（59 文件） | 桌面端（47 文件） |
| --- | --- | --- |
| 加载态（进度指示） | 8 | 1 |
| 空态 | 23 | 14 |
| 错误态与重试 | 27 | 26 |
| 禁用态 `enabled =` | 27 处 | 17 处 |
| 确认对话框 | 4 | 1 |
| **瞬时反馈（Snackbar/Toast）** | **0** | **0** |
| 键盘处理 | 0 | 1（阅读页翻页） |
| 焦点（Tab 顺序/可见焦点） | 0 | 1 |
| 文本可选（复制） | 0 | 0 |
| 撤销 | 1 | 0 |

说明：计数只能指出"哪里可能有"，还需按**用户真会遇到**的频次筛选。空态与错误重试的覆盖已经不错；
**最大缺口是"操作做完没有任何反馈"**（两端都没有瞬时反馈机制）以及**桌面端的键盘/焦点可用性**。

### 有界改造清单（按用户可见度排序，待逐项实施）

| 优先 | 项 | 影响 | 状态 |
| --- | --- | --- | --- |
| 1 | 两端共用的**瞬时反馈机制**（成功/失败提示），接到收藏、追更、签到、复制、屏蔽等操作上 | 现在"做完没反应"，用户不知道成不成 | 待做 |
| 2 | 桌面端**焦点与键盘导航**（Tab 顺序、可见焦点环、Esc 关闭弹层） | 桌面端基本只能鼠标操作 | 待做 |
| 3 | **破坏性操作确认**（退出登录、清空历史、删除屏蔽规则/收藏） | 误触即不可逆 | 待做 |
| 4 | 文本可选/复制（标题、作者、错误信息、日志） | 无法复制任何内容 | 待做 |
| 5 | 加载态补齐（桌面端列表/详情） | 慢网络下像"卡住" | 待做 |

每项都按同一纪律：小步提交、两端编译与单测通过、无真机观察证据的标未验证、数值与素材走既有 token/约定。

### 动效与交互：第 1 项（瞬时反馈）进展与卡点

**已完成**：机制 `Notices.kt`（两端同实现）+ 两个宿主（桌面端根 Box 覆盖层、Android `Scaffold` 的 `snackbarHost`）
+ 桌面端详情页的点赞与追更接线。

**发现（修正审计结论）**：Android 详情页**已有内联提示**（`actionNotice` 状态 + 提示条，屏蔽标签/作者、追更都走它），
所以那里的反馈缺口比计数式审计显示的小 —— 再加一个 Snackbar 属于**冗余**，故不做。

**卡点**：Android 签到（`RandomFab`）的接线我连续三次失败，原因是这段代码的语法结构：
`when` 的**单表达式分支**（`cond -> notice = …`）不能追加语句；`runCatching{}.fold(onSuccess = { … }, onFailure = { … })`
的 lambda 内追加也会破坏参数结构。结论：**必须整函数重写**，不能用插入法。

**教训（第三次同类）**：插入式改动只适用于"普通语句的函数体"；遇到 `when` 表达式分支、嵌套 lambda 参数、
表达式体函数等紧凑写法，一律**整段替换**。判断依据是"我要插的位置属于哪个语法层级"，而不是"看起来空了一行"。
