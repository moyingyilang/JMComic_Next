package com.jmcomic_next.desktop

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.remote.dto.FavoriteFolder
import com.jmcomic_next.lyqs.data.remote.dto.ListItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 收藏页（桌面端，1.9.x）。
 *
 * **文件夹清单不需要额外接口** —— 它就在收藏列表响应里（`FavoriteListPayload.folderList`）。
 * 我先前误判为"共享层缺这个接口"，查得不彻底；DTO 注释里其实写了官方 TS 的用法。
 *
 * 注意字段名是 **`FID`**（`FavoriteFolder.folderId`），DTO 里专门警告过：
 * 用 `id` 会静默拿到空串，表现成"收藏夹列表全是无名项"。所以这里直接用 DTO 的字段。
 *
 * 与历史/追更分开实现（那两页共用 AccountListPage）：收藏多了"文件夹"这一维，
 * 硬塞进共用抽象会把另外两页也搞复杂。
 *
 * 尚未做：排序档位（Android 的档位定义我还没读全）、列表内取消收藏、新建/改名/删文件夹。
 */
@Composable
fun FavoriteScreen(repository: JmRepository, onOpenComic: (ListItem) -> Unit) {
    val loggedIn = repository.auth.isLoggedIn
    var folders by remember { mutableStateOf<List<FavoriteFolder>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }   // null 表示「全部」
    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (loggedIn) "加载中…" else "需要登录后才能查看收藏") }
    val scope = rememberCoroutineScope()

    fun load(next: Int) {
        busy = true
        scope.launch {
            runCatching { repository.favorites(page = next, folderId = selected) }
                .onSuccess { payload ->
                    items = if (next == 1) payload.list else (items + payload.list).distinctBy { it.id }
                    // 文件夹清单每次响应都带，用最新的一次覆盖
                    if (payload.folderList.isNotEmpty()) folders = payload.folderList
                    total = if (payload.totalCount > 0) payload.totalCount else items.size
                    page = next
                    val name = folders.firstOrNull { it.folderId == selected }?.name ?: "全部"
                    status = if (items.isEmpty()) "这一栏还没有作品" else "$name：已加载 ${items.size} 条 / 共 $total 条"
                    Log.line("收藏", "$status（文件夹 ${folders.size} 个）")
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    Log.error("收藏", "加载失败", it)
                }
            busy = false
        }
    }

    LaunchedEffect(loggedIn) { if (loggedIn) load(1) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("收藏", style = MaterialTheme.typography.titleLarge)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (folders.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        enabled = !busy && selected != null,
                        onClick = { selected = null; page = 1; load(1) },
                    ) { Text(if (selected == null) "· 全部" else "全部", style = MaterialTheme.typography.labelSmall) }

                    folders.forEach { f ->
                        Button(
                            enabled = !busy && f.folderId != selected,
                            onClick = { selected = f.folderId; page = 1; load(1) },
                        ) {
                            Text(
                                (if (f.folderId == selected) "· " else "") + (f.name ?: f.folderId),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }

        BlockedNotice(0)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(168.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { item -> ComicCover(repository, item) { onOpenComic(item) } }
            if (items.isNotEmpty() && items.size < total) {
                item {
                    Button(enabled = !busy, onClick = { load(page + 1) }) { Text("加载更多") }
                }
            }
        }
    }
}
