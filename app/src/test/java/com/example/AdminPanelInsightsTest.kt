package com.example

import com.example.model.AdminPanelInsights
import com.example.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPanelInsightsTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    @Test fun enabledAccountIsNotAutomaticallySubscribed() {
        val expired = UserProfile(uid="expired", isActive=true,
            plan="3DAYS", isApproved=true, planExpireMillis=now-day)
        val paid = UserProfile(uid="paid", isActive=true,
            plan="7DAYS", isApproved=true, planExpireMillis=now+day)
        val disabledPaid = UserProfile(uid="disabled", isActive=false,
            plan="1MONTH", isApproved=true, planExpireMillis=now+day)
        val pending = UserProfile(uid="pending", isActive=true,
            plan="7DAYS", isApproved=false, planExpireMillis=now+day)
        val owner = UserProfile(uid="owner", isAdmin=true, isActive=true)
        val users = listOf(expired, paid, disabledPaid, pending, owner)
        val c = AdminPanelInsights.calculate(users,now)
        assertEquals(5,c.totalUsers)
        assertEquals(4,c.enabledAccounts)
        assertEquals(1,c.disabledAccounts)
        assertEquals(2,c.validPlans)
        assertEquals(1,c.expiredPlans)
        assertEquals(1,c.awaitingApproval)
        assertEquals(1,c.admins)
        assertTrue(AdminPanelInsights.matches(expired, AdminPanelInsights.UserFilter.EXPIRED_PLAN,now))
        assertFalse(AdminPanelInsights.matches(expired, AdminPanelInsights.UserFilter.VALID_PLAN,now))
        assertTrue(AdminPanelInsights.matches(disabledPaid,AdminPanelInsights.UserFilter.DISABLED,now))
        assertTrue(AdminPanelInsights.matches(pending, AdminPanelInsights.UserFilter.AWAITING,now))
        assertFalse(AdminPanelInsights.matches(owner, AdminPanelInsights.UserFilter.VALID_PLAN,now))
    }

    @Test fun trialIsValidWithoutApprovalButNotAwaitingApproval() {
        val trial = UserProfile(plan="FREE_TRIAL",isApproved=false,planExpireMillis=now+day)
        assertTrue(AdminPanelInsights.matches(trial,AdminPanelInsights.UserFilter.VALID_PLAN,now))
        assertFalse(AdminPanelInsights.matches(trial,AdminPanelInsights.UserFilter.AWAITING,now))
    }

    @Test fun soonestExpirationSortNeverPutsExpiredBeforeValid() {
        val later=UserProfile(uid="later",name="Z",plan="7DAYS",isApproved=true,planExpireMillis=now+3*day)
        val sooner=UserProfile(uid="sooner",name="A",plan="3DAYS",isApproved=true,planExpireMillis=now+day)
        val expired=UserProfile(uid="expired",name="B",plan="3DAYS",isApproved=true,planExpireMillis=now-day)
        val ordered=AdminPanelInsights.sorted(listOf(later,expired,sooner),AdminPanelInsights.SortOrder.EXPIRING_SOON,now)
        assertEquals(listOf("sooner","later","expired"), ordered.map { it.uid })
    }
}
