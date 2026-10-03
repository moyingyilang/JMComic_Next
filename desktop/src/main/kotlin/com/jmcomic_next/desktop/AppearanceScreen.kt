package com.jmcomic_next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * 外观设置（桌面端，1.9.x）。
 *
 * 为什么先做这个页：观感（风格/壁纸/玻璃浓度）是主观的，让人点着看比"我改一次、
 * 跑一次、你再反馈"快得多。改动即时生效 —— Appearance 里的字段是 mutableState，
 * 界面会立刻重组。
 *
 * 本地图片壁纸用 AWT 的文件选择框（桌面端可用，且不需要额外依赖）。
 */
@Composable
fun AppearanceScreen() {
    var refresh by remember { mutableStateOf(0) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("外观", style = MaterialTheme.typography.titleLarge)

        // 风格
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("界面风格", style = MaterialTheme.typography.titleMedium)
            Text(
                "换的不只是透明度 —— 圆角与壁纸是否模糊一起换。手机上的实时背景模糊在桌面上拿不到，这里用等效近似。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassStyle.entries.forEach { s ->
                    val active = Appearance.style == s
                    Button(
                        onClick = { Appearance.style = s; refresh++ },
                        modifier = Modifier.height(38.dp),
                    ) {
                        Text(
                            if (active) "· ${s.label}" else s.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Text(
                "当前：${Appearance.style.label}（面板透明度 ${(Appearance.style.surfaceAlpha * 100).toInt()}%、圆角 ${Appearance.style.corner}dp" +
                    (if (Appearance.style.blurWallpaper) "、壁纸模糊" else "、壁纸不模糊") + "）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // 壁纸
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("壁纸", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WallpaperPreset.entries.forEach { p ->
                    Column(
                        Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { Appearance.preset = p; Appearance.wallpaperPath = null; refresh++ }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(if (Appearance.dark) p.dark else p.light)),
                        ) {}
                        Text(
                            (if (Appearance.preset == p && Appearance.wallpaperPath == null) "· " else "") + p.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = {
                    val chosen = pickImageFile()
                    if (chosen != null) {
                        Appearance.wallpaperPath = chosen
                        refresh++
                        Log.line("外观", "选择本地壁纸：$chosen")
                    }
                }) { Text("选择本地图片…") }

                if (Appearance.wallpaperPath != null) {
                    Text(
                        File(Appearance.wallpaperPath!!).name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = { Appearance.wallpaperPath = null; refresh++ }) { Text("清除") }
                }
            }
            Text(
                "本地图片壁纸这条路径**尚未验证**（当前实现借用了网络图片加载器读 file://，很可能不通）。" +
                    "如果选了没反应，就是这里的问题，我会改成用本地解码。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 深浅色
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("深浅色", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { Appearance.dark = false; refresh++ }) { Text("浅色") }
                TextButton(onClick = { Appearance.dark = true; refresh++ }) { Text("深色") }
            }
            Text(
                if (Appearance.dark) "当前：深色" else "当前：浅色",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 用 AWT 的文件选择框挑一张图片（桌面端自带，无需额外依赖）。返回绝对路径。 */
private fun pickImageFile(): String? = runCatching {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "选择壁纸图片", java.awt.FileDialog.LOAD)
    dialog.setFilenameFilter { _, name ->
        val n = name.lowercase()
        n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".webp")
    }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    File(dir, file).absolutePath
}.getOrNull()
