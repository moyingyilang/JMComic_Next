package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 竖排侧栏（桌面端，1.9.x）。
 *
 * 布局按用户 1.9.015 之后的明确要求，自上而下：
 *   1. **滑块**（最上，占侧栏高度一半以上 —— 用户要求"滑条不要做短"）；
 *   2. **五个功能按钮**（取自 Android 阅读页底栏：横向/纵向 · 章节 · 评论 · 收藏 · 点赞）；
 *   3. **上一话 / 下一话**（最下，两个挨在一起，**上面是上一话、下面是下一话**）。
 *
 * 五个功能里，桌面端**尚未实现**的先置灰（传 null），不假装能用：
 *   - 横向/纵向：桌面端只有纵向滚动这一种模式，横翻等于再做一套翻页实现；
 *   - 章节：桌面端的章节选择在详情页右栏，阅读页内还没有选择器；
 *   - 评论 / 收藏：功能已有，但需要把回调从详情页接进阅读页（下一步）；
 *   - 点赞：要先确认共享层是否有接口。
 *
 * 滑块用 weight(1f) 吃掉中间**剩余**的全部高度，其余部分刻意压扁（按钮内边距为 0、
 * 文案两字），这样滑块自然占到一半以上 —— 之前显得短，是因为两端元素太占地方。
 *
 * 竖过来的做法没变：BoxWithConstraints 量出可用高度当作旋转前的宽度，再绕中心旋转 -90 度，
 * 于是 Slider 的拇指、主题色与手感全部继承 material3。
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
    onToggleMode: (() -> Unit)? = null,
    onOpenPicker: (() -> Unit)? = null,
    onOpenComments: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleLike: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val max = (total - 1).coerceAtLeast(0)
    val tight = PaddingValues(0.dp)

    Column(
        modifier = modifier.width(40.dp).fillMaxHeight().padding(vertical = 2.dp)
            .onSizeChanged { Log.line("阅读", "侧栏尺寸：总高 " + it.height + "px") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── 1. 滑块：占中间剩余高度的全部（即侧栏一半以上）──
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .onSizeChanged { Log.line("阅读", "侧栏尺寸：滑块区高 " + it.height + "px") },
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Slider(
                    value = current.coerceIn(0, max).toFloat(),
                    onValueChange = { onSeek(it.roundToInt().coerceIn(0, max)) },
                    valueRange = 0f..max.toFloat().coerceAtLeast(1f),
                    modifier = Modifier
                        .width(maxHeight.coerceAtLeast(120.dp))
                        .graphicsLayer { rotationZ = 90f },
                )
            }
        }

        // ── 2. 五个功能按钮（Android 底栏那五个；传 null 的置灰）──
        RailAction("纵向", onToggleMode)
        RailAction("章节", onOpenPicker)
        RailAction("评论", onOpenComments)
        RailAction("收藏", onToggleFavorite)
        RailAction("点赞", onToggleLike)

        // 页码贴在滑块与按钮之外的位置：放到功能按钮下面，
        // 这样滑块能从栏顶一直延伸到功能按钮上方（用户要求"滑条从顶到功能按钮顶"）。
        Text(
            text = "${current + 1}/$total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── 3. 上一话 / 下一话：挨在一起，上为上一话、下为下一话 ──
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
        modifier = Modifier.height(22.dp),
        onClick = { action?.invoke() },
        enabled = action != null,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
