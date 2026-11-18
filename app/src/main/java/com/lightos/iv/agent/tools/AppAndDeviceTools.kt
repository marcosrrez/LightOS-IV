package com.lightos.iv.agent.tools

import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import com.lightos.iv.agent.Tool
import com.lightos.iv.agent.ToolHost
import com.lightos.iv.agent.ToolResult
import com.lightos.iv.agent.bool
import com.lightos.iv.agent.int
import com.lightos.iv.agent.str
import com.lightos.iv.ui.theme.Surface
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Locale

class ListInstalledAppsTool(private val maxResults: Int = 60) : Tool {
    override val name = "list_installed_apps"
    override val description = "List the installed apps the owner can launch. Returns name + packageName."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("limit", buildJsonObject { put("type", "integer"); put("description", "max apps to return") }) }
        putJsonArray("required") {  }
    }

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val limit = (args.int("limit") ?: maxResults).coerceAtLeast(1)
        val apps = host.apps.apps.value.sortedBy { it.name }
        if (apps.isEmpty()) return ToolResult.ok("No apps loaded yet.")
        val rendered = apps.take(limit).joinToString("\n") { "- ${it.name} (${it.packageName})" }
        return ToolResult.ok("${apps.size} apps installed. First $limit:\n$rendered")
    }
}

class SearchAppsTool : Tool {
    override val name = "search_apps"
    override val description = "Find installed apps whose name or package matches the query."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("query", buildJsonObject { put("type", "string"); put("description", "app name to match") }) }
        putJsonArray("required") { put("query") }
    }

    override fun summarize(args: JsonObject): String = "search apps: \"${args.str("query").orEmpty()}\""

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val q = args.str("query")?.lowercase()?.trim().orEmpty()
        if (q.isBlank()) return ToolResult.ok("No query provided.")
        val matches = host.apps.apps.value.filter {
            it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
        if (matches.isEmpty()) return ToolResult.ok("No apps matched \"$q\".")
        return ToolResult.ok(matches.joinToString("\n") { "- ${it.name} (${it.packageName})" })
    }
}

class OpenAppTool : Tool {
    override val name = "open_app"
    override val description = "Launch an installed app by package name. Returns whether it opened."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("packageName", buildJsonObject { put("type", "string"); put("description", "Android package name, e.g. com.spotify.music") }) }
        putJsonArray("required") { put("packageName") }
    }

    override fun summarize(args: JsonObject): String = "open ${args.str("packageName").orEmpty()}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val pkg = args.str("packageName")?.trim().orEmpty()
        if (pkg.isBlank()) return ToolResult.err("packageName is required")
        val intent = host.apps.launchIntentFor(pkg)
            ?: return ToolResult.err("No launchable app for package \"$pkg\". Use search_apps to find the right one.")
        return try {
            host.context.startActivity(intent)
            ToolResult.ok("Opened $pkg.")
        } catch (e: Exception) {
            ToolResult.err("Could not open $pkg: ${e.message}")
        }
    }
}

class RouteToTool : Tool {
    override val name = "route_to"
    override val description = "Open turn-by-turn navigation to a destination."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("destination", buildJsonObject { put("type", "string"); put("description", "address or place name") }) }
        putJsonArray("required") { put("destination") }
    }

    override fun summarize(args: JsonObject): String = "navigate to ${args.str("destination").orEmpty()}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val dest = args.str("destination")?.trim().orEmpty()
        if (dest.isBlank()) return ToolResult.err("destination is required")
        val uri = "google.navigation:q=${Uri.encode(dest)}"
        return try {
            host.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ToolResult.ok("Routing to $dest.")
        } catch (e: Exception) {
            // Fallback to a web maps URL.
            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(dest)}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { host.context.startActivity(web); ToolResult.ok("Routing to $dest (web).") }
            catch (e2: Exception) { ToolResult.err("No maps app: ${e2.message}") }
        }
    }
}

class SearchWebTool : Tool {
    override val name = "search_web"
    override val description = "Open a web search in the browser."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("query", buildJsonObject { put("type", "string") }) }
        putJsonArray("required") { put("query") }
    }

    override fun summarize(args: JsonObject): String = "search web: ${args.str("query").orEmpty()}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val q = args.str("query")?.trim().orEmpty()
        if (q.isBlank()) return ToolResult.err("query is required")
        return try {
            host.context.startActivity(
                Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", q).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            ToolResult.ok("Searching the web for \"$q\".")
        } catch (e: Exception) {
            val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}")
            try {
                host.context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                ToolResult.ok("Searching the web for \"$q\".")
            } catch (e2: Exception) { ToolResult.err("No browser: ${e2.message}") }
        }
    }
}

class ToggleTorchTool : Tool {
    override val name = "toggle_torch"
    override val description = "Turn the flashlight on or off."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("on", buildJsonObject { put("type", "boolean"); put("description", "true to turn on, false to turn off") }) }
        putJsonArray("required") { put("on") }
    }

    override fun summarize(args: JsonObject): String = "torch ${if (args.bool("on") == true) "on" else "off"}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val on = args.bool("on") ?: true
        return try {
            val cm = host.context.getSystemService(android.content.Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
                ?: return ToolResult.err("No camera with a flash")
            cm.setTorchMode(id, on)
            ToolResult.ok("Torch ${if (on) "on" else "off"}.")
        } catch (e: SecurityException) {
            ToolResult.err("Camera permission required to control the torch. Grant Camera in Settings → Permissions.")
        } catch (e: Exception) {
            ToolResult.err("Torch failed: ${e.message}")
        }
    }
}

class GetDeviceStatusTool : Tool {
    override val name = "get_device_status"
    override val description = "Return battery percent, charging state, and Do-Not-Disturb."

    override fun parameters(): JsonObject = buildJsonObject { put("type", "object"); putJsonObject("properties") {  } }

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val s = host.device.status()
        return ToolResult.ok("Battery ${if (s.batteryPercent < 0) "unknown" else "${s.batteryPercent}%"} (${if (s.charging) "charging" else "on battery"}), DND ${if (s.dnd) "on" else "off"}.")
    }
}

class SetSurfaceTool : Tool {
    override val name = "set_surface"
    override val description = "Change the display surface. Surface one of: oled, glass, paper, amber, matte."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("surface", buildJsonObject { put("type", "string"); put("description", "oled | glass | paper | amber | matte") }) }
        putJsonArray("required") { put("surface") }
    }

    override fun summarize(args: JsonObject): String = "set surface ${args.str("surface").orEmpty()}"

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val key = args.str("surface")?.trim()?.lowercase().orEmpty()
        val surface = Surface.entries.firstOrNull { it.key == key }
            ?: return ToolResult.err("Unknown surface \"$key\". Use oled, glass, paper, amber, or matte.")
        host.setSurface(surface)
        host.statusBarLight(surface == Surface.Paper || surface == Surface.Amber)
        return ToolResult.ok("Surface set to ${surface.label}.")
    }
}

class ReadAloudTool : Tool {
    override val name = "read_aloud"
    override val description = "Speak text aloud using the system text-to-speech engine."

    override fun parameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("text", buildJsonObject { put("type", "string"); put("description", "text to speak") }) }
        putJsonArray("required") { put("text") }
    }

    override fun summarize(args: JsonObject): String = "read aloud"

    private var tts: TextToSpeech? = null

    override suspend fun run(args: JsonObject, host: ToolHost): ToolResult {
        val text = args.str("text")?.trim().orEmpty()
        if (text.isBlank()) return ToolResult.err("text is required")
        return try {
            if (tts == null) tts = TextToSpeech(host.context) { /* init */ }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lightos_tts")
            ToolResult.ok("Reading aloud.")
        } catch (e: Exception) {
            ToolResult.err("TTS failed: ${e.message}")
        }
    }
}
