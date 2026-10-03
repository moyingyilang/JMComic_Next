package com.jmcomic_next.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.prefs.ReadProgressStore
import com.jmcomic_next.lyqs.data.remote.dto.ReadImage
import com.jmcomic_next.lyqs.data.remote.dto.ReadPayload

/**
 * 阅读页（2.0.0 桌面端第一版）。
 *
 * 形式选**纵向连续滚动**而不是翻页：桌面端用鼠标滚轮纵向翻比左右翻自然，
 * 而且不用处理"一屏放不下整页"的分段逻辑（那种逻辑在手机上是必需的，在桌面上是负担）。
 *
 * 反切片：服务端把部分漫画整页切成若干条上下错位重排，必须在显示前还原。
 * 判定与算法都在共享层（needsUnscramble / ImageUnscramble），这里只负责调用的时机与缓存。
 */
@Composable
fun ReaderScreen(
    repository: JmRepository,
    progress: ReadProgressStore,
    comicId: String,
    chapterId: String,
    onBack: () -> Unit,
) {
    var payload by remember(chapterId) { mutableStateOf<ReadPayload?>(null) }
    var status by remember(chapterId) { mutableStateOf("正在加载章节…") }

    LaunchedEffect(chapterId) {
        runCatching { repository.read(chapterId) }
            .onSuccess {
                payload = it
                status = "${it.images.size} 页"
                // 记录阅读进度：详情页下次显示时能标出"读到哪一话"
                runCatching { progress.record(comicId, chapterId) }
                System.err.println("[阅读] 已加载 $status（需反切片: ${it.scrambleId}）")
            }
            .onFailure {
                status = "加载失败：${it.message}"
                System.err.println("[阅读] $status")
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
        val p = payload ?: return@Column
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(p.images, key = { _, img -> img.image }) { index, image ->
                PageItem(repository, p, image, index)
            }
        }
    }
}

@Composable
private fun PageItem(repository: JmRepository, payload: ReadPayload, image: ReadImage, index: Int) {
    val url = image.image
    val needsUnscramble = remember(url) {
        runCatching { repository.needsUnscramble(url, payload.id, payload.scrambleId) }.getOrDefault(false)
    }
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(RemoteImage.cached(url)) }

    LaunchedEffect(url) {
        if (bitmap == null) {
            bitmap = if (needsUnscramble) {
                RemoteImage.loadScrambled(url, payload.id, image.fileNameStem)
            } else {
                RemoteImage.load(url)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (bitmap == null) Modifier.height(240.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        if (b != null) {
            Image(
                bitmap = b,
                contentDescription = "第 ${index + 1} 页",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        } else {
            Text("第 ${index + 1} 页 加载中…", style = MaterialTheme.typography.labelSmall)
        }
    }
}
