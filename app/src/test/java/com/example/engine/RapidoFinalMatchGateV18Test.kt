package com.example.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidoFinalMatchGateV18Test {
    @Test fun noEnabledFilterMatchNeverClicks() {
        assertFalse(RapidoFinalMatchGateV18.isEligible(false, false, false, false, false))
    }
    @Test fun anAllowedNormalModeAlternativeMayMatch() {
        assertTrue(RapidoFinalMatchGateV18.isEligible(false, false, true, false, false))
        assertTrue(RapidoFinalMatchGateV18.isEligible(false, false, false, true, false))
        assertTrue(RapidoFinalMatchGateV18.isEligible(false, false, false, false, true))
        assertTrue(RapidoFinalMatchGateV18.isEligible(false, true, false, false, false))
    }
    @Test fun fastestIgnoresAllOtherModeMatches() {
        assertFalse(RapidoFinalMatchGateV18.isEligible(true, false, true, true, true))
        assertTrue(RapidoFinalMatchGateV18.isEligible(true, true, false, false, false))
    }
}
