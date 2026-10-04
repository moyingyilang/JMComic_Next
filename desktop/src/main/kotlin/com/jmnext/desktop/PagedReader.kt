package com.jmnext.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jmnext.data.JmRepository
import com.jmnext.data.remote.dto.ReadPayload

/**
 * 横向逐页阅读（桌面端，1.9.x）。**按 Android 的 `PagedReader` 移植**：
 * 用 `HorizontalPager`，每页一张图、适配整屏，左右翻页。
 *
 * **为什么把 `PagerState` 放在外面（受控组件）**：侧栏滑轨要能跳页 —— `onSeek` 需要拿到
 * `pagerState.scrollToPage(...)`。若状态藏在组件内部，外面就够不到它。所以状态由调用方
 * 用 `rememberPagerState` 创建并传进来，本组件只负责渲染与报告当前页。
 *
 * 与 Android 的差异（如实标注，都是桌面特有）：
 * - 键盘翻页（方向键、PageUp/PageDown）在调用方做 —— Android 只有手势；
 * - 图片复用阅读页已有的 `PageItem`，所以加载中/失败/重试的表现与纵向模式**完全一致**。
 */
@Composable
fun PagedReader(
    repository: JmRepository,
    payload: ReadPayload,
    state: PagerState,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val images = payload.images

    // 当前页变化时通知外层（侧栏滑轨与页级进度都要用）
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect { onPageChange(it) }
    }

    HorizontalPager(state = state, modifier = modifier.fillMaxSize()) { page ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PageItem(repository, payload, images[page], page)
        }
    }
}
