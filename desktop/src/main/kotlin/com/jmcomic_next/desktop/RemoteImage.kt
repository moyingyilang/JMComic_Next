package com.jmcomic_next.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.jmcomic_next.lyqs.data.image.ImageUnscramble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * 桌面端的网络图片加载（2.0.0）。
 *
 * 为什么不直接引 Coil：这里只需要「下载 → 解码 → 交给 Compose」三件事，
 * 用 Skiko（Compose Desktop 自带）与 JDK 就能做到，不增加依赖。
 * 缓存只有内存一层：同一次会话里重复出现的图不会重复下载，退出即失效。
 * 等需要磁盘缓存与请求取消时再换 Coil 更划算。
 */
object RemoteImage {

    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    /** 已缓存时立即返回，否则 null。 */
    fun cached(url: String): ImageBitmap? = cache[url]

    /**
     * 普通加载（不需要反切片）。用 Skiko 解码：少一次像素拷贝。
     */
    suspend fun load(url: String): ImageBitmap? {
        cache[url]?.let { return it }
        val bytes = download(url) ?: return null
        val bitmap = runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull() ?: return null
        cache[url] = bitmap
        return bitmap
    }

    /**
     * 需要反切片时的加载路径。
     *
     * 解码改用 ImageIO：反切片算法在**像素数组**上工作，而
     * `BufferedImage.getRGB/setRGB` 正好是「位图 ↔ ARGB 数组」的现成通道，
     * 不必去碰 Skiko 的内部结构。算法本体在 :shared 的 [ImageUnscramble] 里，两端共用。
     */
    suspend fun loadScrambled(url: String, aid: Int, page: String): ImageBitmap? {
        cache[url]?.let { return it }
        val bytes = download(url) ?: return null
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val src = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@runCatching null
                val w = src.width
                val h = src.height
                val pixels = IntArray(w * h)
                src.getRGB(0, 0, w, h, pixels, 0, w)
                val fixed = ImageUnscramble.unscramble(pixels, w, h, aid, page)
                val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                out.setRGB(0, 0, w, h, fixed, 0, w)
                out.toComposeImageBitmap()
            }.getOrNull()
        } ?: return null
        cache[url] = bitmap
        return bitmap
    }

    /** 下载字节。图片接口对 UA 有要求（缺 UA 会被直接拒绝）。 */
    private suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (jmcomic-next-desktop)")
            conn.inputStream.use { it.readBytes() }
        }.getOrNull()
    }
}

/** 按 URL 取图：先给缓存，再异步下载。返回 null 表示"还没有/拿不到"。 */
@Composable
fun rememberRemoteImage(url: String?): ImageBitmap? {
    if (url.isNullOrBlank()) return null
    var bitmap by remember(url) { mutableStateOf(RemoteImage.cached(url)) }
    LaunchedEffect(url) {
        if (bitmap == null) bitmap = RemoteImage.load(url)
    }
    return bitmap
}
