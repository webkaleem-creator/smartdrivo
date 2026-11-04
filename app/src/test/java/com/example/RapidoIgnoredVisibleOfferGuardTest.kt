package com.example

import com.example.engine.RapidoIgnoredVisibleOfferGuard
import com.example.model.Platform
import com.example.model.RideCandidate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidoIgnoredVisibleOfferGuardTest {
    private fun offer(
        fare: Float = 99f,
        pickup: Float = 0.8f,
        drop: Float = 3.0f,
        address: String = "Charminar Murad Mahal Colony - The English Hub, Hyderabad",
        booking: String? = null
    ) = RideCandidate(
        platform = Platform.RAPIDO,
        fare = fare,
        pickupDistKm = pickup,
        dropDistKm = drop,
        baseFare = 67f,
        tipAmount = 32f,
        pickupAddress = null,
        dropAddress = address,
        dropArea = address,
        bookingId = booking
    )

    @Test fun repeatedUnmatchedVisibleOrderLogsOnlyOnceDespitePickupChanging() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        guard.rememberIgnored(offer(pickup = 0.8f))
        assertTrue(guard.isDuplicateIgnored(offer(pickup = 0.7f)))
        assertTrue(guard.isDuplicateIgnored(offer(pickup = 0.6f)))
        assertTrue(guard.isDuplicateIgnored(offer(pickup = 0.2f)))
    }

    @Test fun confirmedRapidoHomeAllowsSameDetailsAsFreshBooking() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        guard.rememberIgnored(offer())
        assertTrue(guard.isDuplicateIgnored(offer(pickup = 0.7f)))
        guard.clear() // Called ONLY after confirmed Rapido Home / Auto Accept OFF.
        assertFalse(guard.isDuplicateIgnored(offer(pickup = 0.7f)))
    }

    @Test fun differentDestinationOrFareIsNotHidden() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        guard.rememberIgnored(offer())
        assertFalse(guard.isDuplicateIgnored(offer(address = "Dabirpura, Hyderabad")))
        guard.rememberIgnored(offer())
        assertFalse(guard.isDuplicateIgnored(offer(fare = 105f)))
    }

    @Test fun sameLivePopupSmallFareDriftDoesNotCreateExtraIgnoredRow() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        val address = "Milan Function Hall - Limra Mission School, Shah Gunj, Hyderabad"
        val first = offer(fare = 128f, pickup = 0.9f, drop = 3.7f, address = address)
            .copy(baseFare = 109f, tipAmount = 19f)
        val changed = offer(fare = 129f, pickup = 0.9f, drop = 3.7f, address = address)
            .copy(baseFare = 75f, tipAmount = 54f)
        guard.rememberIgnored(first)
        assertTrue(guard.isDuplicateIgnored(changed))
        guard.clear()
        assertFalse(guard.isDuplicateIgnored(changed))
    }

    @Test fun differentBookingIdsMustRemainSeparate() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        guard.rememberIgnored(offer(booking = "A001"))
        assertTrue(guard.isDuplicateIgnored(offer(booking = "A001", pickup = 0.5f)))
        assertFalse(guard.isDuplicateIgnored(offer(booking = "B002")))
    }

    @Test fun differentTripDistanceIsNotHidden() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        guard.rememberIgnored(offer(drop = 3.0f))
        assertFalse(guard.isDuplicateIgnored(offer(drop = 4.5f)))
    }
    @Test fun autoRejectOffPickupOrSavedFilterChangeRequestsReevaluationWithoutNewIdentity() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        val original = offer(fare = 45f, pickup = 1.1f, drop = 2.2f)
        val settings = com.example.model.AppSettings(isAutoRejectBadFaresEnabled = false,
            isFastestModeEnabled = true, maxPickupDistanceKm = 1f)
        guard.rememberIgnored(original, settings)
        assertFalse(guard.needsFilterRecheck(original, settings))
        val matching = original.copy(pickupDistKm = 0.9f)
        assertTrue(guard.isDuplicateIgnored(matching))
        assertTrue(guard.needsFilterRecheck(matching, settings))
        assertTrue(com.example.engine.AreaRulesEngine.evaluateRide(matching, settings = settings)
            is com.example.engine.DecisionResult.Accept)
        guard.rememberIgnored(original, settings)
        assertTrue(guard.needsFilterRecheck(original, settings.copy(maxPickupDistanceKm = 2f)))
    }

    @Test fun reappearingIgnoredAAfterConfirmedGapIsFresh() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        val a = offer(fare = 93f, pickup = 0.8f, drop = 3.1f)
        guard.rememberIgnored(a)
        assertTrue(guard.isDuplicateIgnored(a))
        assertFalse(guard.onNoCompleteOfferObserved(10_000L, true, false))
        assertFalse(guard.onNoCompleteOfferObserved(10_500L, true, false))
        assertTrue(guard.onNoCompleteOfferObserved(11_250L, true, false))
        assertFalse(guard.isDuplicateIgnored(a))
    }

    @Test fun partiallyRenderedOrUntrustedFrameNeverStartsAnOfferGap() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        val a = offer()
        guard.rememberIgnored(a)
        assertFalse(guard.onNoCompleteOfferObserved(10_000L, true, false))
        assertFalse(guard.onNoCompleteOfferObserved(11_500L, true, true))
        assertFalse(guard.onNoCompleteOfferObserved(12_000L, false, false))
        assertFalse(guard.onNoCompleteOfferObserved(12_500L, true, false))
        assertTrue(guard.isDuplicateIgnored(a))
        guard.onCompleteOfferObserved()
        assertFalse(guard.onNoCompleteOfferObserved(14_000L, true, false))
        assertTrue(guard.isDuplicateIgnored(a))
    }

    @Test fun destinationChangeIsNotHiddenByEqualFarePickupAndTripFallbackId() {
        val a = offer(fare = 99f, pickup = 0.8f, drop = 3f,
            address = "Red Hills Lakdikapul Hyderabad")
        val b = offer(fare = 99f, pickup = 0.8f, drop = 3f,
            address = "Charminar Shah Ali Banda Hyderabad")
        val id = "RAPIDO_99.0_0.8_3.0_single"
        assertFalse(RapidoIgnoredVisibleOfferGuard.sameActivePopup(id, a, id, b))
        assertTrue(RapidoIgnoredVisibleOfferGuard.sameActivePopup(id, a, id,
            a.copy(pickupDistKm = 0.7f)))
        assertFalse(RapidoIgnoredVisibleOfferGuard.sameActivePopup(id, a, "NEW_ID", a))
    }

    @Test fun ignoredAThenDistinctBThenDistinctCThenAIsProcessed() {
        val guard = RapidoIgnoredVisibleOfferGuard()
        val a = offer(fare = 67f, address = "Old City Hyderabad Address A")
        val b = offer(fare = 82f, address = "Himayat Nagar Hyderabad Address B")
        val c = offer(fare = 96f, address = "Mehdipatnam Hyderabad Address C")
        guard.rememberIgnored(a)
        assertTrue(guard.isDuplicateIgnored(a))
        assertFalse(guard.isDuplicateIgnored(b))
        guard.rememberIgnored(b)
        assertFalse(guard.isDuplicateIgnored(c))
        guard.rememberIgnored(c)
        assertFalse(guard.isDuplicateIgnored(a))
    }
}
