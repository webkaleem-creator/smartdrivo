package com.example.engine

import com.example.model.AreaGroup
import com.example.model.AreaType
import com.example.model.AppSettings
import com.example.model.FilterMode
import com.example.model.RideCandidate

sealed class DecisionResult {
    data class Accept(val reason: String) : DecisionResult()
    data class Reject(val reason: String) : DecisionResult()
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
        // 0. Fastest Mode Check
        // When Fastest Mode is ON: accept every ride, skip fare/area filters, only check Maximum Pickup Distance km
        if (settings.isFastestModeEnabled) {
            val pickup = candidate.pickupDistKm
            if (pickup != null && settings.maxPickupDistanceKm > 0 && pickup > settings.maxPickupDistanceKm) {
                return DecisionResult.Reject("Fastest Mode: Pickup ${pickup}km exceeds maximum limit (${settings.maxPickupDistanceKm}km)")
            }
            return DecisionResult.Accept("Fastest Mode: Order accepted (only pickup distance checked)")
        }

        // 1. Bundle order check
        if (candidate.isBundledOrder) {
            return DecisionResult.Reject("Bundle Order Filter: Bundle order detected - Auto rejected")
        }

        val pickupText = (pickupLocationTextOverride ?: candidate.pickupAddress.orEmpty()).trim().lowercase()
        val dropText = "${candidate.dropAddress.orEmpty()} ${candidate.dropArea.orEmpty()}".trim().lowercase()

        // 2. NO-GO Area Check
        // If No-Go areas list is empty OR No-Go toggle is OFF: SKIP No-Go filter completely.
        val activeNoGoList = if (isNoGoEnabled) {
            noGoAreas.filter { it.isEnabled }
        } else {
            emptyList()
        }

        if (activeNoGoList.isNotEmpty()) {
            for (noGo in activeNoGoList) {
                val targets = (listOf(noGo.name) + noGo.keywords).map { it.trim() }.filter { it.isNotBlank() }
                for (target in targets) {
                    val lowerTarget = target.lowercase()
                    if (pickupText.contains(lowerTarget)) {
                        return DecisionResult.Reject("No-Go Area Filter: Pickup location matches No-Go area '$target'")
                    }
                    if (dropText.contains(lowerTarget)) {
                        return DecisionResult.Reject("No-Go Area Filter: Drop location matches No-Go area '$target'")
                    }
                }
            }
        }

        // 3. GO-TO Area Check
        // Only apply Go-To filter when BOTH:
        // 1. Go-To toggle is ON/enabled
        // 2. gotoAreas list has at least 1 area
        // If gotoAreas list is empty OR Go-To is disabled: SKIP Go-To filter completely, do not reject.
        val activeGoToList = if (isGoToEnabled) {
            goToAreas.filter { it.isEnabled }
        } else {
            emptyList()
        }

        if (isGoToEnabled && activeGoToList.isNotEmpty()) {
            var matchedGroup: AreaGroup? = null
            for (group in activeGoToList) {
                val targets = (listOf(group.name) + group.keywords).map { it.trim() }.filter { it.isNotBlank() }
                if (targets.any { dropText.contains(it.lowercase()) || pickupText.contains(it.lowercase()) }) {
                    matchedGroup = group
                    break
                }
            }

            if (matchedGroup == null) {
                return DecisionResult.Reject("Go-To Area Filter: Drop address not in any active Go-To area")
            }

            // Check distance constraints from the matched group or areas list
            val effectiveGroup = areas.firstOrNull { g ->
                g.isEnabled && g.type == AreaType.GO_TO && (
                    g.name.equals(matchedGroup.name, ignoreCase = true) ||
                    g.keywords.any { kw -> kw.equals(matchedGroup.name, ignoreCase = true) }
                )
            } ?: matchedGroup

            val pDist = candidate.pickupDistKm ?: 0f
            val dDist = candidate.dropDistKm ?: 0f
            if (pDist > 0 && pDist < effectiveGroup.minPickupKm) {
                return DecisionResult.Reject("Go-To Distance Filter: Pickup ${pDist}km is under min pickup distance (${effectiveGroup.minPickupKm}km) for '${effectiveGroup.name}'")
            }
            if (dDist > 0 && dDist > effectiveGroup.maxDropKm) {
                return DecisionResult.Reject("Go-To Distance Filter: Drop ${dDist}km exceeds max drop distance (${effectiveGroup.maxDropKm}km) for '${effectiveGroup.name}'")
            }
        }

        // BUG 4: Drop distance filter - If drop distance > maxDropDistanceKm from settings -> REJECT order
        val maxDropLimit = when {
            settings.maxDropDistanceKm > 0f -> settings.maxDropDistanceKm
            settings.maxDropKm > 0f -> settings.maxDropKm
            else -> 0f
        }
        val dropDist = candidate.dropDistKm
        if (dropDist != null && maxDropLimit > 0f && dropDist > maxDropLimit) {
            return DecisionResult.Reject("Drop distance (${dropDist}km) exceeds max limit (${maxDropLimit}km)")
        }

        val checkFare = settings.filterMode == FilterMode.FARE_ONLY || settings.filterMode == FilterMode.BOTH
        val checkDistance = settings.filterMode == FilterMode.DISTANCE_ONLY || settings.filterMode == FilterMode.BOTH

        // 4. Global Fare Filter (evaluated if FilterMode is FARE_ONLY or BOTH)
        if (checkFare) {
            val fare = candidate.fare
            if (fare != null) {
                if (settings.minFare > 0 && fare < settings.minFare) {
                    return DecisionResult.Reject("Fare Filter: Fare ₹${fare.toInt()} is below minimum ₹${settings.minFare.toInt()}")
                }
                if (settings.maxFare > 0 && fare > settings.maxFare) {
                    return DecisionResult.Reject("Fare Filter: Fare ₹${fare.toInt()} exceeds maximum ₹${settings.maxFare.toInt()}")
                }
            }
        }

        // 5. Distance Filter (evaluated if FilterMode is DISTANCE_ONLY or BOTH)
        if (checkDistance) {
            val pickup = candidate.pickupDistKm
            if (pickup != null) {
                if (settings.maxPickupDistanceKm > 0 && pickup > settings.maxPickupDistanceKm) {
                    return DecisionResult.Reject("Distance Filter: Pickup ${pickup}km exceeds maximum limit (${settings.maxPickupDistanceKm}km)")
                }
            }

            val drop = candidate.dropDistKm
            if (drop != null) {
                if (settings.maxDropDistanceKm > 0 && drop > settings.maxDropDistanceKm) {
                    return DecisionResult.Reject("Distance Filter: Drop ${drop}km exceeds maximum limit (${settings.maxDropDistanceKm}km)")
                }
            }
        }

        return DecisionResult.Accept("Order satisfies ${settings.filterMode.displayName} criteria and area rules")
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
        goToAreas = goToAreas.map { AreaGroup(name = it, keywords = listOf(it)) },
        noGoAreas = noGoAreas.map { AreaGroup(name = it, keywords = listOf(it), type = AreaType.NO_GO) },
        pickupLocationTextOverride = pickupLocationTextOverride,
        isGoToEnabled = isGoToEnabled,
        isNoGoEnabled = isNoGoEnabled
    )
}
