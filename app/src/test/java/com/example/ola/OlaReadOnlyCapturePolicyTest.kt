package com.example.ola

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OlaReadOnlyCapturePolicyTest {
    private fun allowed(pkg: String?, admin: Boolean = true, logged: Boolean = true,
                        active: Boolean = true, blocked: Boolean = false,
                        banking: Boolean = false): Boolean =
        OlaReadOnlyCapturePolicy.allowed(pkg, admin, logged, active, blocked, banking)

    @Test fun onlyExactOlaDriverAndAdminCanCapture() {
        assertTrue(allowed("com.olacabs.oladriver"))
        assertFalse(allowed("com.olacabs.customer"))
        assertFalse(allowed("com.rapido.passenger"))
        assertFalse(allowed("com.ubercab.driver"))
        assertFalse(allowed(null))
    }

    @Test fun loginSecurityAndBankingAlwaysBlockPreview() {
        assertFalse(allowed("com.olacabs.oladriver", admin = false))
        assertFalse(allowed("com.olacabs.oladriver", logged = false))
        assertFalse(allowed("com.olacabs.oladriver", active = false))
        assertFalse(allowed("com.olacabs.oladriver", blocked = true))
        assertFalse(allowed("com.olacabs.oladriver", banking = true))
    }
}
