package com.jmnext.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

/**
 * 桌面端的统一点击与悬停反馈。
 *
 * 为什么要它：桌面端此前几乎没有任何交互反馈（全项目只有一处 indication），鼠标移上去、
 * 按下去都没有变化 —— 这正是"僵"的重要来源之一。Android 侧不需要它：Material3 的
 * `clickable` 自带涟漪，本来就有反馈。
 *
 * 行为：悬停时轻微放大并给一层淡背景，按下时缩得更小一点（模拟"按下去"），
 * 全部走 [Motion] 的时长与缓动，因此和页面其它动效同一套手感。
 */
@Composable
fun Modifier.jmClickable(
    enabled: Boolean = true,
    hoverScale: Float = 1.015f,
    pressScale: Float = 0.985f,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> pressScale
            hovered -> hoverScale
            else -> 1f
        },
        animationSpec = tween(durationMillis = Motion.QUICK_MS, easing = Motion.Standard),
        label = "hoverScale",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(RoundedCornerShape(10.dp))
        .background(if (hovered && enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color.Transparent)
        .hoverable(interactionSource = interaction, enabled = enabled)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}
