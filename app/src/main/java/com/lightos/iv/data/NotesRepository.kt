package com.lightos.iv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.notesStore: DataStore<Preferences> by preferencesDataStore("lightos_notes")

@Serializable
data class Note(val id: Long, val title: String, val body: String, val date: String)

/** Persisted Commonplace notebook. */
class NotesRepository(context: Context) {
    private val store = context.notesStore
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _notes = MutableStateFlow(defaultNotes())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init {
        scope.launch {
            val raw = store.data.map { it[KEY] }.first()
            _notes.value = raw?.let { runCatching { json.decodeFromString<List<Note>>(it) }.getOrNull() } ?: defaultNotes()
        }
    }

    fun add(title: String, body: String) {
        val note = Note(System.currentTimeMillis(), title.ifBlank { body.take(26) }, body, today())
        _notes.value = listOf(note) + _notes.value
        persist()
    }

    fun update(id: Long, title: String, body: String) {
        _notes.value = _notes.value.map { if (it.id == id) it.copy(title = title, body = body) else it }
        persist()
    }

    fun delete(id: Long) {
        _notes.value = _notes.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() {
        scope.launch { store.edit { it[KEY] = json.encodeToString(_notes.value) } }
    }

    private fun today(): String = SimpleDateFormat("MMM d", Locale.US).format(Date())

    private fun defaultNotes() = listOf(
        Note(1, "Marcus Aurelius · Meditations", "> \"Nowhere either with more quiet or more freedom from trouble does a man retire than into his own soul.\"", "Clipped from Kindle"),
    )

    companion object {
        private val KEY = stringPreferencesKey("notes_json")
    }
}
