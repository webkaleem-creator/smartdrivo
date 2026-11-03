package com.example.engine

import com.example.model.RideCandidate
import com.example.model.Platform
import com.example.model.VehicleType
import org.junit.Assert.*
import org.junit.Test

class OlaReliabilityTest {
    private fun offer() = RideCandidate(120f, 1f, 5f, "Pickup A", "Destination A",
        "Area", Platform.OLA, VehicleType.AUTO)

    @Test fun proseIsNeverBookingIdentity() {
        listOf("Trip distance 5 km", "Ride request", "Booking accepted", "Accept Booking",
            "order expired", "driver id abcdef").forEach { assertNull(OlaOfferSafety.bookingId(it)) }
        assertEquals("123456789", OlaOfferSafety.bookingId("CRN: 123456789"))
        assertEquals("AB-12345", OlaOfferSafety.bookingId("Booking ID: AB-12345"))
    }

    @Test fun internalSessionIdsAreUniqueAndStableOnCandidateCopies() {
        val candidate = offer().copy(bookingId = OlaOfferSafety.internalId())
        assertEquals(candidate.bookingId, candidate.copy().bookingId)
        assertNotEquals(candidate.bookingId, OlaOfferSafety.internalId())
        assertTrue(OlaOfferSafety.sameOffer(candidate, offer()))
    }

    @Test fun replacementsMustBeReevaluatedEvenWithSameFareAndPickup() {
        val old = offer()
        assertFalse(OlaOfferSafety.sameOffer(old, old.copy(dropAddress = "Destination B")))
        assertFalse(OlaOfferSafety.sameOffer(old, old.copy(dropDistKm = 8f)))
        assertFalse(OlaOfferSafety.sameOffer(old.copy(bookingId = "123"), old.copy(bookingId = "456")))
        assertFalse(OlaOfferSafety.sameOffer(old, old.copy(fare = null)))
        assertTrue(OlaOfferSafety.sameOffer(old, old.copy()))
    }

    @Test fun countdownProtectionAndWindowPackageIsolationRemain() {
        assertFalse(OlaOfferSafety.isReadyLabel("Accept in 3"))
        assertTrue(OlaOfferSafety.routesToOla("com.android.systemui", listOf("com.ola.partner")))
        assertFalse(OlaOfferSafety.routesToOla("com.android.systemui", listOf("com.olacabs.customer")))
        assertFalse(OlaOfferSafety.routesToOla("com.rapido.captain", emptyList()))
    }
}
