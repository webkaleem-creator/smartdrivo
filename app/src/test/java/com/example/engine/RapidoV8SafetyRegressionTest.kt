package com.example.engine

import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.model.RideCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidoV8SafetyRegressionTest {
    private fun candidate(pickup: Float?, drop: Float?) = RideCandidate(
        fare = 120f,
        pickupDistKm = pickup,
        dropDistKm = drop,
        pickupAddress = "Address unavailable",
        dropAddress = "Charminar",
        dropArea = "Charminar",
        platform = Platform.RAPIDO
    )

    @Test fun fixAreaAcceptsExplicitZeroButNeverUnknownOrZeroTrip() {
        val fix = FixAreaSettings(enabled = true, words = listOf("Charminar"), maxPickupKm = 0.6f)
        assertTrue(
            FixAreaRuleEngine.evaluate(candidate(0f, 4f), fix, emptyList(), false, true)
                is DecisionResult.Accept
        )
        assertNull(FixAreaRuleEngine.evaluate(candidate(null, 4f), fix, emptyList(), false, true))
        assertNull(FixAreaRuleEngine.evaluate(candidate(0f, 0f), fix, emptyList(), false, true))
        assertNull(FixAreaRuleEngine.evaluate(candidate(0.7f, 4f), fix, emptyList(), false, true))
    }

    @Test fun unknownOrCheckingNeverBecomesFailedOrAccepted() {
        for (signal in listOf(RapidoPostAcceptSignal.Result.UNKNOWN, RapidoPostAcceptSignal.Result.PENDING)) {
            val status = RapidoAcceptanceOutcome.statusFor(signal)
            assertEquals(OrderStatus.UNCONFIRMED, status)
            assertFalse(status == OrderStatus.ACCEPTED || status == OrderStatus.FAILED)
        }
    }

    @Test fun onlyExplicitOutcomeIsFinal() {
        assertEquals(OrderStatus.ACCEPTED, RapidoAcceptanceOutcome.statusFor(RapidoPostAcceptSignal.Result.CONFIRMED))
        assertEquals(OrderStatus.MISSED, RapidoAcceptanceOutcome.statusFor(RapidoPostAcceptSignal.Result.MISSED))
        assertEquals(RapidoPostAcceptSignal.Result.UNKNOWN, RapidoPostAcceptSignal.classify("Offer popup disappeared"))
        assertEquals(RapidoPostAcceptSignal.Result.PENDING, RapidoPostAcceptSignal.classify("Checking order status"))
    }

    @Test fun unlabelledRapidoZeroPickupMatchesRealOfferOrder() {
        val first = RapidoAdapter.extractOrderDataFromTexts(
            listOf("₹63 + ₹4", "0 km", "2.7 km", "Drop: TadBun Falaknuma", "Accept"),
            skipPickupAddress = true
        )
        assertEquals(0f, first.pickupKm ?: -1f, 0.0001f)
        assertEquals(2.7f, first.dropKm ?: -1f, 0.0001f)
        assertEquals(67f, first.totalFare, 0.0001f)

        val second = RapidoAdapter.extractOrderDataFromTexts(
            listOf("₹108", "0 km", "6.4 km", "Drop: Santosh Nagar West", "Accept"),
            skipPickupAddress = true
        )
        assertEquals(0f, second.pickupKm ?: -1f, 0.0001f)
        assertEquals(6.4f, second.dropKm ?: -1f, 0.0001f)
        assertEquals(108f, second.totalFare, 0.0001f)
    }

    @Test fun singleUnlabelledDistanceIsNotInventedAsPickupZero() {
        val incomplete = RapidoAdapter.extractOrderDataFromTexts(
            listOf("₹108", "6.4 km", "Drop: Santosh Nagar West", "Accept"),
            skipPickupAddress = true
        )
        assertNull(incomplete.pickupKm)
        assertFalse(RapidoDistanceValidity.isValidPickup(incomplete.pickupKm))
    }

    @Test fun zeroDropAndUnknownPickupMustNeverPassAutoAcceptValidity() {
        assertFalse(RapidoDistanceValidity.isValidDrop(0f))
        assertFalse(RapidoDistanceValidity.isValidDrop(-0.1f))
        assertFalse(RapidoDistanceValidity.isValidPickup(null))
        assertTrue(RapidoDistanceValidity.isValidPickup(0f))
    }
}

// Screenshot-derived synthetic offer sequences: never read pickup street/address text.
