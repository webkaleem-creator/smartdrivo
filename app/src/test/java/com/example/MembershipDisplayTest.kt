package com.example

import com.example.model.MembershipDisplay
import com.example.model.UserProfile
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

    @Test fun customAndMissingPlanNeverDefaultToThreeDays() {
        assertEquals("SPECIAL 45 DAYS",MembershipDisplay.of(UserProfile(plan="SPECIAL_45_DAYS"),now).plan)
        assertEquals("No Plan",MembershipDisplay.of(UserProfile(),now).plan)
    }
}
