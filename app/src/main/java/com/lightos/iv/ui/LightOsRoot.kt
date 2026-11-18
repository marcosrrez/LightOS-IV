package com.lightos.iv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightos.iv.ConfirmationRequest
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.Screen
import com.lightos.iv.ui.theme.LocalLightPalette

@Composable
fun LightOsRoot(vm: LightOsViewModel) {
    val screen by vm.current.collectAsState()
    val agentOpen by vm.agentOpen.collectAsState()
    val pending by vm.pendingConfirmation.collectAsState()
    val p = LocalLightPalette.current

    Box(Modifier.fillMaxSize().background(p.bg)) {
        when (screen) {
            Screen.Home -> HomeScreen(vm)
            Screen.Apps -> AppDrawerScreen(vm)
            Screen.Notes -> NotesScreen(vm)
            Screen.Timer -> TimerScreen(vm)
            Screen.Report -> ReportScreen(vm)
            Screen.Settings -> SettingsScreen(vm)
        }

        BottomRail(vm, screen, Modifier.align(Alignment.BottomCenter).fillMaxWidth())

        if (agentOpen) AgentSheet(vm)

        pending?.let { ConfirmDialog(it, vm::respondConfirmation) }
    }
}

@Composable
private fun BottomRail(vm: LightOsViewModel, screen: Screen, modifier: Modifier) {
    val p = LocalLightPalette.current
    Row(
        modifier = modifier
            .background(p.bg.copy(alpha = if (p.surface == com.lightos.iv.ui.theme.Surface.Glass) 0.5f else 1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        railTargets().forEach { target ->
            val active = screen == target
            Text(
                text = target.label(),
                modifier = Modifier
                    .clickable { vm.navigate(target) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = if (active) p.fg else p.fgMuted,
            )
        }
    }
}

private fun railTargets() = listOf(Screen.Home, Screen.Apps, Screen.Notes, Screen.Timer, Screen.Report, Screen.Settings)

private fun Screen.label(): String = when (this) {
    Screen.Home -> "Home"; Screen.Apps -> "Apps"; Screen.Notes -> "Notes"
    Screen.Timer -> "Focus"; Screen.Report -> "Report"; Screen.Settings -> "Settings"
}

@Composable
private fun ConfirmDialog(req: ConfirmationRequest, onRespond: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onRespond(false) },
        title = { Text(req.title) },
        text = { Text(req.message) },
        confirmButton = { TextButton(onClick = { onRespond(true) }) { Text("Allow") } },
        dismissButton = { TextButton(onClick = { onRespond(false) }) { Text("Don't allow") } },
    )
}
