package com.example.engine

import com.example.model.AppSettings
import com.example.model.Platform
import org.junit.Assert.*
import org.junit.Test

class RapidoReleaseSafetyTest {
    @Test fun olaDefaultsOff() { assertFalse(AppSettings().olaEnabled) }
    @Test fun rapidoDefaultsOn() { assertTrue(AppSettings().rapidoEnabled) }
    @Test fun historicalOlaPlatformStillReadable() { assertEquals("Ola", Platform.OLA.displayName) }
}