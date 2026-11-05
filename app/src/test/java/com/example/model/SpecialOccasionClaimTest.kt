package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialOccasionClaimTest {
    private val start = 1_780_000_000_000L
    private val day = 86_400_000L
    private fun offer() = SpecialOccasionOffer("diwali2026", true, "Diwali", "3 days free", 3, start)

    @Test fun adminOnDoesNotGrantWithoutClaim() {
        assertFalse(SpecialOccasionClaim().activeFor("uid1", offer(), start + day))
    }
    @Test fun claimAtDifferentDaysGivesIndependentFull72Hours() {
        val first = SpecialOccasionClaim("uid1", "diwali2026", 3, start + 1_000L)
        val second = SpecialOccasionClaim("uid2", "diwali2026", 3, start + day + 1_000L)
        assertEquals(day * 3L, first.expiryMillis() - first.claimedAtMillis)
        assertEquals(day * 3L, second.expiryMillis() - second.claimedAtMillis)
        assertEquals(day, second.expiryMillis() - first.expiryMillis())
        assertTrue(second.activeFor("uid2", offer(), start + day * 3L))
    }
    @Test fun claimDoesNotTransferBetweenUsersOrCampaigns() {
        val claim = SpecialOccasionClaim("uid1", "diwali2026", 3, start + 1_000L)
        assertFalse(claim.activeFor("uid2", offer(), start + day))
        assertFalse(claim.activeFor("uid1", offer().copy(id = "dasera"), start + day))
    }
    @Test fun claimExpiredOrAdminOffCannotGrant() {
        val claim = SpecialOccasionClaim("uid1", "diwali2026", 3, start + 1_000L)
        assertFalse(claim.activeFor("uid1", offer(), start + 4L * day))
        assertFalse(claim.activeFor("uid1", offer().copy(enabled = false), start + day))
    }
    @Test fun futureOrFakeDurationOrBeforeCampaignClaimFailsClosed() {
        val claim = SpecialOccasionClaim("uid1", "diwali2026", 3, start - 1000L)
        assertFalse(claim.activeFor("uid1", offer(), start + 10_000L))
        assertFalse(claim.copy(days = 7, claimedAtMillis = start + 1000L).activeFor("uid1", offer(), start + 10_000L))
        assertFalse(claim.copy(claimedAtMillis = start + 7L * day).activeFor("uid1", offer(), start + day))
    }
    @Test fun unlimitedRequiresClaimAndAdminOn() {
        val unlimited = offer().copy(days = 0)
        val claim = SpecialOccasionClaim("uid1", "diwali2026", 0, start + 500L)
        assertEquals(Long.MAX_VALUE, claim.expiryMillis())
        assertTrue(claim.activeFor("uid1", unlimited, start + 3L * day))
        assertFalse(claim.activeFor("uid1", unlimited.copy(enabled = false), start + 3L * day))
    }
}
