package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcceptedPopupUiSpecTest {
    @Test fun responsiveWidth() {
        assertEquals(0.90f, AcceptedPopupUiSpec.widthFraction(360), 0.0001f)
        assertEquals(0.78f, AcceptedPopupUiSpec.widthFraction(800), 0.0001f)
    }
    @Test fun heightStaysBounded() {
        assertEquals(406, AcceptedPopupUiSpec.maxHeightDp(700))
        assertTrue(AcceptedPopupUiSpec.maxHeightDp(400) <= 400)
        assertEquals(440, AcceptedPopupUiSpec.maxHeightDp(1200))
    }
    @Test fun actualConfirmationHasPriority() {
        assertEquals("Accepted at", AcceptedPopupUiSpec.eventTime(12345L, 10000L).caption)
        assertEquals(12345L, AcceptedPopupUiSpec.eventTime(12345L, 10000L).timestampMs)
    }
    @Test fun clickAloneIsNotCalledConfirmed() {
        assertEquals("Accept tapped at", AcceptedPopupUiSpec.eventTime(0L, 10000L).caption)
    }
    @Test fun unavailableTimeIsNotInvented() {
        assertEquals(null, AcceptedPopupUiSpec.eventTime(0L, 0L).timestampMs)
    }
}
