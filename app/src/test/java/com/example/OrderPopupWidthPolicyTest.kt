package com.example

import com.example.service.OrderPopupWidthPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPopupWidthPolicyTest {
    @Test
    fun shortStatusDoesNotReserveFullFixedWidth() {
        assertEquals(174, OrderPopupWidthPolicy.resolveWidthPx(1080, 1f, 150))
        assertTrue(OrderPopupWidthPolicy.resolveWidthPx(1080, 1f, 150) < 288)
    }

    @Test
    fun longContentIsCappedButCanStillWrap() {
        assertEquals(260, OrderPopupWidthPolicy.resolveWidthPx(1080, 1f, 900))
    }

    @Test
    fun narrowScreenIsNeverExceeded() {
        assertEquals(176, OrderPopupWidthPolicy.resolveWidthPx(200, 1f, 500))
    }

    @Test
    fun verySmallScreenHasValidWidth() {
        assertEquals(1, OrderPopupWidthPolicy.resolveWidthPx(16, 1f, 500))
    }

    @Test
    fun densityScalesConstraints() {
        assertEquals(348, OrderPopupWidthPolicy.resolveWidthPx(1080, 2f, 300))
        assertEquals(520, OrderPopupWidthPolicy.resolveWidthPx(1080, 2f, 900))
    }
}
