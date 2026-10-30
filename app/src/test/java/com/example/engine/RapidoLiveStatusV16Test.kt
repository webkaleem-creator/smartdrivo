package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zero pickup may not be erased by incidental Nearby text. */
class RapidoLiveStatusV16Test {
    @Test fun measuredZeroWinsOverNearby() {
        val data = RapidoAdapter.extractOrderDataFromTexts(
            listOf("₹90", "Nearby", "0 km", "3.2 km", "Accept"),
            skipPickupAddress = true
        )
        assertEquals(0f, data.pickupKm ?: -1f, 0.001f)
        assertEquals(3.2f, data.dropKm ?: -1f, 0.001f)
        assertFalse(data.hasNearby)
    }

    @Test fun nearbyWithOneDistanceRemainsUnknownPickup() {
        val data = RapidoAdapter.extractOrderDataFromTexts(
            listOf("₹90", "Nearby", "3.2 km", "Accept"),
            skipPickupAddress = true
        )
        assertNull(data.pickupKm)
        assertTrue(data.hasNearby)
    }
}
