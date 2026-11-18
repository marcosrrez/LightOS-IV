package com.lightos.iv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** The five hardware-inspired display surfaces. */
enum class Surface(val key: String, val label: String) {
    Oled("oled", "Pure OLED"),
    Glass("glass", "Wallpaper Glass"),
    Paper("paper", "Daylight Paper"),
    Amber("amber", "Warm Amber"),
    Matte("matte", "Matte Charcoal");

    companion object {
        fun fromKey(key: String?): Surface =
            entries.firstOrNull { it.key == key } ?: Oled
    }
}

/**
 * The full token set for a surface. Carried via [LocalLightPalette] so any component
 * can read fg / muted / hairline / accent without going through MaterialTheme.
 */
data class LightPalette(
    val surface: Surface,
    val bg: Color,
    val bgElevated: Color,
    val fg: Color,
    val fgMuted: Color,
    val fgDim: Color,
    val hairline: Color,
    val accent: Color,
    val isLight: Boolean,
) {
    val scrim: Color
        get() = if (surface == Surface.Glass) Color.Black.copy(alpha = 0.34f) else bg
}

val LocalLightPalette = staticCompositionLocalOf {
    LightPalette(
        surface = Surface.Oled,
        bg = Color.Black,
        bgElevated = Color(0xFF0C0C0B),
        fg = Color(0xFFF4F1EA),
        fgMuted = Color(0xFF78756E),
        fgDim = Color(0xFF2C2B28),
        hairline = Color.White.copy(alpha = 0.12f),
        accent = Color(0xFFFFB000),
        isLight = false,
    )
}

private fun paletteFor(surface: Surface): LightPalette = when (surface) {
    Surface.Oled -> LightPalette(
        surface, Color(0xFF000000), Color(0xFF0C0C0B),
        Color(0xFFF4F1EA), Color(0xFF78756E), Color(0xFF2C2B28),
        Color.White.copy(alpha = 0.12f), Color(0xFFFFB000), isLight = false
    )
    Surface.Glass -> LightPalette(
        surface, Color.Black.copy(alpha = 0.34f), Color(0xFF0E0E0D).copy(alpha = 0.76f),
        Color(0xFFF4F1EA), Color(0xFFF4F1EA).copy(alpha = 0.72f), Color(0xFFF4F1EA).copy(alpha = 0.25f),
        Color(0xFFF4F1EA).copy(alpha = 0.16f), Color(0xFFFFB000), isLight = false
    )
    Surface.Paper -> LightPalette(
        surface, Color(0xFFF4F1EA), Color(0xFFE8E4DA),
        Color(0xFF161614), Color(0xFF66635B), Color(0xFFC7C3B8),
        Color(0xFF161614).copy(alpha = 0.12f), Color(0xFF8A5A00), isLight = true
    )
    Surface.Amber -> LightPalette(
        surface, Color(0xFFF3E4C6), Color(0xFFE6D4B1),
        Color(0xFF1E1810), Color(0xFF6E5F49), Color(0xFFC4B191),
        Color(0xFF1E1810).copy(alpha = 0.14f), Color(0xFFB45309), isLight = true
    )
    Surface.Matte -> LightPalette(
        surface, Color(0xFF151513), Color(0xFF1E1E1B),
        Color(0xFFE4E0D6), Color(0xFF827F76), Color(0xFF363530),
        Color(0xFFE4E0D6).copy(alpha = 0.12f), Color(0xFFFFB000), isLight = false
    )
}

@Composable
fun LightOsTheme(surface: Surface = Surface.Oled, content: @Composable () -> Unit) {
    val palette = paletteFor(surface)
    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.fg, onPrimary = palette.bg,
            background = palette.bg, onBackground = palette.fg,
            surface = palette.bgElevated, onSurface = palette.fg,
            surfaceVariant = palette.bgElevated, onSurfaceVariant = palette.fgMuted,
            outline = palette.hairline,
        )
    } else {
        darkColorScheme(
            primary = palette.fg, onPrimary = palette.bg,
            background = palette.bg, onBackground = palette.fg,
            surface = palette.bgElevated, onSurface = palette.fg,
            surfaceVariant = palette.bgElevated, onSurfaceVariant = palette.fgMuted,
            outline = palette.hairline,
        )
    }
    CompositionLocalProvider(LocalLightPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LightOsTypography,
            content = {
                androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = palette.bg) { content() }
            },
        )
    }
}

object LightFonts {
    val sans: FontFamily = FontFamily.SansSerif
    val serif: FontFamily = FontFamily.Serif
    val mono: FontFamily = FontFamily.Monospace
}
