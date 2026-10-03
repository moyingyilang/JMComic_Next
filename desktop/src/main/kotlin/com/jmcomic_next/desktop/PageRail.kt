package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 竖排页码栏（桌面端，1.9.x）。
 *
 * **本文件是 Android 端 `PageSeekRow` 的原样移植，只多了一步"竖过来"。**
 * 上一版我自己手搓了轨道、命中区和 pageAt 换算，用户评价"那个侧栏不像人做的" ——
 * 说得对：自造控件既不像原生，手感也无从校准。现在不保留任何自创部件。
 *
 * Android 原版（`ui/screens/reader/ReaderScreen.kt`，注释原文）：
 *
 *     上行：上一话 · 当前页 · 滑块 · 总页数 · 下一话。 *\/
 *     Row {
 *         IconButton(ChevronLeft)      // 上一话
 *         Text("${currentPage + 1}")   // 当前页
 *         Slider(value = currentPage, valueRange = 0f..max, modifier = Modifier.weight(1f))
 *         Text("$totalPages")          // 总页数
 *         IconButton(ChevronRight)     // 下一话
 *     }
 *
 * 竖过来的做法：用 BoxWithConstraints 量出竖栏的高度，把这个 Row 的**宽度**设为该高度，
 * 再绕中心旋转 -90 度。这样 Slider 的外观、拇指、主题色、无障碍与手感全部直接继承，
 * 我只负责转向。
 *
 * 与原版的两处差异（如实记下，不是"照搬得一模一样"）：
 *  1. 两端的 ChevronLeft/ChevronRight 图标按钮改用文字按钮「上一话 / 下一话」——
 *     桌面端目前没有引入 material-icons 依赖，加依赖只为两个箭头不划算。
 *  2. 增加了竖栏的最小长度兜底（窗口很矮时，旋转前的 Row 需要足够宽度才不被压扁）。
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
    modifier: Modifier = Modifier,
) {
    val max = (total - 1).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier.width(56.dp).fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        // 竖栏高度 = 旋转前 Row 的宽度；给一个最小长度，免得窗口很矮时被压扁
        val length = maxHeight.coerceAtLeast(220.dp)

        Row(
            modifier = Modifier
                .width(length)
                .graphicsLayer { rotationZ = -90f },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(onClick = onPrev, enabled = hasPrev) {
                Text("上一话", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = "${current + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = current.coerceIn(0, max).toFloat(),
                onValueChange = { onSeek(it.roundToInt().coerceIn(0, max)) },
                valueRange = 0f..max.toFloat().coerceAtLeast(1f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onNext, enabled = hasNext) {
                Text("下一话", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
