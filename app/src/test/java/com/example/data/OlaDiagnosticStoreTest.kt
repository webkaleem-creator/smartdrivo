package com.example.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OlaDiagnosticStoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test fun recordsOnlyKnownStagesWithNoScreenContent() {
        OlaDiagnosticStore.clear(context)
        OlaDiagnosticStore.record(context, OlaDiagnosticStore.Stage.EVENT_RECEIVED)
        OlaDiagnosticStore.record(context, OlaDiagnosticStore.Stage.OFFER_PARSED_FARE, 74)
        val report = OlaDiagnosticStore.export(context)
        assertTrue(report.contains("EVENT_RECEIVED"))
        assertTrue(report.contains("OFFER_PARSED_FARE | value=74"))
        assertFalse(report.contains("pickupAddress"))
        assertFalse(report.contains("bookingId"))
    }

    @Test fun survivesReadsAndCanBeCleared() {
        OlaDiagnosticStore.clear(context)
        OlaDiagnosticStore.record(context, OlaDiagnosticStore.Stage.BUTTON_MISSING)
        assertTrue(OlaDiagnosticStore.summary(context).contains("BUTTON_MISSING"))
        assertTrue(OlaDiagnosticStore.export(context).contains("BUTTON_MISSING"))
        OlaDiagnosticStore.clear(context)
        assertTrue(OlaDiagnosticStore.export(context).contains("No Ola events recorded yet"))
    }

    @Test fun limitsNumericValues() {
        OlaDiagnosticStore.clear(context)
        OlaDiagnosticStore.record(context, OlaDiagnosticStore.Stage.ROOT_SELECTED, Int.MAX_VALUE)
        assertTrue(OlaDiagnosticStore.export(context).contains("value=100000"))
        OlaDiagnosticStore.clear(context)
        assertEquals("No Ola events yet", OlaDiagnosticStore.summary(context))
    }
}
