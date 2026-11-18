package com.lightos.iv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lightos.iv.ui.theme.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("lightos_settings")

/** Agent provider config. Point baseUrl at any OpenAI-compatible endpoint. */
data class AgentConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini",
    val apiKey: String = "",
)

class SettingsRepository(context: Context) {
    private val store = context.settingsStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _surface = MutableStateFlow(Surface.Oled)
    val surface: StateFlow<Surface> = _surface.asStateFlow()

    private val _lightMode = MutableStateFlow(true)
    val lightMode: StateFlow<Boolean> = _lightMode.asStateFlow()

    private val _agentConfig = MutableStateFlow(AgentConfig())
    val agentConfig: StateFlow<AgentConfig> = _agentConfig.asStateFlow()

    init {
        scope.launch {
            val prefs = store.data.first()
            _surface.value = Surface.fromKey(prefs[SURFACE_KEY])
            _lightMode.value = prefs[LIGHT_MODE_KEY] ?: true
            _agentConfig.value = AgentConfig(
                baseUrl = prefs[BASE_URL_KEY] ?: "https://api.openai.com/v1",
                model = prefs[MODEL_KEY] ?: "gpt-4o-mini",
                apiKey = prefs[API_KEY_KEY] ?: "",
            )
        }
    }

    fun setSurface(surface: Surface) {
        _surface.value = surface
        scope.launch { store.edit { it[SURFACE_KEY] = surface.key } }
    }

    fun setLightMode(on: Boolean) {
        _lightMode.value = on
        scope.launch { store.edit { it[LIGHT_MODE_KEY] = on } }
    }

    fun setAgentConfig(config: AgentConfig) {
        _agentConfig.value = config
        scope.launch {
            store.edit {
                it[BASE_URL_KEY] = config.baseUrl
                it[MODEL_KEY] = config.model
                it[API_KEY_KEY] = config.apiKey
            }
        }
    }

    companion object {
        private val SURFACE_KEY = stringPreferencesKey("surface")
        private val LIGHT_MODE_KEY = booleanPreferencesKey("light_mode")
        private val BASE_URL_KEY = stringPreferencesKey("base_url")
        private val MODEL_KEY = stringPreferencesKey("model")
        private val API_KEY_KEY = stringPreferencesKey("api_key")
    }
}
