package com.jmcomic_next.lyqs.data

import com.jmcomic_next.lyqs.selftune.PageSample

/**
 * 阅读页的自学习采样核心（纯逻辑，无 Android 依赖，因此可以直接单测）。
 *
 * 分工（重要）：**停留时长必须在屏幕级结算，延迟/命中/字节在页级记录**。
 * 因为阅读页的图片组件是**按页实例化**的：分页模式会预组合相邻页、滚动模式会懒加载与回收，
 * 所以"组件存活时间"不等于"用户停留时间"。若拿它当 dwell，`Fitness` 判断快翻/慢读所依据的
 * 中位停留就会被污染 —— 而"快翻加深预取、慢读减小"正是算法的立论基础。
 *
 * 键用 `ReadImage.fileNameStem`：逐页稳定且唯一（不用可能重复的字段当键）。
 */
object PageSampler {
    private class Record(val enteredAt: Long) {
        var latencyMs: Long? = null
        var hitCache: Boolean = false
        var bytes: Long? = null
    }

    private val records = HashMap<String, Record>()

    /** 屏幕级：当前页变成这一页时调用（记下"用户开始看它"的时刻）。 */
    fun onPageEntered(key: String, now: Long) {
        records[key] = Record(now)
    }

    /**
     * 页级：这一页的图到位时调用。命中缓存/预取时延迟记 0（符合 PageSample 的定义）。
     * [bytes] 传 `ImageBytes.diskSize(...)` 的结果，拿不到就是 `null`（**不要传 0**）。
     */
    fun onImageReady(key: String, now: Long, hitCache: Boolean, bytes: Long?) {
        val r = records[key] ?: return
        r.latencyMs = if (hitCache) 0L else (now - r.enteredAt).coerceAtLeast(0L)
        r.hitCache = hitCache
        r.bytes = bytes
    }

    /**
     * 屏幕级：离开这一页时结算成一个样本；没有记录（例如切章）返回 null。
     * 图始终没到位 → 记成失败样本（`failed = true`），由调用方另外告知调参器"这是一次取消"。
     */
    fun settle(key: String, now: Long): PageSample? {
        val r = records.remove(key) ?: return null
        val dwell = (now - r.enteredAt).coerceAtLeast(0L)
        return PageSample(
            latencyMs = r.latencyMs ?: dwell,
            bytes = r.bytes,
            hitCache = r.hitCache,
            failed = r.latencyMs == null,
            dwellMs = dwell,
        )
    }

    /** 切章或退出阅读页时清空，避免把上一话的记录算到下一话。 */
    fun clear() {
        records.clear()
    }

    /** 仅供测试与诊断：当前挂着的记录数。 */
    fun pendingCount(): Int = records.size
}
