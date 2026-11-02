package com.example.engine

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class OlaSingleActionTest {
    private fun candidate() = RideCandidate(242f, 1.5f, 13.8f, "Pickup", "Destination", "Area",
        Platform.OLA, VehicleType.AUTO)
    private fun target(label: String = "Accept", clickable: Boolean = true,
        enabled: Boolean = true, window: Boolean = true, pkg: Boolean = true,
        action: Boolean = true) = OlaOfferSafety.isSafeDirectTarget(label, null,
            true, enabled, clickable, action, pkg, window, true)

    @Test fun countdownCannotClickButReadyDirectControlCan() {
        assertFalse(target("Accept in 6"))
        assertTrue(target())
        assertFalse(target(clickable = false)) // No unverified ancestor substitution.
        assertFalse(target(enabled = false))
        assertFalse(target(window = false))
        assertFalse(target(pkg = false))
        assertFalse(target(action = false))
        assertFalse(target("Confirm payment"))
    }
    @Test fun attemptedOfferRemainsBlockedAcrossDisappearanceAndRenderingChanges() {
        val offers = OlaVisibleOffer()
        val ride = candidate()
        offers.observe(ride)
        offers.markAttempted(ride)
        repeat(20) { assertTrue(offers.wasAttempted(ride)) }
        assertTrue(offers.wasAttempted(ride.copy(fare = null)))
        offers.disappeared()
        offers.observe(ride)
        assertTrue(offers.wasAttempted(ride))
        assertFalse(offers.wasAttempted(ride.copy(fare = 300f, dropAddress = "Different")))
        val identified = ride.copy(bookingId = "CRN123")
        offers.markAttempted(identified)
        assertTrue(offers.wasAttempted(identified.copy(fare = null)))
    }
    @Test fun changedDataCannotUseOldDecisionAndConfirmationMustBePositive() {
        val ride = candidate()
        assertFalse(OlaOfferSafety.sameOffer(ride, ride.copy(pickupDistKm = 2f)))
        assertFalse(OlaOfferSafety.hasAcceptedUi("Accept"))
        assertFalse(OlaOfferSafety.hasAcceptedUi("Accepted by another driver"))
        assertTrue(OlaOfferSafety.hasAcceptedUi("Booking accepted Navigate to pickup"))
    }
}
