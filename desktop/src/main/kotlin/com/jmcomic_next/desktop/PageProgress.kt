package com.jmcomic_next.desktop

import com.jmcomic_next.lyqs.data.prefs.KeyValueStore

/**
 * 页级阅读进度（桌面端专用，1.9.x）。
 *
 * 为什么放在桌面端而不是共享层：共享层的 `ReadProgressStore` 是**章节粒度**的，
 * Android 端按那个粒度工作（详情页据此显示"继续阅读"）。这里加的是"读到第几页"，
 * 只有桌面端需要 —— 放进共享层会连带改动 Android 的行为，而那边不需要这个信息。
 *
 * 存储形式：key = "comicId|chapterId"，value = 页码（0 基）。用 `|` 分隔是因为
 * 两个 id 都是数字、不会包含它；换成 JSON 反而多一层解析和出错面。
 *
 * 写得很频繁（滚动时），所以只在**页码变化**时写，而不是每帧都写。
 */
class PageProgress(private val prefs: KeyValueStore) {

    fun record(comicId: String, chapterId: String, page: Int) {
        prefs.putString(key(comicId, chapterId), page.toString())
    }

    /** 返回 0 基页码；没有记录时返回 0（从头开始）。 */
    fun lastPage(comicId: String, chapterId: String): Int =
        prefs.getString(key(comicId, chapterId), null)?.toIntOrNull()?.coerceAtLeast(0) ?: 0

    fun clear(comicId: String, chapterId: String) = prefs.remove(key(comicId, chapterId))

    private fun key(comicId: String, chapterId: String) = "$comicId|$chapterId"
}
