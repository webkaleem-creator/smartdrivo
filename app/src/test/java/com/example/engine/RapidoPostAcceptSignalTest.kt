package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class RapidoPostAcceptSignalTest {
    @Test
    fun pickupAndAcceptedScreensConfirm() {
        assertEquals(RapidoPostAcceptSignal.Result.CONFIRMED, RapidoPostAcceptSignal.classify("Go to Pickup Zone"))
        assertEquals(RapidoPostAcceptSignal.Result.CONFIRMED, RapidoPostAcceptSignal.classify("Customer Verified Location Arrived"))
        assertEquals(RapidoPostAcceptSignal.Result.CONFIRMED, RapidoPostAcceptSignal.classify("Thanks for accepting"))
        assertEquals(RapidoPostAcceptSignal.Result.CONFIRMED, RapidoPostAcceptSignal.classify("Booking confirmed"))
    }

    @Test
    fun missedExpiredAndTakenOrdersAreNotAccepted() {
        assertEquals(RapidoPostAcceptSignal.Result.MISSED, RapidoPostAcceptSignal.classify("You missed the order"))
        assertEquals(RapidoPostAcceptSignal.Result.MISSED, RapidoPostAcceptSignal.classify("Order expired"))
        assertEquals(RapidoPostAcceptSignal.Result.MISSED, RapidoPostAcceptSignal.classify("Accepted by another captain"))
    }

    @Test
    fun checkingStatusIsPendingNotConfirmed() {
        assertEquals(RapidoPostAcceptSignal.Result.PENDING, RapidoPostAcceptSignal.classify("Checking order status"))
    }

    @Test
    fun negativeSignalOverridesPositiveSignal() {
        assertEquals(
            RapidoPostAcceptSignal.Result.MISSED,
            RapidoPostAcceptSignal.classify("Go to Pickup Zone. Accepted by another captain.")
        )
    }

    @Test
    fun unrelatedTextIsUnknown() {
        assertEquals(RapidoPostAcceptSignal.Result.UNKNOWN, RapidoPostAcceptSignal.classify("Pickup 1.2 km Drop 4.5 km"))
        assertEquals(RapidoPostAcceptSignal.Result.UNKNOWN, RapidoPostAcceptSignal.classify(""))
    }
}