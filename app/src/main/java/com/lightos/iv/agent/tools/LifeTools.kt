package com.lightos.iv.agent.tools

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.lightos.iv.agent.Tool
import com.lightos.iv.agent.ToolHost
import com.lightos.iv.agent.ToolResult
import com.lightos.iv.agent.int
import com.lightos.iv.agent.str
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SetTimerTool : Tool {
    override val name = "set_timer"
    override val description = "Start a focus timer that counts down on screen and chimes when done."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            put("minutes", buildJsonObject { put("type", "integer"); put("description", "duration in minutes") })
            put("label", buildJsonObject { put("type", "string"); put("description", "optional label shown on screen") })
        }
        putJsonArray("required") { add("minutes") }
    }

    override fun summarize(args: JsonObject): String =
        "focus timer ${args.int("minutes") ?: 25}m" + (args.str("label")?.let { " · $it" } ?: "")

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val mins = (args.int("minutes") ?: 25).coerceIn(1, 600)
        val label = args.str("label")
        host.startFocusTimer(mins, label)
        return ToolResult.ok("Started a ${mins}m focus timer${label?.let { " ($it)" } ?: ""}.")
    }
}

class TakeNoteTool : Tool {
    override val name = "take_note"
    override val description = "Save a note to the Commonplace notebook (persisted on device)."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            put("title", buildJsonObject { put("type", "string"); put("description", "short title") })
            put("body", buildJsonObject { put("type", "string"); put("description", "the note content") })
        }
        putJsonArray("required") { add("body") }
    }

    override fun summarize(args: JsonObject): String = "note: ${args.str("title") ?: args.str("body")?.take(40) ?: ""}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val body = args.str("body")?.trim().orEmpty()
        if (body.isBlank()) return ToolResult.err("body is required")
        val title = args.str("title")?.trim()?.ifBlank { null } ?: body.take(40)
        host.notes.add(title, body)
        return ToolResult.ok("Saved note \"$title\".")
    }
}

class ListRecentNotesTool : Tool {
    override val name = "list_recent_notes"
    override val description = "Return the owner's most recent notes."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("limit", buildJsonObject { put("type", "integer") }) }
    }

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val limit = (args.int("limit") ?: 5).coerceIn(1, 20)
        val notes = host.notes.notes.value.take(limit)
        if (notes.isEmpty()) return ToolResult.ok("No notes yet.")
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        val rendered = notes.joinToString("\n") { "- ${it.title} (${fmt.format(Date(it.createdAt))}): ${it.body.take(160)}" }
        return ToolResult.ok("${notes.size} notes:\n$rendered")
    }
}

class GetScheduleTodayTool : Tool {
    override val name = "get_schedule_today"
    override val description = "Return today's calendar events from the system calendar."

    override fun parameters(): JsonObject = buildJsonObject { put("type", "object"); putJsonObject("properties") {  } }

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        if (!host.calendar.hasPermission())
            return ToolResult.err("Calendar permission not granted. The owner can grant it in Settings → Permissions.")
        val events = try { host.calendar.today() } catch (e: SecurityException) {
            return ToolResult.err("Calendar permission not granted.")
        }
        if (events.isEmpty()) return ToolResult.ok("Nothing scheduled today.")
        val fmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getDefault() }
        val rendered = events.joinToString("\n") { e ->
            val time = if (e.allDay) "all-day" else fmt.format(Date(e.startMs))
            "- $time · ${e.title}${e.location?.let { " @ $it" } ?: ""}"
        }
        return ToolResult.ok("Today (${events.size} events):\n$rendered")
    }
}

class OpenDialerTool : Tool {
    override val name = "open_dialer"
    override val description = "Open the dialer pre-filled with a number. Does not place the call."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("number", buildJsonObject { put("type", "string"); put("description", "phone number to dial") }) }
    }

    override fun summarize(args: JsonObject): String = "dialer ${args.str("number").orEmpty()}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val number = args.str("number")?.trim().orEmpty()
        val uri = Uri.parse("tel:${Uri.encode(if (number.isBlank()) "" else number)}")
        return try {
            host.context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ToolResult.ok(if (number.isBlank()) "Opened the dialer." else "Opened the dialer for $number.")
        } catch (e: Exception) { ToolResult.err("No dialer: ${e.message}") }
    }
}

class MakeDirectCallTool : Tool {
    override val name = "make_direct_call"
    override val description = "Place a phone call directly (CALL_PHONE). Needs the call permission and owner confirmation."
    override val requiresConfirmation = true

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("number", buildJsonObject { put("type", "string"); put("description", "phone number to call") }) }
        putJsonArray("required") { add("number") }
    }

    override fun summarize(args: JsonObject): String = "call ${args.str("number").orEmpty()}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val number = args.str("number")?.trim().orEmpty()
        if (number.isBlank()) return ToolResult.err("number is required")
        val granted = ContextCompat.checkSelfPermission(host.context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return ToolResult.err("Call permission not granted. The owner can grant it in Settings → Permissions.")
        return try {
            host.context.startActivity(
                Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            ToolResult.ok("Calling $number.")
        } catch (e: Exception) { ToolResult.err("Could not place call: ${e.message}") }
    }
}

class SendSmsTool : Tool {
    override val name = "send_sms"
    override val description = "Send an SMS to a number. Needs the SMS permission and owner confirmation."
    override val requiresConfirmation = true

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            put("number", buildJsonObject { put("type", "string"); put("description", "recipient phone number") })
            put("body", buildJsonObject { put("type", "string"); put("description", "message text") })
        }
        putJsonArray("required") { add("number"); add("body") }
    }

    override fun summarize(args: JsonObject): String =
        "send SMS to ${args.str("number").orEmpty()}: ${args.str("body")?.take(60).orEmpty()}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val number = args.str("number")?.trim().orEmpty()
        val body = args.str("body")?.trim().orEmpty()
        if (number.isBlank() || body.isBlank()) return ToolResult.err("number and body are required")
        val granted = ContextCompat.checkSelfPermission(host.context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return ToolResult.err("SMS permission not granted. The owner can grant it in Settings → Permissions.")
        return try {
            SmsManager.getDefault().sendTextMessage(number, null, body, null, null)
            ToolResult.ok("Sent SMS to $number.")
        } catch (e: Exception) { ToolResult.err("Could not send SMS: ${e.message}") }
    }
}
