package com.example

import com.example.engine.DecisionResult
import com.example.engine.FixAreaRuleEngine
import com.example.engine.FixAreaSettings
import com.example.engine.RapidoAutoPriorityFilterEngine
import com.example.model.AppSettings
import com.example.model.AreaGroup
import com.example.model.AreaType
import com.example.model.RideCandidate
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FixAreaRuleEngineTest {
    private val fix = FixAreaSettings(
        enabled = true,
        words = listOf("Charminar", "Tadban"),
        maxPickupKm = 0.7f
    )

    private fun ride(
        destination: String = "Charminar Road",
        pickup: Float = 0.5f
    ) = RideCandidate(
        fare = 25f,
        pickupDistKm = pickup,
        dropDistKm = 15f,
        pickupAddress = "Pickup",
        dropAddress = destination,
        dropArea = destination
    )

    private fun evaluate(
        candidate: RideCandidate,
        noGo: List<AreaGroup> = emptyList()
    ) = FixAreaRuleEngine.evaluate(
        candidate = candidate,
        fix = fix,
        noGoAreas = noGo,
        noGoEnabled = true,
        bundleAllowed = true
    )

    @Test fun matchingDestinationAcceptsWithoutFareOrDropLimit() {
        assertTrue(evaluate(ride()) is DecisionResult.Accept)
    }

    @Test fun maxPickupIsMandatory() {
        assertNull(evaluate(ride(pickup = 0.8f)))
        assertTrue(evaluate(ride(pickup = 0.7f)) is DecisionResult.Accept)
    }

    @Test fun unmatchedDestinationFallsBack() {
        assertNull(evaluate(ride(destination = "Airport")))
        assertNull(evaluate(ride(destination = "CharminarRoad")))
    }

    @Test fun noGoAlwaysVetoes() {
        val blocked = AreaGroup(
            id = "blocked",
            name = "Blocked",
            type = AreaType.NO_GO,
            isEnabled = true,
            keywords = listOf("Charminar")
        )
        assertTrue(evaluate(ride(), listOf(blocked)) is DecisionResult.Reject)
    }

    @Test fun offMeansNormalFiltersRemainActive() {
        assertNull(
            FixAreaRuleEngine.evaluate(
                ride(),
                fix.copy(enabled = false),
                emptyList(),
                true,
                true
            )
        )
    }

    @Test fun fixAreaOverridesUnmatchedGoToInAutoPriority() {
        val result = RapidoAutoPriorityFilterEngine.evaluate(
            candidate = ride(),
            settings = AppSettings(
                isGoToEnabled = true,
                isNoGoEnabled = true,
                isBundleOrderEnabled = true
            ),
            goToAreas = listOf(
                AreaGroup(
                    id = "other",
                    name = "Home",
                    type = AreaType.GO_TO,
                    keywords = listOf("Airport")
                )
            ),
            noGoAreas = emptyList(),
            fixArea = fix
        )
        assertTrue(result is DecisionResult.Accept)
    }
}