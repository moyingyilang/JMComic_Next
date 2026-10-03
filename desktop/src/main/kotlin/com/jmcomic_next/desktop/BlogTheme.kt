package com.jmcomic_next.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

/**
 * 桌面端主题：直接采用 moyingyilang.github.io 的设计令牌（2.0.0）。
 *
 * 这些值不是"照着感觉调的"，而是从博客的 src/styles/global.css 里逐个抄来的：
 * 强调色、三级文字灰、描边、表面色、圆角尺度、动效时长。这样做的好处是
 * **桌面端与博客观感一致**，而且以后博客改色，这里改同名的几个常量即可。
 *
 * 深色不是浅色的反相，而是博客里另给的一套值（强调色从 #0f6cbd 变为 #60cdff，
 * 因为深底上蓝要更亮才看得清；文字改半透明白）。
 */
private object Tokens {
    // 浅色
    val accentLight = Color(0xFF0F6CBD)
    val accentLightHover = Color(0xFF115EA3)
    val accentLightSoft = Color(0x1A0F6CBD)
    val textLight = Color(0xFF16181D)
    val textSecondaryLight = Color(0xFF4A4F5A)
    val textTertiaryLight = Color(0xFF767C88)
    val surfaceLight = Color(0xFFF6F7FA)
    val surfaceSunkenLight = Color(0x090F172A)
    val surfaceHoverLight = Color(0x0D0F172A)
    val strokeLight = Color(0x170F172A)

    // 深色
    val accentDark = Color(0xFF60CDFF)
    val accentDarkHover = Color(0xFF7FD8FF)
    val accentDarkSoft = Color(0x2460CDFF)
    val textDark = Color(0xFFF3F4F7)
    val textSecondaryDark = Color(0xB8FFFFFF)
    val textTertiaryDark = Color(0x80FFFFFF)
    val surfaceDark = Color(0xFF16181E)
    val surfaceSunkenDark = Color(0x3D000000)
    val surfaceHoverDark = Color(0x14FFFFFF)
    val strokeDark = Color(0x1AFFFFFF)

    // 圆角：博客的 --r-sm/md/lg 分别 8/12/18
    val rSm = 8.dp
    val rMd = 12.dp
    val rLg = 18.dp
}

@Composable
fun BlogTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // 壁纸层要按深浅色选预设，所以这里把状态同步到全局
    Appearance.dark = dark

    val scheme = if (dark) {
        darkColorScheme(
            primary = Tokens.accentDark,
            onPrimary = Color(0xFF04263A),
            primaryContainer = Tokens.accentDarkSoft,
            onPrimaryContainer = Tokens.accentDark,
            background = Color.Transparent,   // 透出壁纸
            onBackground = Tokens.textDark,
            surface = Tokens.surfaceDark.copy(alpha = Appearance.style.surfaceAlpha),
            onSurface = Tokens.textDark,
            surfaceVariant = Tokens.surfaceSunkenDark,
            onSurfaceVariant = Tokens.textSecondaryDark,
            outline = Tokens.strokeDark,
            outlineVariant = Tokens.strokeDark,
        )
    } else {
        lightColorScheme(
            primary = Tokens.accentLight,
            onPrimary = Color.White,
            primaryContainer = Tokens.accentLightSoft,
            onPrimaryContainer = Tokens.accentLight,
            background = Color.Transparent,   // 透出壁纸
            onBackground = Tokens.textLight,
            // 半透明表面：所有用 colorScheme.surface 的面板因此一次性变成玻璃，
            // 不必逐处改背景。透明度由 Appearance.style 决定。
            surface = Tokens.surfaceLight.copy(alpha = Appearance.style.surfaceAlpha),
            onSurface = Tokens.textLight,
            surfaceVariant = Tokens.surfaceSunkenLight,
            onSurfaceVariant = Tokens.textSecondaryLight,
            outline = Tokens.strokeLight,
            outlineVariant = Tokens.strokeLight,
        )
    }

    // 字号也照博客：正文 1rem(16sp) 上下、辅助文字 0.85/0.78rem
    val typography = Typography(
        titleMedium = MaterialTheme.typography.titleMedium.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontSize = 13.6.sp, lineHeight = 20.sp),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
    )

    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(Tokens.rSm / 2),
            small = RoundedCornerShape(Tokens.rSm),
            medium = RoundedCornerShape(Tokens.rMd),
            large = RoundedCornerShape(Tokens.rLg),
        ),
        typography = typography,
    ) {
        // 壁纸铺在最底层，上面所有面板是半透明的，于是形成玻璃观感
        Box(Modifier.fillMaxSize()) {
            WallpaperLayer()
            content()
        }
    }
}
