package com.jmcomic_next.desktop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.remote.dto.ListItem
import com.jmcomic_next.lyqs.data.remote.dto.PromoteSection
import kotlinx.coroutines.CancellationException

/**
 * 首页的推荐分区（桌面端，1.9.x 全量移植中）。
 *
 * 与 Android 端的结构一致：`promote()` 一次返回若干分区，每个分区自带一批作品。
 * 这里每个分区渲染成「标题 + 横向滚动的封面」；Android 端每个分区还有「更多」，
 * 走 `promoteList(section.id, page)` 看完整列表 —— 桌面这边**尚未做**，不做假按钮。
 *
 * 桌面上横向滚动比手机上更自然（有滚轮与触控板横滑），所以整块做成横排。
 */
@Composable
fun PromoteHeader(repository: JmRepository, onOpenComic: (ListItem) -> Unit) {
    var sections by remember { mutableStateOf<List<PromoteSection>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { repository.promote() }
            .onSuccess {
                // 首页只放前 4 个分区：再多会把最新列表挤到很下面，
                // 而首页的主要用途还是"看最新"。
                sections = it.filter { s -> s.content.isNotEmpty() }.take(4)
                System.err.println("[首页] 推荐分区 ${it.size} 个，展示 ${sections.size} 个")
            }
            .onFailure {
                if (it is CancellationException) return@onFailure
                // 推荐拉不到不该影响首页显示最新 —— 静默记一行，界面留空
                System.err.println("[首页] 推荐分区加载失败：${it.message}")
            }
    }

    if (sections.isEmpty()) return

    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        sections.forEach { section ->
            Text(
                text = section.title ?: "(未命名分区)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                section.content.forEach { item ->
                    Box(Modifier.width(140.dp)) {
                        ComicCover(repository, item) { onOpenComic(item) }
                    }
                }
            }
        }
    }
}
