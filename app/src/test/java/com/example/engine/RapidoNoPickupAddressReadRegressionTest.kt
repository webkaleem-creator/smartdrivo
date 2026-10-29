package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidoNoPickupAddressReadRegressionTest {
    @Test
    fun skipPickupAddressKeepsExplicitZeroPickupKmAndDropData() {
        val data = RapidoAdapter.extractOrderDataFromTexts(
            texts = listOf(
                "₹120",
                "Pickup: Old Street",
                "Pickup: 0.0 km",
                "Drop: MG Road",
                "Trip: 4.0 km"
            ),
            skipPickupAddress = true
        )

        assertEquals("Address unavailable", data.pickupAddress)
        assertEquals("MG Road", data.dropAddress)
        assertEquals(0f, data.pickupKm ?: -1f, 0.0001f)
        assertEquals(4f, data.dropKm ?: -1f, 0.0001f)
    }

    @Test
    fun skipPickupAddressDoesNotGuessDropFromGenericAddressCandidates() {
        val data = RapidoAdapter.extractOrderDataFromTexts(
            texts = listOf("₹120", "Pickup: Old Street", "0.5 km", "4.0 km"),
            skipPickupAddress = true
        )

        assertEquals("Address unavailable", data.pickupAddress)
        assertEquals("Address unavailable", data.dropAddress)
    }

    @Test
    fun textAfterTripKmStillDoesNotUsePickupAddressAsDrop() {
        val data = RapidoAdapter.extractOrderDataFromTexts(
            texts = listOf("₹120", "0.5 km", "4.0 km", "Pickup", "Old Street"),
            skipPickupAddress = true
        )
        assertEquals("Address unavailable", data.pickupAddress)
        assertEquals("Address unavailable", data.dropAddress)
    }

    @Test
    fun dropHelperDoesNotUsePickupAddressAsDestinationFallback() {
        val drop = RapidoAdapter.extractAutoPriorityDropAddressFromTexts(
            listOf("0.5 km", "4.0 km", "Pickup: Old Street")
        )
        assertEquals(null, drop)
    }

    @Test
    fun dropHelperSkipsSeparatePickupLabelAndValueAfterKmRows() {
        val drop = RapidoAdapter.extractAutoPriorityDropAddressFromTexts(
            listOf("0.5 km", "4.0 km", "Pickup", "Old Street")
        )
        assertEquals(null, drop)
    }

    @Test
    fun pickupAddressIdsAreFilteredButPickupDistanceIdsAreRetained() {
        assertTrue(RapidoAdapter.isRapidoPickupAddressViewId("com.rapido.passenger:id/tv_pickup_address"))
        assertTrue(RapidoAdapter.isRapidoPickupAddressViewId("com.rapido.passenger:id/source_location"))
        assertFalse(RapidoAdapter.isRapidoPickupAddressViewId("com.rapido.passenger:id/pickup_distance"))
        assertFalse(RapidoAdapter.isRapidoPickupAddressViewId("com.rapido.passenger:id/distance_to_pickup"))
    }
}