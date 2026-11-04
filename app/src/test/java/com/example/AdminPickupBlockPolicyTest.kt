package com.example

import com.example.engine.AdminPickupBlockPolicy
import com.example.engine.PickupOnlyLabel
import com.example.model.AdminPickupBlockSettings
import com.example.model.*
import com.example.engine.RapidoIgnoredVisibleOfferGuard
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

    @Test fun wrongPackageWindowInvisibleAndDropFieldsCannotAuthorizePickupMatching() {
        val pkg = "com.rapido.passenger"
        assertTrue(PickupOnlyLabel.trustedNode(pkg, pkg, Platform.RAPIDO, 4, 4, true))
        assertFalse(PickupOnlyLabel.trustedNode(pkg, "com.ubercab.driver", Platform.RAPIDO, 4, 4, true))
        assertFalse(PickupOnlyLabel.trustedNode(pkg, pkg, Platform.RAPIDO, 4, 5, true))
        assertFalse(PickupOnlyLabel.trustedNode(pkg, pkg, Platform.RAPIDO, 4, 4, false))
        assertFalse(PickupOnlyLabel.trustedNode("com.example.fake", "com.example.fake", Platform.RAPIDO, 4, 4, true))
        assertNull(PickupOnlyLabel.fromField("Pickup: Airport", "$pkg:id/drop_address"))
        assertNull(PickupOnlyLabel.fromField("Pickup: Hitech City Destination address: Airport", null))
    }

    @Test fun blockedDuplicatesDoNotRetryButMissingPickupAndSettingChangesReevaluate() {
        val candidate = RideCandidate(45f, .9f, 2.2f, "Address unavailable", "Destination", "Area",
            Platform.RAPIDO, VehicleType.AUTO)
        val guard = RapidoIgnoredVisibleOfferGuard()
        val on = on("airport")
        guard.rememberIgnored(candidate, on, ownerVeto = true)
        repeat(20) {
            assertTrue(guard.hasOwnerVeto()) // Even when normal Auto Reject is ON.
            assertTrue(guard.isDuplicateIgnored(candidate))
            assertFalse(guard.needsFilterRecheck(candidate, on))
        }
        assertTrue(guard.needsFilterRecheck(candidate, on.copy(enabled = false)))
        guard.rememberIgnored(candidate, ownerVeto = true) // Unreadable is not a final blacklist.
        assertTrue(guard.needsFilterRecheck(candidate, on))
        assertEquals(AdminPickupBlockPolicy.Decision.Allow,
            AdminPickupBlockPolicy.evaluate(on, "Hitech City"))
        guard.clear()
        assertFalse(guard.hasOwnerVeto())
    }
}
