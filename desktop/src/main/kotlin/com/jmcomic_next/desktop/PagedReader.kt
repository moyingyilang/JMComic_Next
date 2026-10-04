package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.jmcomic_next.lyqs.data.JmRepository
import com.jmcomic_next.lyqs.data.remote.dto.ReadPayload

/**
 * 横向逐页阅读（桌面端，1.9.x）。**按 Android 的 `PagedReader` 移植**：
 * 用 `HorizontalPager`，每页一张图、适配整屏（`ContentScale.Fit`），左右翻页。
 *
 * 与 Android 的差异（如实标注，都是桌面特有）：
 * - 键盘翻页（左右方向键、PageUp/PageDown）—— Android 只有手势，桌面必须给键盘；
 *   这里先用 pager 自带的拖拽与滚轮，键盘留到下一步一并做（避免一次改太多）；
 * - 图片复用阅读页已有的 `PageItem`（内部可见），因此加载中/失败/重试的表现与纵向模式**完全一致**，
 *   不会出现"两种模式行为不一样"。
 *
 * 与侧栏的联动由调用方负责：本组件只把当前页通过 [onPageChange] 报出去。
 */
@Composable
fun PagedReader(
    repository: JmRepository,
    payload: ReadPayload,
    initialPage: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val images = payload.images
    val lastIndex = (images.size - 1).coerceAtLeast(0)
    val state = rememberPagerState(initialPage = initialPage.coerceIn(0, lastIndex)) { images.size }

    // 当前页变化时通知外层（侧栏滑轨与进度记录要用）
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect { onPageChange(it) }
    }

    HorizontalPager(state = state, modifier = modifier.fillMaxSize()) { page ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PageItem(repository, payload, images[page], page)
        }
    }
}
