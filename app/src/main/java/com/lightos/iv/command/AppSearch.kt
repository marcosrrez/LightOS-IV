package com.lightos.iv.command

import com.lightos.iv.data.AppEntry
import java.text.Normalizer

/**
 * Ranks apps for a query the way people actually type:
 *  0. exact name          "maps"  → Maps
 *  1. name prefix         "ma"    → Maps (before Amazon)
 *  2. word prefix         "music" → YouTube Music
 *  3. initials            "gm"    → Google Maps
 *  4. substring           "tube"  → YouTube
 * Ties keep alphabetical order. Diacritics and case are ignored.
 */
object AppSearch {

    fun rank(query: String, apps: List<AppEntry>, limit: Int = Int.MAX_VALUE): List<AppEntry> {
        val q = normalize(query)
        if (q.isEmpty()) return apps.take(limit)
        return apps.asSequence()
            .mapNotNull { app -> score(q, app.name)?.let { it to app } }
            .sortedWith(compareBy<Pair<Int, AppEntry>> { it.first }.thenBy { normalize(it.second.name) })
            .map { it.second }
            .take(limit)
            .toList()
    }

    /** Lower is better; null means no match. */
    fun score(normalizedQuery: String, name: String): Int? {
        val n = normalize(name)
        if (n.isEmpty()) return null
        val q = normalizedQuery
        if (n == q) return 0
        if (n.startsWith(q)) return 1
        val words = splitWords(name).map(::normalize).filter { it.isNotEmpty() }
        if (words.drop(1).any { it.startsWith(q) }) return 2
        if (q.length >= 2 && words.size >= q.length && words.joinToString("") { it.take(1) }.startsWith(q)) return 3
        if (n.replace(" ", "").contains(q.replace(" ", ""))) return 4
        return null
    }

    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()
            .replace(NON_ALNUM, " ")
            .trim()
            .replace(SPACES, " ")

    /** Splits on spaces/punctuation and camel humps ("WhatsApp" → What, App). */
    private fun splitWords(name: String): List<String> =
        name.split(NON_ALNUM_RAW).flatMap { part -> part.split(CAMEL) }.filter { it.isNotBlank() }

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_ALNUM = Regex("[^a-z0-9]+")
    private val NON_ALNUM_RAW = Regex("[^\\p{L}\\p{N}]+")
    private val CAMEL = Regex("(?<=\\p{Ll})(?=\\p{Lu})")
    private val SPACES = Regex(" +")
}
