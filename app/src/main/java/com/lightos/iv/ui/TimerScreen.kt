package com.lightos.iv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.ui.theme.LightFonts
import com.lightos.iv.ui.theme.LocalLightPalette

@Composable
fun TimerScreen(vm: LightOsViewModel) {
    val p = LocalLightPalette.current
    val timer by vm.timer.collectAsState()
    val totalSec = (timer.remainingMs / 1000).toInt()
    val mm = (totalSec / 60).toString().padStart(2, '0')
    val ss = (totalSec % 60).toString().padStart(2, '0')

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        MonoLabel("Focus".uppercase())
        Text(
            "$mm:$ss",
            fontFamily = LightFonts.mono,
            fontWeight = FontWeight.W200,
            fontSize = 76.sp,
            color = if (timer.running) p.fg else p.fgMuted,
        )
        timer.label?.let { Text(it, fontFamily = LightFonts.sans, fontSize = 14.sp, color = p.fgMuted) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(if (timer.running) "stop" else "start 25m", onClick = {
                if (timer.running) vm.stopTimer() else vm.startFocusTimer(25, null)
            }, primary = !timer.running)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(5 to "5m", 15 to "15m", 25 to "25m", 45 to "45m").forEach { (mins, label) ->
                PillButton(label, onClick = { vm.startFocusTimer(mins, null) })
            }
        }
        MonoLabel("the agent can also start this — try “focus 25m”")
    }
}
