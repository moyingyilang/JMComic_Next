package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 竖排页码栏（桌面端，1.9.x）。
 *
 * 控件与 Android 阅读页的 `PageSeekRow` 一致：上一话 · 当前页 · 滑块 · 总页数 · 下一话；
 * 只是**竖过来**。原版（`ui/screens/reader/ReaderScreen.kt`）注释原文：
 *
 *     上行：上一话 · 当前页 · 滑块 · 总页数 · 下一话。
 *
 * 上一版我把**整条 Row** 旋转，结果它的布局宽度等于竖栏高度（几百 dp），
 * 布局盒被撑开、视觉上跑到画面中间去了 —— 用户指出"侧栏占正中间"。
 * 现在改成：
 *   - 外层是**普通 Column**（宽 48dp、占满高度），由父级 Row 放在右侧，
 *     它是常规子项，位置可预测，不可能跑到中间；
 *   - 只把 **Slider 单独旋转**（上下拖动），并给它**固定宽度**，避免旋转撑开布局盒。
 *
 * 这样 Slider 的外观、拇指、主题色与手感仍然直接继承 Android 用的同一个控件。
 *
 * 与原版的两处差异（如实记下）：
 *  1. 两端箭头图标按钮用文字按钮「上一话 / 下一话」（桌面端未引入 material-icons 依赖）；
 *  2. 滑块长度取可用高度，并设 160dp 下限，避免窗口很矮时滑块短到没法拖。
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

    Column(
        modifier = modifier.width(48.dp).fillMaxHeight().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TextButton(onClick = onPrev, enabled = hasPrev) {
            Text("上一话", style = MaterialTheme.typography.labelSmall)
        }
        Text(
            text = "${current + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // 旋转前长度 = 可用高度（下限 160dp）；旋转后就是竖着的滑块
                val length = maxHeight.coerceAtLeast(160.dp)
                Slider(
                    value = current.coerceIn(0, max).toFloat(),
                    onValueChange = { onSeek(it.roundToInt().coerceIn(0, max)) },
                    valueRange = 0f..max.toFloat().coerceAtLeast(1f),
                    modifier = Modifier
                        .width(length)
                        .graphicsLayer { rotationZ = -90f },
                )
            }
        }
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
