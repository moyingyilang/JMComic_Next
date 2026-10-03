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
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import javax.imageio.ImageIO

/**
 * 网络图片加载（桌面端，1.9.x）。
 *
 * 两条解码路径，各有理由：
 *  - 不需要反切片 → Skiko 解码（Skia 编码器齐全，含 WebP），少一次像素拷贝；
 *  - 需要反切片 → 必须拿到**像素数组**才能还原条带，所以用 ImageIO 的 getRGB/setRGB。
 *    **已知隐患**：JDK 的 ImageIO 不支持 WebP。若服务端对需要反切片的图下发 WebP，
 *    这条路径会解码失败 —— 插桩日志里会明确区分"解码返回 null"与"下载失败"，
 *    下一次测试就能确认是不是这个原因。
 *
 * 缓存只有内存一层（同一次会话内不重复下载），退出即失效。
 */
object RemoteImage {

    /**
     * 有上限的 LRU 缓存。
     *
     * 之前是无上限的 ConcurrentHashMap —— 这在阅读页会出事：一话 80 页，
     * 解码后每页约 1MB（WebP 转 PNG 后更大），整话能吃掉几百 MB 内存。
     * 桌面端一次只看得见一两页，超出窗口的图没必要留着。
     *
     * 上限按"张数"而不是"字节"：因为张数好推理，而单张大小的差别（几十 KB 到几 MB）
     * 用字节上限反而会出现"一张大图挤掉全部小图"的抖动。64 张约覆盖两三屏。
     */
    private val cache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 64
        },
    )


    fun cached(url: String): ImageBitmap? = cache[url]

    /** 普通加载（不需反切片）。 */
    suspend fun load(url: String): ImageBitmap? {
        cache[url]?.let { return it }
        val t0 = System.currentTimeMillis()
        val bytes = download(url, "普通") ?: return null
        val bitmap = runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
        if (bitmap == null) {
            Log.error("图片", "Skiko 解码失败 url=$url bytes=${bytes.size}")
            return null
        }
        Log.line("图片", "普通加载成功 ${bitmap.width}x${bitmap.height} ${bytes.size}B ${System.currentTimeMillis() - t0}ms")
        cache[url] = bitmap
        return bitmap
    }

    /** 需要反切片时的加载路径：ImageIO 解码 → 像素还原 → 回填。 */
    suspend fun loadScrambled(url: String, aid: Int, page: String): ImageBitmap? {
        cache[url]?.let { return it }
        val t0 = System.currentTimeMillis()
        val bytes = download(url, "反切片") ?: return null
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                var src = ImageIO.read(ByteArrayInputStream(bytes))
                if (src == null) {
                    // 字节下到了但 ImageIO 解不出 —— 绝大多数是 WebP（JDK 的 ImageIO 不支持）。
                    // 解法：让 Skiko 先解（Skia 编码器齐全，含 WebP），转成 PNG 再交给 ImageIO。
                    // 这样既保留"拿到像素数组做反切片"的前提，又不受 ImageIO 支持的格式限制。
                    Log.line("图片", "ImageIO 解不出，改用 Skiko 转码 url=$url bytes=${bytes.size} 头部=${bytes.take(12).joinToString(" ") { b -> "%02x".format(b) }}")
                    val skia = Image.makeFromEncoded(bytes)
                    val png = skia.encodeToData(EncodedImageFormat.PNG, 100)?.bytes
                    if (png == null) {
                        Log.error("图片", "Skiko 也无法转码（连 Skia 都解不出）url=$url")
                        return@runCatching null
                    }
                    Log.line("图片", "Skiko 转码为 PNG 成功 ${png.size}B（原 ${bytes.size}B）")
                    src = ImageIO.read(ByteArrayInputStream(png))
                    if (src == null) {
                        Log.error("图片", "转成 PNG 后 ImageIO 仍解不出（异常情况，请把这条日志发我）url=$url")
                        return@runCatching null
                    }
                }
                val w = src.width
                val h = src.height
                val pixels = IntArray(w * h)
                src.getRGB(0, 0, w, h, pixels, 0, w)
                val fixed = ImageUnscramble.unscramble(pixels, w, h, aid, page)
                val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                out.setRGB(0, 0, w, h, fixed, 0, w)
                out.toComposeImageBitmap()
            }.onFailure { Log.error("图片", "反切片解码异常 url=$url", it) }.getOrNull()
        } ?: return null
        Log.line("图片", "反切片成功 ${bitmap.width}x${bitmap.height} ${bytes.size}B ${System.currentTimeMillis() - t0}ms")
        cache[url] = bitmap
        return bitmap
    }

    /** 下载并记录状态码、字节数、异常 —— 失败原因的绝大多数都在这里。 */
    private suspend fun download(url: String, kind: String): ByteArray? = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        try {
            conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (jmcomic-next-desktop)")
            val code = conn.responseCode
            if (code != 200) {
                Log.error("图片", "[$kind] HTTP $code ${conn.responseMessage} ${System.currentTimeMillis() - t0}ms url=$url")
                return@withContext null
            }
            val bytes = conn.inputStream.use { it.readBytes() }
            Log.line("图片", "[$kind] 下载 ${bytes.size}B HTTP $code ${System.currentTimeMillis() - t0}ms")
            bytes
        } catch (t: Throwable) {
            // 超时、连接被拒、DNS、TLS 等都在这里被区分出来
            Log.error("图片", "[$kind] 下载异常 ${System.currentTimeMillis() - t0}ms url=$url", t)
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }
}

/** 按 URL 取图：先给缓存，再异步下载。 */
@Composable
fun rememberRemoteImage(url: String?): ImageBitmap? {
    if (url.isNullOrBlank()) return null
    var bitmap by remember(url) { mutableStateOf(RemoteImage.cached(url)) }
    LaunchedEffect(url) {
        if (bitmap == null) bitmap = RemoteImage.load(url)
    }
    return bitmap
}
