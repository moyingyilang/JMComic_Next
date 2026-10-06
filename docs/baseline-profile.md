# Baseline Profile（Android）：本机生成、接入与验证记录

> 本文档由维护者在**自己的 Android 设备**上实测生成，记录设备侧改动、生成步骤、验证方式与收益数字。
> 未完成/未验证的部分单列在文末，不与已验证内容混写。

## 一、为什么走"设备运行时 profile"而不是只跑官方脚手架

- 官方 `:baselineprofile` 模块依赖 adb + instrumentation，本机（Termux 容器）条件已满足；
- 设备上**本来就存在真实使用产生的运行时 profile**（`/data/misc/profiles/cur/0/<pkg>/primary.prof`），
  它反映的是真实热路径，比合成测试更贴近实际；
- 本机设备为 arm64-v8a、SDK 37、KernelSU root 可用，因此可以直接读取系统 profile 目录。

## 二、设备侧改动（可逆，必须还原）

| 改动 | 内容 | 还原方式 |
| --- | --- | --- |
| adb 授权 | 将 Termux 的 adb 公钥追加到 `/data/misc/adb/adb_keys` | 还原为备份内容（本机原文件为空，直接截断为 0 字节） |
| adb 传输 | 临时开启 `service.adb.tcp.port=5555` 并重启 adbd，`adb connect localhost:5555` | `setprop service.adb.tcp.port ""` + 重启 adbd + `adb disconnect` |
| SELinux 标签 | 对 `adb_keys` 执行 `restorecon` | 不影响还原（文件内容还原即可） |

**注意**：TCP adb 端口监听所有网卡，只应在需要时临时开启；任务结束后确认 `adb devices` 不再列出设备。

## 三、生成方式（无头）

1. 新增 `:baselineprofile` 测试模块（`com.android.test` + `androidx.baselineprofile` 插件，`uiautomator` + `benchmark-macro-junit4` 依赖）；
2. 测试**不依赖任何控件文本或资源 id**：回桌面 → 冷启动 → 等待 → 按屏幕尺寸做几次滑动（覆盖启动、列表加载、图片解码、滚动布局）；
3. app 侧声明 profile 来源：`baselineProfile { variants { create("fullRelease") { from(project(":baselineprofile")) } } }`；
4. 执行 `:app:generateFullReleaseBaselineProfile`，全程不需要人工点击屏幕。

### AGP 9 环境下的四个坑（都已踩过并记录）

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 提示移除 `org.jetbrains.kotlin.android` | AGP 9 内置 Kotlin 支持 | 测试模块不再声明该插件 |
| `Unresolved reference 'saveInSrc'` | 插件 1.5.0 无此属性 | 不写该配置，用默认输出位置 |
| `generateXxxBaselineProfile` 成功但无产出 | app 未声明 profile 来源 | 补 `baselineProfile { variants { ... } }` |
| 变体歧义（full/lite） | 测试模块未镜像 app 的 flavor 维度 | 测试模块加同名 `flavorDimensions` 与 `productFlavors` |
| 设备测试报 `libjli.so: cannot open shared object file`、`Gradle Worker Daemon ... exit value 127` | 容器未挂载 `/proc`，JDK 启动器无法用 `$ORIGIN` 定位 `libjli.so`；而 **Gradle worker daemon 继承的是 daemon 的环境变量**，不是当前 shell 的 | 先 `gradle --stop`，再在带 `LD_LIBRARY_PATH=<JAVA_HOME>/lib` 的环境里重启构建 |
| 日志出现 `Failed to start Emulator console for 5554` | 机器上同时存在另一条 adb 设备记录，UTP 会自行探测设备 | 用 `ANDROID_SERIAL=<目标>` 明确指定被测设备 |

## 四、接入与验证

（待填：APK 内 `assets/dexopt/baseline.prof` 是否存在且非空、版本号是否 2.1.8）

## 五、收益测量

方法：`adb shell am start -W` 冷启动，先 `am force-stop` 再测量，比较"接入前 / 接入后"各至少 3 轮的中位数。

（待填：原始数字与中位数）

## 六、未验证项

（待填：例如其他设备/其他 Android 版本上的表现、profile 对首帧与滚动的影响、lite 变体是否也需要 profile）

---

## 四、接入与验证（已完成）

接入方式：把无头收集到的 HRF 文本放到 **`app/src/main/baseline-prof.txt`**（AGP 识别的路径），
再正常构建 release 包。无需手写任何 Gradle 逻辑，profile 的合并、通配符展开与 R8 重映射都由 AGP 完成。

### 关键数字（同一台设备实测）

| 阶段 | 字节 | 行数 | 含 `com/jmnext`（应用自身）的行数 |
| --- | --- | --- | --- |
| 生成产物（HRF 文本） | 2,199,423 | 22,417 | 1,561 |
| AGP 合并后（`merged_art_profile/fullRelease`） | 2,681,804 | 28,272 | 1,561 |
| R8 重映射后（`minifyFullReleaseWithR8`） | 393,924 | 14,816 | 239 |
| **包内 `assets/dexopt/baseline.prof`** | **接入前 9,381 → 接入后 10,150** | - | - |
| 包内 `assets/dexopt/baseline.profm` | 1,137 → 1,154 | - | - |

内容检查：文件只含类描述符与方法签名（抽样如 `coil3/network/okhttp/OkHttpNetworkFetcher`、
`okhttp3/Call$Factory`）；`禁漫`、`成人` 命中 0 次；精确匹配 `\.com/` 也是 0 次。
（注意：用未转义的 `.com/` 计数会得到虚高的数字，因为 `.` 在正则里匹配任意字符 —— 我一开始就踩了这个坑。）

**重要澄清**：本应用在接入之前**就已经带 baseline profile**（来源是依赖库自带的 profile，
头几行写着 `# Baseline Profiles for navigation-runtime`）。所以"包里有 profile"不是本次新增的成果；
本次新增的是**应用自身代码**的热路径覆盖。

## 五、收益测量（结论：未测出可信收益）

**A/B 设计**：两边都是 2.1.8（versionCode 42）、同一签名，**只差这个 profile**：

| 组 | APK | 三次 `am start -W` 的 TotalTime | 中位数 |
| --- | --- | --- | --- |
| 接入前 | 已发布的 `Android-full-2.1.8.apk` | 500 / 445 / 705 ms | **500 ms** |
| 接入后 | 重新构建的 `app-full-release.apk` | 447 / 667 / 394 ms | **447 ms** |

**测量协议**（写在脚本里，先定后测）：`pm install -r` → 冷启动一次并等待 75 秒
（让 profileinstaller 安装 profile、并触发后台按 profile 编译）→ 每轮先 `am force-stop` 再 `am start -W` 取 `TotalTime`。

**结论**：中位数差 53 ms（约 10%），但**组内离散度约 260~273 ms，远大于组间差异**。
按事先写好的判定标准，这属于噪声 —— 因此本报告的结论是 **"未测出可信收益"**，
而不是"提升约 10%"。若要得到有统计意义的结论，需要改用 macrobenchmark 的帧级指标并增加轮数、
控制设备温度与后台负载。

## 六、设备侧还原记录（已执行并核对）

| 项 | 还原动作 | 核对结果 |
| --- | --- | --- |
| adb 授权 | 用备份覆盖 `/data/misc/adb/adb_keys` 并删除备份 | 0 字节，md5 `d41d8cd98f00b204e9800998ecf8427e`（与原始空文件一致） |
| TCP adb | `setprop service.adb.tcp.port ""` + 重启 adbd + `adb disconnect` | `adb devices` 不再列出设备 |
| 测试包 | `pm uninstall com.jmnext.baselineprofile` | 已卸载 |
| 用户应用 | 重新安装正式 release 包 | `versionName=2.1.8`、`versionCode=42` |
| 屏幕常亮 | `svc power stayon false` | 已关闭 |
| 临时文件 | 删除 `/data/local/tmp/bp`、`m.apk`、`bp-test.apk` | 已清理 |

**容器侧（非设备）保留的一项改动**：为 `libjli.so` 建了两条软链接（`/usr/lib/libjli.so`、`/lib/libjli.so`）。
原因：容器未挂载 `/proc`，JDK 启动器无法用 `$ORIGIN` 找到它，导致 Gradle worker daemon 以 127 退出。
如需撤销：`rm -f /usr/lib/libjli.so /lib/libjli.so`（撤销后需重新用 `LD_LIBRARY_PATH` 启动构建）。

## 七、未验证项与后续

- **未验证**：其他设备与其它 Android 版本上的表现；profile 对**首帧、滚动流畅度**的影响（本次没跑 macrobenchmark）；
  收益的统计显著性（见第五节，当前结论是噪声）；
- **覆盖不完整**：滚动路径的手势注入在这台设备上被系统拒绝（`injectInputEvent` 抛异常），
  测试改用"手势失败不中断"的方式，因此**滚动相关的热路径可能没有被覆盖到**；
- **`lite` 变体未接入**：为消除 full/lite 的变体歧义，`baselineProfile.variants` 目前只声明了 `fullRelease`。
  如需覆盖 lite，可对 `assembleLiteRelease` 再做一次同样的接入与验证；
- **构建期代价**：`app/src/main/baseline-prof.txt` 有 2.2 MB，会让 R8 与打包阶段多做一些工作（增量构建影响未单独测量）。

## 五（重测）：更严格协议下的复测与一个关键发现

上一节的结论"噪声内、未测出收益"我做了复测，因为当时的**协议本身可能测不到 profile**。
复测采用更严格的协议，结果**推翻了"已测到 profile 收益"的前提**，结论也要相应改写。

### 复测协议（先写定，再执行）

- **交替测量**：每轮依次测"接入前"与"接入后"，用交替抵消温度与后台负载漂移；
- **每轮独立安装对应 APK**（避免上一轮残留状态），共 14 次安装；
- 每次安装后：首启一次（让 `profileinstaller` 有机会安装 profile）→ 等待 45 秒 →
  **强制按 profile 编译** `cmd package compile -m speed-profile -f <pkg>`；
- 每边 7 轮，报告全部原始值 + 中位数 + 极差；
- 记录电池与温度作为噪声来源证据。

### 原始数据

| 轮次 | 接入前（`Android-full-2.1.8.apk`） | 接入后（本地重建，含应用自身 profile） |
| --- | --- | --- |
| 1 | 713 ms（预热离群） | 288 ms |
| 2 | 295 ms | 250 ms |
| 3 | 210 ms | 291 ms |
| 4 | 229 ms | 289 ms |
| 5 | 269 ms | 300 ms |
| 6 | 318 ms | 390 ms |
| 7 | 262 ms | 371 ms |
| **中位数** | **269 ms** | **291 ms** |
| 极差 | 210~713 ms | 250~390 ms |

- 两边均为 2.1.8 / versionCode 42、同一签名，只差 profile；
- 14/14 次安装成功（`Success`），14/14 次强制编译成功（`Success`）；
- 设备状态：电量 100%，温度 43.2~43.5 ℃（全程基本恒定）。

**中位数差异 22 ms，方向还是"接入后略慢"**，仍在同一噪声带内。

### 关键发现：profile 根本没有进入 ART 的 profile 目录

要判定"无收益是因为没用上，还是因为没效果"，必须看系统是否真的采用了 profile。
做法：清空 profile 目录后启动应用并等待 25 秒，再检查目录：

```
/data/misc/profiles/cur/0/com.jmnext/   -> 空
/data/misc/profiles/ref/0/com.jmnext/   -> 空
```

**随包发布的 baseline profile 并没有被安装到 ART 的 profile 目录**（这台设备是 Android 17 / HyperOS；
`profileinstaller` 在这类 ROM 上静默失败是已知情形）。
因此那次 `cmd package compile -m speed-profile -f` 极可能是**用空 profile 编译**的。

### 结论（改写）

- 本次 A/B 的差异**不能归因于 profile**：两个分支在编译时都近乎"没有 profile"；
- 所以正确的结论不是"profile 没有效果"，而是 **"本次测量无法判定 profile 的效果"（测量无效/不确定）**；
- 另有一个协议缺陷要记下：我显式执行 `cmd package compile -f`，可能**覆盖**了安装时基于"包内嵌 profile"的编译结果，
  反而抹掉了接入后本应存在的差异来源；
- 必须区分三件事：**(a) profile 在包里**（已验证：9,381 → 10,150 字节）；
  **(b) profile 被安装到设备**（本次实测：没有）；**(c) profile 产生收益**（在 (b) 不成立时无从谈起）。

### 副作用说明（如实记录）

为判定 (b)，我清空了 `/data/misc/profiles/{cur,ref}/0/com.jmnext/`，
这会**丢弃该应用此前累积的运行时 profile**（原先 `cur` 下有约 3,947 字节）。
影响很小：运行时会按实际使用重新累积；但这一点应当告知。

### 复测新增的未验证项

- 该 ROM 是否在**安装时**消费包内嵌 profile（`assets/dexopt/baseline.prof`）—— 未验证；
- 该 ROM 上 `profileinstaller` 能否工作、需要什么条件 —— 未验证；
- 若改用 macrobenchmark 的帧级指标与更多轮数，能否观察到差异 —— 未做。
