package com.example.ola

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OlaShizukuPolicyTest {
    @Test fun missingOrUnauthorizedShizukuIsNotReady() {
        assertEquals(OlaShizukuPolicy.Status.SERVICE_MISSING,
            OlaShizukuPolicy.status(false, false))
        assertEquals(OlaShizukuPolicy.Status.AUTHORIZATION_REQUIRED,
            OlaShizukuPolicy.status(true, false))
        assertEquals(OlaShizukuPolicy.Status.READY,
            OlaShizukuPolicy.status(true, true))
    }

    @Test fun olaBridgeCannotTouchRapidoOrOtherPackages() {
        assertFalse(OlaShizukuPolicy.canUseOlaBridge(true, true, "com.rapido.captain", true, true))
        assertFalse(OlaShizukuPolicy.canUseOlaBridge(true, true, "com.ubercab.driver", true, true))
        assertFalse(OlaShizukuPolicy.canUseOlaBridge(true, true, null, true, true))
    }

    @Test fun permissionMasterAndOlaEnableAreAllRequired() {
        val ola = "com.olacabs.oladriver"
        assertFalse(OlaShizukuPolicy.canUseOlaBridge(false, true, ola, true, true))
        assertFalse(OlaShizukuPolicy.canUseOlaBridge(true, false, ola, true, true))
        assertFalse(OlaShizukuPolicy.canUseOlaBridge(true, true, ola, false, true))
        assertFalse(OlaShizukuPolicy.canUseOlaBridge(true, true, ola, true, false))
        assertTrue(OlaShizukuPolicy.canUseOlaBridge(true, true, ola, true, true))
    }
}
