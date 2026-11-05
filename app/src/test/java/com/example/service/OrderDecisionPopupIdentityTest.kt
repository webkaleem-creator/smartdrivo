package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderDecisionPopupIdentityTest {
    @Test fun repeatedOfferDifferentDetectionTimestampsRetainsSameBanner() {
        val a = "ride_abc123_1793901000000"
        val b = "ride_abc123_1793901000900"
        assertEquals(OrderDecisionPopupIdentity.canonical(a), OrderDecisionPopupIdentity.canonical(b))
        assertTrue(OrderDecisionPopupIdentity.isCurrent(a,b))
    }

    @Test fun newOfferAndDistinctBookingIdsDoNotReuseOldPopup() {
        assertFalse(OrderDecisionPopupIdentity.isCurrent("ride_abc123_1793901000000",
            "ride_def456_1793901000100"))
        assertFalse(OrderDecisionPopupIdentity.isCurrent("booking-1", "booking-2"))
        assertEquals("booking-42", OrderDecisionPopupIdentity.canonical(" booking-42 "))
    }

    @Test fun invalidTimestampKeysMustNotBeModified() {
        assertEquals("ride_abc_123", OrderDecisionPopupIdentity.canonical("ride_abc_123"))
        assertEquals("", OrderDecisionPopupIdentity.canonical("  "))
    }

    @Test fun durationIsPreciselyFiveSecondsAndDuplicateWindowBounded() {
        assertEquals(5_000L, OrderDecisionPopupIdentity.DISPLAY_MS)
        assertTrue(OrderDecisionPopupIdentity.isDuplicate(10_000L, 14_999L))
        assertFalse(OrderDecisionPopupIdentity.isDuplicate(10_000L, 15_000L))
        assertFalse(OrderDecisionPopupIdentity.isDuplicate(null, 10_000L))
        assertFalse(OrderDecisionPopupIdentity.isDuplicate(20_000L, 19_999L))
    }

    @Test fun emptyLatestAllowsFinalStatusWhenDetectionWasNotRegistered() {
        assertTrue(OrderDecisionPopupIdentity.isCurrent("", "ride_abc_1793901000000"))
    }
}
