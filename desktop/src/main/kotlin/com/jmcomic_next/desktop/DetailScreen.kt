package com.jmcomic_next.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.remote.dto.AlbumDetail
import com.jmcomic_next.lyqs.data.remote.dto.SeriesItem

/**
 * 作品详情页（2.0.0 桌面端）。
 *
 * 显示封面、标题、作者、标签、简介与章节列表；点章节进入阅读页。
 *
 * 章节列表按官方客户端的做法**倒序**：读者最常点的是最新一话，
 * 而接口下发的是从旧到新（与 `Detail.tsx` 一致）。
 */
@Composable
fun DetailScreen(
    repository: com.jmcomic_next.lyqs.data.JmRepository,
    comicId: String,
    onBack: () -> Unit,
    onOpenChapter: (SeriesItem) -> Unit,
) {
    var detail by remember(comicId) { mutableStateOf<AlbumDetail?>(null) }
    var status by remember(comicId) { mutableStateOf("正在加载作品…") }

    LaunchedEffect(comicId) {
        System.err.println("[详情] 进入页面 comicId=$comicId")
        runCatching { repository.album(comicId) }
            .onSuccess {
                detail = it
                System.err.println("[详情] 数据到达：${it.name} 作者${it.author.size}人 标签${it.tags.size}个 章节${it.series.size}话")
                status = "${it.name.orEmpty()} · ${it.series.size} 话"
            }
            .onFailure { if (it is kotlinx.coroutines.CancellationException) return@onFailure
                status = "加载失败：${it.message}"
                it.printStackTrace()
                System.err.println("[界面] $status")
            }
    }

    Column(Modifier.fillMaxSize()) {
        System.err.println("[详情] 开始组合，detail=${detail != null}")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Text(status, style = MaterialTheme.typography.titleMedium)
        }

        val d = detail ?: return@Column
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // 左栏：封面与基本信息
            Column(
                modifier = Modifier.width(260.dp).verticalScroll(rememberScrollState()),
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
                InfoLine("标题", d.name.orEmpty())
                InfoLine("作者", d.author.joinToString(" / "))
                if (d.tags.isNotEmpty()) InfoLine("标签", d.tags.joinToString(" "))
                InfoLine("页数", d.totalPhotos.toString())
                if (!d.description.isNullOrBlank()) {
                    Text(d.description, style = MaterialTheme.typography.bodySmall)
                }
            }

            // 无章节作品（单话同人本很常见）：作品 id 自身就是可读单元。
            // 这一条与 Android 端一致 —— 不补的话这类作品点了没反应，
            // 表现成"详情页打开但读不了"。
            if (d.series.isEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(start = 16.dp)) {
                    item {
                        Text(
                            text = "开始阅读",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChapter(SeriesItem(id = d.id, sort = null, name = null)) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                        )
                        Text("该作品没有章节列表，直接阅读整本", style = MaterialTheme.typography.labelSmall)
                    }
                }
                return@Row
            }

            // 右栏：章节列表（倒序，最新的在最上面）
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
                            .clickable { onOpenChapter(chapter) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                    )
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
