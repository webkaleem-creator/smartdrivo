package com.example.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileSpecialOccasionTest {
    @Test fun specialFreeAccessWorksWithoutChangingPaidFields() {
        val now = System.currentTimeMillis()
        val user = UserProfile(plan = "NONE", planPrice = 0, isApproved = false,
            specialOfferUntilMillis = now + 86_400_000L)
        assertTrue(user.isPlanValid)
        assertTrue(user.copy(specialOfferUntilMillis = 0L).plan == "NONE")
        assertFalse(user.copy(specialOfferUntilMillis = 0L).isPlanValid)
    }
    @Test fun disablingPromotionDoesNotRevokePaidMembership() {
        val now = System.currentTimeMillis()
        val paid = UserProfile(plan = "7DAYS", planPrice = 101, isApproved = true,
            planExpireMillis = now + 86_400_000L,
            specialOfferUntilMillis = Long.MAX_VALUE)
        assertTrue(paid.copy(specialOfferUntilMillis = 0L).isPlanValid)
    }
    @Test fun blockedAccountsNeverUsePromotion() {
        val user = UserProfile(isBlocked = true,
            specialOfferUntilMillis = Long.MAX_VALUE)
        assertFalse(user.isPlanValid)
    }
}
