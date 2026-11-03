package com.example.engine

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class OlaVisibleOfferTest {
    private fun offer(id: String? = null) = RideCandidate(105f, 0.4f, 5f,
        "Pickup A", "Drop A", "Area", Platform.OLA, VehicleType.AUTO, id)
    @Test fun twentyDetectionsAndPartialUpdatesShareOneIdentity() {
        val tracker = OlaVisibleOffer()
        val first = tracker.observe(offer("CRN123"))
        repeat(20) {
            assertEquals(first, tracker.observe(offer("CRN123").copy(fare = null, dropAddress = "Address unavailable")))
            assertEquals(first, tracker.observe(offer("CRN123")))
        }
    }
    @Test fun genuineNewOffersCanHaveMatchingFareAndDistance() {
        val tracker = OlaVisibleOffer()
        val a = tracker.observe(offer("CRN123"))
        assertNotEquals(a, tracker.observe(offer("CRN456")))
        tracker.disappeared()
        val b = tracker.observe(offer())
        tracker.disappeared()
        assertNotEquals(b, tracker.observe(offer()))
    }
    @Test fun renderingChangesAreNotReplacementButCannotAuthorizeStaleClick() {
        val a = offer("CRN123")
        val partial = a.copy(fare = null, dropAddress = "Address unavailable")
        assertFalse(OlaOfferSafety.isDifferentOffer(a, partial))
        assertFalse(OlaOfferSafety.sameOffer(a, partial))
        assertFalse(OlaOfferSafety.isDifferentOffer(a, a.copy(fare = 110f)))
        assertTrue(OlaOfferSafety.isDifferentOffer(a, offer("CRN456")))
    }
    @Test fun countdownReadyAndMissingControlBudgets() {
        val watch = OlaWatchProgress()
        assertFalse(watch.observe(0, true, true, false))
        assertFalse(watch.observe(2000, true, false, false))
        assertFalse(watch.observe(2100, true, false, true))
        assertFalse(watch.observe(3000, true, false, false))
        assertTrue(watch.observe(8000, true, false, false))
        val vanished = OlaWatchProgress()
        assertFalse(vanished.observe(0, false, false, false))
        assertTrue(vanished.observe(1500, false, false, false))
        assertFalse(OlaOfferSafety.isReadyLabel("Accept in 4"))
        assertTrue(OlaOfferSafety.isReadyLabel("Accept"))
    }
}
