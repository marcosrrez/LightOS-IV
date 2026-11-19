package com.lightos.iv.command

import com.lightos.iv.data.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {
    private val apps = listOf(
        "Amazon", "Maps", "Google Maps", "Messages", "YouTube Music", "YouTube", "WhatsApp", "Café Bustelo", "2FAS Auth",
    ).map { AppEntry(it, "pkg." + it.lowercase().replace(" ", "")) }

    private fun top(q: String) = AppSearch.rank(q, apps).firstOrNull()?.name

    @Test fun prefixBeatsSubstring() = assertEquals("Maps", top("ma"))
    @Test fun exactWins() = assertEquals("Maps", top("maps"))
    @Test fun wordPrefix() = assertEquals("YouTube Music", top("music"))
    @Test fun initials() = assertEquals("Google Maps", top("gm"))
    @Test fun camelHump() = assertEquals("WhatsApp", top("app"))
    @Test fun diacriticsIgnored() = assertEquals("Café Bustelo", top("cafe"))
    @Test fun digits() = assertEquals("2FAS Auth", top("2fa"))
    @Test fun substringLast() = assertEquals("YouTube", top("tube"))
    @Test fun noMatch() = assertTrue(AppSearch.rank("zzz", apps).isEmpty())
    @Test fun emptyQueryKeepsOrder() = assertEquals(apps, AppSearch.rank("  ", apps))
}
