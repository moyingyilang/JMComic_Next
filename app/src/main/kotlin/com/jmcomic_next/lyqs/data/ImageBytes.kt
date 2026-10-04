package com.jmcomic_next.lyqs.data

import coil3.PlatformContext
import coil3.SingletonImageLoader

/**
 * 取"这张图实际下载了多少字节"。
 *
 * 为什么要单独一个入口：Android 用 Coil 显示图片，而 Coil 的公开钩子拿不到传输字节数
 * （`SuccessResult` 里没有、`coil3.fetch.FetchResult` 是空标记接口、`EventListener` 的方法签名里也没有 ——
 * 已用 `javap` 核实）。但 `SuccessResult` 带 `diskCacheKey`，而 `DiskCache` 能打开快照拿到落盘文件，
 * 于是**落盘文件的真实大小**就是可用的字节数。
 *
 * 与"传输字节数"的差别要讲清：这是 Coil 落盘后的文件大小（含其缓存写入的内容），不是网络栈的计数。
 * 对自学习而言这正是我们关心的量（缓存占用/流量都依它），且**比包一层 Fetcher 侵入小**。
 *
 * 拿不到时返回 `null`（共享层的 `PageSample.bytes` 已支持"未知"）—— **绝不返回 0**：
 * 0 的含义是"命中缓存、没下载"，与"这一端拿不到"是两件事，混了会让算法学错。
 */
object ImageBytes {
    fun diskSize(context: PlatformContext, diskCacheKey: String?): Long? {
        if (diskCacheKey == null) return null
        val cache = runCatching { SingletonImageLoader.get(context).diskCache }.getOrNull() ?: return null
        return runCatching {
            cache.openSnapshot(diskCacheKey)?.use { snapshot ->
                cache.fileSystem.metadata(snapshot.data)?.size
            }
        }.getOrNull()
    }
}
