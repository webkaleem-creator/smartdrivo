package com.example.engine

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*

class RapidoEventRecoveryTest {
    @Test fun emptyOrLatePopupHasBoundedRecoveryBeyondProcessingWatchdog() {
        val gate = RapidoEventRecovery()
        val ticket = gate.signal()!!
        val waits = mutableListOf<Long>()
        repeat(7) { waits += gate.nextDelay(ticket, true, true)!! }
        assertEquals(1595L, waits.sum())
        assertNull(gate.nextDelay(ticket, true, true))
        assertNotNull(gate.signal())
    }
    @Test fun successfulPassHasNoArtificialDelayAndPendingEventIsNotLost() {
        val gate = RapidoEventRecovery()
        val ticket = gate.signal()!!
        assertNull(gate.signal())
        assertEquals(0L, gate.nextDelay(ticket, true, false))
        assertNull(gate.nextDelay(ticket, true, false))
    }
    @Test fun cancelledOrFailedOldReaderCannotReleaseNewReader() {
        val gate = RapidoEventRecovery()
        val old = gate.signal()!!
        gate.cancel()
        val fresh = gate.signal()!!
        gate.finish(old)
        assertFalse(gate.current(old))
        assertTrue(gate.current(fresh))
        assertNull(gate.nextDelay(fresh, false, true))
    }
    @Test fun concurrentEventsCoalesceIntoOneReader() {
        val gate = RapidoEventRecovery()
        val pool = Executors.newFixedThreadPool(8)
        val ready = CountDownLatch(8); val start = CountDownLatch(1)
        try {
            val readers = (1..8).map { pool.submit<RapidoEventRecovery.Ticket?> {
                ready.countDown(); check(start.await(5, TimeUnit.SECONDS)); gate.signal()
            } }
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown()
            assertEquals(1, readers.count { it.get(5, TimeUnit.SECONDS) != null })
        } finally { start.countDown(); pool.shutdownNow() }
    }
}
