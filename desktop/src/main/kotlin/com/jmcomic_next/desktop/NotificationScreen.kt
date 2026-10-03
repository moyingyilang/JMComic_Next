package com.jmcomic_next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.remote.dto.NotificationItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 通知（2.0.0 桌面端）。
 *
 * 这个接口有两个历史坑，桌面端沿用数据层已经处理过的形态：
 *  1. 同一字段在不同响应里可能是字符串或数字 —— 所以 DTO 全按 `JsonElement` 收，
 *     再由访问器解释；直接按严格类型反序列化会让**整条响应**解析失败。
 *  2. `data` 有时是裸数组、有时是 `{list,total}` —— 由 `NotificationPage.from` 兜住。
 *
 * 显示上只列标题、时间与未读标记：`content` 在 DTO 里没有公开访问器
 * （它是 JsonElement），所以正文这一栏暂时拿不到，不假装有。
 */
@Composable
fun NotificationScreen(repository: JmRepository) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var unread by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在取通知…") }

    fun load(next: Int) {
        busy = true
        scope.launch {
            runCatching { repository.notifications(page = next) }
                .onSuccess { paged ->
                    items = if (next == 1) paged.list else (items + paged.list).distinctBy { it.idText }
                    page = next
                    status = "已加载 ${items.size} 条 / 共 ${paged.total} 条"
                    System.err.println("[通知] $status")
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    System.err.println("[通知] $status")
                }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        load(1)
        runCatching { repository.notificationsUnread() }
            .onSuccess {
                unread = it.total
                System.err.println("[通知] 未读 ${it.total} 条")
            }
            .onFailure { if (it !is CancellationException) System.err.println("[通知] 未读数读取失败：${it.message}") }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("通知", style = MaterialTheme.typography.titleLarge)
            if (unread > 0) {
                Text("未读 $unread", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { load(page + 1) }, enabled = !busy) { Text("加载更多") }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.idText ?: it.hashCode().toString() }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 未读标记：用一个小圆点，不用符号
                    if (!item.isRead) {
                        Column(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        ) {}
                    } else {
                        Column(Modifier.size(8.dp)) {}
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.titleText ?: "(无标题)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            listOfNotNull(item.typeText, item.dateText).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
