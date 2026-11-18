package com.lightos.iv.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.data.Note
import com.lightos.iv.ui.theme.LightFonts
import com.lightos.iv.ui.theme.LocalLightPalette

@Composable
fun NotesScreen(vm: LightOsViewModel) {
    val p = LocalLightPalette.current
    val notes by vm.notes.collectAsState()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Commonplace")

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { MonoLabel("title…") },
            singleLine = true,
            colors = textColors(p),
        )
        OutlinedTextField(
            value = body, onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { MonoLabel("write a thought, clip a quote…") },
            minLines = 2, maxLines = 5,
            colors = textColors(p),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("save", onClick = {
                if (body.isNotBlank()) { vm.addNote(title.trim(), body.trim()); title = ""; body = "" }
            }, primary = true)
            if (notes.isNotEmpty()) MonoLabel("${notes.size} entries")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(notes.size) { i -> NoteRow(notes[i], onDelete = { vm.deleteNote(notes[i].id) }) }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onDelete: () -> Unit) {
    val p = LocalLightPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(note.title, fontFamily = LightFonts.sans, fontSize = 14.sp, color = p.fg)
            Text("×", fontFamily = LightFonts.mono, fontSize = 14.sp, color = p.fgMuted,
                modifier = Modifier.clickable(onClick = onDelete))
        }
        Text(note.date, fontFamily = LightFonts.mono, fontSize = 10.sp, color = p.fgMuted)
        Text(note.body, fontFamily = LightFonts.sans, fontSize = 13.sp, color = p.fg)
    }
}

@Composable
private fun textColors(p: com.lightos.iv.ui.theme.LightPalette) =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.fg, unfocusedTextColor = p.fg,
        focusedBorderColor = p.hairline, unfocusedBorderColor = p.hairline,
        cursorColor = p.fg,
    )
