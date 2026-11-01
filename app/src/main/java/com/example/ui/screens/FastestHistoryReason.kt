package com.example.ui.screens

import java.util.Locale

/**
 * Display-only upgrade for Fastest decisions recorded before the specific
 * failure reasons existed. Uses the limit preserved in the original reason,
 * never the user's currently changed filter setting.
 */
internal object FastestHistoryReason {
    private val legacyPattern = Regex(
        """Mode:\s*Fastest\s*\|\s*Pickup unknown, invalid or exceeds saved max\s*([0-9]+(?:\.[0-9]+)?)\s*km""",
        RegexOption.IGNORE_CASE
    )

    fun clarify(storedReason: String, pickupKm: Float): String? {
        val match = legacyPattern.find(storedReason) ?: return null
        val savedMax = match.groupValues[1].toFloatOrNull() ?: return null
        if (!savedMax.isFinite() || savedMax <= 0f) return null
        val limitText = String.format(Locale.ENGLISH, "%.1f", savedMax)
        return when {
            !pickupKm.isFinite() || pickupKm < 0f ->
                "Mode: Fastest | Pickup distance invalid (saved max $limitText km)"
            pickupKm > savedMax ->
                "Mode: Fastest | Pickup ${String.format(Locale.ENGLISH, "%.1f", pickupKm)} km exceeds saved max $limitText km"
            else ->
                // An old History row can store 0 for a missing distance; never
                // invent a claimed measured distance in that case.
                "Mode: Fastest | Pickup could not be verified (saved max $limitText km)"
        }
    }
}
