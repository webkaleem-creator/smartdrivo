package com.example

import com.example.data.db.OrderHistoryRetention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class OrderHistoryRetentionTest {
    private fun at(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            clear()
            set(y, m - 1, d, h, min, 0)
        }.timeInMillis

    @Test fun todayAndEntireYesterdayRetained() {
        val now = at(2026, 11, 1, 9, 37)
        assertTrue(OrderHistoryRetention.isRetained(at(2026, 11, 1), now))
        assertTrue(OrderHistoryRetention.isRetained(at(2026, 10, 31), now))
        assertTrue(OrderHistoryRetention.isRetained(at(2026, 10, 31, 23, 59), now))
        assertFalse(OrderHistoryRetention.isRetained(at(2026, 10, 30, 23, 59), now))
        assertFalse(OrderHistoryRetention.isRetained(at(2026, 11, 2), now))
        assertFalse(OrderHistoryRetention.isRetained(0L, now))
    }

    @Test fun newMidnightExpiresOldYesterday() {
        val row = at(2026, 10, 31, 22)
        assertTrue(OrderHistoryRetention.isRetained(row, at(2026, 11, 1, 23, 59)))
        assertFalse(OrderHistoryRetention.isRetained(row, at(2026, 11, 2)))
        assertTrue(OrderHistoryRetention.isRetained(at(2026, 11, 1), at(2026, 11, 2)))
    }

    @Test fun yearAndLeapMonthRollover() {
        assertEquals(at(2026, 12, 31), OrderHistoryRetention.startOfYesterdayMillis(at(2027, 1, 1)))
        assertEquals(at(2028, 2, 29), OrderHistoryRetention.startOfYesterdayMillis(at(2028, 3, 1)))
    }
}
