package com.example.engine

import com.example.model.AreaGroup
import com.example.model.RideCandidate

data class FixAreaSettings(
    val enabled: Boolean = false,
    val words: List<String> = emptyList(),
    val maxPickupKm: Float = 0.7f
)

object FixAreaRuleEngine {

    private val patternCache =
        object : LinkedHashMap<String, Regex>(32, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, Regex>?
            ): Boolean = size > 64
        }

    private fun matches(word: String, destination: String): Boolean {
        val pattern = synchronized(patternCache) {
            patternCache[word] ?: Regex(
                "(?<![\\p{L}\\p{N}])" +
                    Regex.escape(word) +
                    "(?![\\p{L}\\p{N}])",
                RegexOption.IGNORE_CASE
            ).also { patternCache[word] = it }
        }
        return pattern.containsMatchIn(destination)
    }

    fun evaluate(
        candidate: RideCandidate,
        fix: FixAreaSettings,
        noGoAreas: List<AreaGroup>,
        noGoEnabled: Boolean,
        bundleAllowed: Boolean
    ): DecisionResult? {
        if (!fix.enabled || fix.words.isEmpty()) return null

        // Existing Bundle OFF behavior must not be bypassed by Fix Area.
        if (candidate.isBundledOrder && !bundleAllowed) return null

        val pickup = candidate.pickupDistKm ?: return null
        if (!pickup.isFinite() || pickup < 0f ||
            !fix.maxPickupKm.isFinite() || fix.maxPickupKm <= 0f ||
            pickup > fix.maxPickupKm
        ) return null

        // A Fix Area match must not bypass the mandatory positive fare/trip guards.
        if (candidate.fare == null || !candidate.fare.isFinite() || candidate.fare <= 0f ||
            !RapidoDistanceValidity.isValidDrop(candidate.dropDistKm)
        ) return null

        val drop =
            "${candidate.dropAddress.orEmpty()} ${candidate.dropArea.orEmpty()}"
                .trim()

        if (drop.isBlank() ||
            drop.equals("Address unavailable", ignoreCase = true)
        ) return null

        val fixedWord = fix.words.take(10).firstOrNull { raw ->
            val word = raw.trim()
            word.isNotEmpty() && matches(word, drop)
        } ?: return null

        // No-Go veto is mandatory even when Fix Area matches.
        if (noGoEnabled) {
            for (group in noGoAreas) {
                if (!group.isEnabled) continue

                for (raw in group.keywords) {
                    val word = raw.trim()

                    if (word.isBlank() ||
                        word.equals(group.name, ignoreCase = true)
                    ) continue

                    if (matches(word, drop)) {
                        return DecisionResult.Reject(
                            "No-Go Area: destination matches '$word' in '${group.name}'"
                        )
                    }
                }
            }
        }

        return DecisionResult.Accept(
            "Mode: Fix Area | Destination '$fixedWord' matched" +
                " | Pickup ${pickup}km <= ${fix.maxPickupKm}km"
        )
    }
}