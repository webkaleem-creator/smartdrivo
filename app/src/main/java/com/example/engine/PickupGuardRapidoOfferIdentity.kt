package com.example.engine

import com.example.model.Platform
import com.example.model.RideCandidate
import java.util.Locale

/** An id-less offer needs stable fare, trip and destination, not pickup-address text. */
object PickupGuardRapidoOfferIdentity {
    fun sameVisibleOffer(original: RideCandidate, refreshed: RideCandidate): Boolean {
        if (original.platform != Platform.RAPIDO || refreshed.platform != Platform.RAPIDO) return false
        val a = original.bookingId?.trim().orEmpty()
        val b = refreshed.bookingId?.trim().orEmpty()
        if (a.isNotBlank() || b.isNotBlank()) {
            return a.isNotBlank() && b.isNotBlank() &&
                a.equals(b, ignoreCase = true) &&
                RapidoIgnoredVisibleOfferGuard.sameVisibleOffer(original, refreshed)
        }
        // Missing booking IDs require both complete destination strings. This
        // prevents accidentally rejecting a different offer with the same fare.
        fun clean(v: String?) = v.orEmpty().trim().lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
        val originalDrop = clean(original.dropAddress)
        val newDrop = clean(refreshed.dropAddress)
        if (originalDrop.length < 8 || originalDrop != newDrop) return false
        if (originalDrop in setOf("address unavailable", "drop location", "detected drop location"))
            return false
        return RapidoIgnoredVisibleOfferGuard.sameVisibleOffer(original, refreshed)
    }
}
