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
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 搜索页（桌面端）。
 *
 * 与 Android 端一致的约定（`SearchFilters.kt` 是权威来源，这里照抄）：
 *  1. 结果带 redirect_aid 表示「按作品编号精确命中」，直接打开详情而不是列列表；
 *  2. 被屏蔽规则挡掉的条数要在顶部明示（BlockedNotice），不能静默少几条；
 *  3. 排序 5 档：`""` 最新、`mv` 最多点阅、`mp` 最多图片、`tf` 最多爱心、`old` 最旧；
 *     检索字段 5 档：`site` 站内、`work` 作品、`author` 作者、`tag` 标签、`character` 登场人物；
 *     另有年、月两个可选条件；
 *  4. 「最旧」这一档官方客户端会在**本地按 addDate 二次排序**，这里照做（服务端那档不可靠）。
 *
 * 桌面端此前只有关键词输入，这些筛选是功能对齐审计里列为"严重"的缺口之一。
 * 回车即搜：桌面上敲完回车是本能动作，只给按钮会显得别扭。
 */
private val SEARCH_ORDERS = listOf(
    "" to "最新",
    "mv" to "最多点阅",
    "mp" to "最多图片",
    "tf" to "最多爱心",
    "old" to "最旧",
)

private val SEARCH_TYPES = listOf(
    "site" to "站内搜索",
    "work" to "作品",
    "author" to "作者",
    "tag" to "标签",
    "character" to "登场人物",
)

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
    // 筛选：桌面端此前只有关键词，这是审计里的严重缺口
    var order by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("site") }
    var year by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun runSearch(nextPage: Int) {
        if (query.isBlank()) return
        busy = true
        scope.launch {
            runCatching {
                repository.search(
                    query = query.trim(),
                    page = nextPage,
                    order = order.ifEmpty { null },
                    type = type.ifEmpty { null },
                    year = year.takeIf { it.isNotEmpty() },
                    month = month.takeIf { it.isNotEmpty() },
                )
            }
                .onSuccess { result ->
                    // 精确命中作品编号：直接进详情
                    val redirect = result.redirectAid
                    if (nextPage == 1 && !redirect.isNullOrBlank()) {
                        Log.line("搜索", "精确命中作品 id=$redirect，直接打开详情")
                        onOpenComic(ListItem(id = redirect, name = query.trim()))
                        busy = false
                        return@launch
                    }
                    var list = if (nextPage == 1) result.page.items else (items + result.page.items).distinctBy { it.id }
                    // 「最旧」在本地按 addDate 二次排序（照 Android）
                    if (order == "old") {
                        list = list.sortedBy { it.addDate.orEmpty() }
                    }
                    items = list
                    hidden += result.page.hidden
                    total = result.page.total
                    page = nextPage
                    status = "共 $total 条，已加载 ${items.size} 条"
                    Log.line(
                        "搜索",
                        "$status（本页屏蔽 ${result.page.hidden} 条；排序=$order 字段=$type 年=$year 月=$month" +
                            if (order == "old") "，已本地二次排序）" else "）",
                    )
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "搜索失败：${it.message}"
                    Log.error("搜索", status, it)
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("排序：", style = MaterialTheme.typography.labelMedium)
            SEARCH_ORDERS.forEach { (key, label) ->
                TextButton(enabled = !busy, onClick = { order = key }) {
                    Text((if (order == key) "· " else "") + label)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("字段：", style = MaterialTheme.typography.labelMedium)
            SEARCH_TYPES.forEach { (key, label) ->
                TextButton(enabled = !busy, onClick = { type = key }) {
                    Text((if (type == key) "· " else "") + label)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("年月：", style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = year,
                onValueChange = { input -> year = input.filter { it.isDigit() }.take(4) },
                label = { Text("年（如 2026）") },
                singleLine = true,
                modifier = Modifier.width(150.dp),
            )
            OutlinedTextField(
                value = month,
                onValueChange = { input -> month = input.filter { it.isDigit() }.take(2) },
                label = { Text("月（1-12）") },
                singleLine = true,
                modifier = Modifier.width(130.dp),
            )
            TextButton(enabled = !busy, onClick = { year = ""; month = "" }) { Text("不限") }
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
