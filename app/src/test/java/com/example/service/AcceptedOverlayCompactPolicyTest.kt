package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcceptedOverlayCompactPolicyTest {
    @Test fun phoneWidthIsCompactAndContained() {
        // Typical 360dp wide screen at density 2.
        assertEquals(619, AcceptedOverlayCompactPolicy.widthPx(720, 2f))
        assertTrue(AcceptedOverlayCompactPolicy.widthPx(720, 2f) < 655)
    }

    @Test fun tabletsAndLargePhonesNeverGetHugeOverlay() {
        assertEquals(1020, AcceptedOverlayCompactPolicy.widthPx(1800, 3f))
    }

    @Test fun narrowPhonesAndInvalidDensityStayWithinScreen() {
        assertEquals(258, AcceptedOverlayCompactPolicy.widthPx(300, 1f))
        assertTrue(AcceptedOverlayCompactPolicy.widthPx(120, 2f) <= 120)
        assertTrue(AcceptedOverlayCompactPolicy.widthPx(300, Float.NaN) <= 300)
    }

    @Test fun contentStaysReadableButBounded() {
        assertEquals(3, AcceptedOverlayCompactPolicy.MAX_DROP_ADDRESS_LINES)
        assertTrue(AcceptedOverlayCompactPolicy.HEADER_BADGE_HEIGHT_DP <= 38)
        assertTrue(AcceptedOverlayCompactPolicy.FARE_CELL_HEIGHT_DP <= 43)
        assertTrue(AcceptedOverlayCompactPolicy.ROUTE_CELL_HEIGHT_DP <= 48)
    }
}
