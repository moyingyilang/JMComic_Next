package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
 * 账号相关的三个列表页：收藏、历史、追更（2.0.0 桌面端）。
 *
 * 三者版式相同、都要求登录，所以共用一个 [AccountListPage]：
 * 拉第一页 → 网格 → 「加载更多」。这样以后调整间距、列宽、空状态只需改一处。
 *
 * 未登录时不发请求，直接提示去登录 —— 让用户看到"需要登录"而不是"加载失败"。
 */
private const val PAGE_SIZE_HINT = "每页由接口决定，点「加载更多」翻下一页"

@Composable
private fun AccountListPage(
    repository: JmRepository,
    title: String,
    emptyHint: String,
    onOpenComic: (ListItem) -> Unit,
    loader: suspend (Int) -> Pair<List<ListItem>, Int>,
) {
    val loggedIn = repository.auth.isLoggedIn
    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (loggedIn) "加载中…" else "需要登录后才能查看$title") }
    val scope = rememberCoroutineScope()

    fun load(next: Int) {
        busy = true
        scope.launch {
            runCatching { loader(next) }
                .onSuccess { (list, pageTotal) ->
                    items = if (next == 1) list else (items + list).distinctBy { it.id }
                    // 总数要用接口给的，不能用"已加载条数" —— 后者永远等于 items.size，
                    // 于是"加载更多"永远显示、条数也一直是错的（这是之前的真实缺陷）。
                    total = if (pageTotal > 0) pageTotal else items.size
                    page = next
                    status = if (items.isEmpty()) emptyHint else "已加载 ${items.size} 条 / 共 $total 条"
                    System.err.println("[$title] $status")
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    System.err.println("[$title] $status")
                }
            busy = false
        }
    }

    LaunchedEffect(loggedIn) { if (loggedIn) load(1) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (items.isNotEmpty() && items.size >= total) {
                    Text(PAGE_SIZE_HINT, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (items.isNotEmpty()) {
                Button(onClick = { load(page + 1) }, enabled = !busy) { Text("加载更多") }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(168.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { item -> ComicCover(repository, item) { onOpenComic(item) } }
        }
    }
}

@Composable
fun HistoryScreen(repository: JmRepository, onOpenComic: (ListItem) -> Unit) = AccountListPage(
    repository = repository,
    title = "历史",
    emptyHint = "还没有观看记录",
    onOpenComic = onOpenComic,
    loader = { page ->
        val payload = repository.history(page = page)
        payload.list to payload.totalCount
    },
)

@Composable
fun TrackingScreen(repository: JmRepository, onOpenComic: (ListItem) -> Unit) = AccountListPage(
    repository = repository,
    title = "追更",
    emptyHint = "还没有追更的作品",
    onOpenComic = onOpenComic,
    loader = { page ->
        val payload = repository.trackingList(page = page)
        payload.items to payload.hidden
    },
)
