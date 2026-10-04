package com.jmnext.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * 动效 token（Android 侧）：与桌面端 `desktop/.../Motion.kt` **保持同一组数值**。
 *
 * 为什么两端各有一份而不是放进 `shared`：这些值里有 `Easing`，而 `shared` 是不依赖 Compose 的
 * 纯 Kotlin 数据层 —— 为了几个常量把 Compose 引进共享层不划算。两端的数值由
 * `docs/MOTION.md` 记录并人工保持一致；改动时务必同时改两处。
 */
object Motion {
    /** 点击、悬停等即时反馈。 */
    const val QUICK_MS = 120
    /** 常规过渡：淡入、展开收起。 */
    const val NORMAL_MS = 220
    /** 整页切换。 */
    const val PAGE_MS = 280
    /** 图片淡入。 */
    const val IMAGE_MS = 260

    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> quick() = tween<T>(durationMillis = QUICK_MS, easing = Standard)
    fun <T> normal() = tween<T>(durationMillis = NORMAL_MS, easing = Standard)
}
