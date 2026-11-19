package com.lightos.iv.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lightos.iv.R

/**
 * Bundled variable fonts (never loaded from the network). See DESIGN.md §3.
 * Public Sans for the interface; Newsreader only for reading and the Today greeting.
 */
@OptIn(ExperimentalTextApi::class)
private fun sans(weight: Int) = Font(
    R.font.public_sans,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun serif(weight: Int, opticalSize: Float) = Font(
    R.font.newsreader,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize),
    ),
)

val PublicSans = FontFamily(sans(300), sans(400), sans(500))
val NewsreaderDisplay = FontFamily(serif(400, 48f))
val NewsreaderText = FontFamily(serif(400, 16f), serif(500, 16f))

private const val TABULAR = "tnum"

/** Even vertical rhythm: center glyphs inside the line box, no extra top/bottom trim. */
private val Rhythm = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

@Immutable
data class LightType(
    val clock: TextStyle,
    val word: TextStyle,
    val title: TextStyle,
    val item: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val display: TextStyle,
    val reading: TextStyle,
)

val DefaultType = LightType(
    clock = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight(300),
        fontSize = 60.sp, lineHeight = 64.sp, letterSpacing = (-0.035).em,
        fontFeatureSettings = TABULAR, lineHeightStyle = Rhythm,
    ),
    word = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight(400),
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.022).em, lineHeightStyle = Rhythm,
    ),
    title = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight(500),
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.015).em, lineHeightStyle = Rhythm,
    ),
    item = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight(400),
        fontSize = 19.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em, lineHeightStyle = Rhythm,
    ),
    body = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight(400),
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.005).em, lineHeightStyle = Rhythm,
    ),
    label = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight(500),
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.em, lineHeightStyle = Rhythm,
    ),
    caption = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight(400),
        fontSize = 12.5.sp, lineHeight = 16.sp, letterSpacing = 0.01.em,
        fontFeatureSettings = TABULAR, lineHeightStyle = Rhythm,
    ),
    display = TextStyle(
        fontFamily = NewsreaderDisplay, fontWeight = FontWeight(400),
        fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.01).em, lineHeightStyle = Rhythm,
    ),
    reading = TextStyle(
        fontFamily = NewsreaderText, fontWeight = FontWeight(400),
        fontSize = 18.sp, lineHeight = 28.sp, letterSpacing = 0.em, lineHeightStyle = Rhythm,
    ),
)
