package com.example.engine

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class HistoryInsertGateTest {
    @Test fun clickPathDoesNotWaitWhileEveryFinalWriterWaitsForItsOwnInsert() = runBlocking {
        val gate = HistoryInsertGate()
        val rapido = gate.begin("RAPIDO")!!
        val uber = gate.begin("UBER")!!
        val writer = async(start = CoroutineStart.UNDISPATCHED) { gate.await("RAPIDO") }
        // begin() already returned; no DB/UI wait was required to dispatch a safe click.
        assertFalse(writer.isCompleted)
        gate.complete("UBER", uber, true)
        assertFalse(writer.isCompleted)
        gate.complete("RAPIDO", rapido, true)
        assertTrue(writer.await())
    }
}
