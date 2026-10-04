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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
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
     * 有上限的 LRU 缓存，**按总字节数**限制（256MB）。
     *
     * 从前按张数限 64 张。而一张 2116x3037 的解码位图约 25MB（宽 x 高 x 4 字节），
     * 64 张的上限就是约 1.6GB —— 阅读时前后翻页很快把缓存填满，直接导致系统换页、
     * 整机变慢（用户报过"宿主机 SoC 空转"与"应用连 API 都超时"，两件事同源）。
     * 按字节算才给出真实的内存上限：大图多占、小图少占，逐出仍按最久未使用。
     */
    private const val CACHE_MAX_BYTES = 256L * 1024 * 1024

    /** 一张解码位图占的内存：宽 x 高 x 4 字节（ARGB）。 */
    private fun bitmapBytes(b: ImageBitmap): Long = b.width.toLong() * b.height * 4

    private val cache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {
            private var bytes = 0L

            override fun put(key: String, value: ImageBitmap): ImageBitmap? {
                val old = super.put(key, value)
                bytes += bitmapBytes(value)
                if (old == null) {
                    // 新键：只加不减
                } else {
                    bytes -= bitmapBytes(old)
                }
                // accessOrder = true 时，entries 的迭代顺序就是"最久未使用在前"，
                // 所以从头逐出即可，不必自己维护使用顺序。
                val it = entries.iterator()
                while (bytes > CACHE_MAX_BYTES && it.hasNext()) {
                    val e = it.next()
                    bytes -= bitmapBytes(e.value)
                    it.remove()
                }
                return old
            }
        },
    )


    fun cached(url: String): ImageBitmap? = cache[url]

    /** 每个 URL 上次真正下载的字节数（命中缓存则没有记录，天然为 0）。供自学习采样查询。 */
    private val sizes = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun downloadedSize(url: String): Long = sizes[url] ?: 0L

    /** 一次加载的结果：图 + **本次真正下载的字节数**（命中缓存时为 0）。 */
    data class Loaded(val bitmap: ImageBitmap?, val bytes: Long)

    /**
     * 带字节数的普通加载。阅读页用它喂自学习的样本（[com.jmcomic_next.lyqs.selftune.PageSample] 要 bytes）。
     *
     * 为什么单独一个方法而不是加个可变字段：预取与显示会**并发**调它，共享一个 "last" 值会取到别人的数字，
     * 那种采样比不采样更糟。
     */
    suspend fun loadSized(url: String): Loaded {
        cache[url]?.let { return Loaded(it, 0L) }
        val t0 = System.currentTimeMillis()
        val bytes = download(url, "普通") ?: return Loaded(null, 0L)
        val bitmap = runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
        if (bitmap == null) {
            Log.error("图片", "Skiko 解码失败 url=$url bytes=${bytes.size}")
            return Loaded(null, bytes.size.toLong())
        }
        sizes[url] = bytes.size.toLong()
        Log.line("图片", "普通加载成功 ${bitmap.width}x${bitmap.height} ${bytes.size}B ${System.currentTimeMillis() - t0}ms")
        cache[url] = bitmap
        return Loaded(bitmap, bytes.size.toLong())
    }

    /** 普通加载（不需反切片）。原有调用点不变。 */
    suspend fun load(url: String): ImageBitmap? = loadSized(url).bitmap

    /** 需要反切片时的加载路径：ImageIO 解码 → 像素还原 → 回填。 */
    suspend fun loadScrambled(url: String, aid: Int, page: String): ImageBitmap? {
        cache[url]?.let { return it }
        val t0 = System.currentTimeMillis()
        val bytes = download(url, "反切片") ?: return null
        // 先试新路径：用 Skia 自己把各 band 画到画布上完成反切片 ——
        // 不需要把像素取出来（三次取像素的尝试都失败了，见 PARITY.md），
        // 因此省掉了"编码 PNG + ImageIO 再解一次 + getRGB/setRGB"这一大段。
        // 本地实测：该段从约 1700 ms 降到约 82 ms；结果与解码源图逐像素一致。
        // 失败（返回 null）就继续走下面的 PNG 兜底路径，并在日志里写明走了哪条。
        withContext(Dispatchers.IO) { runCatching { unscrambleViaCanvas(bytes, aid, page) }.getOrNull() }
            ?.let {
                Log.line("图片", "反切片：Skia 画布路径 " + it.width + "x" + it.height + " " + bytes.size + "B " + (System.currentTimeMillis() - t0) + "ms")
                cache[url] = it
                return it
            }
        Log.line("图片", "反切片：PNG 兜底路径（画布路径未成功）url=" + url)
        val tDownloaded = System.currentTimeMillis()
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                var src = ImageIO.read(ByteArrayInputStream(bytes))
                if (src == null) {
                    // 字节下到了但 ImageIO 解不出 —— 绝大多数是 WebP（JDK 的 ImageIO 不支持）。
                    // 解法：让 Skiko 先解（Skia 编码器齐全，含 WebP），转成 PNG 再交给 ImageIO。
                    // 这样既保留"拿到像素数组做反切片"的前提，又不受 ImageIO 支持的格式限制。
                    Log.line("图片", "ImageIO 解不出，改用 Skiko 转码 url=$url bytes=${bytes.size} 头部=${bytes.take(12).joinToString(" ") { b -> "%02x".format(b) }}")
                    // 计时：这条路径要走"Skiko 解码 → 编码 PNG → ImageIO 再解码"，
                    // 体积放大 8 倍左右（日志实测中位数 8.21），但耗时一直没量过 ——
                    // 先量再决定要不要改成 Skiko 直读像素。
                    val tTranscode0 = System.currentTimeMillis()
                    val skia = Image.makeFromEncoded(bytes)
                    val png = skia.encodeToData(EncodedImageFormat.PNG, 100)?.bytes
                    if (png == null) {
                        Log.error("图片", "Skiko 也无法转码（连 Skia 都解不出）url=$url")
                        return@runCatching null
                    }
                    Log.line("图片", "Skiko 转码为 PNG 成功 ${png.size}B（原 ${bytes.size}B，耗时 ${System.currentTimeMillis() - tTranscode0} ms）")
                    src = ImageIO.read(ByteArrayInputStream(png))
                    if (src == null) {
                        Log.error("图片", "转成 PNG 后 ImageIO 仍解不出（异常情况，请把这条日志发我）url=$url")
                        return@runCatching null
                    }
                }
                val tDecoded = System.currentTimeMillis()
                val w = src.width
                val h = src.height
                val pixels = IntArray(w * h)
                src.getRGB(0, 0, w, h, pixels, 0, w)
                val tPixels = System.currentTimeMillis()
                val fixed = ImageUnscramble.unscramble(pixels, w, h, aid, page)
                val tUnscrambled = System.currentTimeMillis()
                val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                out.setRGB(0, 0, w, h, fixed, 0, w)
                val tFilled = System.currentTimeMillis()
                val tBeforeConvert = System.currentTimeMillis()
                val converted = out.toComposeImageBitmap()
                Log.line("图片", "反切片耗时明细：下载 " + (tDownloaded - t0) + "ms，解码 " + (tDecoded - tDownloaded) + "ms，取像素 " + (tPixels - tDecoded) + "ms，还原 " + (tUnscrambled - tPixels) + "ms，回填 " + (tFilled - tUnscrambled) + "ms，转位图 " + (System.currentTimeMillis() - tBeforeConvert) + "ms")
                converted
            }.onFailure {
                if (it is CancellationException) return@onFailure
                Log.error("图片", "反切片解码异常 url=$url", it) }.getOrNull()
        } ?: return null
        Log.line("图片", "反切片成功 ${bitmap.width}x${bitmap.height} ${bytes.size}B ${System.currentTimeMillis() - t0}ms")
        cache[url] = bitmap
        return bitmap
    }

    /**
     * 反切片新路径：让 Skia 把各 band 按新顺序画到画布上。
     *
     * 反切片的本质是"把某些行搬到别的位置"，所以不必把像素取出来自己搬 ——
     * 用 drawImageRect 逐 band 画一次即可。band 的计算**直接复用共享层**
     * （ImageUnscramble.bandsFor），公式一字不改。
     *
     * 本地实测（合成图 2116x3037，与用户日志同尺寸）：解码 1.2 ms、画 band 81.2 ms；
     * 对照旧路径的"编码 PNG 666.7 ms + ImageIO 读 PNG 759.9 ms + getRGB 277.5 ms"。
     * 正确性也验过：输出与"解码后的源图"逐像素一致（比对 9111 个采样点）。
     *
     * 任一步异常或尺寸异常都返回 null，由调用方退回 PNG 兜底路径 —— 宁可慢，不可画错。
     */
    private fun unscrambleViaCanvas(bytes: ByteArray, aid: Int, page: String): ImageBitmap? {
        val tA = System.currentTimeMillis()
        val src = Image.makeFromEncoded(bytes)
        val tB = System.currentTimeMillis()
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return null
        val bands = ImageUnscramble.bandsFor(w, h, aid, page)
        val surf = Surface.makeRasterN32Premul(w, h)
        val canvas = surf.canvas
        for (b in bands) {
            canvas.drawImageRect(
                src,
                Rect.makeLTRB(0f, b.srcY.toFloat(), w.toFloat(), (b.srcY + b.height).toFloat()),
                Rect.makeLTRB(0f, b.dstY.toFloat(), w.toFloat(), (b.dstY + b.height).toFloat()),
            )
        }
        val tC = System.currentTimeMillis()
        val out = surf.makeImageSnapshot().toComposeImageBitmap()
        val tD = System.currentTimeMillis()
        // 分阶段打点：解码 / 画 band / snapshot+转位图 各自耗时。
        // 为什么拆开：外层"反切片总耗时"含网络下载，不拆开就分不清瓶颈在下载、解码还是绘制。
        // 实测（改造前）：总耗时中位数 1394ms、画band 98ms，差额约 1.1s 只能在解码这一段 —— 需要直接量。
        Log.line("图片", "反切片阶段：解码 " + (tB - tA) + "ms，画 band " + (tC - tB) + "ms（" + bands.size + " 个），转位图 " + (tD - tC) + "ms（" + w + "x" + h + "）")
        return out
    }

    /** 下载并记录状态码、字节数、异常 —— 失败原因的绝大多数都在这里。 */
    private suspend fun download(url: String, kind: String): ByteArray? = withContext(Dispatchers.IO) {
        // 跳过非 http 资源：作品无封面时接口给的是本地占位图名（例如 nopic-Male.gif），
        // 那不是 URL，交给 HTTP 客户端会抛 IllegalArgumentException（用户日志里出现过）。
        if (!url.startsWith("http")) {
            Log.line("图片", "跳过非 http 资源：" + url)
            return@withContext null
        }
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
