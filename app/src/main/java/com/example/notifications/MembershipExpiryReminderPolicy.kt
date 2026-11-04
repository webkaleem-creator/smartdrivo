package com.example.notifications

/**
 * All expiry thresholds are based on the absolute, server-synced planExpireMillis.
 * The operating system may deliver inexact background alarms later than their targets.
 */
internal object MembershipExpiryReminderPolicy {
    private const val HOUR_MS = 60L * 60L * 1000L
    val reminderLeadTimesMs = longArrayOf(72L * HOUR_MS, 24L * HOUR_MS, 6L * HOUR_MS)

    data class Schedule(val stage: Int, val atMillis: Long)

    fun currentStage(expiryMillis: Long, nowMillis: Long): Int {
        if (expiryMillis <= nowMillis || expiryMillis <= 0L) return -1
        return reminderLeadTimesMs.indices.lastOrNull {
            nowMillis >= expiryMillis - reminderLeadTimesMs[it]
        } ?: -1
    }

    fun next(expiryMillis: Long, nowMillis: Long, lastSentStage: Int): Schedule? {
        if (expiryMillis <= nowMillis || expiryMillis <= 0L) return null
        val stageDue = currentStage(expiryMillis, nowMillis)
        if (stageDue > lastSentStage) {
            // One immediate reminder for the most urgent stage, not three late alerts.
            return Schedule(stageDue, nowMillis + 5_000L)
        }
        val nextStage = (lastSentStage + 1).coerceAtLeast(0)
        if (nextStage >= reminderLeadTimesMs.size) return null
        val dueAt = expiryMillis - reminderLeadTimesMs[nextStage]
        return Schedule(nextStage, maxOf(nowMillis + 5_000L, dueAt))
    }

    // The 3-day pass would otherwise receive a three-day reminder immediately
    // upon purchase. Skip that stage; keep the 24h and 6h reminders.
    fun effectiveLastSentStage(planId: String, lastSentStage: Int): Int =
        if (planId.equals("3DAYS", ignoreCase = true)) maxOf(lastSentStage, 0)
        else lastSentStage

    fun remainingText(expiryMillis: Long, nowMillis: Long): String {
        val hours = ((expiryMillis - nowMillis).coerceAtLeast(0L) + HOUR_MS - 1L) / HOUR_MS
        val days = hours / 24L
        val remainder = hours % 24L
        val dayText = if (days == 1L) "1 day" else "$days days"
        val hourText = if (remainder == 1L) "1 hour" else "$remainder hours"
        return if (days > 0L) "$dayText $hourText" else hourText
    }
}
