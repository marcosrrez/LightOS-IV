package com.lightos.iv.design

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalPalette = staticCompositionLocalOf { paletteFor(Surface.Oled) }
val LocalType = staticCompositionLocalOf { DefaultType }
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Shorthand accessors: `Light.palette.ink`, `Light.type.word`. */
object Light {
    val palette: Palette
        @Composable get() = LocalPalette.current
    val type: LightType
        @Composable get() = LocalType.current
    val reduceMotion: Boolean
        @Composable get() = LocalReduceMotion.current
}

/**
 * Provides the active surface. Switching surfaces crossfades every role over `fade.base`
 * instead of snapping, so changing from OLED to Paper feels like a light coming up.
 */
@Composable
fun LightTheme(surface: Surface, content: @Composable () -> Unit) {
    val target = remember(surface) { paletteFor(surface) }
    val spec = Motion.fadeBase<Color>()
    val ground by animateColorAsState(target.ground, spec, label = "ground")
    val raised by animateColorAsState(target.raised, spec, label = "raised")
    val ink by animateColorAsState(target.ink, spec, label = "ink")
    val inkSoft by animateColorAsState(target.inkSoft, spec, label = "inkSoft")
    val inkFaint by animateColorAsState(target.inkFaint, spec, label = "inkFaint")
    val signal by animateColorAsState(target.signal, spec, label = "signal")
    val palette = target.copy(
        ground = ground, raised = raised, ink = ink,
        inkSoft = inkSoft, inkFaint = inkFaint, signal = signal,
    )

    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val selection = TextSelectionColors(handleColor = palette.ink, backgroundColor = palette.ink.copy(alpha = 0.24f))

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalType provides DefaultType,
        LocalReduceMotion provides reduceMotion,
        LocalTextSelectionColors provides selection,
        content = content,
    )
}
