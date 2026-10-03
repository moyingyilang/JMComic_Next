package com.jmcomic_next.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.remote.dto.CreatorAuthor
import com.jmcomic_next.lyqs.data.remote.dto.CreatorWork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 画师与作品库（桌面端，1.9.x 全量移植的最后一块）。
 *
 * 这是**独立于漫画列表的内容源**（JM 的"作品库"，同人志那类），所以单独一页。
 * Android 端的页面标题就是"画师与作品库"，我照原名用，不再叫"创作者库"——
 * 之前那个名字用户看不懂，问过我一次。
 *
 * 三种视图在同一页里切换（桌面端不做多级路由，省得来回跳）：
 *  画师列表 → 点画师 → 该作者的作品；作品列表 → 点作品 → 作品信息（含相关作品）。
 *
 * 分页沿用其它页的做法：追加时按 id 去重（服务端翻页会重叠，重复 key 会让 Compose 崩）。
 */
@Composable
fun CreatorScreen(repository: JmRepository) {
    var mode by remember { mutableStateOf("author") }   // author / work
    var query by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf<List<CreatorAuthor>>(emptyList()) }
    var works by remember { mutableStateOf<List<CreatorWork>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在取画师列表…") }
    var selectedAuthor by remember { mutableStateOf<CreatorAuthor?>(null) }
    var workInfo by remember { mutableStateOf<Pair<String, List<CreatorWork>>?>(null) }
    val scope = rememberCoroutineScope()

    fun load(next: Int) {
        busy = true
        scope.launch {
            runCatching {
                if (mode == "author") {
                    repository.creatorAuthors(page = next, query = query.trim())
                } else {
                    repository.creatorWorks(page = next, searchValue = query.trim())
                }
            }
                .onSuccess { r ->
                    if (mode == "author") {
                        val list = r.items.filterIsInstance<CreatorAuthor>()
                        authors = if (next == 1) list else (authors + list).distinctBy { it.id }
                        status = "画师 ${authors.size} 位 / 共 ${r.total}"
                    } else {
                        val list = r.items.filterIsInstance<CreatorWork>()
                        works = if (next == 1) list else (works + list).distinctBy { it.id }
                        status = "作品 ${works.size} 部 / 共 ${r.total}"
                    }
                    total = r.total
                    page = next
                    Log.line("作品库", "$status" + if (query.isNotBlank()) "（搜索：$query）" else "")
                }
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    status = "加载失败：${it.message}"
                    Log.error("作品库", "加载失败 mode=$mode", it)
                }
            busy = false
        }
    }

    LaunchedEffect(mode) { page = 1; load(1) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = { if (mode != "author") { mode = "author"; selectedAuthor = null; workInfo = null } }) {
                Text(if (mode == "author") "· 画师" else "画师")
            }
            Button(onClick = { if (mode != "work") { mode = "work"; selectedAuthor = null; workInfo = null } }) {
                Text(if (mode == "work") "· 作品" else "作品")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(if (mode == "author") "搜索画师" else "搜索作品") },
                singleLine = true,
                modifier = Modifier.width(300.dp),
            )
            Button(enabled = !busy, onClick = { page = 1; load(1) }) { Text("搜索") }
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // 加宽：点了画师就显示他的作品；点了作品就显示作品信息
        selectedAuthor?.let { a ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("画师：${a.name ?: a.id}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = {
                    selectedAuthor = null
                    workInfo = null
                    load(1)
                }) { Text("返回列表") }
            }
        }
        workInfo?.let { (title, related) ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("作品：$title", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (related.isNotEmpty()) {
                    Text("相关作品 ${related.size} 部（见下方）", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { workInfo = null }) { Text("关闭") }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (mode == "author") {
                items(authors, key = { it.id }) { a ->
                    Column(
                        modifier = Modifier.clickable {
                            selectedAuthor = a
                            workInfo = null
                            status = "正在取 ${a.name ?: a.id} 的作品…"
                            scope.launch {
                                runCatching { repository.creatorWorksByAuthor(a.id) }
                                    .onSuccess { r ->
                                        works = r.items
                                        total = r.total
                                        status = "${a.name ?: a.id} 的作品 ${r.items.size} 部"
                                    }
                                    .onFailure {
                                        if (it is CancellationException) return@onFailure
                                        status = "取该作者作品失败：${it.message}"
                                    }
                            }
                        },
                    ) {
                        val avatar = remember(a.avatar) { a.avatar }
                        val bmp = rememberRemoteImage(avatar)
                        Box(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (bmp != null) {
                                Image(bmp, contentDescription = a.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text((a.name ?: "?").take(1), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Text(a.name ?: a.id, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp), maxLines = 2)
                        a.updateDate?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            } else {
                items(works, key = { it.id }) { w ->
                    Column(
                        modifier = Modifier.clickable {
                            status = "正在取作品信息…"
                            scope.launch {
                                runCatching { repository.creatorWorkInfo(w.id) }
                                    .onSuccess { info ->
                                        workInfo = (info.title ?: w.title ?: w.id) to info.relatedWorks
                                        if (info.relatedWorks.isNotEmpty()) works = info.relatedWorks
                                        status = "作品信息已取到（相关 ${info.relatedWorks.size} 部）"
                                    }
                                    .onFailure {
                                        if (it is CancellationException) return@onFailure
                                        status = "取作品信息失败：${it.message}"
                                    }
                            }
                        },
                    ) {
                        val bmp = rememberRemoteImage(w.image)
                        Box(
                            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (bmp != null) {
                                Image(bmp, contentDescription = w.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                        Text(w.title ?: w.id, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp), maxLines = 2)
                        Text(
                            listOfNotNull(w.authorName, w.date).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            if ((mode == "author" && authors.size < total) || (mode == "work" && works.size < total)) {
                item {
                    Button(enabled = !busy, onClick = { load(page + 1) }) { Text("加载更多") }
                }
            }
        }
    }
}
