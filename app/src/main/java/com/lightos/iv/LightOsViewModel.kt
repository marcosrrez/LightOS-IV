package com.lightos.iv

import android.app.Application
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lightos.iv.agent.Agent
import com.lightos.iv.agent.AgentEvent
import com.lightos.iv.agent.ChatMessage
import com.lightos.iv.agent.DeviceContext
import com.lightos.iv.agent.LlmClient
import com.lightos.iv.agent.ToolHost
import com.lightos.iv.agent.buildSystemPrompt
import com.lightos.iv.data.AgentConfig
import com.lightos.iv.data.AppEntry
import com.lightos.iv.data.CalEvent
import com.lightos.iv.data.Note
import com.lightos.iv.ui.theme.Surface
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen { Home, Apps, Notes, Timer, Report, Settings }

data class ConfirmationRequest(val title: String, val message: String)

data class TimerUiState(val running: Boolean, val remainingMs: Long, val label: String?)

class LightOsViewModel(app: Application) : AndroidViewModel(app) {
    private val la = app as LightOsApp
    private val notesRepo = la.notesRepo
    private val appRepo = la.appRepo
    private val calendarRepo = la.calendarRepo
    private val deviceRepo = la.deviceRepo
    private val settingsRepo = la.settingsRepo

    val surface: StateFlow<Surface> = settingsRepo.surface
    val lightMode: StateFlow<Boolean> = settingsRepo.lightMode
    val agentConfig: StateFlow<AgentConfig> = settingsRepo.agentConfig
    val notes: StateFlow<List<Note>> = notesRepo.notes
    val installedApps: StateFlow<List<AppEntry>> = appRepo.apps

    // Navigation
    private val _screenStack = MutableStateFlow(listOf(Screen.Home))
    private val _current = MutableStateFlow(Screen.Home)
    val current: StateFlow<Screen> = _current.asStateFlow()

    private val _agentOpen = MutableStateFlow(false)
    val agentOpen: StateFlow<Boolean> = _agentOpen.asStateFlow()

    // Agent conversation + trace
    private val _events = MutableStateFlow<List<AgentEvent>>(emptyList())
    val events: StateFlow<List<AgentEvent>> = _events.asStateFlow()
    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()
    private var conversation: List<ChatMessage> = emptyList()
    private val _lastAnswer = MutableStateFlow<String?>(null)
    val lastAnswer: StateFlow<String?> = _lastAnswer.asStateFlow()

    // Confirmation gate
    private val _pendingConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = _pendingConfirmation.asStateFlow()
    private var confirmDeferred: CompletableDeferred<Boolean>? = null

    // Timer (wall-clock based so it survives backgrounding)
    private val _timer = MutableStateFlow(TimerUiState(false, 0L, null))
    val timer: StateFlow<TimerUiState> = _timer.asStateFlow()
    private var timerEndMs: Long = 0L
    private var tickJob: kotlinx.coroutines.Job? = null

    private val host = ToolHost(
        context = getApplication(),
        notes = notesRepo,
        apps = appRepo,
        calendar = calendarRepo,
        device = deviceRepo,
        onStep = {  },
        onConfirm = { title, msg -> requestConfirmation(title, msg) },
        onSurface = { setSurface(it) },
        onTimerStart = { mins, label -> startFocusTimer(mins, label) },
        onStatusBarLight = {  },
    )

    // ---- Navigation ---------------------------------------------------------
    fun navigate(screen: Screen) {
        if (_current.value == screen) return
        _screenStack.update { it + screen }
        _current.value = screen
    }

    fun onBack() {
        val stack = _screenStack.value
        if (stack.size <= 1) return // launcher stays on home
        val next = stack.dropLast(1)
        _screenStack.value = next
        _current.value = next.last()
    }

    fun openAgent(value: Boolean) { _agentOpen.value = value }

    // ---- Surfaces -----------------------------------------------------------
    fun setSurface(surface: Surface) { settingsRepo.setSurface(surface) }
    fun setLightMode(on: Boolean) { settingsRepo.setLightMode(on) }

    // ---- Notes -------------------------------------------------------------
    fun addNote(title: String, body: String) = notesRepo.add(title, body)
    fun deleteNote(id: Long) = notesRepo.delete(id)

    // ---- Timer -------------------------------------------------------------
    fun startFocusTimer(minutes: Int, label: String?) {
        timerEndMs = System.currentTimeMillis() + minutes * 60_000L
        _timer.value = TimerUiState(true, minutes * 60_000L, label)
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val remaining = timerEndMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _timer.value = TimerUiState(false, 0L, label)
                    chime()
                    return@launch
                }
                _timer.value = TimerUiState(true, remaining, label)
                delay(1000)
            }
        }
    }

    fun stopTimer() {
        tickJob?.cancel()
        _timer.value = TimerUiState(false, 0L, null)
    }

    private fun chime() {
        try {
            val v = getApplication<Application>().getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(120, 80))
            } else {
                @Suppress("DEPRECATION") v.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    // ---- Confirmation gate --------------------------------------------------
    private suspend fun requestConfirmation(title: String, message: String): Boolean {
        val d = CompletableDeferred<Boolean>()
        confirmDeferred = d
        _pendingConfirmation.value = ConfirmationRequest(title, message)
        val result = d.await()
        _pendingConfirmation.value = null
        confirmDeferred = null
        return result
    }

    fun respondConfirmation(approved: Boolean) {
        confirmDeferred?.complete(approved)
    }

    // ---- Agent -------------------------------------------------------------
    fun sendAgentMessage(text: String) {
        if (text.isBlank()) return
        val cfg = agentConfig.value
        if (cfg.apiKey.isBlank() && !cfg.baseUrl.contains("localhost") && !cfg.baseUrl.contains("127.0.0.1")) {
            _events.value = _events.value + AgentEvent(AgentEvent.Kind.Error, "Set an API key in Settings first (or point the base URL at a local server).")
            _agentOpen.value = true
            return
        }
        _agentOpen.value = true
        _thinking.value = true
        _events.value = _events.value + AgentEvent(AgentEvent.Kind.Thought, "› $text")
        viewModelScope.launch {
            // Rebuild the client with current config (config may change between runs).
            val liveAgent = Agent(
                client = LlmClient(cfg.baseUrl, cfg.apiKey),
                tools = la.toolSet,
                systemPrompt = { buildSystemPrompt(deviceContext()) },
            )
            conversation = liveAgent.run(cfg.model, conversation, text, host) { ev ->
                _events.value = _events.value + ev
                if (ev.kind == AgentEvent.Kind.Answer) _lastAnswer.value = ev.text
            }
            _thinking.value = false
        }
    }

    fun resetAgent() {
        conversation = emptyList()
        _events.value = emptyList()
        _lastAnswer.value = null
    }

    private fun deviceContext(): DeviceContext {
        val status = deviceRepo.status()
        val clock = SimpleDateFormat("h:mm a, EEE", Locale.getDefault()).format(Date())
        val nextEvent: String? = try {
            calendarRepo.today().firstOrNull { it.startMs >= System.currentTimeMillis() }
                ?.let { SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(it.startMs)) + " " + it.title }
        } catch (_: SecurityException) { null }
        return DeviceContext(
            clockText = clock,
            surface = surface.value.label,
            installedAppCount = installedApps.value.size,
            batteryPercent = status.batteryPercent,
            charging = status.charging,
            dnd = status.dnd,
            nextEvent = nextEvent,
        )
    }

    fun refreshApps() = appRepo.load()

    fun launchApp(packageName: String): Boolean {
        val intent = appRepo.launchIntentFor(packageName) ?: return false
        return try { getApplication<Application>().startActivity(intent); true }
        catch (e: Exception) { false }
    }

    @Suppress("unused")
    suspend fun calendarEvents(): List<CalEvent> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try { calendarRepo.today() } catch (_: Exception) { emptyList() }
    }

    fun calendarHasPermission(): Boolean = calendarRepo.hasPermission()
}
