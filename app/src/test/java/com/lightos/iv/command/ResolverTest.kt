package com.lightos.iv.command

import com.lightos.iv.data.AppEntry
import com.lightos.iv.data.ToolId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolverTest {
    private val apps = listOf("Maps", "Messages", "Notion", "Spotify").map { AppEntry(it, "pkg.${it.lowercase()}") }
    private fun first(q: String, ready: Boolean = true) = Resolver.resolve(q, apps, ready).first()

    @Test fun empty() = assertTrue(Resolver.resolve("  ", apps, true).isEmpty())
    @Test fun note() = assertEquals(Suggestion.SaveNote("buy milk"), first("note: buy milk"))
    @Test fun jot() = assertEquals(Suggestion.SaveNote("call the bank"), first("jot call the bank"))
    @Test fun focusDefault() = assertEquals(Suggestion.StartFocus(25), first("focus"))
    @Test fun focusMinutes() = assertEquals(Suggestion.StartFocus(45), first("focus 45m"))
    @Test fun focusSuffix() = assertEquals(Suggestion.StartFocus(15), first("15 min focus"))
    @Test fun math() = assertEquals(Suggestion.Calculate("45 * 12", "540"), first("45 * 12"))
    @Test fun appPrefix() = assertEquals("Spotify", (first("spo") as Suggestion.OpenApp).app.name)
    @Test fun toolBeforeWeakApp() = assertEquals(Suggestion.OpenTool(ToolId.Notes), first("notes"))
    @Test fun askIsAlwaysLast() {
        val all = Resolver.resolve("what's the weather", apps, true)
        assertEquals(Suggestion.Ask("what's the weather", true), all.last())
    }
    @Test fun webSearchWhenAssistantOff() {
        val s = Resolver.resolve("weather", apps, false).last() as Suggestion.Ask
        assertEquals(false, s.assistantReady)
    }
}
