package com.example.engine

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class OlaActionOutcomeTest {
    @Test fun acknowledgementNeverProvesBooking() {
        assertEquals(OrderStatus.UNCONFIRMED, OlaActionOutcome.status(true, false))
        assertEquals(OrderStatus.FAILED, OlaActionOutcome.status(false, false))
        assertEquals(OrderStatus.ACCEPTED, OlaActionOutcome.status(true, true))
    }
    @Test fun incompleteFareOnlyReportIsDistinguishableWithoutLoggingText() {
        val offer = RideCandidate(64f, null, null, "Address unavailable", null, null, Platform.OLA)
        assertEquals(1, OlaOfferSafety.fieldFlags(offer))
        assertEquals(7, OlaOfferSafety.fieldFlags(offer.copy(pickupDistKm = 0f, dropDistKm = 4f)))
    }
}
