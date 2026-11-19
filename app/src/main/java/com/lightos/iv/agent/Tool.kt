package com.lightos.iv.agent

import android.content.Context
import com.lightos.iv.data.AppRepository
import com.lightos.iv.data.CalendarRepository
import com.lightos.iv.data.DeviceRepository
import com.lightos.iv.data.NotesRepository
import com.lightos.iv.design.Surface
import kotlinx.serialization.json.JsonObject

/** Result of executing a tool call. `content` is fed back to the model as the tool message. */
data class ToolResult(val content: String, val isError: Boolean = false) {
    companion object {
        fun ok(content: String) = ToolResult(content)
        fun err(message: String) = ToolResult(message, isError = true)
    }
}

/**
 * The bridge a tool uses to reach Android and the live UI state. Built once by
 * the view-model and passed to every tool execution. Tools should never touch the
 * view-model directly — they go through this host so side effects stay auditable.
 */
class ToolHost(
    val context: Context,
    val notes: NotesRepository,
    val apps: AppRepository,
    val calendar: CalendarRepository,
    val device: DeviceRepository,
    private val onStep: (String) -> Unit,
    private val onConfirm: suspend (String, String) -> Boolean,
    private val onSurface: (Surface) -> Unit,
    private val onTimerStart: (Int, String?) -> Unit,
    private val onStatusBarLight: (Boolean) -> Unit,
) {
    fun emitStep(text: String) = onStep(text)
    suspend fun confirm(title: String, message: String): Boolean = onConfirm(title, message)
    fun setSurface(surface: Surface) = onSurface(surface)
    fun startFocusTimer(minutes: Int, label: String?) = onTimerStart(minutes, label)
    fun statusBarLight(isLight: Boolean) = onStatusBarLight(isLight)
}

/** A single capability the agent can call. JSON schema is emitted to the model. */
interface Tool {
    val name: String
    val description: String

    /** When true, the executor asks the user to confirm before [run] is called. */
    val requiresConfirmation: Boolean get() = false

    /** JSON Schema "parameters" object describing the tool's arguments. */
    fun parameters(): JsonObject

    /** Human-readable summary of this invocation, shown in the confirm dialog / step log. */
    fun summarize(args: JsonObject): String = name

    suspend fun run(args: JsonObject, host: ToolHost): ToolResult
}
