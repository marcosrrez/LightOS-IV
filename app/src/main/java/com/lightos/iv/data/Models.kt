package com.lightos.iv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A launchable activity. Pure data (no Android types) so search and the resolver are
 * unit-testable. `userSerial` identifies the profile (personal / work).
 */
data class AppEntry(
    val name: String,
    val packageName: String,
    val className: String = "",
    val userSerial: Long = 0L,
) {
    val key: String get() = "$packageName/$className/$userSerial"
}

/** Built-in tools that can sit on the home screen next to apps. */
@Serializable
enum class ToolId(val label: String) {
    Notes("Notes"),
    Focus("Focus"),
    Today("Today"),
    Assistant("Assistant"),
    Settings("Settings"),
}

/** One of the home-screen words: an app or a built-in tool, with an optional custom label. */
@Serializable
sealed interface HomeSlot {
    val label: String

    @Serializable
    @SerialName("app")
    data class App(
        override val label: String,
        val packageName: String,
        val className: String? = null,
    ) : HomeSlot

    @Serializable
    @SerialName("tool")
    data class Tool(val tool: ToolId, val customLabel: String? = null) : HomeSlot {
        override val label: String get() = customLabel ?: tool.label
    }
}

const val MAX_HOME_WORDS = 7
