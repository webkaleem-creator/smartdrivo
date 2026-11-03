package com.example.ui.screens

import com.example.model.FilterMode
import com.example.model.OlaAdminFilterSettings

/**
 * UI-only state rules. Never run pending text edits through the ON/OFF switch.
 * The persisted Ola configuration and validation rules are unchanged.
 */
internal object OlaAdminFilterUiState {
    fun switched(saved: OlaAdminFilterSettings, enabled: Boolean): OlaAdminFilterSettings =
        saved.copy(enabled = enabled)

    fun showFare(mode: FilterMode): Boolean = mode != FilterMode.DISTANCE_ONLY

    fun showDistance(mode: FilterMode): Boolean = mode != FilterMode.FARE_ONLY
}
