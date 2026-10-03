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
 * 随机本子（桌面端，1.9.x，整文件重写）。
 *
 * 布局约定与历史/追更一致（顶部条 fillMaxWidth + 列表 weight(1f)）——
 * 旧版把顶部条写成 fillMaxSize，列表因此零高度（"数据到了但列表空"）。
 *
 * 与 Android 端的差别如实记档：那边还会按收藏标签偏好排序（RandomRanking），
 * 需要逐部作品取标签（一批约 20 部 = 20 次额外请求），桌面端尚未做。
 */
@Composable
fun RandomScreen(repository: JmRepository, onOpenComic: (ListItem) -> Unit) {
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<ListItem>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在取一批随机作品…") }

    fun roll() {
        busy = true
        scope.launch {
            runCatching { repository.randomRecommend() }
                .onSuccess {
                    items = it
                    status = "这一批 ${it.size} 条"
                    Log.line("随机", status)
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    Log.error("随机", "加载失败", it)
                }
            busy = false
        }
    }

    LaunchedEffect(Unit) { roll() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("随机本子", style = MaterialTheme.typography.titleLarge)
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(enabled = !busy, onClick = { roll() }) { Text(if (busy) "取中…" else "换一批") }
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
