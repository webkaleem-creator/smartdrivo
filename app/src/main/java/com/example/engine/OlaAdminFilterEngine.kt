package com.example.engine

import com.example.model.FilterMode
import com.example.model.OlaAdminFilterSettings
import com.example.model.Platform
import com.example.model.RideCandidate

/** Permanent Ola numeric limits; shared safety applies in OlaAdminAcceptancePolicy. */
object OlaAdminFilterEngine {
    sealed class Result {
        data object Match : Result()
        data class Ignore(val reason: String) : Result()
    }

    fun evaluate(candidate: RideCandidate, settings: OlaAdminFilterSettings): Result {
        if (candidate.platform != Platform.OLA) return Result.Ignore("Not an Ola offer")
        if (!settings.enabled || !settings.valid())
            return Result.Ignore("Ola Global Filter disabled or settings invalid")

        val fare = candidate.fare
        val pickup = candidate.pickupDistKm
        val trip = candidate.dropDistKm
        val checkFare = settings.filterMode != FilterMode.DISTANCE_ONLY
        val checkDistance = settings.filterMode != FilterMode.FARE_ONLY

        if (checkFare && (fare == null || !fare.isFinite() ||
                    fare < settings.minFare || fare > settings.maxFare))
            return Result.Ignore("Ola Global Filter: fare outside saved range")
        if (checkDistance && (pickup == null || !pickup.isFinite() || pickup < 0f ||
                    pickup > settings.maxPickupKm || trip == null ||
                    !trip.isFinite() || trip <= 0f || trip > settings.maxTripKm))
            return Result.Ignore("Ola Global Filter: pickup or trip outside saved range")
        return Result.Match
    }
}
