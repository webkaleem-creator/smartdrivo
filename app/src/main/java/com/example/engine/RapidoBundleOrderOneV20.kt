package com.example.engine

/**
 * Selects only Order 1 from a Rapido Bundle card. The combined header fare and
 * all Order 2 text must be excluded BEFORE normal fare/km/address parsing.
 *
 * Missing or ambiguous section boundaries fail closed (null): never assume
 * the first visible amount or trip number belongs to Order 1.
 */
object RapidoBundleOrderOneV20 {
    private val orderOne = Regex("(?i)\\border\\s*1\\b")
    private val orderTwo = Regex("(?i)\\border\\s*2\\b")
    private val exactOne = Regex("(?i)^order\\s*1$")
    private val exactTwo = Regex("(?i)^order\\s*2$")

    data class Part(val index: Int, val text: String)

    fun selectParts(texts: List<String>): List<Part>? {
        // Prefer dedicated accessibility label nodes to an aggregate parent
        // containing both Order 1 and Order 2 text.
        val begin = texts.indexOfFirst { exactOne.matches(it.trim()) }
        if (begin >= 0) {
            val end = (begin + 1 until texts.size).firstOrNull { exactTwo.matches(texts[it].trim()) }
                ?: return null
            if (end <= begin + 1) return null
            val scoped = (begin + 1 until end).mapNotNull { i ->
                val text = texts[i].trim()
                if (text.isBlank() || orderOne.containsMatchIn(text) ||
                    orderTwo.containsMatchIn(text) ||
                    text.contains("Bundle Order", ignoreCase = true)
                ) null else Part(i, text)
            }
            return scoped.takeIf { it.isNotEmpty() }
        }

        // Fallback for an accessibility tree that exposes the entire card or
        // an Order 1 row as one text node rather than separate label nodes.
        val parts = mutableListOf<Part>()
        var started = false
        var closed = false
        for ((index, raw) in texts.withIndex()) {
            if (closed) break
            var line = raw
            if (!started) {
                val one = orderOne.find(line) ?: continue
                started = true
                line = line.substring(one.range.last + 1)
            }
            val two = orderTwo.find(line)
            if (two != null) {
                line = line.substring(0, two.range.first)
                closed = true
            }
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() &&
                !trimmed.contains("Bundle Order", ignoreCase = true)) {
                parts += Part(index, trimmed)
            }
        }
        // An incomplete card without a visible Order 2 boundary cannot be
        // conclusively isolated; request the next accessibility event.
        return parts.takeIf { started && closed && it.isNotEmpty() }
    }

    fun <T> select(
        entries: List<T>,
        textOf: (T) -> String,
        withText: (T, String) -> T
    ): List<T>? = selectParts(entries.map(textOf))?.map { part ->
        withText(entries[part.index], part.text)
    }
}
