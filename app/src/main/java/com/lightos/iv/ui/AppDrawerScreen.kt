package com.lightos.iv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.data.AppEntry
import com.lightos.iv.ui.theme.LightFonts
import com.lightos.iv.ui.theme.LocalLightPalette

private val alphabet = ('A'..'Z').toList() + '#'

@Composable
fun AppDrawerScreen(vm: LightOsViewModel) {
    val apps by vm.installedApps.collectAsState()
    val p = LocalLightPalette.current
    var query by remember { mutableStateOf("") }

    val filtered = remember(apps, query) {
        val q = query.lowercase().trim()
        if (q.isBlank()) apps else apps.filter { it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }
    val firstIndexByLetter = remember(filtered) {
        val map = HashMap<Char, Int>()
        filtered.forEachIndexed { i, app ->
            val c = app.name.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (c in 'A'..'Z') c else '#'
            if (key !in map) map[key] = i
        }
        map
    }

    val listState = rememberLazyListState()
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(end = 40.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                placeholder = { MonoLabel("search apps…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = p.fg, unfocusedTextColor = p.fg,
                    focusedBorderColor = p.hairline, unfocusedBorderColor = p.hairline,
                    cursorColor = p.fg,
                ),
            )
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(filtered.size) { i -> AppRow(filtered[i], onClick = { vm.launchApp(filtered[i].packageName) }) }
            }
        }

        // Niagara-style alphabet scrubber on the right edge.
        val density = LocalDensity.current
        Box(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(40.dp).pointerInput(Unit) {
                val h = size.height.toFloat()
                detectDragGestures(
                    onDragStart = { offset ->
                        pickLetter(offset.y, h, density) { activeLetter = it; scroll(it, firstIndexByLetter, listState, scope) }
                    },
                ) { change, _ ->
                    pickLetter(change.position.y, h, density) { new ->
                        if (new != activeLetter) { activeLetter = new; scroll(new, firstIndexByLetter, listState, scope) }
                    }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.fillMaxHeight().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                alphabet.forEach { c ->
                    Text(
                        c.toString(),
                        fontFamily = LightFonts.mono,
                        fontSize = 9.sp,
                        color = if (c == activeLetter) p.accent else p.fgMuted,
                        fontWeight = if (c == activeLetter) FontWeight.W700 else FontWeight.W400,
                    )
                }
            }
        }
    }
}

private fun pickLetter(y: Float, height: Float, density: androidx.compose.ui.unit.Density, onPick: (Char) -> Unit) {
    if (height <= 0f) return
    val frac = (y / height).coerceIn(0f, 0.9999f)
    val idx = (frac * alphabet.size).toInt()
    onPick(alphabet[idx])
}

private fun scroll(
    letter: Char?,
    firstIndexByLetter: Map<Char, Int>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val idx = firstIndexByLetter[letter ?: return] ?: return
    scope.launch { listState.scrollToItem(idx) }
}

@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit) {
    val p = LocalLightPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.name, fontFamily = LightFonts.sans, fontSize = 15.sp, color = p.fg)
            Text(app.packageName, fontFamily = LightFonts.mono, fontSize = 10.sp, color = p.fgMuted)
        }
        Text("↗", fontFamily = LightFonts.mono, fontSize = 14.sp, color = p.fgMuted)
    }
}
