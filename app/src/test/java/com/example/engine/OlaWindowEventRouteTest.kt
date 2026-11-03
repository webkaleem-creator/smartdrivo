package com.example.engine

import org.junit.Assert.*
import org.junit.Test

class OlaWindowEventRouteTest {
    @Test fun olaWindowChangeIsNotDiscardedWhileRapidoIsAbsent() {
        assertEquals("com.ola.partner", OlaWindowEventRoute.packageFor("com.ola.partner", emptyList()))
        assertEquals("com.olacabs.oladriver", OlaWindowEventRoute.packageFor(null,
            listOf("com.android.systemui", "com.olacabs.oladriver")))
    }
    @Test fun otherPlatformsAndConsumerPackagesNeverBecomeOlaTargets() {
        assertNull(OlaWindowEventRoute.packageFor("com.rapido.captain", listOf("com.ubercab.driver")))
        assertNull(OlaWindowEventRoute.packageFor("com.olacabs.customer", listOf("com.olacabs.consumer")))
        assertEquals("com.ola.partner", OlaWindowEventRoute.packageFor("com.rapido.captain",
            listOf("com.rapido.captain", "com.ola.partner")))
    }
}
