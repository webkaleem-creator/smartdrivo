package com.example

import com.example.engine.AdminPickupBlockPolicy
import com.example.engine.PickupOnlyLabel
import com.example.model.AdminPickupBlockSettings
import org.junit.Assert.*
import org.junit.Test

class AdminPickupBlockPolicyTest {
    private fun on(vararg words: String) =
        AdminPickupBlockSettings(enabled = true, blockedWords = words.toList())

    @Test fun commaSeparatedWordsAreNormalizedDeduplicated() {
        assertEquals(listOf("tank bund", "charminar", "hyderabad railway station"),
            AdminPickupBlockPolicy.parse(" Tank Bund,CHARMINAR, Tank Bund\nHyderabad Railway Station "))
    }

    @Test fun matchingPickupBlocksRegardlessOfCaseOrPunctuation() {
        val s = on("tank bund", "charminar")
        assertEquals(AdminPickupBlockPolicy.Decision.Blocked("tank bund"),
            AdminPickupBlockPolicy.evaluate(s, "Near TANK BUND, Hyderabad"))
        assertEquals(AdminPickupBlockPolicy.Decision.Blocked("charminar"),
            AdminPickupBlockPolicy.evaluate(s, "Near (Charminar) Circle"))
    }

    @Test fun matchesFullWordsNotSubstrings() {
        val s = on("tank", "railway station")
        assertEquals(AdminPickupBlockPolicy.Decision.Allow,
            AdminPickupBlockPolicy.evaluate(s, "Tankers Road"))
        assertEquals(AdminPickupBlockPolicy.Decision.Allow,
            AdminPickupBlockPolicy.evaluate(s, "Railway Stationary Market"))
    }

    @Test fun destinationCannotTriggerPickupBlock() {
        val s = on("charminar")
        val pickupOnly = "Hitech City"
        val unrelatedDrop = "Charminar"
        assertEquals(AdminPickupBlockPolicy.Decision.Allow,
            AdminPickupBlockPolicy.evaluate(s, pickupOnly))
        assertFalse(PickupOnlyLabel.fromField("Drop: " + unrelatedDrop, null) != null)
    }

    @Test fun enabledWithMissingPickupFailsClosed() {
        val s = on("airport")
        assertEquals(AdminPickupBlockPolicy.Decision.Unreadable,
            AdminPickupBlockPolicy.evaluate(s, null))
        assertEquals(AdminPickupBlockPolicy.Decision.Unreadable,
            AdminPickupBlockPolicy.evaluate(s, "Address unavailable"))
    }

    @Test fun offIgnoresUnreadablePickupAndBlockingKeywords() {
        val s = AdminPickupBlockSettings(false, listOf("airport"))
        assertEquals(AdminPickupBlockPolicy.Decision.Allow,
            AdminPickupBlockPolicy.evaluate(s, null))
        assertEquals(AdminPickupBlockPolicy.Decision.Allow,
            AdminPickupBlockPolicy.evaluate(s, "Airport Road"))
    }

    @Test fun protectsAgainstEmptyEnabledConfiguration() {
        assertFalse(AdminPickupBlockPolicy.valid(on()))
        assertTrue(AdminPickupBlockPolicy.valid(on("airport")))
    }

    @Test fun requiresExplicitPickupFieldNeverDropField() {
        assertEquals("Tank Bund", PickupOnlyLabel.fromField("Pickup: Tank Bund", null))
        assertEquals("Hitech City",
            PickupOnlyLabel.fromField("Hitech City", "com.rapido.captain:id/pickup_address"))
        assertNull(PickupOnlyLabel.fromField("Charminar", "com.rapido.captain:id/drop_address"))
        assertEquals("Tank Bund",
            PickupOnlyLabel.fromField("Tank Bund", "com.rapido.captain:id/tvPickupAddress"))
        assertNull(PickupOnlyLabel.fromField("1.5 km",
            "com.rapido.captain:id/pickupKmDistance"))
        assertNull(PickupOnlyLabel.fromField("Pickup: 1.2 km", null))
        assertNull(PickupOnlyLabel.fromField("Drop: Tank Bund", null))
    }
}
