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

        if (candidate.isBundledOrder) {
            return DecisionResult.Reject(
                "Bundle Order Filter: Bundle order detected"
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
                    if (dropText.contains(word.lowercase())) {
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
            val activeGoTo = goToAreas.filter {
                it.isEnabled && it.keywords.isNotEmpty()
            }

            if (activeGoTo.isNotEmpty()) {
                val matchingGroups = activeGoTo.filter { group ->
                    group.keywords
                        .map { it.trim() }
                        .filter {
                            it.isNotBlank() &&
                                !it.equals(group.name, ignoreCase = true)
                        }
                        .any { word ->
                            dropText.contains(word.lowercase())
                        }
                }

                if (matchingGroups.isEmpty()) {
                    return DecisionResult.Reject(
                        "Go-To Area Filter: destination not found in any active Go-To area"
                    )
                }

                val fare = candidate.fare
                val pickup = candidate.pickupDistKm
                val drop = candidate.dropDistKm
                var firstFailure: String? = null

                for (matchedGroup in matchingGroups) {
                    // Backward compatibility:
                    // Old GO TO groups stored this value in maxFare.
                    // Until that group is saved again, treat it as Minimum Fare.
                    val effectiveMinFare =
                        if (matchedGroup.maxFare > 0f) {
                            matchedGroup.maxFare
                        } else {
                            matchedGroup.minFare
                        }

                    if (
                        effectiveMinFare > 0f &&
                        fare != null &&
                        fare < effectiveMinFare
                    ) {
                        if (firstFailure == null) {
                            firstFailure =
                                "Go-To '${matchedGroup.name}': fare ₹${fare.toInt()} is below minimum ₹${effectiveMinFare.toInt()}"
                        }
                        continue
                    }

                    if (
                        matchedGroup.maxPickupKm > 0f &&
                        pickup != null &&
                        pickup > matchedGroup.maxPickupKm
                    ) {
                        if (firstFailure == null) {
                            firstFailure =
                                "Go-To '${matchedGroup.name}': pickup ${pickup}km exceeds max ${matchedGroup.maxPickupKm}km"
                        }
                        continue
                    }

                    if (
                        matchedGroup.maxDropKm > 0f &&
                        drop != null &&
                        drop > matchedGroup.maxDropKm
                    ) {
                        if (firstFailure == null) {
                            firstFailure =
                                "Go-To '${matchedGroup.name}': drop ${drop}km exceeds max ${matchedGroup.maxDropKm}km"
                        }
                        continue
                    }

                    return DecisionResult.Accept(
                        "Go-To destination matched '${matchedGroup.name}' and group limits passed"
                    )
                }

                return DecisionResult.Reject(
                    firstFailure ?: "Go-To Area: matched groups did not pass their limits"
                )
            }
        }

        // Fastest mode only when Go-To priority is not active.
        if (settings.isFastestModeEnabled) {
            val pickup = candidate.pickupDistKm
            if (
                pickup != null &&
                settings.maxPickupDistanceKm > 0f &&
                pickup > settings.maxPickupDistanceKm
            ) {
                return DecisionResult.Reject(
                    "Fastest Mode: Pickup ${pickup}km exceeds ${settings.maxPickupDistanceKm}km"
                )
            }

            return DecisionResult.Accept(
                "Fastest Mode: pickup distance passed"
            )
        }

        // Normal Fare / Distance / Both modes.
        val checkFare =
            settings.filterMode == FilterMode.FARE_ONLY ||
                settings.filterMode == FilterMode.BOTH

        val checkDistance =
            settings.filterMode == FilterMode.DISTANCE_ONLY ||
                settings.filterMode == FilterMode.BOTH

        if (checkFare) {
            val fare = candidate.fare

            if (fare != null && fare > 0f) {
                if (
                    settings.minFare > 0f &&
                    fare < settings.minFare
                ) {
                    return DecisionResult.Reject(
                        "Fare Filter: Fare ₹${fare.toInt()} is below minimum ₹${settings.minFare.toInt()}"
                    )
                }

                if (
                    settings.maxFare > 0f &&
                    fare > settings.maxFare
                ) {
                    return DecisionResult.Reject(
                        "Fare Filter: Fare ₹${fare.toInt()} exceeds maximum ₹${settings.maxFare.toInt()}"
                    )
                }
            }
        }

        if (checkDistance) {
            val pickup = candidate.pickupDistKm
            if (
                pickup != null &&
                settings.maxPickupDistanceKm > 0f &&
                pickup > settings.maxPickupDistanceKm
            ) {
                return DecisionResult.Reject(
                    "Distance Filter: Pickup ${pickup}km exceeds ${settings.maxPickupDistanceKm}km"
                )
            }

            val drop = candidate.dropDistKm
            if (
                drop != null &&
                settings.maxDropDistanceKm > 0f &&
                drop > settings.maxDropDistanceKm
            ) {
                return DecisionResult.Reject(
                    "Distance Filter: Drop ${drop}km exceeds ${settings.maxDropDistanceKm}km"
                )
            }
        }

        return DecisionResult.Accept(
            "Order satisfies ${settings.filterMode.displayName} criteria"
        )
    }

    @JvmName("evaluateRideWithStrings")
    fun evaluateRide(
        candidate: RideCandidate,
        areas: List<AreaGroup> = emptyList(),
        settings: AppSettings,
        goToAreas: List<String> = emptyList(),
        noGoAreas: List<String> = emptyList(),
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