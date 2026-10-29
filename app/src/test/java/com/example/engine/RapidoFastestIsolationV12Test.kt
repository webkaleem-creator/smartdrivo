package com.example.engine

import com.example.model.AppSettings
import com.example.model.RideCandidate
import com.example.model.Platform
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidoFastestIsolationV12Test {
    private fun ride(pickup: Float?, bundled: Boolean = false) = RideCandidate(
        fare = 120f, pickupDistKm = pickup, dropDistKm = 6f,
        pickupAddress = "Address unavailable", dropAddress = "Blocked Destination",
        dropArea = "Blocked Destination", platform = Platform.RAPIDO,
        isBundledOrder = bundled, tipAmount = 50f
    )

    private val fastest = AppSettings(
        isFastestModeEnabled = true,
        maxPickupDistanceKm = 0.6f,
        isGoToEnabled = true, isNoGoEnabled = true,
        isPrimaryFilterEnabled = false,
        isSecondaryBothFilterEnabled = false,
        isTertiaryBothFilterEnabled = false,
        isPlusAmountFilterEnabled = true,
        minFare = 9999f, maxDropDistanceKm = 1f
    )

    @Test fun pickupZeroAndUnderMaximumPassDespiteOtherSavedFilters() {
        assertTrue(AreaRulesEngine.evaluateRide(ride(0f), settings = fastest) is DecisionResult.Accept)
        assertTrue(AreaRulesEngine.evaluateRide(ride(0.6f), settings = fastest) is DecisionResult.Accept)
    }

    @Test fun overMaximumAndUnknownPickupFailClosed() {
        listOf(null, 0.61f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { pickup ->
            assertTrue("Must fail for pickup=$pickup",
                AreaRulesEngine.evaluateRide(ride(pickup), settings = fastest) is DecisionResult.Reject)
        }
        assertTrue(AreaRulesEngine.evaluateRide(ride(0.1f), settings = fastest.copy(maxPickupDistanceKm = 0f)) is DecisionResult.Reject)
    }

    @Test fun bundleOffRemainsManualSafetyGate() {
        assertTrue(AreaRulesEngine.evaluateRide(ride(0.1f, bundled = true), settings = fastest) is DecisionResult.Ignore)
    }

    @Test fun autoPriorityUsesFastestDecisionNotFixAreaOrAmount() {
        assertTrue(RapidoAutoPriorityFilterEngine.evaluate(
            ride(0.1f), settings = fastest,
            fixArea = FixAreaSettings(enabled = true, words = listOf("Not Matching"))
        ) is DecisionResult.Accept)
        assertTrue(RapidoAutoPriorityFilterEngine.evaluate(
            ride(1.0f), settings = fastest
        ) is DecisionResult.Reject)
    }
}
