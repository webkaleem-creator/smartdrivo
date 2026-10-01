package com.example.engine

import androidx.compose.runtime.Immutable
import com.example.model.AreaGroup
import com.example.model.AppSettings
import com.example.model.FilterMode
import com.example.model.RideCandidate

sealed class DecisionResult {
    @Immutable
    data class Accept(val reason: String) : DecisionResult()

    @Immutable
    data class Reject(val reason: String) : DecisionResult()

    @Immutable
    data class Ignore(val reason: String) : DecisionResult()
}

object AreaRulesEngine {

    fun evaluateRide(
        candidate: RideCandidate,
        areas: List<AreaGroup> = emptyList(),
        settings: AppSettings,
        goToAreas: List<AreaGroup> = emptyList(),
        noGoAreas: List<AreaGroup> = emptyList(),
        pickupLocationTextOverride: String? = null,
        isGoToEnabled: Boolean = settings.isGoToEnabled,
        isNoGoEnabled: Boolean = settings.isNoGoEnabled
    ): DecisionResult {
        val pickupText =
            (pickupLocationTextOverride ?: candidate.pickupAddress.orEmpty())
                .trim()
                .lowercase()

        val dropText =
            "${candidate.dropAddress.orEmpty()} ${candidate.dropArea.orEmpty()}"
                .trim()
                .lowercase()

        if (candidate.isBundledOrder && !settings.isBundleOrderEnabled) {
            return DecisionResult.Ignore(
                "Bundle Order OFF â€¢ Manual action"
            )
        }

        // NO GO: DROP / DESTINATION ONLY.
        // Group name is a label only. Pickup location must NOT trigger No-Go.
        if (isNoGoEnabled) {
            val activeNoGo = noGoAreas.filter {
                it.isEnabled && it.keywords.isNotEmpty()
            }

            for (group in activeNoGo) {
                val words = group.keywords
                    .map { it.trim() }
                    .filter {
                        it.isNotBlank() &&
                        !it.equals(group.name, ignoreCase = true)
                    }

                for (word in words) {
                    if (Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(word.lowercase()) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE).containsMatchIn(dropText)) {
                        return DecisionResult.Reject(
                            "No-Go Area: destination matches '$word' in '${group.name}'"
                        )
                    }
                }
            }
        }
        // GO TO PRIORITY:
        // 1. Destination/drop can match ANY active Go-To group.
        // 2. Every active group works independently and keeps its own limits.
        // 3. If multiple groups match, accept when ANY matching group passes all 3 limits.
        // 4. Global Fare/Distance/Fastest filters are bypassed while Go-To priority is active.
        if (isGoToEnabled) {
            val activeGoTo = goToAreas.filter { it.isEnabled }

            if (activeGoTo.isNotEmpty()) {
                data class GoToMatch(
                    val group: com.example.model.AreaGroup,
                    val area: com.example.model.AreaEntry
                )

                val matches = mutableListOf<GoToMatch>()

                for (group in activeGoTo) {
                    // New per-area format.
                    if (group.areas.isNotEmpty()) {
                        for (area in group.areas) {
                            val word = area.name.trim()
                            if (
                                word.isNotBlank() &&
                                Regex(
                                    "(?<![\\p{L}\\p{N}])" +
                                        Regex.escape(word) +
                                        "(?![\\p{L}\\p{N}])",
                                    RegexOption.IGNORE_CASE
                                ).containsMatchIn(dropText)
                            ) {
                                matches.add(GoToMatch(group, area))
                            }
                        }
                    } else {
                        // Backward compatibility for old saved groups.
                        for (wordRaw in group.keywords) {
                            val word = wordRaw.trim()
                            if (
                                word.isNotBlank() &&
                                !word.equals(group.name, ignoreCase = true) &&
                                Regex(
                                    "(?<![\\p{L}\\p{N}])" +
                                        Regex.escape(word) +
                                        "(?![\\p{L}\\p{N}])",
                                    RegexOption.IGNORE_CASE
                                ).containsMatchIn(dropText)
                            ) {
                                val oldMinFare =
                                    if (group.maxFare > 0f) group.maxFare
                                    else group.minFare

                                matches.add(
                                    GoToMatch(
                                        group,
                                        com.example.model.AreaEntry(
                                            name = word,
                                            minFare = oldMinFare,
                                            maxPickupKm = group.maxPickupKm,
                                            maxDropKm = group.maxDropKm
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                if (matches.isEmpty()) {
                    return DecisionResult.Reject(
                        "Go-To Area Filter: destination not found in any active Go-To area"
                    )
                }

                val fare = candidate.fare
                val pickup = candidate.pickupDistKm
                val drop = candidate.dropDistKm
                var firstFailure: String? = null

                for (match in matches) {
                    val area = match.area

                    if (
                        area.minFare > 0f &&
                        fare != null &&
                        fare < area.minFare
                    ) {
                        if (firstFailure == null) {
                            firstFailure =
                                "Go-To '${area.name}': fare ₹${fare.toInt()} is below minimum ₹${area.minFare.toInt()}"
                        }
                        continue
                    }

                    if (
                        area.maxPickupKm > 0f &&
                        pickup != null &&
                        pickup > area.maxPickupKm
                    ) {
                        if (firstFailure == null) {
                            firstFailure =
                                "Go-To '${area.name}': pickup ${pickup}km exceeds max ${area.maxPickupKm}km"
                        }
                        continue
                    }

                    if (
                        area.maxDropKm > 0f &&
                        drop != null &&
                        drop > area.maxDropKm
                    ) {
                        if (firstFailure == null) {
                            firstFailure =
                                "Go-To '${area.name}': drop ${drop}km exceeds max ${area.maxDropKm}km"
                        }
                        continue
                    }

                    return DecisionResult.Accept(
                        "Go-To destination matched '${area.name}' and area limits passed"
                    )
                }

                return DecisionResult.Reject(
                    firstFailure ?: "Go-To Area: matched areas did not pass their limits"
                )
            }
        }

        // Normal Home filter + independent Separate Both filter.
        //
        // Home MATCH OR Separate Both MATCH = ACCEPT.
        // Both FAIL = No condition matched.
        //
        // No-Go / Go-To / Fastest rules above keep their existing priority.

        val fare = candidate.fare
        val pickup = candidate.pickupDistKm
        val drop = candidate.dropDistKm

        val homeFareMatches =
            when {
                settings.filterMode == FilterMode.DISTANCE_ONLY ->
                    true

                fare == null || fare <= 0f ->
                    true

                settings.minFare > 0f &&
                    fare < settings.minFare ->
                    false

                settings.maxFare > 0f &&
                    fare > settings.maxFare ->
                    false

                else ->
                    true
            }

        val homeDistanceMatches =
            when {
                settings.filterMode == FilterMode.FARE_ONLY ->
                    true

                pickup != null &&
                    settings.maxPickupDistanceKm > 0f &&
                    pickup > settings.maxPickupDistanceKm ->
                    false

                drop != null &&
                    settings.maxDropDistanceKm > 0f &&
                    drop > settings.maxDropDistanceKm ->
                    false

                else ->
                    true
            }

        val homeMatched =
            homeFareMatches &&
                homeDistanceMatches

        val secondaryMatched =
            settings.isSecondaryBothFilterEnabled &&
                fare != null &&
                fare > 0f &&
                pickup != null &&
                drop != null &&
                (
                    settings.secondaryBothMinFare <= 0f ||
                        fare >= settings.secondaryBothMinFare
                ) &&
                (
                    settings.secondaryBothMaxPickupDistanceKm <= 0f ||
                        pickup <=
                            settings.secondaryBothMaxPickupDistanceKm
                ) &&
                (
                    settings.secondaryBothMaxDropDistanceKm <= 0f ||
                        drop <=
                            settings.secondaryBothMaxDropDistanceKm
                )

        if (homeMatched || secondaryMatched) {
            return DecisionResult.Accept(
                when {
                    homeMatched && secondaryMatched ->
                        "Home ${settings.filterMode.displayName} and Separate Both filters matched"

                    secondaryMatched ->
                        "Separate Both filter matched"

                    else ->
                        "Home ${settings.filterMode.displayName} filter matched"
                }
            )
        }

        return DecisionResult.Reject(
            "No condition matched â€¢ Home ${settings.filterMode.displayName} failed" +
                if (settings.isSecondaryBothFilterEnabled) {
                    " â€¢ Separate Both failed"
                } else {
                    " â€¢ Separate Both OFF"
                }
        )
    }

    @JvmName("evaluateRideWithStrings")
    fun evaluateRide(
        candidate: RideCandidate,
        areas: List<AreaGroup> = emptyList(),
        settings: AppSettings,
        goToAreas: List<String>,
        noGoAreas: List<String>,
        pickupLocationTextOverride: String? = null,
        isGoToEnabled: Boolean = settings.isGoToEnabled,
        isNoGoEnabled: Boolean = settings.isNoGoEnabled
    ): DecisionResult = evaluateRide(
        candidate = candidate,
        areas = areas,
        settings = settings,
        goToAreas = goToAreas.map {
            AreaGroup(
                name = "Go-To",
                keywords = listOf(it)
            )
        },
        noGoAreas = noGoAreas.map {
            AreaGroup(
                name = "No-Go",
                keywords = listOf(it),
                type = com.example.model.AreaType.NO_GO
            )
        },
        pickupLocationTextOverride = pickupLocationTextOverride,
        isGoToEnabled = isGoToEnabled,
        isNoGoEnabled = isNoGoEnabled
    )
}
