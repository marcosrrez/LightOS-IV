package com.lightos.iv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lightos.iv.design.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("lightos_settings")

/** Agent provider config. Any OpenAI-compatible endpoint. */
data class AgentConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini",
    val apiKey: String = "",
) {
    val isLocal: Boolean
        get() = baseUrl.contains("://localhost") || baseUrl.contains("://127.0.0.1") || baseUrl.contains("://10.0.2.2")
    val isReady: Boolean get() = baseUrl.isNotBlank() && model.isNotBlank() && (apiKey.isNotBlank() || isLocal)
}

/**
 * Settings, read once synchronously at startup (a few ms) so the first frame already uses
 * the saved surface instead of flashing the default one.
 */
class SettingsRepository(context: Context) {
    private val store = context.settingsStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val slotSerializer = ListSerializer(HomeSlot.serializer())

    private val initial: Preferences = runBlocking { store.data.first() }

    private val _surface = MutableStateFlow(Surface.fromKey(initial[SURFACE]))
    val surface: StateFlow<Surface> = _surface.asStateFlow()

    private val _agentConfig = MutableStateFlow(
        AgentConfig(
            baseUrl = initial[BASE_URL] ?: AgentConfig().baseUrl,
            model = initial[MODEL] ?: AgentConfig().model,
            apiKey = initial[API_KEY] ?: "",
        )
    )
    val agentConfig: StateFlow<AgentConfig> = _agentConfig.asStateFlow()

    /** Null until first run has chosen defaults. */
    private val _slots = MutableStateFlow(
        initial[SLOTS]?.let { runCatching { json.decodeFromString(slotSerializer, it) }.getOrNull() }
    )
    val slots: StateFlow<List<HomeSlot>?> = _slots.asStateFlow()

    private val _focusEndsAt = MutableStateFlow(initial[FOCUS_END] ?: 0L)
    val focusEndsAt: StateFlow<Long> = _focusEndsAt.asStateFlow()

    fun setSurface(surface: Surface) {
        _surface.value = surface
        scope.launch { store.edit { it[SURFACE] = surface.key } }
    }

    fun setAgentConfig(config: AgentConfig) {
        _agentConfig.value = config
        scope.launch {
            store.edit {
                it[BASE_URL] = config.baseUrl
                it[MODEL] = config.model
                it[API_KEY] = config.apiKey
            }
        }
    }

    fun setSlots(slots: List<HomeSlot>) {
        _slots.value = slots
        scope.launch { store.edit { it[SLOTS] = json.encodeToString(slotSerializer, slots) } }
    }

    fun setFocusEndsAt(epochMs: Long) {
        _focusEndsAt.value = epochMs
        scope.launch { store.edit { it[FOCUS_END] = epochMs } }
    }

    companion object {
        private val SURFACE = stringPreferencesKey("surface")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL = stringPreferencesKey("model")
        private val API_KEY = stringPreferencesKey("api_key")
        private val SLOTS = stringPreferencesKey("home_slots_v1")
        private val FOCUS_END = longPreferencesKey("focus_ends_at")
    }
}
