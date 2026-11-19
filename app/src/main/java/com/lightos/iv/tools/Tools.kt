package com.lightos.iv.tools

import android.Manifest
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.data.CalEvent
import com.lightos.iv.data.Note
import com.lightos.iv.design.Field
import com.lightos.iv.design.LRow
import com.lightos.iv.design.LText
import com.lightos.iv.design.Light
import com.lightos.iv.design.PressStyle
import com.lightos.iv.design.PrimaryAction
import com.lightos.iv.design.Rule
import com.lightos.iv.design.SectionLabel
import com.lightos.iv.design.Space
import com.lightos.iv.design.TextAction
import com.lightos.iv.design.ToolFrame
import com.lightos.iv.design.pressable
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------------------
// Notes
// ---------------------------------------------------------------------------------------

@Composable
fun NotesScreen(vm: LightOsViewModel) {
    val p = Light.palette
    val t = Light.type
    val notes by vm.notes.collectAsState()
    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }

    fun save() {
        val text = draft.trim()
        if (text.isEmpty()) return
        val e = editing
        if (e != null) vm.updateNote(e.id, text.lineSequence().first().take(48), text) else vm.addNote("", text)
        draft = ""
        editing = null
    }

    ToolFrame("Notes") {
        Column(Modifier.padding(horizontal = Space.gutterTool)) {
            Field(
                value = draft,
                onValueChange = { draft = it },
                placeholder = if (editing != null) "Edit note" else "Write something",
                style = t.reading,
                singleLine = false,
                minLines = 2,
                imeAction = ImeAction.Default,
            )
            if (draft.isNotBlank() || editing != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = Space.s8),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    editing?.let { e ->
                        TextAction("Delete", color = p.danger) { vm.deleteNote(e.id); editing = null; draft = "" }
                        Spacer(Modifier.weight(1f))
                    }
                    TextAction("Cancel", color = p.inkSoft) { draft = ""; editing = null }
                    Spacer(Modifier.size(Space.s8))
                    PrimaryAction(if (editing != null) "Update" else "Save") { save() }
                }
            }
        }

        if (notes.isEmpty()) {
            LText(
                "Notes you save here, or by typing “note …” in Search, appear in this list.",
                t.body, color = p.inkSoft,
                modifier = Modifier.padding(horizontal = Space.gutterTool, vertical = Space.s24),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(top = Space.s16)) {
                items(notes.size, key = { notes[it].id }) { i ->
                    val n = notes[i]
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .pressable(PressStyle.Overlay) { editing = n; draft = n.body }
                            .padding(horizontal = Space.gutterTool, vertical = Space.s12),
                    ) {
                        LText(relativeDay(n.createdAt), t.caption, color = p.inkFaint)
                        LText(n.body, t.reading, maxLines = 4, modifier = Modifier.padding(top = Space.s4))
                    }
                    if (i < notes.lastIndex) Rule(Modifier.padding(horizontal = Space.gutterTool))
                }
            }
        }
    }
}

@Composable
private fun relativeDay(epochMs: Long): String {
    val context = LocalContext.current
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = epochMs }
    val time = DateFormat.getTimeFormat(context).format(Date(epochMs))
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val dayDiff = if (sameYear) now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR) else 99
    return when (dayDiff) {
        0 -> "Today, $time"
        1 -> "Yesterday, $time"
        else -> DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), if (sameYear) "MMMd" else "yMMMd"), Date(epochMs)).toString()
    }
}

// ---------------------------------------------------------------------------------------
// Focus
// ---------------------------------------------------------------------------------------

@Composable
fun FocusScreen(vm: LightOsViewModel) {
    val p = Light.palette
    val t = Light.type
    val context = LocalContext.current
    val focus by vm.focus.collectAsState()
    var minutes by remember { mutableIntStateOf(25) }

    ToolFrame("Focus") {
        Column(
            Modifier.fillMaxSize().padding(horizontal = Space.gutterTool),
            verticalArrangement = Arrangement.Center,
        ) {
            val shown = if (focus.running) focus.remainingMs else minutes * 60_000L
            val totalSec = (shown + 999) / 1000
            LText(
                "%d:%02d".format(totalSec / 60, totalSec % 60), t.clock,
                color = if (focus.running) p.ink else p.inkSoft,
            )
            if (focus.running) {
                val ends = DateFormat.getTimeFormat(context).format(Date(focus.endsAt))
                LText("Ends at $ends", t.caption, color = p.signal, modifier = Modifier.padding(top = Space.s8))
            } else {
                Row(Modifier.padding(top = Space.s16)) {
                    listOf(15, 25, 45, 60).forEach { m ->
                        TextAction("$m min", color = if (m == minutes) p.ink else p.inkSoft) { minutes = m }
                    }
                }
            }
            Box(Modifier.padding(top = Space.s32)) {
                if (focus.running) PrimaryAction("Stop") { vm.stopFocus() }
                else PrimaryAction("Start focus") { vm.startFocus(minutes) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Today
// ---------------------------------------------------------------------------------------

@Composable
fun TodayScreen(vm: LightOsViewModel) {
    val p = Light.palette
    val t = Light.type
    val context = LocalContext.current
    val notes by vm.notes.collectAsState()
    val focus by vm.focus.collectAsState()
    var events by remember { mutableStateOf<List<CalEvent>?>(null) }
    var permissionAsked by remember { mutableIntStateOf(0) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionAsked++ }

    LaunchedEffect(permissionAsked) { events = vm.calendarToday() }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 5 -> "Good night."
        hour < 12 -> "Good morning."
        hour < 17 -> "Good afternoon."
        else -> "Good evening."
    }

    Column(Modifier.fillMaxSize()) {
        ToolFrame("Today") {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LText(greeting, t.display, modifier = Modifier.padding(horizontal = Space.gutterTool))
                LText(
                    DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd"), Date()).toString(),
                    t.caption, color = p.inkSoft,
                    modifier = Modifier.padding(horizontal = Space.gutterTool, vertical = Space.s4),
                )

                if (focus.running) {
                    LText(
                        "Focus · ${focus.remainingMinutes} min left", t.caption, color = p.signal,
                        modifier = Modifier.padding(horizontal = Space.gutterTool, vertical = Space.s8),
                    )
                }

                SectionLabel("Schedule")
                when (val list = events) {
                    null -> LRow(
                        "Show your calendar",
                        detail = "LightOS reads today's events. Nothing leaves your phone unless you ask the assistant.",
                        onClick = { permission.launch(Manifest.permission.READ_CALENDAR) },
                    )
                    else -> if (list.isEmpty()) {
                        LText(
                            "Nothing scheduled today.", t.body, color = p.inkSoft,
                            modifier = Modifier.padding(horizontal = Space.gutterTool, vertical = Space.s8),
                        )
                    } else list.forEach { e ->
                        val time = if (e.allDay) "All day" else DateFormat.getTimeFormat(context).format(Date(e.startMs))
                        LRow(e.title, detail = listOfNotNull(time, e.location).joinToString(" · "))
                    }
                }

                notes.firstOrNull()?.let { latest ->
                    SectionLabel("From your notes")
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = Space.row)
                            .padding(horizontal = Space.gutterTool, vertical = Space.s8),
                    ) {
                        LText(latest.body, t.reading, maxLines = 6)
                    }
                }
                Spacer(Modifier.size(Space.s48))
            }
        }
    }
}
