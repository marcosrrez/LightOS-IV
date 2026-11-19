package com.lightos.iv.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.core.content.ContextCompat
import com.lightos.iv.BuildConfig
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.Sheet
import com.lightos.iv.data.AgentConfig
import com.lightos.iv.data.HomeSlot
import com.lightos.iv.data.MAX_HOME_WORDS
import com.lightos.iv.design.Field
import com.lightos.iv.design.LRow
import com.lightos.iv.design.LText
import com.lightos.iv.design.Light
import com.lightos.iv.design.PrimaryAction
import com.lightos.iv.design.SectionLabel
import com.lightos.iv.design.Space
import com.lightos.iv.design.ToolFrame
import com.lightos.iv.home.SurfacePicker

@Composable
fun SettingsScreen(vm: LightOsViewModel) {
    val p = Light.palette
    val t = Light.type
    val context = LocalContext.current
    val surface by vm.surface.collectAsState()
    val slots by vm.slots.collectAsState()
    val cfg by vm.agentConfig.collectAsState()

    var baseUrl by remember(cfg) { mutableStateOf(cfg.baseUrl) }
    var model by remember(cfg) { mutableStateOf(cfg.model) }
    var apiKey by remember(cfg) { mutableStateOf(cfg.apiKey) }

    var refresh by remember { mutableIntStateOf(0) }
    fun granted(perm: String) = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    ToolFrame("Settings") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionLabel("Surface")
            SurfacePicker(current = surface, onPick = vm::setSurface)

            SectionLabel("Home words")
            slots.forEachIndexed { i, slot ->
                LRow(
                    slot.label,
                    detail = if (slot is HomeSlot.Tool) "LightOS tool" else null,
                    trailing = "Edit",
                    onClick = { vm.openSheet(Sheet.Word(i)) },
                )
            }
            if (slots.size < MAX_HOME_WORDS) LRow("Add a word", titleColor = p.inkSoft, onClick = { vm.openSheet(Sheet.PickSlot(null)) })

            SectionLabel("Assistant")
            Column(Modifier.padding(horizontal = Space.gutterTool), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                LText(
                    "Any OpenAI-compatible provider. Requests you send to the assistant, with the context it needs (time, battery, your next event), go to this address.",
                    t.caption, color = p.inkSoft,
                )
                Field(baseUrl, { baseUrl = it }, "Provider address", capitalization = KeyboardCapitalization.None)
                Field(model, { model = it }, "Model", capitalization = KeyboardCapitalization.None)
                Field(apiKey, { apiKey = it }, "API key (blank for a local server)", secret = true, capitalization = KeyboardCapitalization.None)
                Row(Modifier.fillMaxWidth().padding(top = Space.s8), horizontalArrangement = Arrangement.End) {
                    PrimaryAction("Save") { vm.setAgentConfig(AgentConfig(baseUrl.trim(), model.trim(), apiKey.trim())) }
                }
            }

            SectionLabel("Permissions")
            key(refresh) {
                PermissionRow("Calendar", "For Today and the assistant", Manifest.permission.READ_CALENDAR, granted(Manifest.permission.READ_CALENDAR)) { ask.launch(it) }
                PermissionRow("Phone calls", "The assistant can call after you confirm", Manifest.permission.CALL_PHONE, granted(Manifest.permission.CALL_PHONE)) { ask.launch(it) }
                PermissionRow("Text messages", "The assistant can text after you confirm", Manifest.permission.SEND_SMS, granted(Manifest.permission.SEND_SMS)) { ask.launch(it) }
            }

            SectionLabel("System")
            LRow("Default home app", detail = "Choose LightOS as your home screen", onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
            LRow("About", detail = "LightOS ${BuildConfig.VERSION_NAME}. Fonts: Public Sans and Newsreader (SIL Open Font License).")
            Spacer(Modifier.size(Space.s48))
        }
    }
}

@Composable
private fun key(k: Int, content: @Composable () -> Unit) = androidx.compose.runtime.key(k) { content() }

@Composable
private fun PermissionRow(title: String, detail: String, permission: String, granted: Boolean, onAsk: (String) -> Unit) {
    LRow(
        title,
        detail = detail,
        trailing = if (granted) "Allowed" else "Allow",
        onClick = if (granted) null else ({ onAsk(permission) }),
    )
}
