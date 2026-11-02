package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OlaHistoryReasonSummaryTest {
    @Test fun twelveUnlimitedFailuresAreCondensedWithoutLosingPrimaryExplanation() {
        val original = buildString {
            append("No condition matched\n")
            append("Order Filter 1 (Distance): Trip 14.8 km > max 1.2 km\n")
            append("Filter 2: OFF\nFilter 3: OFF\n")
            repeat(12) { n -> append("Unlimited Filter ${n + 1}: Trip 14.8 > max 3.0 km\n") }
        }
        assertEquals(
            "No enabled filter matched\nFilter 1 (Distance): Trip 14.8 km > max 1.2 km\n+12 other filters not matched",
            OlaHistoryReasonSummary.summarize(original)
        )
    }

    @Test fun pickupAndTripArePreservedFromDecisionTime() {
        val reason = "No condition matched\nOrder Filter 1 (Distance): Pickup 1.0 km > max 0.6 km; Trip 8.6 km > max 1.6 km\nFilter 2: OFF"
        assertEquals(
            "No enabled filter matched\nFilter 1 (Distance): Pickup 1.0 km > max 0.6 km • Trip 8.6 km > max 1.6 km",
            OlaHistoryReasonSummary.summarize(reason)
        )
    }

    @Test fun unlimitedOnlyAndOffFiltersAreHandled() {
        assertEquals(
            "No enabled filter matched\nUnlimited Filter 4: Fare 96 < min 130",
            OlaHistoryReasonSummary.summarize("No condition matched\nOrder Filter 1: OFF\nFilter 2: OFF\nUnlimited Filter 4: Fare 96 < min 130")
        )
        assertEquals("No enabled filter matched",
            OlaHistoryReasonSummary.summarize("No condition matched\nOrder Filter 1: OFF\nFilter 2: OFF\nFilter 3: OFF"))
    }

    @Test fun explicitFastestAndOtherReasonsStayUntouched() {
        assertNull(OlaHistoryReasonSummary.summarize("Mode: Fastest | Pickup 1.6km exceeds saved max 0.5km"))
        assertNull(OlaHistoryReasonSummary.summarize("No-Go Area: Shivaji Nagar"))
        assertNull(OlaHistoryReasonSummary.summarize("Fare unavailable - order skipped"))
    }
}
