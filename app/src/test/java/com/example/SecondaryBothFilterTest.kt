package com.example

import com.example.engine.AreaRulesEngine
import com.example.engine.DecisionResult
import com.example.model.AppSettings
import com.example.model.FilterMode
import com.example.model.RideCandidate
import org.junit.Assert.assertTrue
import org.junit.Test

class SecondaryBothFilterTest {

    private fun ride(
        fare: Float,
        pickup: Float,
        drop: Float
    ) = RideCandidate(
        fare = fare,
        pickupDistKm = pickup,
        dropDistKm = drop,
        pickupAddress = "Pickup",
        dropAddress = "Normal Destination",
        dropArea = "Normal Destination"
    )

    @Test
    fun homeFails_secondaryPasses_accept() {
        val settings = AppSettings(
            filterMode = FilterMode.BOTH,

            minFare = 100f,
            maxFare = 120f,
            maxPickupDistanceKm = 0.3f,
            maxDropDistanceKm = 2f,

            isSecondaryBothFilterEnabled = true,
            secondaryBothMinFare = 60f,
            secondaryBothMaxPickupDistanceKm = 0.6f,
            secondaryBothMaxDropDistanceKm = 3.5f,

            isGoToEnabled = false,
            isNoGoEnabled = false
        )

        val result = AreaRulesEngine.evaluateRide(
            candidate = ride(
                fare = 75f,
                pickup = 0.5f,
                drop = 3.0f
            ),
            settings = settings
        )

        assertTrue(result is DecisionResult.Accept)
    }

    @Test
    fun homePasses_secondaryFails_accept() {
        val settings = AppSettings(
            filterMode = FilterMode.BOTH,

            minFare = 50f,
            maxFare = 100f,
            maxPickupDistanceKm = 1f,
            maxDropDistanceKm = 4f,

            isSecondaryBothFilterEnabled = true,
            secondaryBothMinFare = 100f,
            secondaryBothMaxPickupDistanceKm = 0.2f,
            secondaryBothMaxDropDistanceKm = 1f,

            isGoToEnabled = false,
            isNoGoEnabled = false
        )

        val result = AreaRulesEngine.evaluateRide(
            candidate = ride(
                fare = 75f,
                pickup = 0.5f,
                drop = 3.0f
            ),
            settings = settings
        )

        assertTrue(result is DecisionResult.Accept)
    }

    @Test
    fun bothFail_noConditionMatch() {
        val settings = AppSettings(
            filterMode = FilterMode.BOTH,

            minFare = 100f,
            maxFare = 120f,
            maxPickupDistanceKm = 0.3f,
            maxDropDistanceKm = 2f,

            isSecondaryBothFilterEnabled = true,
            secondaryBothMinFare = 80f,
            secondaryBothMaxPickupDistanceKm = 0.4f,
            secondaryBothMaxDropDistanceKm = 2.5f,

            isGoToEnabled = false,
            isNoGoEnabled = false
        )

        val result = AreaRulesEngine.evaluateRide(
            candidate = ride(
                fare = 75f,
                pickup = 0.5f,
                drop = 3.0f
            ),
            settings = settings
        )

        assertTrue(result is DecisionResult.Reject)

        val reason =
            (result as DecisionResult.Reject).reason

        assertTrue(
            reason.contains(
                "No condition matched",
                ignoreCase = true
            )
        )
    }
}