package com.jmcomic_next.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
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
import com.jmcomic_next.lyqs.data.prefs.ReaderMode
import com.jmcomic_next.lyqs.data.remote.dto.SeriesItem
import com.jmcomic_next.lyqs.data.prefs.ReadProgressStore
import com.jmcomic_next.lyqs.data.remote.dto.ReadImage
import com.jmcomic_next.lyqs.data.remote.dto.ReadPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 阅读页（桌面端，1.9.x 全量移植中）。
 *
 * 形式是**纵向连续滚动**：桌面上鼠标滚轮纵向翻比左右翻自然，也不用处理
 * "一屏放不下整页"的分段逻辑（那在手机上是必需的，在桌面上是负担）。
 *
 * 这一版补上三件"能不能真用起来"的事：
 *  1. 上一话 / 下一话 —— 章节顺序由详情页传入（接口下发的是从旧到新）
 *  2. 单页失败可以点一下重试（原来失败就永远停在加载中）
 *  3. 进入章节时记录进度，详情页据此显示"继续阅读"
 *
 * 反切片：服务端把部分漫画整页切成若干条错位重排，必须在显示前还原。
 * 判定与算法都在共享层（needsUnscramble / ImageUnscramble），这里只管调用的时机与缓存。
 */
@Composable
fun ReaderScreen(
    repository: JmRepository,
    progress: ReadProgressStore,
    comicId: String,
    chapterId: String,
    chapterIds: List<String> = emptyList(),
    onBack: () -> Unit,
    onSwitchChapter: (String) -> Unit = {},
    // 评论入口由 Main 传入（阅读页不持有路由）；收藏与点赞在栏内直接调 repository。
    onOpenComments: (String) -> Unit = {},
) {
    var payload by remember(chapterId) { mutableStateOf<ReadPayload?>(null) }
    // 页级进度：桌面端专用（共享层那份是章节粒度，Android 按那个工作）
    val pageProgress = remember { PageProgress(PreferencesKeyValueStore("jm_read_page")) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // 当前页（0 基）：从列表状态派生，滚动时自动更新，给右侧页码栏用
    val currentPage by androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex }
    // 章节选择对话框的数据与开关（按 A 方案：打开时才请求一次 album 拿 series，含话名）
    var series by remember(comicId) { mutableStateOf<List<SeriesItem>>(emptyList()) }
    var pickerOpen by remember(comicId) { mutableStateOf(false) }
    // 阅读模式：复用共享层的 ReaderMode（Scroll = 纵向连续滚动 / Page = 横向逐页适配整屏）。
    // 这一步只做状态与持久化，翻页行为在下一步换成 PagedReader。
    val modePrefs = remember { PreferencesKeyValueStore("jm_reader_mode") }
    var mode by remember {
        mutableStateOf(if (modePrefs.getString("mode", null) == "page") ReaderMode.Page else ReaderMode.Scroll)
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by remember(chapterId) { mutableStateOf("正在加载章节…") }
    var retryToken by remember(chapterId) { mutableStateOf(0) }

    val index = remember(chapterId, chapterIds) { chapterIds.indexOf(chapterId) }
    val prevId = if (index > 0) chapterIds.getOrNull(index - 1) else null
    val nextId = if (index >= 0 && index < chapterIds.lastIndex) chapterIds.getOrNull(index + 1) else null

    LaunchedEffect(chapterId, retryToken) {
        payload = null
        runCatching { repository.read(chapterId) }
            .onSuccess {
                payload = it
                status = "${it.images.size} 页"
                runCatching { progress.record(comicId, chapterId) }
                System.err.println("[阅读] 已加载 $status（章节 ${index + 1}/${chapterIds.size}）")
            }
            .onFailure {
                if (it is CancellationException) return@onFailure
                status = "加载失败：${it.message}（点“重试”）"
                System.err.println("[阅读] $status")
            }
    }

    // 恢复到上次读到的页：等章节数据到位后再滚（此前只记到章节，重进会从头开始）
    androidx.compose.runtime.LaunchedEffect(payload) {
        if (payload != null) {
            val saved = pageProgress.lastPage(comicId, chapterId)
            if (saved > 0) {
                Log.line("阅读", "恢复到第 ${saved + 1} 页")
                listState.scrollToItem(saved)
            }
        }
    }

    // 滚动时记页（只在页码变化时写，避免每帧都落盘）
    androidx.compose.runtime.LaunchedEffect(listState, chapterId) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { pageProgress.record(comicId, chapterId, it) }
    }

    // 章节选择对话框（按 Android 的 ChapterPickerDialog 移植，见该文件注释）
    if (pickerOpen && series.isNotEmpty()) {
        ChapterPickerDialog(
            series = series,
            currentChapterId = chapterId,
            onPick = { id ->
                pickerOpen = false
                onSwitchChapter(id)
            },
            onDismiss = { pickerOpen = false },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Text(status, style = MaterialTheme.typography.titleMedium)
            if (chapterIds.isNotEmpty() && index >= 0) {
                Text(
                    "第 ${index + 1} / ${chapterIds.size} 话",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (payload == null) {
                TextButton(onClick = { retryToken += 1 }) { Text("重试") }
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            }
        }

        val p = payload ?: return@Column

        // 横向翻页用的 pager 状态。放在这里（payload 可用之后）是因为页数取自 p.images.size；
        // 用全限定名调用，避免再动 import。（第 3 步接线时由 PagedReader 使用。）
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = currentPage.coerceIn(0, (p.images.size - 1).coerceAtLeast(0)),
        ) { p.images.size }

        // 横向模式也要记页：上面那条 flow 用的是 listState，横向下它不动。
        // 只在 Page 模式写入，避免两种模式互相覆盖对方的位置。
        androidx.compose.runtime.LaunchedEffect(pagerState, chapterId, mode) {
            androidx.compose.runtime.snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { if (mode == ReaderMode.Page) pageProgress.record(comicId, chapterId, it) }
        }

        // ── 预加载下一话 ──
        // 只预取"下一话的图片列表"（一次 read 请求），**不整话下载图片** ——
        // 整话预下载会把内存与流量放大几十倍，这里刻意不做；代价就是翻到下一话时
        // 首页图片仍要现取，但省掉了一次"章节信息"请求的等待。
        // 触发点：本话读到 60% 之后；进入该章节只跑一次，来回滚动不会反复请求。
        var prefetched by remember(chapterId) { mutableStateOf(false) }
        val prefetchAt = (p.images.size * 6) / 10
        LaunchedEffect(chapterId, currentPage, nextId) {
            if (!prefetched && nextId != null && currentPage >= prefetchAt) {
                prefetched = true
                val t0 = System.currentTimeMillis()
                runCatching { repository.read(nextId) }
                    .onSuccess {
                        Log.line(
                            "阅读",
                            "已预加载下一话信息（用时 ${System.currentTimeMillis() - t0} ms，图片 ${it.images.size} 张）",
                        )
                    }
                    .onFailure {
                        if (it is CancellationException) return@onFailure
                        // 预加载失败不影响当前阅读，只记日志
                        Log.error("阅读", "预加载下一话失败（不影响当前阅读）", it)
                    }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f)) {
        // 两种模式共用侧栏：只替换内容区（LazyColumn ↔ PagedReader），PageRail 留在外面
        if (mode == ReaderMode.Page) {
            PagedReader(
                repository = repository,
                payload = p,
                state = pagerState,
                onPageChange = { },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            // 页面之间不留空隙：漫画是连续的，8dp 间距会把跨页画面割断
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            itemsIndexed(p.images, key = { _, img -> img.image }) { idx, image ->
                PageItem(repository, p, image, idx)
            }
            item {
                // 底部也放一次"下一话"：连续滚动读完一话后，手停在这里
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(enabled = nextId != null, onClick = { nextId?.let(onSwitchChapter) }) {
                        Text(if (nextId != null) "下一话" else "已经是最后一话")
                    }
                    if (nextId != null) {
                        Box(Modifier.padding(start = 12.dp)) {
                            TextButton(onClick = onBack) { Text("回详情页") }
                        }
                    }
                }
            }
        }
        }

        // 右侧竖排页码栏（仿 Android 版）：当前页 / 总页数 + 竖向进度
        // 与 Android 的 PageSeekRow 同一套控件（只竖过来），见 PageRail.kt
        PageRail(
            current = if (mode == ReaderMode.Page) pagerState.currentPage else currentPage,
            total = p.images.size,
            onSeek = { page ->
                // 跳页用 scrollToItem：直接定位，不做动画（长列表做动画会又慢又抖）
                Log.line("阅读", "跳页 → 第 ${page + 1} 页")
                scope.launch { if (mode == ReaderMode.Page) pagerState.scrollToPage(page) else listState.scrollToItem(page) }
            },
            hasPrev = prevId != null,
            hasNext = nextId != null,
            onPrev = { prevId?.let(onSwitchChapter) },
            onNext = { nextId?.let(onSwitchChapter) },
            onOpenComments = { onOpenComments(comicId) },
            onToggleMode = {
                // 切换模式前先把位置同步到对方：否则横向翻到第 80 页、切回纵向会回到旧位置
                // （两种模式索引同一张图片列表，语义相同，只是载体不同，所以同步的是同一个下标）
                scope.launch {
                    if (mode == ReaderMode.Page) {
                        listState.scrollToItem(pagerState.currentPage.coerceIn(0, (p.images.size - 1).coerceAtLeast(0)))
                    } else {
                        pagerState.scrollToPage(currentPage.coerceIn(0, (p.images.size - 1).coerceAtLeast(0)))
                    }
                }
                // 与 Android 同一个枚举、同一个语义；桌面端额外把选择存下来
                mode = if (mode == ReaderMode.Scroll) ReaderMode.Page else ReaderMode.Scroll
                modePrefs.putString("mode", if (mode == ReaderMode.Page) "page" else "scroll")
                Log.line("阅读", "阅读模式切换为 " + (if (mode == ReaderMode.Page) "横向翻页" else "纵向滚动"))
            },
            onOpenPicker = {
                // 章节选择：点一次才请求 album（A 方案），拿到含话名的 series 再开对话框
                scope.launch {
                    runCatching { repository.album(comicId) }
                        .onSuccess {
                            series = it.series
                            pickerOpen = true
                        }
                        .onFailure {
                            if (it is CancellationException) return@onFailure
                            Log.error("阅读", "打开章节选择失败", it)
                        }
                }
            },
            onToggleFavorite = {
                scope.launch {
                    runCatching { repository.toggleFavorite(comicId) }
                        .onSuccess { Log.line("阅读", "收藏状态已切换（写操作，未验证）") }
                        .onFailure {
                            if (it is CancellationException) return@onFailure
                            Log.error("阅读", "切换收藏失败", it)
                        }
                }
            },
            onToggleLike = {
                scope.launch {
                    runCatching { repository.like(comicId) }
                        .onSuccess { Log.line("阅读", "已点赞（写操作，未验证）") }
                        .onFailure {
                            if (it is CancellationException) return@onFailure
                            Log.error("阅读", "点赞失败", it)
                        }
                }
            },
        )
        }
    }
}

@Composable
internal fun PageItem(repository: JmRepository, payload: ReadPayload, image: ReadImage, index: Int) {
    val url = image.image
    val needsUnscramble = remember(url) {
        runCatching { repository.needsUnscramble(url, payload.id, payload.scrambleId) }.getOrDefault(false)
    }
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(RemoteImage.cached(url)) }
    var failed by remember(url) { mutableStateOf(false) }
    var attempt by remember(url) { mutableStateOf(0) }

    LaunchedEffect(url, attempt) {
        if (bitmap == null) {
            failed = false
            val loaded = if (needsUnscramble) {
                RemoteImage.loadScrambled(url, payload.id, image.fileNameStem)
            } else {
                RemoteImage.load(url)
            }
            bitmap = loaded
            failed = loaded == null
            if (loaded == null) System.err.println("[阅读] 第 ${index + 1} 页加载失败：$url")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()


            .then(if (bitmap == null) Modifier.height(220.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        when {
            b != null -> Image(
                bitmap = b,
                contentDescription = "第 ${index + 1} 页",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )

            // 失败可以点一下重试：原来失败就永远停在"加载中"，读者只能退出去再进来
            failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { attempt += 1 }.padding(24.dp),
            ) {
                Text("第 ${index + 1} 页加载失败", style = MaterialTheme.typography.bodyMedium)
                Text("点这里重试", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            else -> Text("第 ${index + 1} 页 加载中…", style = MaterialTheme.typography.labelSmall)
        }
    }
}
