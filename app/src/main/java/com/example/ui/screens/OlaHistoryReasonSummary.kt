package com.example.ui.screens

/**
 * Display-only compact summary for Ola's detailed direct-filter diagnostics.
 * The database keeps every decision-time failure; no filter evaluation changes.
 */
internal object OlaHistoryReasonSummary {
    fun summarize(reason: String): String? {
        if (!reason.contains("No condition matched", ignoreCase = true)) return null

        val entries = reason.lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.startsWith("Order Filter 1", ignoreCase = true) ||
                    line.startsWith("Filter 2:", ignoreCase = true) ||
                    line.startsWith("Filter 3:", ignoreCase = true) ||
                    line.startsWith("Unlimited Filter", ignoreCase = true)
            }
            .filterNot { it.endsWith(": OFF", ignoreCase = true) ||
                it.endsWith(": matched", ignoreCase = true) ||
                it.endsWith(": not matched", ignoreCase = true)
            }
            .toList()

        if (entries.isEmpty()) return "No enabled filter matched"
        val primary = entries.first()
            .replace(Regex("^Order Filter 1", RegexOption.IGNORE_CASE), "Filter 1")
            .replace(Regex("\\s*;\\s*"), " • ")
        val remaining = entries.size - 1

        return buildString {
            append("No enabled filter matched")
            append("\n")
            append(primary)
            if (remaining > 0) {
                append("\n")
                append("+")
                append(remaining)
                append(" other filter")
                if (remaining != 1) append("s")
                append(" not matched")
            }
        }
    }
}
