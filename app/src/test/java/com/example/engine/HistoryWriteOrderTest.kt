package com.example.engine

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class HistoryWriteOrderTest {
    @Test fun reversedCoroutineSchedulingPreservesCallbackOrderAndPlatformIsolation() = runBlocking {
        val order = HistoryWriteOrder()
        val first = order.enqueue("OLA")
        val second = order.enqueue("OLA")
        val other = order.enqueue("RAPIDO")
        val writes = mutableListOf<String>()
        val late = async(start = CoroutineStart.UNDISPATCHED) {
            second.awaitPrevious(); writes += "new"; order.complete("OLA", second)
        }
        assertFalse(late.isCompleted)
        other.awaitPrevious(); writes += "rapido"; order.complete("RAPIDO", other)
        first.awaitPrevious(); writes += "old"; order.complete("OLA", first)
        withTimeout(1000) { late.await() }
        assertEquals(listOf("rapido", "old", "new"), writes)
    }

    @Test fun cancelledMiddleWriterCannotReleaseOrBypassItsPredecessor() = runBlocking {
        val order = HistoryWriteOrder()
        val first = order.enqueue("row")
        val cancelled = order.enqueue("row")
        order.complete("row", cancelled)
        val last = order.enqueue("row")
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { last.awaitPrevious() }
        assertFalse(waiting.isCompleted)
        order.complete("row", first)
        withTimeout(1000) { waiting.await() }
        order.complete("row", last)
        order.enqueue("row").awaitPrevious()
    }
}
