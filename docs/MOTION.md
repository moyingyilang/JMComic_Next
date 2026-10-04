# 动效约定（MOTION）

这份文件定义项目的动效手感，避免"每处都不一样导致整体显僵"。改动前请先读这里。

## 一、token 在哪

| 平台 | 文件 |
| --- | --- |
| 桌面端 | `desktop/src/main/kotlin/com/jmnext/desktop/Motion.kt` |
| Android | `app/src/main/kotlin/com/jmnext/ui/Motion.kt` |

两端**各有一份、数值必须一致**。没有放进 `shared` 的原因：这些值里有 Compose 的 `Easing`，
而 `shared` 是不依赖 Compose 的纯 Kotlin 数据层，为了几个常量把 Compose 引进共享层不划算。
**改一处就要改另一处**，并同步更新本文件。

## 二、取值与用途

| token | 值 | 用在哪 |
| --- | --- | --- |
| `QUICK_MS` | 120 | 点击、悬停、按下等即时反馈（要"跟手"，所以最短） |
| `NORMAL_MS` | 220 | 淡入淡出、展开收起 |
| `PAGE_MS` | 280 | 整页/整屏切换（要看清过程，所以最长） |
| `IMAGE_MS` | 260 | 图片从占位到实图 |
| `Standard` / `Enter` / `Exit` | 三条贝塞尔 | 常规 / 进场 / 退场 |
| `gentleSpring()` | 低阻尼、中低刚度 | 需要轻微回弹处（缩放、抬手） |

原则：**即时反馈短、页面过渡长**；需要"重量感"用弹簧，其余用缓动。禁止在界面里硬写时长或曲线。

## 三、当前落地情况（如实记）

| 位置 | 状态 |
| --- | --- |
| Android 全局图片淡入（Coil `crossfade(true)`） | 已接，时长由 Coil 内部管理，未走 token（Coil 允许自定义，但默认值可用） |
| 桌面封面淡入（`ComicCover`） | 已接，走 `IMAGE_MS` + `Standard` |
| 桌面列表项（9 处 `animateItem()`） | 已接，位移/淡入由 Compose 默认弹簧驱动，未走 token |
| 桌面交互反馈（`Modifier.jmClickable`） | 已接，走 `QUICK_MS` + `Standard` |
| 桌面路由转场（`Main.kt` 的 `AnimatedContent`） | 已接，进场 `PAGE_MS` + `Enter`，退场 `QUICK_MS` + `Exit` |
| **Android 阅读页现有的数值** | **刻意未改**：`ReaderScreen` 里的 `slideIn/OutVertically` 等是此前按手感调过的，没有真机对比前不擅自改动 |
| Android 其余 `tween/spring`（约 5 个文件） | **待统一**：未逐个替换，避免在看不到效果的情况下改动可用逻辑 |

## 四、未验证说明

以上所有动效，我只验证到"**代码编译通过、调用链正确**"。
**实际手感（快慢、幅度、是否跟手）没有任何真机/真桌面观察证据** —— 需要使用者确认。
`AnimatedContent` 那处还有一类只有肉眼能发现的坑：块内若误用外层状态而非动画提供的
`target`，动画会"空转"（新旧两帧渲染同一页面），编译不会报错。
