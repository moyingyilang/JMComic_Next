package com.jmcomic_next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * 界面风格与壁纸（桌面端，1.9.x 主题移植第一步）。
 *
 * Android 端有五套风格（WindowGlass / Translucent / FlatBlur / Miuix / Material），
 * 换的不只是配色 —— 圆角、表面透明度、描边、是否模糊一起换。
 *
 * **桌面端的限制先说清**：Android/Windows 那种"实时背景模糊"（Mica/Acrylic 取窗口后面的
 * 内容做模糊）在 Compose Desktop 上拿不到 —— 它需要窗口系统配合。这里用的是等效观感的
 * 近似做法：**壁纸层自己模糊**（Skia 的 blur，能做到），面板用**半透明表面**盖在上面。
 * 结果看起来是磨砂玻璃，但严格说不是真 backdrop blur，这一点不含糊。
 *
 * 面板的透明与圆角来自 [GlassStyle]，值取自 Android 端的 Palettes.kt（近似对齐，
 * 尚未逐个 token 对齐 —— 那一步等这版观感定了再做）。
 */
enum class GlassStyle(val label: String, val surfaceAlpha: Float, val corner: Int, val blurWallpaper: Boolean) {
    WindowGlass("毛玻璃", surfaceAlpha = 0.72f, corner = 12, blurWallpaper = true),
    Translucent("半透明", surfaceAlpha = 0.55f, corner = 18, blurWallpaper = true),
    FlatBlur("扁平模糊", surfaceAlpha = 0.85f, corner = 8, blurWallpaper = true),
    Miuix("MIUI", surfaceAlpha = 0.90f, corner = 22, blurWallpaper = false),
    Material("Material", surfaceAlpha = 1.00f, corner = 12, blurWallpaper = false),
}

/** 内置壁纸：与 Android 端一样提供几套内置渐变，桌面端另可指定本地图片。 */
enum class WallpaperPreset(val label: String, val light: List<Color>, val dark: List<Color>) {
    Aurora("极光", listOf(Color(0xFFBBD9F2), Color(0xFFD9C7F0), Color(0xFFF3D7E3)),
        listOf(Color(0xFF14243A), Color(0xFF241C38), Color(0xFF3A1F35))),
    Sand("沙丘", listOf(Color(0xFFF6E7D3), Color(0xFFEBD9C4), Color(0xFFD9E3E8)),
        listOf(Color(0xFF2A241C), Color(0xFF3A3226), Color(0xFF1E2A30))),
    Mint("薄荷", listOf(Color(0xFFD6F0E4), Color(0xFFCDE4F2), Color(0xFFE8E6F5)),
        listOf(Color(0xFF16281F), Color(0xFF152430), Color(0xFF211F33))),
    Ink("水墨", listOf(Color(0xFFEDEFF2), Color(0xFFDDE2E8), Color(0xFFF5F6F8)),
        listOf(Color(0xFF101214), Color(0xFF1A1D21), Color(0xFF0C0E10))),
}

/**
 * 当前外观设置（全局可读，界面侧栏可改）。
 *
 * 每个字段的 setter 顺手把值写进本地存储 —— 这样界面侧只管赋值，
 * 不必记得调用保存；重启后自动恢复。
 */
object Appearance {
    private val prefs = PreferencesKeyValueStore("jm_appearance")

    var style: GlassStyle = GlassStyle.entries.firstOrNull { it.name == prefs.getString("style", null) }
        ?: GlassStyle.WindowGlass
        set(value) { field = value; prefs.putString("style", value.name) }

    var preset: WallpaperPreset = WallpaperPreset.entries.firstOrNull { it.name == prefs.getString("preset", null) }
        ?: WallpaperPreset.Aurora
        set(value) { field = value; prefs.putString("preset", value.name) }

    /** 用户指定的本地图片路径；为空表示用内置渐变。 */
    var wallpaperPath: String? = prefs.getString("wallpaperPath", null)
        set(value) { field = value; if (value == null) prefs.remove("wallpaperPath") else prefs.putString("wallpaperPath", value) }

    var dark: Boolean = prefs.getString("dark", null) == "true"
        set(value) { field = value; prefs.putString("dark", value.toString()) }
}

/**
 * 壁纸层。放在所有内容下面，铺满窗口。
 *
 * 图片壁纸用 Skiko 的 blur 模糊 —— 这是"玻璃"观感的关键：
 * 壁纸本身糊掉之后，上面盖半透明面板才像磨砂玻璃，否则只是一层蒙版。
 */
@Composable
fun WallpaperLayer(modifier: Modifier = Modifier) {
    val preset = Appearance.preset
    val dark = Appearance.dark
    val colors = if (dark) preset.dark else preset.light
    val blurRadius = if (Appearance.style.blurWallpaper) 48f else 0f

    Box(modifier.fillMaxSize()) {
        if (blurRadius > 0f) {
            // 模糊会把边缘透出底色，所以画得比窗口大一圈
            Box(
                Modifier
                    .fillMaxSize()
                    .blur(blurRadius.dp)
                    .background(Brush.linearGradient(colors)),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors)))
        }
        // 用户指定了图片就用它（叠在渐变上，透明处仍见渐变）。
        // **本地解码**：直接读文件字节交给 Skiko，不走网络加载器 ——
        // 之前用 file:// 借网络加载器读，那是为 HTTP 写的，读不到本地文件。
        Appearance.wallpaperPath?.let { path ->
            val image = remember(path) {
                runCatching {
                    val bytes = File(path).readBytes()
                    org.jetbrains.skia.Image.makeFromEncoded(bytes)
                        .toComposeImageBitmap()
                }.onFailure { Log.error("外观", "本地壁纸解码失败：$path", it) }.getOrNull()
            }
            if (image != null) {
                androidx.compose.foundation.Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }
    }
}

/** 玻璃面板：半透明表面 + 一圈描边。所有卡片/侧栏/顶栏都用它，风格切换时一起变。 */
@Composable
fun Modifier.glassPanel(
    alpha: Float = Appearance.style.surfaceAlpha,
    corner: Int = Appearance.style.corner,
    shape: Shape = RoundedCornerShape(corner.dp),
): Modifier = this
    .clip(shape)
    .background(
        MaterialThemeSurface(alpha),
    )
    .border(1.dp, MaterialThemeStroke(), shape)

/** 面板底色：浅色用白、深色用近黑，透明度由风格决定。 */
@Composable
private fun MaterialThemeSurface(alpha: Float): Color =
    (if (Appearance.dark) Color(0xFF16181E) else Color(0xFFF6F7FA)).copy(alpha = alpha)

/** 描边色：与博客令牌一致（浅色 rgba(15,23,42,0.09)、深色 白 10%）。 */
@Composable
private fun MaterialThemeStroke(): Color =
    if (Appearance.dark) Color(0x1AFFFFFF) else Color(0x170F172A)
