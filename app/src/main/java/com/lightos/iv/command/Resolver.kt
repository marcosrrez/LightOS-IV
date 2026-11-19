package com.lightos.iv.command

import com.lightos.iv.data.AppEntry
import com.lightos.iv.data.ToolId

/** Something the command line can do right now, without the network. */
sealed interface Suggestion {
    val title: String
    val detail: String?

    data class OpenApp(val app: AppEntry) : Suggestion {
        override val title get() = app.name
        override val detail: String? get() = null
    }

    data class OpenTool(val tool: ToolId) : Suggestion {
        override val title get() = tool.label
        override val detail get() = "LightOS"
    }

    data class StartFocus(val minutes: Int) : Suggestion {
        override val title get() = "Start a $minutes-minute focus"
        override val detail: String? get() = null
    }

    data class SaveNote(val text: String) : Suggestion {
        override val title get() = "Save note"
        override val detail get() = text
    }

    data class Calculate(val expression: String, val result: String) : Suggestion {
        override val title get() = "= $result"
        override val detail get() = "Copy result"
    }

    data class Ask(val text: String, val assistantReady: Boolean) : Suggestion {
        override val title get() = if (assistantReady) "Ask the assistant" else "Search the web"
        override val detail get() = text
    }
}

/**
 * The instant layer of the command line (DESIGN.md principle 3). Pure and synchronous:
 * it never touches the network, so it can run on every keystroke.
 *
 * Order: explicit commands (note, focus) → math → apps and tools → ask / web search.
 */
object Resolver {

    private val NOTE = Regex("^(?:note|jot|remember|todo)\\s*[:\\-]?\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val FOCUS = Regex("^(?:focus|timer)(?:\\s+(?:for\\s+)?(\\d{1,3})\\s*(?:m|min|mins|minutes)?)?$", RegexOption.IGNORE_CASE)
    private val FOCUS_SUFFIX = Regex("^(\\d{1,3})\\s*(?:m|min|mins|minutes)\\s+(?:focus|timer)$", RegexOption.IGNORE_CASE)

    fun resolve(
        input: String,
        apps: List<AppEntry>,
        assistantReady: Boolean,
        maxApps: Int = 4,
    ): List<Suggestion> {
        val raw = input.trim()
        if (raw.isEmpty()) return emptyList()
        val out = mutableListOf<Suggestion>()

        NOTE.matchEntire(raw)?.let { out += Suggestion.SaveNote(it.groupValues[1].trim()) }

        val focusMinutes = FOCUS.matchEntire(raw)?.let { m -> m.groupValues[1].toIntOrNull() ?: 25 }
            ?: FOCUS_SUFFIX.matchEntire(raw)?.groupValues?.get(1)?.toIntOrNull()
        if (focusMinutes != null && focusMinutes in 1..600) out += Suggestion.StartFocus(focusMinutes)

        if (MathEval.looksLikeMath(raw)) {
            MathEval.evaluate(raw)?.let { out += Suggestion.Calculate(raw, it) }
        }

        val q = AppSearch.normalize(raw)
        val tools = ToolId.entries.mapNotNull { t -> AppSearch.score(q, t.label)?.let { it to t } }
        val ranked = AppSearch.rank(raw, apps, maxApps)
        // Tools that match as well as the best app go first; weaker tool matches go after apps.
        val bestApp = ranked.firstOrNull()?.let { AppSearch.score(q, it.name) } ?: Int.MAX_VALUE
        tools.filter { it.first <= bestApp }.sortedBy { it.first }.forEach { out += Suggestion.OpenTool(it.second) }
        ranked.forEach { out += Suggestion.OpenApp(it) }
        tools.filter { it.first > bestApp }.sortedBy { it.first }.forEach { out += Suggestion.OpenTool(it.second) }

        out += Suggestion.Ask(raw, assistantReady)
        return out
    }
}
