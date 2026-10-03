package com.jmcomic_next.desktop

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
 * 随机本子（2.0.0 桌面端）。
 *
 * 接口一次给一批（不分页），所以版式是「一批 + 换一批」。
 * 这与 Android 端的随机页不同：那边还要按收藏标签的偏好排序（RandomRanking），
 * 属于"推荐质量"的增强，这里先做基础形态。
 */
@Composable
fun RandomScreen(repository: JmRepository, onOpenComic: (ListItem) -> Unit) {
    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var hidden by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在取一批随机作品…") }
    val scope = rememberCoroutineScope()

    fun roll() {
        busy = true
        scope.launch {
            runCatching { repository.randomRecommend() }
                .onSuccess {
                    items = it
                    status = "这一批 ${it.size} 条"
                    System.err.println("[随机] $status")
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    System.err.println("[随机] $status")
                }
            busy = false
        }
    }

    LaunchedEffect(Unit) { roll() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("随机本子", style = MaterialTheme.typography.titleLarge)
                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { roll() }, enabled = !busy) { Text(if (busy) "取中…" else "换一批") }
        }
        BlockedNotice(hidden)
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

/**
 * 周刊（2.0.0 桌面端）。
 *
 * 刊期与类型都来自接口：先取刊期列表，默认用**最新一期**与第一个类型，
 * 避免让用户先做两次选择才能看到内容；选完之后可以切换。
 */
@Composable
fun WeekScreen(repository: JmRepository, onOpenComic: (ListItem) -> Unit) {
    var issues by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }   // id to 标题
    var types by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }    // id to 名称
    var issueId by remember { mutableStateOf<String?>(null) }
    var typeId by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var hidden by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在取刊期…") }
    val scope = rememberCoroutineScope()

    fun loadList(issue: String, type: String, next: Int) {
        busy = true
        scope.launch {
            runCatching { repository.weekList(issueId = issue, type = type, page = next) }
                .onSuccess { paged ->
                    items = if (next == 1) paged.items else (items + paged.items).distinctBy { it.id }
                    hidden += paged.hidden
                    page = next
                    status = "已加载 ${items.size} 条" + if (paged.hidden > 0) "（本页屏蔽 ${paged.hidden} 条）" else ""
                    System.err.println("[周刊] $status")
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    System.err.println("[周刊] $status")
                }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        runCatching { repository.weekIssues() }
            .onSuccess { payload ->
                issues = payload.categories.map { it.id to (it.title ?: it.time ?: it.id) }
                types = payload.type.map { it.id to (it.title ?: it.name ?: it.id) }
                val issue = issues.firstOrNull()?.first
                val type = types.firstOrNull()?.first
                issueId = issue
                typeId = type
                System.err.println("[周刊] 刊期 ${issues.size} 个、类型 ${types.size} 个，默认取最新一期")
                if (issue != null && type != null) loadList(issue, type, 1) else status = "没有可用的刊期"
            }
            .onFailure {
                if (it is CancellationException) return@onFailure
                status = "刊期加载失败：${it.message}"
                System.err.println("[周刊] $status")
            }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("周刊", style = MaterialTheme.typography.titleLarge)
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (issues.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("刊期", style = MaterialTheme.typography.labelSmall)
                    issues.take(24).forEach { (id, title) ->
                        Button(
                            enabled = !busy && id != issueId,
                            onClick = { issueId = id; typeId?.let { t -> loadList(id, t, 1) } },
                        ) { Text(title, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        BlockedNotice(hidden)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(168.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { item -> ComicCover(repository, item) { onOpenComic(item) } }
            if (items.isNotEmpty()) {
                item {
                    Button(
                        enabled = !busy,
                        onClick = { issueId?.let { i -> typeId?.let { t -> loadList(i, t, page + 1) } } },
                    ) { Text("加载更多") }
                }
            }
        }
    }
}
