package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialOfferSnapshotPolicyTest {
    private fun livePayload() = mapOf(
        "id" to "diwali-2026", "enabled" to true, "title" to "Diwali Offer",
        "description" to "3 days free", "days" to 3,
        "startsAtMillis" to (System.currentTimeMillis() - 1000L)
    )

    @Test fun cacheThenIdenticalServerDataMustGrantOnlyAfterServerConfirmation() {
        val data = livePayload()
        val cached = SpecialOfferSnapshotPolicy.reconcile(fromCache = true, data = data)
        assertEquals(SpecialOfferSyncStatus.CACHE_ONLY, cached.status)
        assertFalse(cached.offer.isLive())
        val confirmed = SpecialOfferSnapshotPolicy.reconcile(fromCache = false, data = data)
        assertEquals(SpecialOfferSyncStatus.SERVER_LIVE, confirmed.status)
        assertTrue(confirmed.offer.isLive())
    }

    @Test fun serverOffDoesNotGrantEvenWithPriorCacheOn() {
        val off = livePayload() + mapOf("enabled" to false)
        val result = SpecialOfferSnapshotPolicy.reconcile(fromCache = false, data = off)
        assertEquals(SpecialOfferSyncStatus.SERVER_NO_OFFER, result.status)
        assertFalse(result.offer.isLive())
    }

    @Test fun serverMissingCampaignFailsClosed() {
        val result = SpecialOfferSnapshotPolicy.reconcile(fromCache = false, data = null)
        assertEquals(SpecialOfferSyncStatus.SERVER_NO_OFFER, result.status)
        assertFalse(result.offer.isLive())
    }
}
