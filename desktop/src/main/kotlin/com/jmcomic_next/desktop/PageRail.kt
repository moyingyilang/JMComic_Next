package com.jmcomic_next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 竖排侧栏（桌面端，1.9.x）。
 *
 * **滑轨是自己画的，不再用 material3 的 Slider 旋转**。
 *
 * 为什么换：实测日志里给滑轨划的区域是 484px / 总高 662px（占 73%），
 * 但旋转后的 Slider **并不撑满这块空间** —— 控件有自己的内在尺寸与内边距，
 * 旋转只是把绘制转过去，尺寸仍受它自己约束，所以看上去一直短。
 * 自己画一条轨道就没有这层不确定性：轨道高度 = 我给的高度。
 *
 * 布局（自上而下，按用户逐条要求）：
 *   1. 滑轨：`fillMaxHeight(0.7f)` —— 固定占侧栏 70% 高度，不参与"剩余空间"分配；
 *   2. 页码一行；
 *   3. 五个功能按钮（Android 阅读页底栏那五个；传 null 的置灰）；
 *   4. 上一话 / 下一话：两个挨在一起，上面上一话、下面下一话。
 *
 * 交互：点击或上下拖动轨道任意位置即跳到对应页（换算按轨道实际像素高度，随窗口变化自动正确）。
 */
@Composable
fun PageRail(
    current: Int,
    total: Int,
    onSeek: (Int) -> Unit,
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modeLabel: String = "纵向",
    onToggleMode: (() -> Unit)? = null,
    onOpenPicker: (() -> Unit)? = null,
    onOpenComments: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleLike: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val max = (total - 1).coerceAtLeast(0)
    val frac = if (max <= 0) 0f else current.coerceIn(0, max).toFloat() / max
    var trackH by remember { mutableStateOf(0) }
    val tight = PaddingValues(0.dp)
    val density = LocalDensity.current

    Column(
        modifier = modifier.width(40.dp).fillMaxHeight().padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── 1. 滑轨：固定占 70%（不靠"剩余空间"，避免被按钮挤短）──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(vertical = 6.dp)
                .onSizeChanged {
                    trackH = it.height
                    Log.line("阅读", "侧栏滑轨高 " + it.height + "px / " + with(density) { it.height.toDp() })
                },
            contentAlignment = Alignment.Center,
        ) {
            // 底轨
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            // 已读部分：从顶往下
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(6.dp)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            // 把手
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (((trackH - with(density) { 18.dp.roundToPx() }) * frac).toInt()).coerceAtLeast(0)) }
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            // 手势层：点击或拖动都换算成页码（按轨道实际像素高度，窗口变化后依然正确）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(max, trackH) {
                        detectTapGestures { o ->
                            if (trackH > 0 && max > 0) {
                                onSeek(((o.y / trackH) * max).roundToInt().coerceIn(0, max))
                            }
                        }
                    }
                    .pointerInput(max, trackH) {
                        detectVerticalDragGestures { ch, _ ->
                            if (trackH > 0 && max > 0) {
                                onSeek(((ch.position.y / trackH) * max).roundToInt().coerceIn(0, max))
                            }
                        }
                    },
            )
        }

        // ── 2. 页码 ──
        Text(
            text = "${current + 1}/$total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── 3. 五个功能按钮 ──
        RailAction(modeLabel, onToggleMode)
        RailAction("章节", onOpenPicker)
        RailAction("评论", onOpenComments)
        RailAction("收藏", onToggleFavorite)
        RailAction("点赞", onToggleLike)

        // ── 4. 上一话（上）/ 下一话（下）──
        TextButton(onClick = onPrev, enabled = hasPrev, contentPadding = tight, modifier = Modifier.height(26.dp)) {
            Text("<", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onNext, enabled = hasNext, contentPadding = tight, modifier = Modifier.height(26.dp)) {
            Text(">", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** 侧栏里的功能按钮：传 null 表示桌面端还没实现，置灰并保持位置。 */
@Composable
private fun RailAction(label: String, action: (() -> Unit)?) {
    TextButton(
        onClick = { action?.invoke() },
        enabled = action != null,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.height(22.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
