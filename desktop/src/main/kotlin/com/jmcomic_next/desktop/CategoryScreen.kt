package com.jmcomic_next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.jmcomic_next.lyqs.data.remote.dto.ListItem
import com.jmcomic_next.lyqs.data.JmRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 分类（2.0.0 桌面端）。
 *
 * 左侧是本子分类树（一个个分类 + 它们的子分类），右侧是筛选结果。
 * 分类树来自 `categories()`，筛选走 `categoryFilter(c = slug)`。
 *
 * 与 Android 端的一致之处：列表过滤仍在数据层做，页面只显示"被挡掉多少条"。
 */
@Composable
fun CategoryScreen(repository: JmRepository, onOpenComic: (ListItem) -> Unit) {
    // slug 与显示名；空 slug 表示"全部"
    var nodes by remember { mutableStateOf<List<Triple<String, String, List<Pair<String, String>>>>>(emptyList()) }
    var current by remember { mutableStateOf<String?>(null) }
    var currentName by remember { mutableStateOf("全部") }
    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var hidden by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在取分类…") }
    val scope = rememberCoroutineScope()

    fun filter(slug: String?, name: String, next: Int) {
        busy = true
        scope.launch {
            runCatching { repository.categoryFilter(c = slug, page = next) }
                .onSuccess { paged ->
                    items = if (next == 1) paged.items else items + paged.items
                    hidden += paged.hidden
                    page = next
                    status = "$name：已加载 ${items.size} 条" +
                        if (paged.hidden > 0) "（本页屏蔽 ${paged.hidden} 条）" else ""
                    System.err.println("[分类] $status")
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    System.err.println("[分类] $status")
                }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        runCatching { repository.categories() }
            .onSuccess { payload ->
                nodes = payload.categories.map { node ->
                    Triple(
                        node.slug,
                        node.name ?: node.slug,
                        node.subCategories.map { it.slug to (it.name ?: it.slug) },
                    )
                }
                status = "分类 ${nodes.size} 个"
                System.err.println("[分类] $status")
                filter(null, "全部", 1)
            }
            .onFailure {
                if (it is CancellationException) return@onFailure
                status = "分类加载失败：${it.message}"
                System.err.println("[分类] $status")
            }
    }

    Row(Modifier.fillMaxSize()) {
        // 左：分类树
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("分类", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
            NavChip("全部", current == null) { current = null; currentName = "全部"; filter(null, "全部", 1) }
            nodes.forEach { (slug, name, subs) ->
                NavChip(name, current == slug) { current = slug; currentName = name; filter(slug, name, 1) }
                if (current == slug && subs.isNotEmpty()) {
                    subs.forEach { (subSlug, subName) ->
                        Text(
                            text = "· $subName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { current = subSlug; currentName = subName; filter(subSlug, subName, 1) }
                                .padding(start = 20.dp, top = 4.dp, bottom = 4.dp),
                        )
                    }
                }
            }
        }

        // 右：结果
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(currentName, style = MaterialTheme.typography.titleLarge)
                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (items.isNotEmpty()) {
                    Button(onClick = { filter(current, currentName, page + 1) }, enabled = !busy) { Text("加载更多") }
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
            }
        }
    }
}

@Composable
private fun NavChip(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}
