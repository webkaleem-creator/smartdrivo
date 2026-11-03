package com.example.engine

import com.example.model.AppSettings
import com.example.model.AreaGroup
import com.example.model.FilterMode
import com.example.model.OlaAdminFilterSettings
import com.example.model.RideCandidate

/** Ola-only numeric rules replace Home; shared destination and bundle safety remains. */
object OlaAdminAcceptancePolicy {
    fun evaluate(
        candidate: RideCandidate,
        ola: OlaAdminFilterSettings,
        shared: AppSettings,
        goToAreas: List<AreaGroup> = emptyList(),
        noGoAreas: List<AreaGroup> = emptyList(),
        noGoEnabled: Boolean = shared.isNoGoEnabled
    ): DecisionResult {
        when (val own = OlaAdminFilterEngine.evaluate(candidate, ola)) {
            is OlaAdminFilterEngine.Result.Ignore -> return DecisionResult.Ignore(own.reason)
            OlaAdminFilterEngine.Result.Match -> Unit
        }
        // Only an in-memory copy: persisted Home/Rapido/Uber numeric settings untouched.
        // Fastest OFF ensures it cannot bypass destination No-Go/Go-To or Bundle safety.
        val safetyOnly = shared.copy(
            isFastestModeEnabled = false,
            isPrimaryFilterEnabled = true,
            isSecondaryBothFilterEnabled = false,
            isTertiaryBothFilterEnabled = false,
            extraFareRules = emptyList(),
            filterMode = FilterMode.FARE_ONLY,
            minFare = 0f,
            maxFare = 0f
        )
        return when (val mandatory = AreaRulesEngine.evaluateRide(
            candidate = candidate,
            settings = safetyOnly,
            goToAreas = goToAreas,
            noGoAreas = noGoAreas,
            pickupLocationTextOverride = "",
            isGoToEnabled = shared.isGoToEnabled,
            isNoGoEnabled = noGoEnabled
        )) {
            is DecisionResult.Accept -> DecisionResult.Accept(
                "Ola Global Filter (${ola.filterMode.displayName}) matched; shared rules passed"
            )
            is DecisionResult.Ignore -> DecisionResult.Ignore("Ola shared rule: ${mandatory.reason}")
            is DecisionResult.Reject -> DecisionResult.Ignore("Ola shared area rule: ${mandatory.reason}")
        }
    }
}
