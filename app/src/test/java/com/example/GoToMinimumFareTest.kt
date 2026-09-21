package com.example

import com.example.engine.AreaRulesEngine
import com.example.engine.DecisionResult
import com.example.model.AreaGroup
import com.example.model.AreaType
import com.example.model.AppSettings
import com.example.model.RideCandidate
import org.junit.Assert.assertTrue
import org.junit.Test

class GoToMinimumFareTest {

    private val settings = AppSettings(
        isGoToEnabled = true,
        isNoGoEnabled = false
    )

    private val group = AreaGroup(
        id = "goto-min-fare-test",
        name = "Test",
        isEnabled = true,
        type = AreaType.GO_TO,
        keywords = listOf("Tadban"),
        minFare = 50f,
        maxFare = 0f,
        maxPickupKm = 5f,
        maxDropKm = 20f
    )

    private fun ride(fare: Float) = RideCandidate(
        fare = fare,
        pickupDistKm = 0.7f,
        dropDistKm = 5.0f,
        pickupAddress = "Pickup Area",
        dropAddress = "Tadban Hyderabad",
        dropArea = "Tadban"
    )

    @Test
    fun goToFareBelowMinimumIsRejected() {
        val result = AreaRulesEngine.evaluateRide(
            candidate = ride(49f),
            settings = settings,
            goToAreas = listOf(group),
            noGoAreas = emptyList(),
            isGoToEnabled = true,
            isNoGoEnabled = false
        )

        assertTrue(result is DecisionResult.Reject)
        assertTrue(
            (result as DecisionResult.Reject)
                .reason
                .contains("below minimum", ignoreCase = true)
        )
    }

    @Test
    fun goToFareAtMinimumIsAccepted() {
        val result = AreaRulesEngine.evaluateRide(
            candidate = ride(50f),
            settings = settings,
            goToAreas = listOf(group),
            noGoAreas = emptyList(),
            isGoToEnabled = true,
            isNoGoEnabled = false
        )

        assertTrue(result is DecisionResult.Accept)
    }

    @Test
    fun goToFareAboveMinimumIsAccepted() {
        val result = AreaRulesEngine.evaluateRide(
            candidate = ride(80f),
            settings = settings,
            goToAreas = listOf(group),
            noGoAreas = emptyList(),
            isGoToEnabled = true,
            isNoGoEnabled = false
        )

        assertTrue(result is DecisionResult.Accept)
    }

    @Test
    fun oldSavedMaxFareValueMigratesAsMinimumFare() {
        val legacyGroup = group.copy(
            minFare = 50f,
            maxFare = 70f
        )

        val rejected = AreaRulesEngine.evaluateRide(
            candidate = ride(60f),
            settings = settings,
            goToAreas = listOf(legacyGroup),
            noGoAreas = emptyList(),
            isGoToEnabled = true,
            isNoGoEnabled = false
        )

        val accepted = AreaRulesEngine.evaluateRide(
            candidate = ride(70f),
            settings = settings,
            goToAreas = listOf(legacyGroup),
            noGoAreas = emptyList(),
            isGoToEnabled = true,
            isNoGoEnabled = false
        )

        assertTrue(rejected is DecisionResult.Reject)
        assertTrue(accepted is DecisionResult.Accept)
    }
}