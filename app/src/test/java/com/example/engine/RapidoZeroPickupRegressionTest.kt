package com.example.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidoZeroPickupRegressionTest {
    @Test
    fun explicitZeroPickupIsValidInsideAnyPositiveMaximum() {
        assertTrue(RapidoDistanceValidity.isValidPickup(0f))
        assertTrue(RapidoDistanceValidity.isValidPickup(0f) && 0f <= 0.6f)
        assertTrue(RapidoDistanceValidity.isValidPickup(0.5f) && 0.5f <= 0.6f)
    }

    @Test
    fun negativeOrMissingPickupIsNotValidZeroPickup() {
        assertFalse(RapidoDistanceValidity.isValidPickup(null))
        assertFalse(RapidoDistanceValidity.isValidPickup(-0.01f))
        assertFalse(RapidoDistanceValidity.isValidPickup(Float.NaN))
    }

    @Test
    fun zeroDropIsNotAValidTrip() {
        assertFalse(RapidoDistanceValidity.isValidDrop(0f))
        assertFalse(RapidoDistanceValidity.isValidDrop(null))
        assertFalse(RapidoDistanceValidity.isValidDrop(Float.NaN))
        assertTrue(RapidoDistanceValidity.isValidDrop(0.1f))
    }

    @Test
    fun earlyReadGateRecognizesZeroPickupWhenOtherSignalsExist() {
        assertTrue(
            RapidoEarlyReadGate.hasPartialOrderSignal(
                fare = 100f,
                pickupKm = 0f,
                tripKm = 4f,
                strictAcceptFound = true
            )
        )
        assertFalse(
            RapidoEarlyReadGate.rejectHomeOverlay(
                homeDetected = true,
                fare = 100f,
                pickupKm = 0f,
                tripKm = 4f,
                strictAcceptFound = true
            )
        )
    }

    @Test
    fun parserPreservesExplicitZeroPickupAndZeroDropForDifferentDecisions() {
        val data = requireNotNull(RapidoAdapter.extractOrderDataFromTexts(
            texts = listOf("â‚¹120", "Pickup 0 km", "Trip 4 km"),
            skipPickupAddress = true
        ))
        assertTrue("explicit pickup zero should be retained", data.pickupKm == 0f)
        assertTrue("positive trip should be retained", data.dropKm == 4f)

        val zeroTrip = requireNotNull(RapidoAdapter.extractOrderDataFromTexts(
            texts = listOf("â‚¹120", "Pickup 0 km", "Trip 0 km"),
            skipPickupAddress = true
        ))
        assertTrue("explicit pickup zero should be retained", zeroTrip.pickupKm == 0f)
        assertTrue("explicit zero trip should remain visible to the rejection guard", zeroTrip.dropKm == 0f)
        assertFalse(RapidoDistanceValidity.isValidDrop(zeroTrip.dropKm))
    }
}