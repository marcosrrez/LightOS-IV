package com.lightos.iv.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.data.AgentConfig
import com.lightos.iv.ui.theme.LightFonts
import com.lightos.iv.ui.theme.LocalLightPalette
import com.lightos.iv.ui.theme.Surface

@Composable
fun SettingsScreen(vm: LightOsViewModel) {
    val p = LocalLightPalette.current
    val ctx = LocalContext.current
    val surface by vm.surface.collectAsState()
    val cfg by vm.agentConfig.collectAsState()

    var baseUrl by remember(cfg) { mutableStateOf(cfg.baseUrl) }
    var model by remember(cfg) { mutableStateOf(cfg.model) }
    var apiKey by remember(cfg) { mutableStateOf(cfg.apiKey) }

    fun granted(perm: String) =
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
    var calGranted by remember { mutableStateOf(granted(Manifest.permission.READ_CALENDAR)) }
    var callGranted by remember { mutableStateOf(granted(Manifest.permission.CALL_PHONE)) }
    var smsGranted by remember { mutableStateOf(granted(Manifest.permission.SEND_SMS)) }

    val calLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { calGranted = it }
    val callLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { callGranted = it }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { smsGranted = it }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("Settings")

        MonoLabel("AI PROVIDER (OPENAI-COMPATIBLE)", muted = false)
        OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { MonoLabel("base url, e.g. https://api.openai.com/v1 or http://localhost:11434/v1") },
            colors = textColors(p))
        OutlinedTextField(value = model, onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { MonoLabel("model, e.g. gpt-4o-mini or llama3.1") },
            colors = textColors(p))
        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            placeholder = { MonoLabel("api key (blank for local servers)") },
            colors = textColors(p))
        PillButton("save provider", onClick = { vm.setAgentConfig(AgentConfig(baseUrl.trim(), model.trim(), apiKey.trim())) }, primary = true)

        MonoLabel("SURFACE", muted = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Surface.Oled, Surface.Glass, Surface.Paper, Surface.Amber, Surface.Matte).forEach { s ->
                PillButton(s.key, onClick = { vm.setSurface(s) }, primary = surface == s)
            }
        }

        MonoLabel("PERMISSIONS", muted = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(if (calGranted) "calendar ✓" else "grant calendar",
                onClick = { if (!calGranted) calLauncher.launch(Manifest.permission.READ_CALENDAR) })
            PillButton(if (callGranted) "call ✓" else "grant call",
                onClick = { if (!callGranted) callLauncher.launch(Manifest.permission.CALL_PHONE) })
            PillButton(if (smsGranted) "sms ✓" else "grant sms",
                onClick = { if (!smsGranted) smsLauncher.launch(Manifest.permission.SEND_SMS) })
        }
        MonoLabel("the agent asks you to confirm before it calls or texts.")

        MonoLabel("v10.0 · native + agentic")
    }
}

@Composable
private fun textColors(p: com.lightos.iv.ui.theme.LightPalette) =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.fg, unfocusedTextColor = p.fg,
        focusedBorderColor = p.hairline, unfocusedBorderColor = p.hairline,
        cursorColor = p.fg,
    )
