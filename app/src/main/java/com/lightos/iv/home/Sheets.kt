package com.lightos.iv.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.Screen
import com.lightos.iv.Sheet
import com.lightos.iv.command.AppSearch
import com.lightos.iv.data.HomeSlot
import com.lightos.iv.data.MAX_HOME_WORDS
import com.lightos.iv.data.ToolId
import com.lightos.iv.design.Field
import com.lightos.iv.design.LRow
import com.lightos.iv.design.LText
import com.lightos.iv.design.Light
import com.lightos.iv.design.PrimaryAction
import com.lightos.iv.design.Shapes
import com.lightos.iv.design.SheetHost
import com.lightos.iv.design.Space
import com.lightos.iv.design.Surface
import com.lightos.iv.design.TextAction
import com.lightos.iv.design.paletteFor
import com.lightos.iv.design.pressable

@Composable
fun SheetLayer(vm: LightOsViewModel) {
    val sheet by vm.sheet.collectAsState()
    BackHandler(enabled = sheet != null) { vm.closeSheet() }

    SheetHost(item = sheet, onDismiss = vm::closeSheet) { s ->
        when (s) {
            Sheet.HomeMenu -> HomeMenu(vm)
            is Sheet.Word -> WordMenu(vm, s.index)
            is Sheet.PickSlot -> PickSlot(vm, s.index)
            is Sheet.AppMenu -> {
                SheetTitle(s.app.name)
                val slots by vm.slots.collectAsState()
                if (slots.size < MAX_HOME_WORDS && slots.none { it is HomeSlot.App && it.packageName == s.app.packageName }) {
                    LRow("Add to home", onClick = {
                        vm.putSlot(null, HomeSlot.App(s.app.name, s.app.packageName, s.app.className))
                        vm.closeSheet()
                        vm.whisper("Added ${s.app.name} to home")
                    })
                }
                LRow("App info", onClick = { vm.closeSheet(); vm.appInfo(s.app) })
                LRow("Uninstall", titleColor = Light.palette.danger, onClick = { vm.closeSheet(); vm.uninstall(s.app) })
            }
        }
    }
}

@Composable
fun SheetTitle(text: String, detail: String? = null) {
    Column(Modifier.padding(horizontal = Space.gutterTool).padding(bottom = Space.s8)) {
        LText(text, Light.type.title, maxLines = 1)
        if (detail != null) LText(detail, Light.type.caption, color = Light.palette.inkSoft)
    }
}

@Composable
private fun HomeMenu(vm: LightOsViewModel) {
    val surface by vm.surface.collectAsState()
    SheetTitle("Home")
    SurfacePicker(current = surface, onPick = vm::setSurface)
    Spacer(Modifier.size(Space.s8))
    LRow("Add a word", onClick = { vm.openSheet(Sheet.PickSlot(null)) })
    LRow("Today", onClick = { vm.open(Screen.Today) })
    LRow("Settings", onClick = { vm.open(Screen.Settings) })
}

/** Five swatches, each drawn in its own surface's ground and ink. */
@Composable
fun SurfacePicker(current: Surface, onPick: (Surface) -> Unit) {
    val p = Light.palette
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.gutterTool, vertical = Space.s8),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface.entries.forEach { s ->
            val sp = paletteFor(s)
            val selected = s == current
            Column(
                Modifier.pressable { onPick(s) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .border(if (selected) 2.dp else 1.dp, if (selected) p.ink else p.rule, Shapes.pill)
                        .padding(4.dp)
                        .background(if (s == Surface.Glass) sp.raised else sp.ground, Shapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    LText("Aa", Light.type.caption, color = sp.ink)
                }
                LText(
                    s.label, Light.type.caption,
                    color = if (selected) p.ink else p.inkSoft,
                    modifier = Modifier.padding(top = Space.s4),
                )
            }
        }
    }
}

@Composable
private fun WordMenu(vm: LightOsViewModel, index: Int) {
    val slots by vm.slots.collectAsState()
    val slot = slots.getOrNull(index) ?: return
    var renaming by remember(index) { mutableStateOf(false) }
    var name by remember(index) { mutableStateOf(slot.label) }

    SheetTitle(slot.label, if (slot is HomeSlot.Tool) "LightOS tool" else null)
    if (renaming) {
        val fr = remember { FocusRequester() }
        LaunchedEffect(Unit) { fr.requestFocus() }
        Column(Modifier.padding(horizontal = Space.gutterTool)) {
            Field(
                value = name, onValueChange = { name = it }, placeholder = "Word",
                style = Light.type.item, focusRequester = fr,
                onSubmit = { vm.renameSlot(index, name); vm.closeSheet() },
            )
            Row(Modifier.fillMaxWidth().padding(top = Space.s12), horizontalArrangement = Arrangement.End) {
                TextAction("Cancel") { renaming = false }
                PrimaryAction("Save") { vm.renameSlot(index, name); vm.closeSheet() }
            }
        }
    } else {
        LRow("Change", onClick = { vm.openSheet(Sheet.PickSlot(index)) })
        LRow("Rename", onClick = { renaming = true })
        if (index > 0) LRow("Move up", onClick = { vm.moveSlot(index, -1); vm.openSheet(Sheet.Word(index - 1)) })
        if (index < slots.lastIndex) LRow("Move down", onClick = { vm.moveSlot(index, 1); vm.openSheet(Sheet.Word(index + 1)) })
        LRow("Remove from home", titleColor = Light.palette.danger, onClick = { vm.removeSlot(index); vm.closeSheet() })
    }
}

@Composable
private fun PickSlot(vm: LightOsViewModel, index: Int?) {
    val apps by vm.apps.collectAsState()
    var query by remember { mutableStateOf("") }
    val tools = remember(query) {
        ToolId.entries.filter { query.isBlank() || AppSearch.score(AppSearch.normalize(query), it.label) != null }
    }
    val ranked = remember(apps, query) { AppSearch.rank(query, apps) }

    SheetTitle(if (index == null) "Add a word" else "Change word")
    Column(Modifier.fillMaxHeight(0.8f)) {
        Box(Modifier.padding(horizontal = Space.gutterTool)) {
            Field(value = query, onValueChange = { query = it }, placeholder = "Search apps and tools", style = Light.type.body)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(tools.size) { i ->
                val tool = tools[i]
                LRow(tool.label, detail = "LightOS tool", onClick = {
                    vm.putSlot(index, HomeSlot.Tool(tool)); vm.closeSheet()
                })
            }
            items(ranked.size, key = { ranked[it].key }) { i ->
                val app = ranked[i]
                LRow(app.name, onClick = {
                    vm.putSlot(index, HomeSlot.App(app.name, app.packageName, app.className)); vm.closeSheet()
                })
            }
        }
    }
}

/** Calls, texts and other irreversible actions: everything shown verbatim, verb on the button. */
@Composable
fun ConfirmSheet(vm: LightOsViewModel) {
    val req by vm.confirm.collectAsState()
    BackHandler(enabled = req != null) { vm.respondConfirmation(false) }
    SheetHost(item = req, onDismiss = { vm.respondConfirmation(false) }) { r ->
        SheetTitle(r.title)
        LText(
            r.body, Light.type.body,
            modifier = Modifier.padding(horizontal = Space.gutterTool, vertical = Space.s8),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.gutterTool, vertical = Space.s12),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextAction("Cancel") { vm.respondConfirmation(false) }
            Spacer(Modifier.size(Space.s8))
            PrimaryAction(r.verb) { vm.respondConfirmation(true) }
        }
    }
}
