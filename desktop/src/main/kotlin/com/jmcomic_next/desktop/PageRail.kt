package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
 * 竖排页码栏（桌面端，1.9.x）。控件仍是 Android `PageSeekRow` 那一套
 * （上一话 · 当前页 · 滑块 · 总页数 · 下一话），只是竖过来。
 *
 * 用户 1.9.014 之后的反馈与本次改动（逐条对应）：
 *  1. "往里缩一点" —— 宽度 48dp 收窄到 **36dp**；
 *  2. "滑条拉长点" —— 滑块独占中间全部高度（两端只留页码文字与一个按钮的高度），
 *     最小长度从 160dp 提到 200dp；
 *  3. "上一话下一话用左右大于小于号，左边上一话，右边下一话" ——
 *     文字按钮换成 `<`（上）与 `>`（下）：竖过来之后"左"对应"上"，与用户描述一致；
 *  4. "最外层把功能按钮放上" —— `<` 贴最上方、`>` 贴最下方（竖栏的两端就是"最外层"），
 *     页码文字与滑块夹在中间。
 *
 * 竖过来的做法：BoxWithConstraints 量出中间可用高度，把它作为 Row 的宽度，
 * 再绕中心旋转 -90 度 —— Slider 的外观、拇指、主题色与手感全部继承自 material3。
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
    val tight = PaddingValues(0.dp)          // 窄栏里按钮内边距要收掉，否则把宽度撑开

    Column(
        modifier = modifier.width(36.dp).fillMaxHeight().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 最外层（上）：上一话
        TextButton(onClick = onPrev, enabled = hasPrev, contentPadding = tight) {
            Text("<", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "${current + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // 滑块：独占中间的剩余高度（"滑条拉长点"）
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val length = maxHeight.coerceAtLeast(200.dp)
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
        // 最外层（下）：下一话
        TextButton(onClick = onNext, enabled = hasNext, contentPadding = tight) {
            Text(">", style = MaterialTheme.typography.titleMedium)
        }
    }
}
