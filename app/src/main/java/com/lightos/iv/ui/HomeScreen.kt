package com.lightos.iv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.Screen
import com.lightos.iv.ui.theme.LightFonts
import com.lightos.iv.ui.theme.LocalLightPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: LightOsViewModel) {
    val p = LocalLightPalette.current
    val timer by vm.timer.collectAsState()

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); now = System.currentTimeMillis() }
    }

    val time = remember(now) { SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now)) }
    val date = remember(now) { SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(now)) }

    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        // Clock, top-left, quiet.
        Column(Modifier.align(Alignment.TopStart).padding(top = 16.dp)) {
            Text(time, fontFamily = LightFonts.mono, fontSize = 40.sp, color = p.fg, letterSpacing = (-1).sp)
            MonoLabel(date.uppercase())
            if (timer.running) {
                val mm = (timer.remainingMs / 60000).toInt()
                Text("$mm min focus", fontFamily = LightFonts.mono, fontSize = 12.sp, color = p.accent)
            }
        }

        // Six resting words, centered.
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            homeWords().forEach { (word, action) ->
                Text(
                    text = word,
                    fontFamily = LightFonts.sans,
                    fontWeight = FontWeight.W400,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp,
                    color = p.fg,
                    modifier = Modifier.clickable { action(vm) },
                )
            }
        }

        // The agent entry point, bottom-center above the rail.
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
                .clickable { vm.openAgent(true) },
        ) {
            Text(
                "⌘  ask anything",
                fontFamily = LightFonts.mono,
                fontSize = 13.sp,
                color = p.fgMuted,
            )
        }
    }
}

private fun homeWords(): List<Pair<String, (LightOsViewModel) -> Unit>> = listOf(
    "Notes" to { it.navigate(Screen.Notes) },
    "Focus" to { it.navigate(Screen.Timer) },
    "Apps" to { it.navigate(Screen.Apps) },
    "Report" to { it.navigate(Screen.Report) },
    "Surfaces" to { it.navigate(Screen.Settings) },
    "Assistant" to { it.openAgent(true) },
)
