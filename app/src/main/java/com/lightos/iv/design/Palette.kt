package com.lightos.iv.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

/** The five display surfaces. `key` is the persisted / agent-facing id. See DESIGN.md §2. */
enum class Surface(val key: String, val label: String) {
    Oled("oled", "OLED"),
    Glass("glass", "Glass"),
    Paper("paper", "Paper"),
    Amber("amber", "Amber"),
    Matte("matte", "Matte");

    companion object {
        fun fromKey(key: String?): Surface = entries.firstOrNull { it.key == key } ?: Oled
    }
}

/** A complete set of color roles. Components read roles, never literal colors. */
@Immutable
data class Palette(
    val surface: Surface,
    val ground: Color,
    val raised: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val signal: Color,
    val danger: Color,
    val scrim: Color,
    val isLight: Boolean,
    val textShadow: Shadow? = null,
) {
    val rule: Color get() = ink.copy(alpha = 0.12f)
    val press: Color get() = ink.copy(alpha = 0.08f)
}

private val DarkScrim = Color.Black.copy(alpha = 0.60f)
private val LightScrim = Color(0xFF2A2416).copy(alpha = 0.35f)

fun paletteFor(surface: Surface): Palette = when (surface) {
    Surface.Oled -> Palette(
        surface = surface,
        ground = Color(0xFF000000),
        raised = Color(0xFF121211),
        ink = Color(0xFFF2EFE8),
        inkSoft = Color(0xFFA3A09A),
        inkFaint = Color(0xFF6B6965),
        signal = Color(0xFFFFB547),
        danger = Color(0xFFFF6B5E),
        scrim = DarkScrim,
        isLight = false,
    )
    Surface.Glass -> Palette(
        surface = surface,
        ground = Color.Black.copy(alpha = 0.40f),
        raised = Color(0xFF141413).copy(alpha = 0.88f),
        ink = Color.White,
        inkSoft = Color.White.copy(alpha = 0.72f),
        inkFaint = Color.White.copy(alpha = 0.48f),
        signal = Color(0xFFFFC266),
        danger = Color(0xFFFF8A80),
        scrim = DarkScrim,
        isLight = false,
        textShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 6f),
    )
    Surface.Paper -> Palette(
        surface = surface,
        ground = Color(0xFFF2F0EB),
        raised = Color(0xFFFBFAF7),
        ink = Color(0xFF141413),
        inkSoft = Color(0xFF5E5C57),
        inkFaint = Color(0xFF8F8C85),
        signal = Color(0xFFB4530A),
        danger = Color(0xFFB3261E),
        scrim = LightScrim,
        isLight = true,
    )
    Surface.Amber -> Palette(
        surface = surface,
        ground = Color(0xFFEFE0C0),
        raised = Color(0xFFF6EAD0),
        ink = Color(0xFF1F1810),
        inkSoft = Color(0xFF6A5A43),
        inkFaint = Color(0xFF93826A),
        signal = Color(0xFF9A3E0A),
        danger = Color(0xFFA3241A),
        scrim = LightScrim,
        isLight = true,
    )
    Surface.Matte -> Palette(
        surface = surface,
        ground = Color(0xFF161615),
        raised = Color(0xFF20201E),
        ink = Color(0xFFE6E2D8),
        inkSoft = Color(0xFF9C988F),
        inkFaint = Color(0xFF6C6962),
        signal = Color(0xFFE8A33D),
        danger = Color(0xFFF07167),
        scrim = DarkScrim,
        isLight = false,
    )
}
