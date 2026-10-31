package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidoPostAcceptCadenceTest {
    @Test fun shorterIntervalsRetainObservationWindowAndAvoidEarlyTimeout() {
        assertEquals(40L, RapidoPostAcceptCadence.INITIAL_DELAY_MS)
        assertEquals(150L, RapidoPostAcceptCadence.POLL_INTERVAL_MS)
        assertEquals(27, RapidoPostAcceptCadence.POLL_SAMPLES)
        assertEquals(3_900L, RapidoPostAcceptCadence.POLL_SPAN_MS)
        assertTrue(RapidoPostAcceptCadence.POLL_SPAN_MS >= 3_500L)
    }
}
