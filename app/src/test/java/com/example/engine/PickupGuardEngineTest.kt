package com.example.engine

import com.example.model.AreaGroup
import com.example.model.AreaType
import com.example.model.PickupGuardSettings
import com.example.model.Platform
import com.example.model.RideCandidate
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupGuardEngineTest {
    private val config = PickupGuardSettings(enabled = true, maxPickupKm = 0.6f)
    private val noGo = listOf(AreaGroup(name = "Avoid", type = AreaType.NO_GO, keywords = listOf("Shivaji Nagar")))
    private fun offer(km: Float?, dest: String? = "Safe Street", platform: Platform = Platform.OLA) =
        RideCandidate(fare = 100f, pickupDistKm = km, dropDistKm = 2f, pickupAddress = null,
            dropAddress = dest, dropArea = null, platform = platform)
    private fun eval(ride: RideCandidate) = PickupGuardEngine.evaluate(ride, config, noGo, true)

    @Test fun zeroAndBoundaryStayManual() {
        for (km in listOf(0f, 0.3f, 0.6f))
            assertTrue(eval(offer(km)) is PickupGuardEngine.Result.ManualChoice)
    }
    @Test fun pickupOutsideRejects() {
        assertTrue(eval(offer(0.7f)) is PickupGuardEngine.Result.AutoReject)
    }
    @Test fun outOfRangeDoesNotDependOnDestination() {
        assertTrue(eval(offer(0.7f, null)) is PickupGuardEngine.Result.AutoReject)
    }
    @Test fun noGoWinsEvenInsideLimit() {
        assertTrue(eval(offer(0.2f, "Shivaji Nagar")) is PickupGuardEngine.Result.AutoReject)
    }
    @Test fun unknownValuesFailClosed() {
        assertTrue(eval(offer(null)) is PickupGuardEngine.Result.IgnoreUnreadable)
        assertTrue(eval(offer(Float.NaN)) is PickupGuardEngine.Result.IgnoreUnreadable)
        assertTrue(eval(offer(0.4f, null)) is PickupGuardEngine.Result.IgnoreUnreadable)
    }
    @Test fun sharedAcrossPlatformsWithoutOtherFilterDependencies() {
        for (platform in Platform.values())
            assertTrue(eval(offer(0.3f, platform = platform).copy(fare = 1f, isBundledOrder = true))
                is PickupGuardEngine.Result.ManualChoice)
    }
    @Test fun groupNameAloneIsNotANoGoKeyword() {
        assertTrue(eval(offer(0.4f, "Avoid")) is PickupGuardEngine.Result.ManualChoice)
    }
}
