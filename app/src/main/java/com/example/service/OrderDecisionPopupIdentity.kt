package com.example.service

/**
 * Pure popup identity and timing policy.
 * Accessibility events for the SAME visible offer may carry different 13-digit
 * detection timestamps. Treat those as one offer; real booking IDs stay exact.
 */
internal object OrderDecisionPopupIdentity {
    const val DISPLAY_MS = 5_000L
    const val DEDUPE_MS = 5_000L
    private val timestampedRide = Regex("^ride_(.+)_([0-9]{13})$")

    fun canonical(recordKey: String): String {
        val key = recordKey.trim()
        val match = timestampedRide.matchEntire(key) ?: return key
        return "ride_" + match.groupValues[1]
    }

    fun isCurrent(latestKey: String, recordKey: String): Boolean {
        if (latestKey.isBlank() || recordKey.isBlank()) return true
        return canonical(latestKey) == canonical(recordKey)
    }

    fun isDuplicate(lastShownAt: Long?, nowMillis: Long): Boolean =
        lastShownAt != null && nowMillis >= lastShownAt &&
            nowMillis - lastShownAt < DEDUPE_MS
}
