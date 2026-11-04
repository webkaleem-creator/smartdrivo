package com.example.engine

import com.example.model.Platform
import com.example.model.RideCandidate
import java.util.Locale
import kotlin.math.abs

/**
 * Remembers one still-visible, already-ignored Rapido offer (Auto Reject OFF).
 * Repeated accessibility events may change its pickup distance as the driver
 * moves. Do not convert those events into separate History records.
 *
 * Clear on a verified offer-free transition, master OFF, or a different
 * stable offer. Never treat one incomplete accessibility frame as a new ride.
 * Never infer a new booking from pickup km alone.
 */
class RapidoIgnoredVisibleOfferGuard {
    private var lastIgnored: RideCandidate? = null
    private var filterContext: Any? = null
    @Synchronized fun needsFilterRecheck(candidate: RideCandidate, context: Any): Boolean {
        val previous = lastIgnored ?: return false
        return context != filterContext || previous.pickupDistKm != candidate.pickupDistKm
    }
    private var firstIgnoredAtMs: Long = 0L
    private var offerMissingSinceMs: Long? = null

    @Synchronized
    fun onCompleteOfferObserved() {
        offerMissingSinceMs = null
    }

    /**
     * A verified transition must contain two trustworthy no-offer reads
     * separated by 1200 ms. Unrelated windows and partially loaded offer
     * cards cannot terminate the previous presentation.
     * Returns true only after a confirmed presentation gap.
     */
    @Synchronized
    fun onNoCompleteOfferObserved(
        nowMs: Long,
        hasTrustedRapidoRoot: Boolean,
        hasPartialOffer: Boolean
    ): Boolean {
        if (!hasTrustedRapidoRoot || hasPartialOffer) {
            offerMissingSinceMs = null
            return false
        }
        val since = offerMissingSinceMs
        if (since == null || nowMs < since) {
            offerMissingSinceMs = nowMs
            return false
        }
        if (nowMs - since < 1_200L) return false
        clear()
        return true
    }

    @Synchronized
    fun isDuplicateIgnored(candidate: RideCandidate): Boolean {
        val previous = lastIgnored ?: return false
        val elapsed = System.currentTimeMillis() - firstIgnoredAtMs
        val duplicate = sameVisibleOffer(previous, candidate) ||
            (elapsed in 0L..3_000L && sameBriefFareVariation(previous, candidate))
        if (!duplicate) clear()
        return duplicate
    }

    @Synchronized
    fun rememberIgnored(candidate: RideCandidate, context: Any? = null) {
        if (candidate.platform == Platform.RAPIDO) {
            lastIgnored = candidate
            filterContext = context
            firstIgnoredAtMs = System.currentTimeMillis()
            offerMissingSinceMs = null
        }
    }

    @Synchronized
    fun clear() {
        lastIgnored = null
        filterContext = null
        firstIgnoredAtMs = 0L
        offerMissingSinceMs = null
    }

    companion object {
        /**
         * The service's fast fallback ID contains fare/pickup/trip only.
         * Do not hide a different destination merely because this ID matches.
         */
        fun sameActivePopup(
            activeId: String?,
            previous: RideCandidate?,
            incomingId: String,
            incoming: RideCandidate
        ): Boolean = activeId == incomingId &&
            (previous == null || sameVisibleOffer(previous, incoming))

        // RAPIDO_SAME_VISIBLE_PRICE_DRIFT_V1
        // A live card can temporarily show â‚¹128 then â‚¹129 as text nodes update.
        // This is NOT a general same-destination matcher. Require same complete
        // destination, trip, nearby pickup, no booking IDs and <= â‚¹1 variation.
        fun sameBriefFareVariation(previous: RideCandidate, current: RideCandidate): Boolean {
            if (previous.platform != Platform.RAPIDO || current.platform != Platform.RAPIDO) return false
            if (!previous.bookingId.isNullOrBlank() || !current.bookingId.isNullOrBlank()) return false
            if (previous.isBundledOrder != current.isBundledOrder) return false
            val oldDrop = previous.dropAddress?.trim()?.lowercase(Locale.ROOT)?.replace(Regex("\\s+"), " ").orEmpty()
            val newDrop = current.dropAddress?.trim()?.lowercase(Locale.ROOT)?.replace(Regex("\\s+"), " ").orEmpty()
            if (oldDrop.length < 12 || oldDrop != newDrop || oldDrop == "address unavailable") return false
            val fare1 = previous.fare ?: return false
            val fare2 = current.fare ?: return false
            val trip1 = previous.dropDistKm ?: return false
            val trip2 = current.dropDistKm ?: return false
            val pickup1 = previous.pickupDistKm ?: return false
            val pickup2 = current.pickupDistKm ?: return false
            return fare1.isFinite() && fare2.isFinite() &&
                fare1 > 0f && fare2 > 0f && abs(fare1 - fare2) <= 1.01f &&
                trip1.isFinite() && trip2.isFinite() &&
                trip1 > 0f && trip2 > 0f && abs(trip1 - trip2) <= 0.11f &&
                pickup1.isFinite() && pickup2.isFinite() &&
                abs(pickup1 - pickup2) <= 0.16f
        }

        fun sameVisibleOffer(previous: RideCandidate, current: RideCandidate): Boolean {
            if (previous.platform != Platform.RAPIDO || current.platform != Platform.RAPIDO) return false

            val oldId = previous.bookingId?.trim().orEmpty()
            val newId = current.bookingId?.trim().orEmpty()
            if (oldId.isNotEmpty() && newId.isNotEmpty()) {
                return oldId.equals(newId, ignoreCase = true)
            }

            fun close(a: Float?, b: Float?, tolerance: Float): Boolean =
                a != null && b != null && a.isFinite() && b.isFinite() && abs(a - b) <= tolerance

            if (!close(previous.fare, current.fare, 0.01f)) return false
            if (!close(previous.dropDistKm, current.dropDistKm, 0.11f)) return false
            if (previous.isBundledOrder != current.isBundledOrder) return false

            // Keep different base-fare/tip offers distinct when both values exist.
            if (previous.baseFare != null && current.baseFare != null &&
                !close(previous.baseFare, current.baseFare, 0.01f)
            ) return false
            if (previous.tipAmount != null && current.tipAmount != null &&
                !close(previous.tipAmount, current.tipAmount, 0.01f)
            ) return false

            fun cleanAddress(value: String?): String {
                val cleaned = value.orEmpty().trim().lowercase(Locale.ROOT)
                    .replace(Regex("\\s+"), " ")
                return if (cleaned in setOf(
                        "", "address unavailable", "drop location", "detected drop location"
                    )) "" else cleaned
            }

            val oldDrop = cleanAddress(previous.dropAddress)
            val newDrop = cleanAddress(current.dropAddress)
            if (oldDrop.isNotEmpty() && newDrop.isNotEmpty() && oldDrop != newDrop) return false

            // Pickup changes (e.g. 0.8 to 0.7 km) are intentionally ignored.
            return true
        }
    }
}
