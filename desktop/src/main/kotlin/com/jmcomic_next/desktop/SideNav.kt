package com.jmcomic_next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 左侧导航（2.0.0 桌面端）。
 *
 * 条目按 Android 端的路由清单来（见 desktop/PARITY.md），先做出外壳让观感可评，
 * 功能逐页接。桌面上左侧常驻导航比底部标签栏更合适：宽度够、不用来回切换。
 */
val NAV_ITEMS: List<Pair<String, String>> = listOf(
    "home" to "首页",
    "search" to "搜索",
    "category" to "分类",
    "week" to "周刊",
    "random" to "随机本子",
    "favorites" to "收藏",
    "history" to "历史",
    "tracking" to "追更",
    "tags" to "标签",
    "creator" to "画师与作品库",
    "notifications" to "通知",
    "block" to "屏蔽设置",
    "about" to "关于",
    "profile" to "我的",
)

@Composable
fun SideNav(selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(176.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "JMComic_Next",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
        NAV_ITEMS.forEach { (route, title) ->
            val active = route == selected
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    )
                    .clickable { onSelect(route) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}
