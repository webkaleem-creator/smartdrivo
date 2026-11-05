package com.example

import com.example.model.MembershipDisplay
import com.example.model.UserProfile
import com.example.model.SpecialOccasionOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MembershipDisplayTest {
    private val now = 1_780_000_000_000L
    private val day = 86_400_000L

    @Test fun adminIgnoresLeftoverPaidPlan() {
        val u=UserProfile(isAdmin=true,plan="3DAYS")
        val shown=MembershipDisplay.of(u,now)
        assertEquals("Admin Access",shown.plan)
        assertEquals("Unlimited",shown.days)
        assertEquals("Active",shown.status)
        assertTrue(shown.active)
        assertEquals("3DAYS",u.plan)
    }

    @Test fun everyDriverSeesStoredPlan() {
        val plans=listOf(
            "FREE_TRIAL" to "1-Day Free Trial",
            "3DAYS" to "3 Days Plan",
            "7DAYS" to "7 Days Plan",
            "15DAYS" to "15 Days Plan",
            "1MONTH" to "1 Month Plan",
            "30DAYS" to "1 Month Plan",
            "1YEAR" to "1 Year Plan",
            "NONE" to "No Plan"
        )
        for ((id,label) in plans) {
            val u=UserProfile(plan=id,isApproved=true,planExpireMillis=now+day)
            assertEquals(label,MembershipDisplay.of(u,now).plan)
        }
    }

    @Test fun approvalAndExpiryAreNotInvented() {
        val u=UserProfile(plan="7DAYS",isApproved=true,planExpireMillis=now+day+1L)
        assertEquals("2 days left",MembershipDisplay.of(u,now).days)
        val expired=MembershipDisplay.of(u.copy(planExpireMillis=now-1L),now)
        assertEquals("Expired",expired.status)
        assertFalse(expired.active)
        assertEquals("Awaiting approval",MembershipDisplay.of(u.copy(isApproved=false),now).days)
        assertTrue(MembershipDisplay.of(u.copy(plan="FREE_TRIAL",isApproved=false),now).active)
    }

    private fun diwaliOffer(days: Int = 3) = SpecialOccasionOffer(
        id = "diwali-2026",
        enabled = true,
        title = "SmartDrivo Diwali Special Offer",
        description = "Diwali free access",
        days = days,
        startsAtMillis = now - 1_000L
    )

    @Test fun diwaliPromoShowsActualFreePlanAndRemainingDays() {
        val offer = diwaliOffer()
        val user = UserProfile(
            plan = "NONE", planExpireMillis = 0L, isApproved = false,
            specialOfferUntilMillis = (now - 1_000L) + (offer.days.toLong() * day)
        )
        val summary = MembershipDisplay.of(user, now, offer)
        assertEquals("SmartDrivo Diwali Special Offer", summary.plan)
        assertEquals("Active", summary.status)
        assertEquals("3 days left", summary.days)
        assertTrue(summary.active)
    }

    @Test fun paidMembershipRetainsPriorityOverSpecialOffer() {
        val offer = diwaliOffer()
        val user = UserProfile(
            plan = "3DAYS", isApproved = true, planExpireMillis = now + day,
            specialOfferUntilMillis = (now - 1_000L) + (offer.days.toLong() * day)
        )
        val summary = MembershipDisplay.of(user, now, offer)
        assertEquals("3 Days Plan", summary.plan)
        assertEquals("1 day left", summary.days)
        assertTrue(summary.active)
    }

    @Test fun expiredPaidMembershipFallsBackToLiveSpecialOffer() {
        val offer = diwaliOffer()
        val user = UserProfile(
            plan = "3DAYS", isApproved = true, planExpireMillis = now - day,
            specialOfferUntilMillis = (now - 1_000L) + (offer.days.toLong() * day)
        )
        val summary = MembershipDisplay.of(user, now, offer)
        assertEquals("SmartDrivo Diwali Special Offer", summary.plan)
        assertTrue(summary.active)
    }

    @Test fun unlimitedOnlyWhenOfferOnAndServerVerified() {
        val offer = diwaliOffer(days = 0)
        val user = UserProfile(
            specialOfferUntilMillis = Long.MAX_VALUE
        )
        assertEquals("Unlimited", MembershipDisplay.of(user, now, offer).days)
        val off = MembershipDisplay.of(user, now, offer.copy(enabled = false))
        assertFalse(off.active)
        assertEquals("Inactive", off.status)
    }

    @Test fun blockedUsersDoNotShowActiveOffer() {
        val offer = diwaliOffer()
        val user = UserProfile(
            isBlocked = true,
            specialOfferUntilMillis = (now - 1_000L) + (offer.days.toLong() * day)
        )
        assertFalse(MembershipDisplay.of(user, now, offer).active)
    }

    @Test fun customAndMissingPlanNeverDefaultToThreeDays() {
        assertEquals("SPECIAL 45 DAYS",MembershipDisplay.of(UserProfile(plan="SPECIAL_45_DAYS"),now).plan)
        assertEquals("No Plan",MembershipDisplay.of(UserProfile(),now).plan)
    }
}
