package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Auto Priority is a badge; its values must be parsed as an ordinary Rapido offer. */
class RapidoAutoPriorityNormalRouteV15Test {
    @Test fun autoPriorityBadgeDoesNotRequireSeparateFareDistanceParser() {
        val oneOffer = listOf(
            "Auto Priority", "₹90", "0.4 km", "TadBun Falaknuma",
            "3.2 km", "Kamatipura - Grace Multispeciality hospital", "Accept"
        )
        val parsed = RapidoAdapter.extractOrderDataFromTexts(oneOffer, skipPickupAddress = true)
        assertEquals(90f, parsed.totalFare, 0.01f)
        assertEquals(0.4f, parsed.pickupKm ?: -1f, 0.01f)
        assertEquals(3.2f, parsed.dropKm ?: -1f, 0.01f)
        val validation = RapidoAdapter.validateRapidoOrderPopupFromTexts(oneOffer, hasAcceptButton = true)
        assertTrue(validation.isValid)
    }

    @Test fun autoPriorityIncompleteDataDoesNotBecomeValidJustFromBadge() {
        val missingPickup = listOf("Auto Priority", "₹90", "Kamatipura", "Accept")
        assertFalse(RapidoAdapter.validateRapidoOrderPopupFromTexts(missingPickup, true).isValid)
    }
}
