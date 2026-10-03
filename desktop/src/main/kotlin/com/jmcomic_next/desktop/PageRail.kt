package com.jmcomic_next.desktop

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 竖排页码栏（桌面端，1.9.x，仿 Android 版）。
 *
 * 放在阅读区右侧的一条窄栏里：显示**当前页 / 总页数**，下面一段竖向进度表示已读比例。
 *
 * 为什么竖着放：漫画是纵向滚动的，页码信息放在右侧一列最省横向空间，
 * 也不会像浮动条那样遮住画面（画面本身要求"两页之间不留空隙"）。
 *
 * 本版只做**显示**；拖动跳页是下一步 —— 手势是新的不确定项，而开发容器里看不到界面，
 * 一次只加一件事更容易定位问题。
 */
@Composable
fun PageRail(current: Int, total: Int, modifier: Modifier = Modifier) {
    val safeTotal = maxOf(total, 1)
    val shown = (current + 1).coerceIn(1, safeTotal)

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$shown",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "/ $safeTotal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // 竖向进度：从顶部按已读比例填充
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(4.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(shown.toFloat() / safeTotal)
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
