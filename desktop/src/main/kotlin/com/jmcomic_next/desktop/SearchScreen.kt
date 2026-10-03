package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.remote.dto.ListItem
import kotlinx.coroutines.launch

/**
 * 搜索页（2.0.0 桌面端）。
 *
 * 两条与 Android 端一致的约定：
 *  1. 搜索结果里若带 redirect_aid，说明是「按作品编号精确命中」，直接打开详情而不是列列表；
 *  2. 被屏蔽规则挡掉的条数要在顶部明示（见 BlockedNotice），不能静默少几条。
 *
 * 回车即搜：桌面上敲完回车是本能动作，只给按钮会显得别扭。
 */
@Composable
fun SearchScreen(
    repository: JmRepository,
    onOpenComic: (ListItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var hidden by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("输入关键词后回车搜索") }
    val scope = rememberCoroutineScope()

    fun runSearch(nextPage: Int) {
        if (query.isBlank()) return
        busy = true
        scope.launch {
            runCatching { repository.search(query.trim(), page = nextPage) }
                .onSuccess { result ->
                    // 精确命中作品编号：直接进详情
                    val redirect = result.redirectAid
                    if (nextPage == 1 && !redirect.isNullOrBlank()) {
                        System.err.println("[搜索] 精确命中作品 id=$redirect，直接打开详情")
                        onOpenComic(ListItem(id = redirect, name = query.trim()))
                        busy = false
                        return@launch
                    }
                    items = if (nextPage == 1) result.page.items else items + result.page.items
                    hidden += result.page.hidden
                    total = result.page.total
                    page = nextPage
                    status = "共 $total 条，已加载 ${items.size} 条"
                    System.err.println("[搜索] $status（本页屏蔽 ${result.page.hidden} 条）")
                }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) return@onFailure
                    status = "搜索失败：${it.message}"
                    System.err.println("[搜索] $status")
                }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索作品") },
                singleLine = true,
                modifier = Modifier.width(360.dp).onKeyEvent { e ->
                    if (e.key == Key.Enter) { runSearch(1); true } else false
                },
            )
            Button(enabled = !busy && query.isNotBlank(), onClick = { runSearch(1) }) {
                Text(if (busy) "搜索中…" else "搜索")
            }
            Text(status, style = MaterialTheme.typography.labelSmall)
        }

        BlockedNotice(hidden)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(168.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { item ->
                ComicCover(repository, item) { onOpenComic(item) }
            }
            if (items.isNotEmpty() && items.size < total) {
                item {
                    Button(onClick = { runSearch(page + 1) }, enabled = !busy) { Text("加载更多") }
                }
            }
        }
    }
}
