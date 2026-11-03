package com.example.engine

import com.example.model.AppSettings
import com.example.model.AreaGroup
import com.example.model.FilterMode
import com.example.model.OlaAdminFilterSettings
import com.example.model.Platform
import com.example.model.RideCandidate
import org.junit.Assert.assertTrue
import org.junit.Test

class OlaAdminAcceptancePolicyTest {
    private val ola = OlaAdminFilterSettings(enabled = true,
        filterMode = FilterMode.FARE_ONLY, minFare = 50f, maxFare = 999f,
        maxPickupKm = 0.6f, maxTripKm = 0.6f)
    private val home = AppSettings(filterMode = FilterMode.FARE_ONLY,
        minFare = 5000f, maxFare = 999f, isPrimaryFilterEnabled = true)
    private fun order(fare: Float = 63f, pickup: Float = 1.2f,
        trip: Float = 1.5f, bundled: Boolean = false,
        platform: Platform = Platform.OLA) = RideCandidate(
            fare = fare, pickupDistKm = pickup, dropDistKm = trip,
            pickupAddress = null, dropAddress = "New Idgah Rd, Ramnaspura",
            dropArea = null, platform = platform, isBundledOrder = bundled
        )
    @Test fun fare63PassesOlaDespiteHomeMinimum5000() {
        assertTrue(OlaAdminAcceptancePolicy.evaluate(order(), ola, home) is DecisionResult.Accept)
    }
    @Test fun fareOnlyDoesNotUseOlaDistanceLimits() {
        assertTrue(OlaAdminAcceptancePolicy.evaluate(
            order(pickup = 10f, trip = 30f), ola, home) is DecisionResult.Accept)
    }
    @Test fun ownFareMissBlocksEvenIfHomeMatches() {
        val result = OlaAdminAcceptancePolicy.evaluate(
            order(fare = 49f), ola, home.copy(minFare = 0f))
        assertTrue(result is DecisionResult.Ignore)
        assertTrue((result as DecisionResult.Ignore).reason.contains("Ola Global Filter"))
    }
    @Test fun distanceOnlyUsesOlaSavedDistances() {
        val settings = ola.copy(filterMode = FilterMode.DISTANCE_ONLY,
            maxPickupKm = 2f, maxTripKm = 2f)
        assertTrue(OlaAdminAcceptancePolicy.evaluate(order(fare = 1f), settings, home)
            is DecisionResult.Accept)
        assertTrue(OlaAdminAcceptancePolicy.evaluate(order(pickup = 2.1f), settings, home)
            is DecisionResult.Ignore)
    }
    @Test fun noGoStillBlocksAndFastestCannotOverride() {
        val blocked = AreaGroup(name = "Blocked", keywords = listOf("Ramnaspura"))
        val result = OlaAdminAcceptancePolicy.evaluate(
            order(), ola, home.copy(isFastestModeEnabled = true),
            noGoAreas = listOf(blocked), noGoEnabled = true)
        assertTrue(result is DecisionResult.Ignore)
        assertTrue((result as DecisionResult.Ignore).reason.contains("No-Go"))
    }
    @Test fun goToRequiredDestinationStillBlocks() {
        val target = AreaGroup(name = "Target", keywords = listOf("Charminar"))
        val result = OlaAdminAcceptancePolicy.evaluate(
            order(), ola, home.copy(isGoToEnabled = true), goToAreas = listOf(target))
        assertTrue(result is DecisionResult.Ignore)
        assertTrue((result as DecisionResult.Ignore).reason.contains("Go-To"))
    }
    @Test fun bundleOffRemainsManual() {
        assertTrue(OlaAdminAcceptancePolicy.evaluate(
            order(bundled = true), ola, home.copy(isBundleOrderEnabled = false))
            is DecisionResult.Ignore)
    }
    @Test fun offAndNonOlaAreNotAuthorized() {
        assertTrue(OlaAdminAcceptancePolicy.evaluate(order(), ola.copy(enabled = false), home)
            is DecisionResult.Ignore)
        assertTrue(OlaAdminAcceptancePolicy.evaluate(
            order(platform = Platform.RAPIDO), ola, home) is DecisionResult.Ignore)
    }
}
