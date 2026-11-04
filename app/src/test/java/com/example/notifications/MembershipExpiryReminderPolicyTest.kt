package com.example.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MembershipExpiryReminderPolicyTest {
    private val hour = 3_600_000L
    private val now = 1_800_000_000_000L

    @Test fun schedulesThreeDayReminderForFutureExpiry() {
        val expiry = now + 10L * 24L * hour
        val next = MembershipExpiryReminderPolicy.next(expiry, now, -1)!!
        assertEquals(0, next.stage)
        assertEquals(expiry - 72L * hour, next.atMillis)
    }

    @Test fun schedulesImmediateStageIfPlanAlreadyWithinThreeDays() {
        val expiry = now + 71L * hour
        val next = MembershipExpiryReminderPolicy.next(expiry, now, -1)!!
        assertEquals(0, next.stage)
        assertEquals(now + 5_000L, next.atMillis)
    }

    @Test fun skipsOldMissedStagesInsteadOfSpammingThreeNotifications() {
        val expiry = now + 5L * hour
        val next = MembershipExpiryReminderPolicy.next(expiry, now, -1)!!
        assertEquals(2, next.stage)
        assertEquals(now + 5_000L, next.atMillis)
        assertNull(MembershipExpiryReminderPolicy.next(expiry, now, 2))
    }

    @Test fun afterThreeDayNotificationSchedulesOneDayReminder() {
        val expiry = now + 71L * hour
        val next = MembershipExpiryReminderPolicy.next(expiry, now, 0)!!
        assertEquals(1, next.stage)
        assertEquals(expiry - 24L * hour, next.atMillis)
    }

    @Test fun afterOneDayReminderSchedulesSixHourReminder() {
        val expiry = now + 23L * hour
        val next = MembershipExpiryReminderPolicy.next(expiry, now, 1)!!
        assertEquals(2, next.stage)
        assertEquals(expiry - 6L * hour, next.atMillis)
    }

    @Test fun expiredPlansCannotSendNewNotifications() {
        assertNull(MembershipExpiryReminderPolicy.next(now, now, -1))
        assertNull(MembershipExpiryReminderPolicy.next(now - hour, now, -1))
        assertEquals(-1, MembershipExpiryReminderPolicy.currentStage(now - hour, now))
    }

    @Test fun threeDayPlanSkipsImmediatePurchaseNotification() {
        val expiry = now + 72L * hour
        val effectiveLast = MembershipExpiryReminderPolicy.effectiveLastSentStage("3DAYS", -1)
        val schedule = MembershipExpiryReminderPolicy.next(expiry, now, effectiveLast)!!
        assertEquals(1, schedule.stage)
        assertEquals(expiry - 24L * hour, schedule.atMillis)
    }

    @Test fun sevenDayPlanKeepsThreeDayReminder() {
        val expiry = now + 7L * 24L * hour
        assertEquals(-1, MembershipExpiryReminderPolicy.effectiveLastSentStage("7DAYS", -1))
        assertEquals(0, MembershipExpiryReminderPolicy.next(expiry, now, -1)!!.stage)
    }

    @Test fun remainingTextIncludesDaysAndHours() {
        assertEquals("2 days 23 hours",
            MembershipExpiryReminderPolicy.remainingText(now + 71L * hour, now))
        assertEquals("5 hours",
            MembershipExpiryReminderPolicy.remainingText(now + 5L * hour, now))
    }
}
