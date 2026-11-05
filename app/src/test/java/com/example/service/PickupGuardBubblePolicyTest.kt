package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupGuardBubblePolicyTest {
    @Test fun bubbleRequiresMainAutoAcceptEvenWhenOwnerAndServiceAreReady() {
        assertTrue(PickupGuardBubblePolicy.shouldShow(true, true, true, true, false, true))
        assertFalse(PickupGuardBubblePolicy.shouldShow(false, true, true, true, false, true))
        assertFalse(PickupGuardBubblePolicy.shouldShow(false, true, true, true, false, false))
    }
    @Test fun serviceAndAuthenticationGatesRemainFailClosed() {
        assertFalse(PickupGuardBubblePolicy.shouldShow(true, false, true, true, false, true))
        assertFalse(PickupGuardBubblePolicy.shouldShow(true, true, false, true, false, true))
        assertFalse(PickupGuardBubblePolicy.shouldShow(true, true, true, false, false, true))
    }
    @Test fun bankingAndOverlayPermissionHideBubble() {
        assertFalse(PickupGuardBubblePolicy.shouldShow(true, true, true, true, true, true))
        assertFalse(PickupGuardBubblePolicy.shouldShow(true, true, true, true, false, false))
    }
    @Test fun dragSnapsToNearestEdgeAndClampsDegenerateDimensions() {
        assertEquals(8, PickupGuardBubblePolicy.snapX(28, 8, 340))
        assertEquals(340, PickupGuardBubblePolicy.snapX(300, 8, 340))
        assertEquals(12, PickupGuardBubblePolicy.snapX(200, 12, 12))
    }
}
