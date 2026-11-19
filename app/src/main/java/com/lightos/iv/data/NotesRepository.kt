package com.lightos.iv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.notesStore: DataStore<Preferences> by preferencesDataStore("lightos_notes")

@Serializable
data class Note(val id: Long, val title: String, val body: String, val createdAt: Long = id)

/**
 * The notebook. Starts empty (no sample entries). Every mutation waits for the initial
 * load, so a note written in the first moments after launch can't be overwritten by it.
 */
class NotesRepository(context: Context) {
    private val store = context.notesStore
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Note.serializer())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loaded = CompletableDeferred<Unit>()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init {
        scope.launch {
            val raw = store.data.first()[KEY]
            val stored = raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()
            _notes.update { pending -> (pending + stored).distinctBy { it.id }.sortedByDescending { it.createdAt } }
            loaded.complete(Unit)
        }
    }

    fun add(title: String, body: String): Note {
        val now = System.currentTimeMillis()
        val note = Note(now, title.ifBlank { body.lineSequence().first().take(48) }, body, now)
        _notes.update { listOf(note) + it }
        persist()
        return note
    }

    fun update(id: Long, title: String, body: String) {
        _notes.update { list -> list.map { if (it.id == id) it.copy(title = title, body = body) else it } }
        persist()
    }

    fun delete(id: Long) {
        _notes.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    private fun persist() {
        scope.launch {
            loaded.await()
            val snapshot = _notes.value
            store.edit { it[KEY] = json.encodeToString(serializer, snapshot) }
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("notes_json_v2")
    }
}
