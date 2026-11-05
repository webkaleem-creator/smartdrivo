package com.example.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PickupGuardQuickControlPolicyTest {
    @Test fun pickupPresetsAndBoundaries() {
        assertEquals(listOf(0.4f, 0.6f, 0.8f), PickupGuardQuickControlPolicy.QUICK_PICKUP_LIMITS)
        for (km in PickupGuardQuickControlPolicy.QUICK_PICKUP_LIMITS) {
            assertTrue(PickupGuardQuickControlPolicy.validPickup(km))
        }
        assertTrue(PickupGuardQuickControlPolicy.validPickup(30f))
        for (km in listOf(0f, -0.1f, 30.1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFalse(PickupGuardQuickControlPolicy.validPickup(km))
        }
    }

    @Test fun timedPauseNeverExpiresEarly() {
        assertTrue(PickupGuardQuickControlPolicy.isPaused(8000L, 7999L))
        assertFalse(PickupGuardQuickControlPolicy.isPaused(8000L, 8000L))
        assertFalse(PickupGuardQuickControlPolicy.isPaused(0L, 5L))
    }

    @Test fun restartRequiresAllCurrentSecurityGates() {
        fun allow(wasOn:Boolean=true, masterOn:Boolean=false, authorized:Boolean=true,
                  service:Boolean=true, banking:Boolean=false, overlay:Boolean=true) =
            PickupGuardQuickControlPolicy.mayRestoreMaster(wasOn,masterOn,authorized,service,banking,overlay)
        assertTrue(allow())
        assertFalse(allow(wasOn=false))
        assertFalse(allow(masterOn=true))
        assertFalse(allow(authorized=false))
        assertFalse(allow(service=false))
        assertFalse(allow(banking=true))
        assertFalse(allow(overlay=false))
    }

    @Test fun longPressThresholdIsDistinctFromTap() {
        assertTrue(PickupGuardQuickControlPolicy.LONG_PRESS_MS >= 450L)
    }
}
