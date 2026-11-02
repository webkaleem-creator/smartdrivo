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

    @Test fun fastestRejectReasonsArePreciseWithoutChangingOutcomes() {
        val over = AreaRulesEngine.evaluateRide(ride(1.2f), settings = fastest)
        assertTrue(over is DecisionResult.Reject)
        assertTrue((over as DecisionResult.Reject).reason.contains("Pickup 1.2km exceeds saved max 0.6km"))
        val missing = AreaRulesEngine.evaluateRide(ride(null), settings = fastest)
        assertTrue((missing as DecisionResult.Reject).reason.contains("Pickup distance unknown"))
        val invalid = AreaRulesEngine.evaluateRide(ride(Float.NaN), settings = fastest)
        assertTrue((invalid as DecisionResult.Reject).reason.contains("Pickup distance invalid"))
        val badLimit = AreaRulesEngine.evaluateRide(ride(0.3f), settings = fastest.copy(maxPickupDistanceKm = 0f))
        assertTrue((badLimit as DecisionResult.Reject).reason.contains("Saved pickup max invalid"))
        assertTrue(AreaRulesEngine.evaluateRide(ride(0.6f), settings = fastest) is DecisionResult.Accept)
    }

    @Test fun bundleOffCannotOverrideRapidoFastestPickupLimit() {
        assertTrue(AreaRulesEngine.evaluateRide(ride(0.1f, bundled = true), settings = fastest) is DecisionResult.Accept)
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
    @Test fun normalAndOtherPlatformsKeepBundleVeto() {
        assertTrue(AreaRulesEngine.evaluateRide(ride(0.1f, true),
            settings = fastest.copy(isFastestModeEnabled = false)) is DecisionResult.Ignore)
        for (platform in listOf(Platform.OLA, Platform.UBER)) {
            assertTrue(AreaRulesEngine.evaluateRide(ride(0.1f, true).copy(platform = platform),
                settings = fastest) is DecisionResult.Ignore)
        }
        assertTrue(AreaRulesEngine.evaluateRide(ride(1f, true), settings = fastest) is DecisionResult.Reject)
    }

    @Test fun fastestDecisionMicrobenchmark() {
        // JVM filter-cost experiment only; no threshold or claim about phone latency.
        val candidate = ride(0.1f, true)
        repeat(2000) { AreaRulesEngine.evaluateRide(candidate, settings = fastest) }
        fun elapsed(rechecks: Int): Long {
            val start = System.nanoTime()
            repeat(10000) {
                repeat(rechecks) {
                    assertTrue(AreaRulesEngine.evaluateRide(candidate, settings = fastest) is DecisionResult.Accept)
                }
            }
            return System.nanoTime() - start
        }
        println("RAPIDO_JVM_FILTER_EXPERIMENT iterations=10000 duplicateNs=${elapsed(2)} singleNs=${elapsed(1)}")
    }
}
