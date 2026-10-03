package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.remote.dto.ListItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 追更（桌面端，1.9.x，整文件重写）。
 *
 * 布局约定与历史页一致（顶部条 fillMaxWidth + 列表 weight(1f)），原因见 HistoryScreen
 * 的注释：旧版共用页面给列表留下零高度，"数据到了但列表空"。
 *
 * 结构说明：Android 端的追更**不是独立页面**，而是收藏页里的一个标签；
 * 桌面端暂时保留为独立页（侧栏有入口），是否合并到收藏页待定 —— 这一点如实记在
 * PARITY.md，不假装已经与 Android 一致。
 */
@Composable
fun TrackingScreen(repository: JmRepository, onOpenComic: (ListItem) -> Unit) {
    val loggedIn = repository.auth.isLoggedIn
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var hidden by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (loggedIn) "加载中…" else "需要登录后才能查看追更") }

    fun load(next: Int) {
        busy = true
        scope.launch {
            runCatching { repository.trackingList(page = next) }
                .onSuccess { paged ->
                    items = if (next == 1) paged.items else (items + paged.items).distinctBy { it.id }
                    hidden += paged.hidden
                    total = if (paged.total > 0) paged.total else items.size
                    page = next
                    status = if (items.isEmpty()) "还没有追更的作品" else "已加载 ${items.size} 条 / 共 $total 条"
                    if (paged.hidden > 0) status += "（本页被屏蔽挡掉 ${paged.hidden} 条）"
                    Log.line("追更", status)
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    Log.error("追更", "加载失败", it)
                }
            busy = false
        }
    }

    LaunchedEffect(loggedIn) { if (loggedIn) load(1) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("追更", style = MaterialTheme.typography.titleLarge)
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (items.isNotEmpty() && items.size < total) {
                Button(enabled = !busy, onClick = { load(page + 1) }) { Text("加载更多") }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(168.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(items, key = { it.id }) { item -> ComicCover(repository, item) { onOpenComic(item) } }
        }
    }
}
