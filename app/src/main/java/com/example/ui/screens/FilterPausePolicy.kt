package com.example.ui.screens

import com.example.model.AppSettings
import com.example.model.AreaGroup

/**
 * Pure presentation policy. It does not write saved settings or alter order decisions.
 * GO TO group state drives the UI even when the legacy settings flow is briefly stale.
 */
internal object FilterPausePolicy {
    data class State(val fastest: Boolean, val goTo: Boolean) {
        val normalFilterPauseReason: String?
            get() = if (fastest) "Fastest Mode" else if (goTo) "GO TO" else null

        // GO TO, NO GO and +Amount remain available together.
        val fastestPauseReason: String?
            get() = if (fastest) "Fastest Mode" else null
    }

    fun resolve(settings: AppSettings, goToGroups: List<AreaGroup>): State {
        val hasEnabledGoTo = goToGroups.any { group ->
            group.isEnabled && (
                group.areas.any { it.name.isNotBlank() } ||
                group.keywords.any { it.isNotBlank() && !it.equals(group.name, true) }
            )
        }
        return State(
            fastest = settings.isFastestModeEnabled,
            goTo = !settings.isFastestModeEnabled && hasEnabledGoTo
        )
    }
}
