package com.example.engine

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class OlaHistoryInsertGateTest {
    @Test fun decisionWaitsForInsertWithoutFourHundredMillisecondRace() = runBlocking {
        val gate = OlaHistoryInsertGate()
        val ticket = gate.begin("offer_A")!!
        val decision = async(start = CoroutineStart.UNDISPATCHED) { gate.await("offer_A") }
        assertFalse(decision.isCompleted)
        assertNull(gate.begin("offer_A"))
        gate.complete("offer_A", ticket, true)
        assertTrue(decision.await())
        assertTrue(gate.await("offer_A"))
    }
    @Test fun twoSequentialOffersCannotReleaseEachOthersDecisions() = runBlocking {
        val gate = OlaHistoryInsertGate()
        val a = gate.begin("A")!!; val b = gate.begin("B")!!
        val decisionB = async(start = CoroutineStart.UNDISPATCHED) { gate.await("B") }
        gate.complete("A", a, true)
        assertFalse(decisionB.isCompleted)
        gate.complete("B", b, true)
        assertTrue(decisionB.await())
    }
    @Test fun failureAndCancellationNeverPretendPersistenceSucceeded() = runBlocking {
        val gate = OlaHistoryInsertGate()
        val failed = gate.begin("A")!!
        gate.complete("A", failed, false)
        assertFalse(gate.await("A"))
        val retried = gate.begin("A")!!
        val cancelled = async(start = CoroutineStart.UNDISPATCHED) { gate.await("A") }
        cancelled.cancelAndJoin()
        assertFalse(retried.isCancelled)
        gate.complete("A", retried, true)
        assertTrue(gate.await("A"))
    }
}
