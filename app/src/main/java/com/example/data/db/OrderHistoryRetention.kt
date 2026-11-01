package com.example.data.db

import java.util.Calendar
import java.util.TimeZone

/** Retain Today and Yesterday in Asia/Kolkata (not a rolling 48 hours). */
object OrderHistoryRetention {
    private fun startOfDay(now: Long, shift: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, shift)
        }.timeInMillis
    fun startOfYesterdayMillis(now: Long = System.currentTimeMillis()) = startOfDay(now, -1)
    fun startOfTodayMillis(now: Long = System.currentTimeMillis()) = startOfDay(now, 0)
    fun startOfTomorrowMillis(now: Long = System.currentTimeMillis()) = startOfDay(now, 1)
    fun isRetained(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean =
        timestamp >= startOfYesterdayMillis(now) && timestamp < startOfTomorrowMillis(now)
}
