package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FastestHistoryReasonTest {
    private val oldReason = "Mode: Fastest | Pickup unknown, invalid or exceeds saved max 0.6km"

    @Test fun oldHistoryWithKnownOverLimitPickupIsSpecific() {
        assertEquals(
            "Mode: Fastest | Pickup 1.2 km exceeds saved max 0.6 km",
            FastestHistoryReason.clarify(oldReason, 1.2f)
        )
    }

    @Test fun doesNotUseCurrentSettingsAndNeverInventsUnknownPickup() {
        assertEquals(
            "Mode: Fastest | Pickup could not be verified (saved max 0.6 km)",
            FastestHistoryReason.clarify(oldReason, 0f)
        )
        assertEquals(
            "Mode: Fastest | Pickup distance invalid (saved max 0.6 km)",
            FastestHistoryReason.clarify(oldReason, Float.NaN)
        )
    }

    @Test fun otherFilterReasonsAndAlreadySpecificReasonsPassThrough() {
        assertNull(FastestHistoryReason.clarify("Filter 1: Pickup too far", 1.2f))
        assertNull(FastestHistoryReason.clarify("Mode: Fastest | Pickup 1.2km exceeds saved max 0.6km", 1.2f))
    }
}
