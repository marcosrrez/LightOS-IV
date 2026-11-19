package com.lightos.iv

import android.app.ActivityOptions
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lightos.iv.agent.Agent
import com.lightos.iv.agent.AgentEvent
import com.lightos.iv.agent.ChatMessage
import com.lightos.iv.agent.DeviceContext
import com.lightos.iv.agent.LlmClient
import com.lightos.iv.agent.ToolHost
import com.lightos.iv.agent.buildSystemPrompt
import com.lightos.iv.command.Suggestion
import com.lightos.iv.data.AgentConfig
import com.lightos.iv.data.AppEntry
import com.lightos.iv.data.CalEvent
import com.lightos.iv.data.HomeSlot
import com.lightos.iv.data.MAX_HOME_WORDS
import com.lightos.iv.data.Note
import com.lightos.iv.data.ToolId
import com.lightos.iv.design.Surface
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen { Home, Notes, Focus, Today, Settings }

/** Bottom sheets. Only one is open at a time. */
sealed interface Sheet {
    data object HomeMenu : Sheet
    data class Word(val index: Int) : Sheet
    data class PickSlot(val index: Int?) : Sheet // null = add a new word
    data class AppMenu(val app: AppEntry) : Sheet
}

data class ConfirmRequest(val title: String, val body: String, val verb: String)

data class WhisperMsg(val text: String, val id: Long = System.nanoTime())

/** Signals the home layer reacts to (drawer close, reset). */
enum class HomeSignal { GoHome, Hidden }

class LightOsViewModel(app: Application) : AndroidViewModel(app) {
    private val la = app as LightOsApp
    private val notesRepo = la.notesRepo
    private val appRepo = la.appRepo
    private val calendarRepo = la.calendarRepo
    private val deviceRepo = la.deviceRepo
    private val settingsRepo = la.settingsRepo
    private val focusCtl = la.focus

    val surface: StateFlow<Surface> = settingsRepo.surface
    val agentConfig: StateFlow<AgentConfig> = settingsRepo.agentConfig
    val notes: StateFlow<List<Note>> = notesRepo.notes
    val apps: StateFlow<List<AppEntry>> = appRepo.apps
    val focus: StateFlow<FocusState> = focusCtl.state
    val slots: StateFlow<List<HomeSlot>> = settingsRepo.slots
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepo.slots.value.orEmpty())

    // ---- Layers ----------------------------------------------------------------------
    private val stack = MutableStateFlow(listOf(Screen.Home))
    private val _screen = MutableStateFlow(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _sheet = MutableStateFlow<Sheet?>(null)
    val sheet: StateFlow<Sheet?> = _sheet.asStateFlow()

    private val _commandOpen = MutableStateFlow(false)
    val commandOpen: StateFlow<Boolean> = _commandOpen.asStateFlow()

    private val _confirm = MutableStateFlow<ConfirmRequest?>(null)
    val confirm: StateFlow<ConfirmRequest?> = _confirm.asStateFlow()
    private var confirmDeferred: CompletableDeferred<Boolean>? = null

    private val _whisper = MutableStateFlow<WhisperMsg?>(null)
    val whisper: StateFlow<WhisperMsg?> = _whisper.asStateFlow()
    private var whisperJob: Job? = null

    private val _homeSignals = MutableSharedFlow<HomeSignal>(extraBufferCapacity = 8)
    val homeSignals: SharedFlow<HomeSignal> = _homeSignals.asSharedFlow()

    /** Bumped each time home is arrived at, to replay the words' arrival motion. */
    private val _arrival = MutableStateFlow(0)
    val arrival: StateFlow<Int> = _arrival.asStateFlow()

    // ---- Assistant ---------------------------------------------------------------------
    private val _events = MutableStateFlow<List<AgentEvent>>(emptyList())
    val events: StateFlow<List<AgentEvent>> = _events.asStateFlow()
    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()
    private var conversation: List<ChatMessage> = emptyList()
    private var agentJob: Job? = null

    private val host = ToolHost(
        context = getApplication(),
        notes = notesRepo,
        apps = appRepo,
        calendar = calendarRepo,
        device = deviceRepo,
        onStep = { },
        onConfirm = { tool, summary -> requestConfirmation(tool, summary) },
        onSurface = { setSurface(it) },
        onTimerStart = { mins, _ -> focusCtl.start(mins) },
        onStatusBarLight = { },
    )

    // ---- Navigation ----------------------------------------------------------------------
    fun open(screen: Screen) {
        _sheet.value = null
        _commandOpen.value = false
        if (_screen.value == screen) return
        stack.value = if (screen == Screen.Home) listOf(Screen.Home) else listOf(Screen.Home, screen)
        _screen.value = screen
    }

    fun openTool(tool: ToolId) = when (tool) {
        ToolId.Notes -> open(Screen.Notes)
        ToolId.Focus -> open(Screen.Focus)
        ToolId.Today -> open(Screen.Today)
        ToolId.Settings -> open(Screen.Settings)
        ToolId.Assistant -> openCommand()
    }

    /** Pops one screen. The launcher never exits on back. */
    fun popScreen() {
        val s = stack.value
        if (s.size <= 1) return
        stack.value = s.dropLast(1)
        _screen.value = stack.value.last()
        if (_screen.value == Screen.Home) _arrival.value++
    }

    /** Home button: close everything and return to the resting home screen. */
    fun goHome() {
        respondConfirmation(false)
        _sheet.value = null
        _commandOpen.value = false
        val wasAway = _screen.value != Screen.Home
        stack.value = listOf(Screen.Home)
        _screen.value = Screen.Home
        _homeSignals.tryEmit(HomeSignal.GoHome)
        if (wasAway) _arrival.value++
    }

    fun onShown() {
        focusCtl.refresh()
        _arrival.value++
    }

    fun onHidden() {
        _sheet.value = null
        _homeSignals.tryEmit(HomeSignal.Hidden)
    }

    fun openSheet(sheet: Sheet) { _sheet.value = sheet }
    fun closeSheet() { _sheet.value = null }
    fun openCommand() { _sheet.value = null; _commandOpen.value = true }
    fun closeCommand() { _commandOpen.value = false }

    fun whisper(text: String) {
        whisperJob?.cancel()
        _whisper.value = WhisperMsg(text)
        whisperJob = viewModelScope.launch { delay(1_600); _whisper.value = null }
    }

    // ---- Surfaces & home words ---------------------------------------------------------------
    fun setSurface(surface: Surface) = settingsRepo.setSurface(surface)

    fun moveSlot(index: Int, delta: Int) {
        val list = slots.value.toMutableList()
        val to = index + delta
        if (index !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(index))
        settingsRepo.setSlots(list)
    }

    fun removeSlot(index: Int) {
        val list = slots.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        settingsRepo.setSlots(list)
    }

    fun putSlot(index: Int?, slot: HomeSlot) {
        val list = slots.value.toMutableList()
        if (index != null && index in list.indices) list[index] = slot
        else if (list.size < MAX_HOME_WORDS) list += slot
        settingsRepo.setSlots(list)
    }

    fun renameSlot(index: Int, label: String) {
        val list = slots.value.toMutableList()
        val s = list.getOrNull(index) ?: return
        val clean = label.trim().ifBlank { return }
        list[index] = when (s) {
            is HomeSlot.App -> s.copy(label = clean)
            is HomeSlot.Tool -> s.copy(customLabel = clean)
        }
        settingsRepo.setSlots(list)
    }

    // ---- Launching ---------------------------------------------------------------------
    /** Launches with the system clip-reveal growing out of [bounds] (window coordinates). */
    fun launch(app: AppEntry, view: View?, bounds: android.graphics.Rect?): Boolean {
        val opts = if (view != null && bounds != null) {
            ActivityOptions.makeClipRevealAnimation(view, bounds.left, bounds.top, bounds.width(), bounds.height()).toBundle()
        } else null
        val ok = appRepo.launch(app, bounds, opts)
        if (!ok) whisper("${app.name} couldn't be opened.")
        return ok
    }

    fun launchSlot(slot: HomeSlot, view: View?, bounds: android.graphics.Rect?) {
        when (slot) {
            is HomeSlot.Tool -> openTool(slot.tool)
            is HomeSlot.App -> {
                val app = appRepo.find(slot.packageName, slot.className)
                if (app != null) launch(app, view, bounds)
                else {
                    val intent = appRepo.launchIntentFor(slot.packageName)
                    if (intent != null) runCatching { getApplication<Application>().startActivity(intent) }
                    else whisper("${slot.label} isn't installed anymore. Long-press to change it.")
                }
            }
        }
    }

    fun appInfo(app: AppEntry) = appRepo.openAppInfo(app)
    fun uninstall(app: AppEntry) = appRepo.uninstall(app)

    // ---- Focus & notes ---------------------------------------------------------------------
    fun startFocus(minutes: Int) {
        focusCtl.start(minutes)
        whisper("Focus set for $minutes minutes")
    }

    fun stopFocus() = focusCtl.stop()

    fun addNote(title: String, body: String) {
        notesRepo.add(title, body)
        whisper("Saved")
    }

    fun updateNote(id: Long, title: String, body: String) = notesRepo.update(id, title, body)
    fun deleteNote(id: Long) = notesRepo.delete(id)

    // ---- Command line ------------------------------------------------------------------
    fun run(s: Suggestion, view: View?, bounds: android.graphics.Rect?) {
        when (s) {
            is Suggestion.OpenApp -> { closeCommand(); launch(s.app, view, bounds) }
            is Suggestion.OpenTool -> openTool(s.tool)
            is Suggestion.StartFocus -> { closeCommand(); startFocus(s.minutes) }
            is Suggestion.SaveNote -> { closeCommand(); addNote("", s.text) }
            is Suggestion.Calculate -> {
                val cm = getApplication<Application>().getSystemService(ClipboardManager::class.java)
                cm?.setPrimaryClip(ClipData.newPlainText("result", s.result))
                whisper("Copied ${s.result}")
            }
            is Suggestion.Ask -> if (s.assistantReady) ask(s.text) else webSearch(s.text)
        }
    }

    private fun webSearch(q: String) {
        closeCommand()
        val ctx = getApplication<Application>()
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", q).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val ok = runCatching { ctx.startActivity(search) }.isSuccess
        if (!ok) runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    // ---- Assistant ---------------------------------------------------------------------
    fun ask(text: String) {
        if (text.isBlank()) return
        val cfg = agentConfig.value
        if (!cfg.isReady) {
            _events.value = _events.value + AgentEvent(AgentEvent.Kind.Error, "Add an assistant provider in Settings to ask questions.")
            return
        }
        _events.value = _events.value + AgentEvent(AgentEvent.Kind.Thought, text)
        _thinking.value = true
        agentJob?.cancel()
        agentJob = viewModelScope.launch {
            val agent = Agent(
                client = LlmClient(cfg.baseUrl, cfg.apiKey),
                tools = la.toolSet,
                systemPrompt = { buildSystemPrompt(deviceContext()) },
            )
            conversation = agent.run(cfg.model, conversation.takeLast(24), text, host) { ev ->
                _events.value = _events.value + ev
            }
            _thinking.value = false
        }
    }

    fun stopAsking() {
        agentJob?.cancel()
        _thinking.value = false
        respondConfirmation(false)
    }

    fun resetConversation() {
        stopAsking()
        conversation = emptyList()
        _events.value = emptyList()
    }

    private suspend fun requestConfirmation(tool: String, summary: String): Boolean {
        val (title, verb) = when (tool) {
            "make_direct_call" -> "Place this call?" to "Call"
            "send_sms" -> "Send this text?" to "Send"
            else -> "Allow this action?" to "Allow"
        }
        val d = CompletableDeferred<Boolean>()
        confirmDeferred = d
        _confirm.value = ConfirmRequest(title, summary, verb)
        return try { d.await() } finally {
            _confirm.value = null
            confirmDeferred = null
        }
    }

    fun respondConfirmation(approved: Boolean) {
        confirmDeferred?.complete(approved)
    }

    private suspend fun deviceContext(): DeviceContext = withContext(Dispatchers.IO) {
        val status = deviceRepo.status()
        val clock = SimpleDateFormat("h:mm a, EEEE, MMMM d", Locale.getDefault()).format(Date())
        val nextEvent: String? = try {
            calendarRepo.today().firstOrNull { it.startMs >= System.currentTimeMillis() }
                ?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it.startMs)) + " " + it.title }
        } catch (_: SecurityException) { null }
        DeviceContext(
            clockText = clock,
            surface = surface.value.label,
            installedAppCount = apps.value.size,
            batteryPercent = status.batteryPercent,
            charging = status.charging,
            dnd = status.dnd,
            nextEvent = nextEvent,
        )
    }

    // ---- Settings & data ---------------------------------------------------------------
    fun setAgentConfig(config: AgentConfig) {
        settingsRepo.setAgentConfig(config)
        whisper("Assistant saved")
    }

    suspend fun calendarToday(): List<CalEvent>? = withContext(Dispatchers.IO) {
        if (!calendarRepo.hasPermission()) null else runCatching { calendarRepo.today() }.getOrDefault(emptyList())
    }

    fun calendarHasPermission(): Boolean = calendarRepo.hasPermission()
}
