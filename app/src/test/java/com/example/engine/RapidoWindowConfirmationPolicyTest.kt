package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class RapidoWindowConfirmationPolicyTest {


    @Test fun acceptedPickupScreenCountsAsPositive() {
        assertEquals(RapidoPostAcceptSignal.Result.CONFIRMED, RapidoPostAcceptSignal.classify("Go to Pickup Zone"))
        assertEquals(RapidoPostAcceptSignal.Result.CONFIRMED, RapidoWindowConfirmationPolicy.resolve(listOf(RapidoPostAcceptSignal.Result.UNKNOWN, RapidoPostAcceptSignal.Result.CONFIRMED)))
    }

    @Test fun pendingAndUnknownAreNotAccepted() {
        assertEquals(RapidoPostAcceptSignal.Result.PENDING, RapidoWindowConfirmationPolicy.resolve(listOf(RapidoPostAcceptSignal.Result.UNKNOWN, RapidoPostAcceptSignal.Result.PENDING)))
        assertEquals(RapidoPostAcceptSignal.Result.UNKNOWN, RapidoWindowConfirmationPolicy.resolve(emptyList()))
    }

    @Test fun explicitMissedIsNotAccepted() {
        assertEquals(RapidoPostAcceptSignal.Result.MISSED, RapidoWindowConfirmationPolicy.resolve(listOf(RapidoPostAcceptSignal.Result.MISSED, RapidoPostAcceptSignal.Result.UNKNOWN)))
    }

    @Test fun conflictingLiveWindowsMustNotAutoConfirm() {
        assertEquals(RapidoPostAcceptSignal.Result.UNKNOWN, RapidoWindowConfirmationPolicy.resolve(listOf(RapidoPostAcceptSignal.Result.CONFIRMED, RapidoPostAcceptSignal.Result.MISSED)))
    }
}
