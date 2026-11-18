package com.lightos.iv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typographic scale mirroring the LightOS design tokens:
 * xs 11, sm 13, base 16, homeWord 26, heroSerif 34, displayClock 54.
 * System fonts are used so the launcher needs no network for typography; the
 * Public Sans / Newsreader / DM Mono faces can be bundled later for fidelity.
 */
val LightOsTypography = Typography(
    displayLarge = TextStyle(fontFamily = LightFonts.mono, fontWeight = FontWeight.W300, fontSize = 54.sp, letterSpacing = (-0.02).sp),
    headlineMedium = TextStyle(fontFamily = LightFonts.serif, fontWeight = FontWeight.W400, fontSize = 34.sp, letterSpacing = (-0.01).sp),
    headlineSmall = TextStyle(fontFamily = LightFonts.sans, fontWeight = FontWeight.W500, fontSize = 26.sp, letterSpacing = (-0.02).sp),
    titleMedium = TextStyle(fontFamily = LightFonts.sans, fontWeight = FontWeight.W500, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = LightFonts.sans, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = LightFonts.sans, fontWeight = FontWeight.W400, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = LightFonts.mono, fontWeight = FontWeight.W400, fontSize = 11.sp, letterSpacing = (0.04).sp),
    labelLarge = TextStyle(fontFamily = LightFonts.mono, fontWeight = FontWeight.W500, fontSize = 13.sp, letterSpacing = (0.06).sp),
    labelSmall = TextStyle(fontFamily = LightFonts.mono, fontWeight = FontWeight.W400, fontSize = 11.sp, letterSpacing = (0.08).sp),
)
