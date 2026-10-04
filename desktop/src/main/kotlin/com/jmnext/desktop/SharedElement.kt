package com.jmnext.desktop

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * 共享元素（封面 → 详情）在桌面端的接入件。
 *
 * **当前状态：已就位但尚未接线。** 文件本身可用（编译通过），但目前没有地方提供作用域，
 * 因此 [jmSharedElement] 一律走"拿不到作用域就什么都不做"的分支 —— 等于没有效果，也不会出错。
 * 接线需要两处（见文件末尾说明），是一次结构性改动，尚未做。
 *
 * 结构照 Android 侧（`app/.../ui/SharedTransition.kt`）：同名同套路，便于两端对着看。
 *
 * 为什么要有"拿不到就什么都不做"这条退路：`sharedElement` 既需要 [SharedTransitionScope]（记录前后位置），
 * 又需要所在页面的动画作用域。少了任何一个就崩，会让整页打不开；退回空操作最多是"没有过渡"。
 */
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalPageVisibility = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** 封面用的共享 key；列表与详情两侧必须用同一个 key，否则配不上对。 */
fun jmCoverKey(comicId: String): String = "jm-cover-$comicId"

/**
 * 把本元素登记为共享元素。`key` 为 null 或作用域缺失时不做事。
 * 时长与缓动取自 [Motion]，与其它动效同一套手感。
 */
@Composable
fun Modifier.jmSharedElement(key: String?): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val visibility = LocalPageVisibility.current ?: return this
    if (key == null) return this
    return with(shared) {
        this@jmSharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> tween(durationMillis = Motion.PAGE_MS, easing = Motion.Enter) },
        )
    }
}

/*
 * 接线清单（尚未执行）：
 *   1) Main.kt：把 AnimatedContent 那段包进
 *        SharedTransitionLayout { CompositionLocalProvider(LocalSharedScope provides this) { ... } }
 *      并把 AnimatedContent 的 lambda 内再包一层
 *        CompositionLocalProvider(LocalPageVisibility provides this@AnimatedContent) { ... }
 *      —— 注意：AnimatedContentScope 本身就是 AnimatedVisibilityScope，可直接用。
 *   2) ComicCover.kt：给图片 Box 加 .jmSharedElement(jmCoverKey(item.id))；
 *      DetailScreen.kt：给封面 Box 加 .jmSharedElement(jmCoverKey(d.id))。
 * 这两处必须同一个 key；漏一处只是没有过渡，不会崩。
 */
