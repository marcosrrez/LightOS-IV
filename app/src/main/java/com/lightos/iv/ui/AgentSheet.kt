package com.lightos.iv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.agent.AgentEvent
import com.lightos.iv.ui.theme.LightFonts
import com.lightos.iv.ui.theme.LocalLightPalette

@Composable
fun AgentSheet(vm: LightOsViewModel) {
    val events by vm.events.collectAsState()
    val thinking by vm.thinking.collectAsState()
    val p = LocalLightPalette.current
    var input by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(p.bg.copy(alpha = 0.9f))) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Assistant", fontFamily = LightFonts.sans, fontSize = 18.sp, color = p.fg)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("reset", fontFamily = LightFonts.mono, fontSize = 12.sp, color = p.fgMuted,
                        modifier = Modifier.clickable { vm.resetAgent() })
                    Text("close", fontFamily = LightFonts.mono, fontSize = 12.sp, color = p.fgMuted,
                        modifier = Modifier.clickable { vm.openAgent(false) })
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (events.isEmpty()) {
                    MonoLabel("Type a request. e.g. “open spotify”, “start a 25m focus timer”, “what's on my calendar today”, “route me to the nearest coffee shop”.")
                }
                events.forEach { ev -> EventRow(ev) }
                if (thinking) {
                    Text("…", fontFamily = LightFonts.mono, fontSize = 14.sp, color = p.fgMuted)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask anything", fontFamily = LightFonts.mono, fontSize = 14.sp, color = p.fgMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = p.fg, unfocusedTextColor = p.fg,
                        focusedBorderColor = p.hairline, unfocusedBorderColor = p.hairline,
                        cursorColor = p.fg,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                )
                Text(
                    "send ⏎",
                    fontFamily = LightFonts.mono,
                    fontSize = 13.sp,
                    color = if (input.isBlank()) p.fgMuted else p.accent,
                    modifier = Modifier.clickable {
                        if (input.isNotBlank()) { vm.sendAgentMessage(input.trim()); input = "" }
                    },
                )
            }
        }
    }
}

@Composable
private fun EventRow(ev: AgentEvent) {
    val p = LocalLightPalette.current
    when (ev.kind) {
        AgentEvent.Kind.Thought -> Text(
            ev.text, fontFamily = LightFonts.mono, fontSize = 12.sp,
            color = p.fgMuted, fontStyle = FontStyle.Italic,
        )
        AgentEvent.Kind.Tool -> Text(
            "› ${ev.text}", fontFamily = LightFonts.mono, fontSize = 12.sp, color = p.accent,
        )
        AgentEvent.Kind.Result -> Text(
            ev.text, fontFamily = LightFonts.mono, fontSize = 11.sp, color = p.fgDim,
            modifier = Modifier.padding(start = 12.dp),
        )
        AgentEvent.Kind.Answer -> Text(
            ev.text, fontFamily = LightFonts.sans, fontSize = 15.sp, color = p.fg,
        )
        AgentEvent.Kind.Error -> Text(
            ev.text, fontFamily = LightFonts.mono, fontSize = 12.sp, color = p.accent,
        )
    }
}
