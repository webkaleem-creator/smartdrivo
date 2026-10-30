package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Same numeric pickup and trip values are two different values on a real card. */
class RapidoEqualDistancesV18Test {
    @Test fun equalPickupAndTripSeparatedByAddressAreNotDeduplicated() {
        val offer = RapidoAdapter.extractOrderDataFromTexts(
            listOf(
                "Auto", "₹50", "0.6 km",
                "Shalibanda Charminar - Gazibanda, Hyderabad",
                "0.6 km",
                "Shalibanda Charminar - Bilal Sweets, Hyderabad",
                "Accept"
            ),
            skipPickupAddress = true
        )
        assertEquals(0.6f, offer.pickupKm ?: -1f, 0.001f)
        assertEquals(0.6f, offer.dropKm ?: -1f, 0.001f)
    }

    @Test fun measuredZeroRemainsZeroEvenWithNearbyBackgroundText() {
        val offer = RapidoAdapter.extractOrderDataFromTexts(
            listOf("₹52", "Nearby Services", "0 km", "2.0 km", "Accept"),
            skipPickupAddress = true
        )
        assertEquals(0f, offer.pickupKm ?: -1f, 0.001f)
        assertEquals(2.0f, offer.dropKm ?: -1f, 0.001f)
        assertFalse(offer.hasNearby)
    }

    @Test fun nearbyWordAloneNeverFabricatesNumericZero() {
        val offer = RapidoAdapter.extractOrderDataFromTexts(
            listOf("₹52", "Nearby", "2.0 km", "Accept"),
            skipPickupAddress = true
        )
        assertNull(offer.pickupKm)
    }
}
