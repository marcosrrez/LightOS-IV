package com.lightos.iv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.Screen
import com.lightos.iv.data.CalEvent
import com.lightos.iv.ui.theme.LightFonts
import com.lightos.iv.ui.theme.LocalLightPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(vm: LightOsViewModel) {
    val p = LocalLightPalette.current
    var events by remember { mutableStateOf<List<CalEvent>?>(null) }
    val hasPermission = remember { vm.calendarHasPermission() }

    LaunchedEffect(hasPermission) {
        events = if (hasPermission) vm.calendarEvents() else emptyList()
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        val hour = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toIntOrNull() ?: 12
        val greeting = if (hour < 12) "Good morning." else if (hour < 17) "Good afternoon." else "Good evening."

        Text(greeting, fontFamily = LightFonts.serif, fontSize = 26.sp, color = p.fg)
        MonoLabel(date.uppercase())

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MonoLabel("Today's Commitments", muted = false)
            if (hasPermission) MonoLabel("${events?.size ?: 0}")
        }

        if (!hasPermission) {
            InfoCard {
                MonoLabel("Calendar permission not granted.", muted = false)
                MonoLabel("Grant it in Settings → Permissions, then reopen this report.")
            }
        } else {
            val list = events
            if (list.isNullOrEmpty()) {
                MonoLabel("Your schedule is clear today.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list.size) { i ->
                        val e = list[i]
                        val time = if (e.allDay) "all-day"
                        else SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(e.startMs))
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("$time  ·  ${e.title}", fontFamily = LightFonts.sans, fontSize = 14.sp, color = p.fg)
                            e.location?.let { Text(it, fontFamily = LightFonts.mono, fontSize = 11.sp, color = p.fgMuted) }
                        }
                    }
                }
            }
        }

        // Daily reflection (the agent can typeset more via “read stoicism” → reader).
        InfoCard {
            MonoLabel("Reflection", muted = false)
            Text(
                "“You have power over your mind — not outside events. Realize this, and you will find strength.”\n— Marcus Aurelius",
                fontFamily = LightFonts.serif, fontSize = 15.sp, color = p.fg,
            )
        }
    }
}
