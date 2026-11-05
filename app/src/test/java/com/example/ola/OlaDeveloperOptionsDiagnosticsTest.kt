package com.example.ola

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OlaDeveloperOptionsDiagnosticsTest {
    @Test fun zeroDeveloperOrUsbFlagIsNotFalselyMarkedOff() {
        val dev = OlaDeveloperOptionsDiagnostics.maskedStatus(0)
        assertEquals("App-reported 0 (masked; actual state unknown)", dev)
        assertFalse(dev.contains("OFF"))
    }

    @Test fun unknownAndMaskedFlagsRemainTransparent() {
        assertEquals("Unavailable to this app",
            OlaDeveloperOptionsDiagnostics.maskedStatus(null))
        assertEquals("App-reported 1 (not independently verified)",
            OlaDeveloperOptionsDiagnostics.maskedStatus(1))
        assertEquals("Unexpected app-reported value (2)",
            OlaDeveloperOptionsDiagnostics.maskedStatus(2))
    }

    @Test fun wirelessIndicatorsAreNotMistakenForLiveAdbConnection() {
        assertEquals("Reported ON (connection not tested)",
            OlaDeveloperOptionsDiagnostics.wirelessStatus(1))
        assertEquals("Reported OFF (connection not tested)",
            OlaDeveloperOptionsDiagnostics.wirelessStatus(0))
        assertEquals("Unavailable to this app",
            OlaDeveloperOptionsDiagnostics.wirelessStatus(null))
    }
}
