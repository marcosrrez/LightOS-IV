package com.lightos.iv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightos.iv.ui.theme.LocalLightPalette
import com.lightos.iv.ui.theme.LightFonts

@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, muted: Boolean = true) {
    val p = LocalLightPalette.current
    Text(
        text = text,
        fontFamily = LightFonts.mono,
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        color = if (muted) p.fgMuted else p.fg,
        modifier = modifier,
    )
}

@Composable
fun ScreenHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = MaterialThemeHeadline())
    }
}

@Composable
private fun MaterialThemeHeadline() =
    androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
        fontFamily = LightFonts.sans,
    )

@Composable
fun PillButton(text: String, onClick: () -> Unit, primary: Boolean = false, modifier: Modifier = Modifier) {
    val p = LocalLightPalette.current
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (primary) p.fg else p.bgElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = LightFonts.mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            letterSpacing = 1.5.sp,
            color = if (primary) p.bg else p.fg,
        )
    }
}

@Composable
fun InfoCard(content: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalLightPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(p.bgElevated)
            .border(1.dp, p.hairline, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) { content() }
}
