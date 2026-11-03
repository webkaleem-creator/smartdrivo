package com.example.engine

import com.example.model.RideCandidate

/** One local identity for a continuous offer presentation, including partial renders. */
class OlaVisibleOffer {
    private var observed: RideCandidate? = null
    private var id: String? = null
    private val attempted = ArrayDeque<RideCandidate>()
    private var attemptedVisibleId: String? = null
    @Synchronized fun wasAttempted(live: RideCandidate): Boolean =
        (id != null && id == attemptedVisibleId && observed?.let {
            !OlaOfferSafety.isDifferentOffer(it, live)
        } == true) || attempted.any {
        (it.bookingId != null && it.bookingId == live.bookingId) || OlaOfferSafety.sameOffer(it, live)
    }
    @Synchronized fun markAttempted(live: RideCandidate) {
        attemptedVisibleId = id
        attempted.addLast(live)
        if (attempted.size > 32) attempted.removeFirst()
    }
    @Synchronized fun observe(live: RideCandidate): String {
        val old = observed
        if (old == null || OlaOfferSafety.isDifferentOffer(old, live)) {
            id = OlaOfferSafety.internalId()
            observed = live
        } else {
            observed = live.copy(bookingId = live.bookingId ?: old.bookingId,
                fare = live.fare ?: old.fare, pickupDistKm = live.pickupDistKm ?: old.pickupDistKm,
                dropDistKm = live.dropDistKm ?: old.dropDistKm,
                pickupAddress = live.pickupAddress?.takeUnless { it == "Address unavailable" } ?: old.pickupAddress,
                dropAddress = live.dropAddress?.takeUnless { it == "Address unavailable" } ?: old.dropAddress)
        }
        return requireNotNull(id)
    }
    @Synchronized fun disappeared() { observed = null; id = null }
}

/** Read-only observation budgets; reaching one never authorizes a click. */
class OlaWatchProgress {
    private var missingSince: Long? = null
    fun observe(now: Long, offerVisible: Boolean, countdown: Boolean, ready: Boolean): Boolean {
        if (countdown || ready) { missingSince = null; return false }
        val since = missingSince ?: now.also { missingSince = it }
        return now - since >= if (offerVisible) 5_000L else 1_500L
    }
}
