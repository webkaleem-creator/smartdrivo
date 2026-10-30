package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PickupDistancePresentationV19Test {
    @Test fun numericZeroDisplaysAsZero() {
        assertEquals("0.0 km", PickupDistancePresentationV19.label(0f))
    }

    @Test fun positiveDistanceDisplaysNormally() {
        assertEquals("0.6 km", PickupDistancePresentationV19.label(0.6f))
    }

    @Test fun unknownIsNotFalselyShownAsZero() {
        assertEquals("Unavailable", PickupDistancePresentationV19.label(null))
        assertEquals("Unavailable", PickupDistancePresentationV19.label(Float.NaN))
        assertEquals("Unavailable", PickupDistancePresentationV19.label(-1f))
    }
}
