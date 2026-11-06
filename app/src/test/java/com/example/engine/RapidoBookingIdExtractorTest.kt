package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real phone regression: 2026-11-06 17:23:46 showed
 * RAPIDO SNAPSHOT orderId=e, fare=77.0 pickup=0.3 trip=2.4.
 * That one-letter ID came from finding "ID" inside the word "Ride".
 */
class RapidoBookingIdExtractorTest {
    @Test fun normalOfferTextCannotBecomeOneLetterBookingId() {
        assertNull(RapidoBookingIdExtractor.fromTexts(listOf(
            "Ride", "Ride details", "Saidabad", "India",
            "Auto Boost", "₹77", "Pickup: 0.3 km", "Trip 2.4 km",
            "Accept", "Ride e", "ID e"
        )))
        assertFalse(RapidoBookingIdExtractor.isValidId("e"))
        assertFalse(RapidoBookingIdExtractor.isValidId("ride"))
        assertFalse(RapidoBookingIdExtractor.isValidId("1234"))
    }

    @Test fun explicitGenuineIdentifiersArePreserved() {
        assertEquals("12345678",
            RapidoBookingIdExtractor.fromTexts(listOf("Booking ID: 12345678")))
        assertEquals("AB12C56",
            RapidoBookingIdExtractor.fromTexts(listOf("Ride ID - AB12C56")))
        assertEquals("123456",
            RapidoBookingIdExtractor.fromTexts(listOf("CRN #123456")))
        assertEquals("12345-6789",
            RapidoBookingIdExtractor.fromTexts(listOf("ID: 12345-6789")))
        assertEquals("AB12345",
            RapidoBookingIdExtractor.fromTexts(listOf("#AB12345")))
        assertTrue(RapidoBookingIdExtractor.isValidId("AB12C56"))
    }

    @Test fun idSubstringInOrdinaryWordsCannotPolluteHistory() {
        assertNull(RapidoBookingIdExtractor.fromTexts(listOf(
            "Ride Experience", "Pickup Address", "Idli Shop",
            "Solid Ride", "ID: Address", "Booking ID: NA",
            "#e", "Accept", "Trip 3 km"
        )))
    }

    @Test fun distinctRideIdsStayDistinct() {
        val a = RapidoBookingIdExtractor.fromTexts(listOf("Booking ID: AB10001"))
        val b = RapidoBookingIdExtractor.fromTexts(listOf("Booking ID: AB10002"))
        assertEquals("AB10001", a)
        assertEquals("AB10002", b)
        assertTrue(a != b)
    }
}
