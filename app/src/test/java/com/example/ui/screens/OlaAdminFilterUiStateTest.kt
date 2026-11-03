package com.example.ui.screens

import com.example.model.FilterMode
import com.example.model.OlaAdminFilterSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OlaAdminFilterUiStateTest {
    @Test
    fun togglePreservesAllSavedOlaSettings() {
        val saved = OlaAdminFilterSettings(
            enabled = false,
            filterMode = FilterMode.BOTH,
            minFare = 80f,
            maxFare = 240f,
            maxPickupKm = 2f,
            maxTripKm = 6f
        )
        val enabled = OlaAdminFilterUiState.switched(saved, true)
        assertEquals(saved.copy(enabled = true), enabled)
        assertEquals(saved, OlaAdminFilterUiState.switched(enabled, false))
    }

    @Test
    fun modeVisibilityDoesNotMutateValues() {
        assertTrue(OlaAdminFilterUiState.showFare(FilterMode.FARE_ONLY))
        assertFalse(OlaAdminFilterUiState.showDistance(FilterMode.FARE_ONLY))
        assertFalse(OlaAdminFilterUiState.showFare(FilterMode.DISTANCE_ONLY))
        assertTrue(OlaAdminFilterUiState.showDistance(FilterMode.DISTANCE_ONLY))
        assertTrue(OlaAdminFilterUiState.showFare(FilterMode.BOTH))
        assertTrue(OlaAdminFilterUiState.showDistance(FilterMode.BOTH))
    }
}
