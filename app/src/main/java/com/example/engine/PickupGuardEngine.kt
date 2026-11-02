package com.example.engine

import com.example.model.AreaGroup
import com.example.model.PickupGuardSettings
import com.example.model.RideCandidate

/** Pure shared policy. No clicks, history writes or existing mode changes. */
object PickupGuardEngine {
    sealed class Result {
        data class ManualChoice(val reason: String) : Result()
        data class AutoReject(val reason: String) : Result()
        data class IgnoreUnreadable(val reason: String) : Result()
    }
    fun evaluate(
        offer: RideCandidate, guard: PickupGuardSettings,
        noGoAreas: List<AreaGroup>, noGoEnabled: Boolean
    ): Result {
        val maximum = guard.maxPickupKm
        if (!guard.enabled || !maximum.isFinite() || maximum <= 0f || maximum > 30f)
            return Result.IgnoreUnreadable("Pickup Guard OFF or invalid maximum")
        val pickup = offer.pickupDistKm
        if (pickup == null || !pickup.isFinite() || pickup < 0f)
            return Result.IgnoreUnreadable("Pickup Guard: pickup distance missing/invalid")
        val activeNoGo = noGoEnabled && noGoAreas.any { group ->
            group.isEnabled && group.keywords.any { it.isNotBlank() && !it.equals(group.name, ignoreCase = true) }
        }
        val destination = offer.dropAddress.orEmpty().trim() + " " + offer.dropArea.orEmpty().trim()
        val missingDestination = destination.trim().isEmpty() ||
            destination.trim().equals("Address unavailable", ignoreCase = true) ||
            destination.trim().equals("Drop Location", ignoreCase = true) ||
            destination.trim().equals("Detected Drop Location", ignoreCase = true)
        if (activeNoGo && missingDestination)
            return Result.IgnoreUnreadable("Pickup Guard: destination missing; cannot verify No-Go")
        if (activeNoGo) {
            val matched = AreaRulesEngine.firstNoGoDestinationMatch(
                offer.dropAddress, offer.dropArea, noGoAreas
            )
            if (matched != null) return Result.AutoReject("Pickup Guard: $matched")
        }
        return if (pickup > maximum)
            Result.AutoReject("Pickup Guard: pickup ${pickup} km exceeds ${maximum} km")
        else Result.ManualChoice("Pickup Guard: pickup ${pickup} km within ${maximum} km; manual choice")
    }
}
