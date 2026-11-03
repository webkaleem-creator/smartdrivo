package com.example.engine

import com.example.model.FilterMode
import com.example.model.OlaAdminFilterSettings
import com.example.model.Platform
import com.example.model.RideCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OlaAdminFilterEngineTest {
    private fun offer(fare: Float? = 100f, pickup: Float? = 0.5f,
        trip: Float? = 5f, platform: Platform = Platform.OLA) = RideCandidate(
        fare = fare, pickupDistKm = pickup, dropDistKm = trip,
        pickupAddress = null, dropAddress = "Attapur, Hyderabad",
        dropArea = "Attapur", platform = platform
    )
    private val on = OlaAdminFilterSettings(enabled = true,
        minFare = 60f, maxFare = 200f, maxPickupKm = 0.6f, maxTripKm = 6f)

    @Test fun offDoesNotAuthorizeNewOlaAction() {
        assertTrue(OlaAdminFilterEngine.evaluate(offer(), on.copy(enabled = false))
            is OlaAdminFilterEngine.Result.Ignore)
    }
    @Test fun otherPlatformsStayIndependent() {
        assertTrue(OlaAdminFilterEngine.evaluate(offer(platform = Platform.RAPIDO), on)
            is OlaAdminFilterEngine.Result.Ignore)
    }
    @Test fun bothRequiresFarePickupAndTrip() {
        assertEquals(OlaAdminFilterEngine.Result.Match, OlaAdminFilterEngine.evaluate(offer(), on))
        assertTrue(OlaAdminFilterEngine.evaluate(offer(fare = 45f), on)
            is OlaAdminFilterEngine.Result.Ignore)
        assertTrue(OlaAdminFilterEngine.evaluate(offer(pickup = 0.7f), on)
            is OlaAdminFilterEngine.Result.Ignore)
        assertTrue(OlaAdminFilterEngine.evaluate(offer(trip = 6.1f), on)
            is OlaAdminFilterEngine.Result.Ignore)
    }
    @Test fun zeroPickupAndLimitsIncluded() {
        assertEquals(OlaAdminFilterEngine.Result.Match,
            OlaAdminFilterEngine.evaluate(offer(fare = 60f, pickup = 0f, trip = 6f), on))
    }
    @Test fun fareAndDistanceOnlyAreIndependentModes() {
        assertEquals(OlaAdminFilterEngine.Result.Match,
            OlaAdminFilterEngine.evaluate(offer(pickup = 10f, trip = 30f),
                on.copy(filterMode = FilterMode.FARE_ONLY)))
        assertEquals(OlaAdminFilterEngine.Result.Match,
            OlaAdminFilterEngine.evaluate(offer(fare = 1f),
                on.copy(filterMode = FilterMode.DISTANCE_ONLY)))
    }
    @Test fun invalidNumbersAndRangesFailClosed() {
        assertTrue(OlaAdminFilterEngine.evaluate(offer(pickup = Float.NaN), on)
            is OlaAdminFilterEngine.Result.Ignore)
        assertTrue(OlaAdminFilterEngine.evaluate(offer(trip = null), on)
            is OlaAdminFilterEngine.Result.Ignore)
        assertTrue(!on.copy(maxFare = 20f).valid())
        assertTrue(!on.copy(maxTripKm = Float.POSITIVE_INFINITY).valid())
    }
}
