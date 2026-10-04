package com.jmnext.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * "正在加载"的统一表达：一个细转圈 + 一句文案。
 *
 * 为什么要单独抽：审计发现桌面端 17 个文件只有文字状态（"正在加载…"），仅 1 个文件有真正的进度指示 ——
 * 慢网络下用户看到的是一行不动的字，像卡住。
 *
 * 关键前提（不是装饰问题）：**必须先分清 pending 与 error**。此前详情页用同一个 `status` 同时承载
 * "正在加载作品…" 与 "加载失败：…"，若直接加转圈，出错时也会一直转。所以接线时是先拆状态、再加指示。
 *
 * 转圈尺寸有意取小（16dp、2dp 线宽）：它是"进行中"的提示，不该抢内容的位置。
 */
@Composable
fun LoadingHint(
    text: String = "正在加载…",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(text, style = style)
    }
}

/**
 * "状态行"的统一表达：进行中显示转圈 + 文案，否则显示普通文案。
 *
 * 桌面端各页面此前都是 [busy] 布尔 + [status] 文字（初始值"正在…"，返回后换成结果），
 * 但渲染时无论进行中还是已结束都只画一行不动的字 —— 慢网络下像卡住。
 * 这里只做一件事：把"进行中"与"已完成/失败"分成两种画法，页面把渲染那行换成它即可。
 *
 * 有意只在 `busy` 为真时转圈：失败与成功都走普通文案（失败文案仍可选可复制）。
 */
@Composable
fun StatusLine(
    status: String,
    busy: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (busy) LoadingHint(status, modifier) else Text(status, style = style, color = color, modifier = modifier)
}
