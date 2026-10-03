package com.jmcomic_next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp

/**
 * 竖排页码栏（桌面端，1.9.x，仿 Android 版）。
 *
 * 显示**当前页 / 总页数** + 竖向进度；**点击或上下拖动即可跳页**。
 *
 * 为什么竖着放：漫画纵向滚动，页码放右侧一列最省横向空间，也不会像浮动条那样遮住画面
 * （画面本身要求"两页之间不留空隙"）。
 *
 * 命中区比视觉条宽（14dp vs 4dp）：4dp 的细条点不中，而视觉上又不该占宽 ——
 * 所以拖拽区域做得比轨道宽，视觉仍是细线。
 *
 * 页码换算用**像素高度**（onSizeChanged 拿到的实际高度），不是 dp 常量：
 * 这样窗口大小变化后换算仍然正确。
 */
@Composable
fun PageRail(current: Int, total: Int, onSeek: (Int) -> Unit, modifier: Modifier = Modifier) {
    val safeTotal = maxOf(total, 1)
    val shown = (current + 1).coerceIn(1, safeTotal)
    var railHeight by remember { mutableStateOf(0) }

    // 纵向位置 → 页码（0 基）
    fun pageAt(y: Float): Int =
        ((y / maxOf(railHeight, 1).toFloat()) * safeTotal).toInt().coerceIn(0, safeTotal - 1)

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$shown", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            "/ $safeTotal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(14.dp)
                .height(160.dp)
                .onSizeChanged { railHeight = it.height }
                .pointerInput(safeTotal, railHeight) {
                    detectTapGestures { offset -> onSeek(pageAt(offset.y)) }
                }
                .pointerInput(safeTotal, railHeight) {
                    detectVerticalDragGestures { change, _ -> onSeek(pageAt(change.position.y)) }
                },
            contentAlignment = Alignment.Center,
        ) {
            // 轨道
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            // 已读比例
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(4.dp)
                    .fillMaxHeight(shown.toFloat() / safeTotal)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            "点/拖跳页",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
