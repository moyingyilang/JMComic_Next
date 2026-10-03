package com.jmcomic_next.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.jmcomic_next.lyqs.data.prefs.ReadProgressStore
import com.jmcomic_next.lyqs.data.remote.dto.AlbumDetail
import com.jmcomic_next.lyqs.data.remote.dto.ListItem
import com.jmcomic_next.lyqs.data.remote.dto.SeriesItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 作品详情页（桌面端，1.9.x 全量移植中）。
 *
 * 已有：封面、标题、作者、标签、页数、简介、章节列表（倒序）、无章节作品直接开读、
 * 收藏/取消收藏、相关作品。
 *
 * 关于收藏：`toggleFavorite` 是**对服务端的写操作**，只在用户按下按钮时调用，
 * 页面加载时绝不自动触发。开发期间我没有用自己的验证去点它（用的是用户账号），
 * 所以"收藏成功"这条路径的真实结果未经我验证，界面显示服务端返回的原文。
 *
 * 尚未做：评论入口、追更、下载（在《PARITY.md》里挂着）。
 */
@Composable
fun DetailScreen(
    repository: JmRepository,
    comicId: String,
    onBack: () -> Unit,
    // 第二个参数是本章节的顺序（从旧到新），阅读页据此判断上一话/下一话
    onOpenChapter: (SeriesItem, List<String>) -> Unit,
    progress: ReadProgressStore,
    onOpenComic: (ListItem) -> Unit,
) {
    var detail by remember(comicId) { mutableStateOf<AlbumDetail?>(null) }
    var status by remember(comicId) { mutableStateOf("正在加载作品…") }
    var favorite by remember(comicId) { mutableStateOf(false) }
    // 追更状态：只做本地翻转。**初始态没有从接口取**（详情接口不下发这个字段，
    // Android 端为此单独请求一次）；所以第一次进来按钮一律显示"追更"，
    // 若作品其实已在追更列表里，点一下会变成取消追更。这是已知简化。
    var tracked by remember(comicId) { mutableStateOf(false) }
    var trackBusy by remember { mutableStateOf(false) }
    var favMessage by remember(comicId) { mutableStateOf<String?>(null) }
    var favBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(comicId) {
        System.err.println("[详情] 进入页面 comicId=$comicId")
        runCatching { repository.album(comicId) }
            .onSuccess {
                detail = it
                favorite = it.isFavorite
                status = "${it.name.orEmpty()} · ${it.series.size} 话 · 相关 ${it.relatedList.size} 部"
                System.err.println("[详情] 数据到达：${it.name} 作者${it.author.size}人 标签${it.tags.size}个 章节${it.series.size}话 相关${it.relatedList.size}部")
            }
            .onFailure {
                if (it is CancellationException) return@onFailure
                status = "加载失败：${it.message}"
                it.printStackTrace()
            }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Text(status, style = MaterialTheme.typography.titleMedium)
        }

        val d = detail ?: return@Column
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // 左栏：封面与基本信息
            Column(
                modifier = Modifier.width(280.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val coverUrl = remember(d.id) { runCatching { repository.coverUrl(d.id) }.getOrNull() }
                val bitmap = rememberRemoteImage(coverUrl)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (bitmap != null) {
                        Image(bitmap, contentDescription = d.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !favBusy,
                        onClick = {
                            favBusy = true
                            favMessage = null
                            scope.launch {
                                runCatching { repository.toggleFavorite(d.id) }
                                    .onSuccess {
                                        favorite = !favorite
                                        favMessage = if (favorite) "已加入收藏" else "已取消收藏"
                                        System.err.println("[详情] 收藏切换：$favMessage")
                                    }
                                    .onFailure {
                                        if (it is CancellationException) return@onFailure
                                        favMessage = "操作失败：${it.message}"
                                        System.err.println("[详情] $favMessage")
                                    }
                                favBusy = false
                            }
                        },
                    ) { Text(if (favorite) "已收藏" else "收藏") }

                    // 未登录不显示：否则点了必然失败，还得多跳一次登录页（照抄 Android 端）
                    if (repository.auth.isLoggedIn) {
                        Button(
                            enabled = !trackBusy,
                            onClick = {
                                trackBusy = true
                                scope.launch {
                                    runCatching { repository.toggleTracking(d.id) }
                                        .onSuccess {
                                            tracked = !tracked
                                            Log.line("详情", "追更切换：" + (if (tracked) "已追更" else "已取消追更"))
                                        }
                                        .onFailure {
                                            if (it is CancellationException) return
                                            Log.error("详情", "追更切换失败", it)
                                        }
                                    trackBusy = false
                                }
                            },
                        ) { Text(if (tracked) "已追更" else "追更") }
                    }

                    if (!repository.auth.isLoggedIn) {
                        Text(
                            "未登录",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
                favMessage?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                // 继续阅读：进度存在本地（章节粒度）。只有"上次读的章节确实还在这一话列表里"
                // 才显示 —— 否则会指向一个不存在的章节，点一下就报错（与 Android 端同一条规矩）。
                val orderedIds = d.series.map { it.id }
                val lastRead = remember(comicId, d.series.size) { progress.lastChapterId(comicId) }
                val resumeIndex = lastRead?.let { orderedIds.indexOf(it) } ?: -1
                if (d.series.isNotEmpty() && resumeIndex >= 0) {
                    val target = d.series[resumeIndex]
                    Button(
                        onClick = { onOpenChapter(target, orderedIds) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("继续阅读 · 第 ${target.sort ?: (resumeIndex + 1).toString()} 话")
                    }
                }


                InfoLine("标题", d.name.orEmpty())
                InfoLine("作者", d.author.joinToString(" / "))
                if (d.tags.isNotEmpty()) InfoLine("标签", d.tags.joinToString(" "))
                InfoLine("页数", d.totalPhotos.toString())
                if (!d.description.isNullOrBlank()) {
                    Text(d.description, style = MaterialTheme.typography.bodySmall)
                }

                // 相关作品（横向滚动）：接口直接给的就是 ListItem，可直接用现成卡片
                if (d.relatedList.isNotEmpty()) {
                    Text("相关作品", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        d.relatedList.forEach { item ->
                            Box(Modifier.width(140.dp)) {
                                ComicCover(repository, item) { onOpenComic(item) }
                            }
                        }
                    }
                }
            }

            // 右栏：章节列表（倒序，最新的在最上面）
            if (d.series.isEmpty()) {
                // 无章节作品（单话同人本很常见）：作品 id 自身就是可读单元。
                // 与 Android 端一致 —— 不补的话这类作品点了没反应。
                LazyColumn(modifier = Modifier.fillMaxSize().padding(start = 16.dp)) {
                    item {
                        Text(
                            text = "开始阅读",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChapter(SeriesItem(id = d.id, sort = null, name = null), listOf(d.id)) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                        )
                        Text("该作品没有章节列表，直接阅读整本", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(d.series.reversed(), key = { it.id }) { chapter ->
                        Text(
                            text = "第 ${chapter.sort ?: "?"} 话" + (chapter.name?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChapter(chapter, d.series.map { it.id }) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
