package com.example.engine

import com.example.model.OrderStatus
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.*

class OrderPopupLifecycleTest {
    @Test fun duplicatesRemainSuppressedForDecisionLifetimeNotFiveSeconds() {
        val gate = OrderPopupLifecycle(); gate.detect("one")
        assertEquals(OrderPopupLifecycle.Result.PUBLISH,gate.publish("one",OrderStatus.IGNORED,"reason",gate.epoch()))
        repeat(100) { assertEquals(OrderPopupLifecycle.Result.DUPLICATE,
            gate.publish("one",OrderStatus.IGNORED,"reason",gate.epoch())) }
        assertEquals(OrderPopupLifecycle.Result.PUBLISH,gate.publish("one",OrderStatus.ACCEPTED,"confirmed",gate.epoch()))
        assertEquals(OrderPopupLifecycle.Result.DOWNGRADE,gate.publish("one",OrderStatus.UNCONFIRMED,"unknown",gate.epoch()))
    }
    @Test fun aliasesAndLateOldInsertCannotStealNewOfferOwnership() {
        val gate = OrderPopupLifecycle()
        gate.detect("provisionalA"); gate.detect("provisionalB")
        gate.alias("provisionalA","rowA")
        assertEquals(OrderPopupLifecycle.Result.STALE,gate.publish("rowA",OrderStatus.IGNORED,"old",gate.epoch()))
        gate.alias("provisionalB","rowB")
        assertEquals(OrderPopupLifecycle.Result.PUBLISH,gate.publish("rowB",OrderStatus.IGNORED,"new",gate.epoch()))
        gate.detect("anotherB"); gate.alias("anotherB","rowB")
        assertEquals(OrderPopupLifecycle.Result.DUPLICATE,gate.publish("rowB",OrderStatus.IGNORED,"new",gate.epoch()))
    }
    @Test fun pendingAndPrePauseRequestsCannotBecomeFinalPopups() {
        val gate = OrderPopupLifecycle(); gate.detect("one"); val epoch = gate.epoch()
        assertEquals(OrderPopupLifecycle.Result.PENDING,gate.publish("one",OrderStatus.PROCESSING,"internal",epoch))
        gate.invalidate(); gate.detect("two")
        assertEquals(OrderPopupLifecycle.Result.STALE,gate.publish("one",OrderStatus.ACCEPTED,"late",epoch))
    }
    @Test fun boundedInsertRetryKeepsFinalWriterWaitingForItsOwnLogicalInsert() = runBlocking {
        val gate = HistoryInsertGate(); val ticket = gate.begin("row")!!
        val writer = async(start=CoroutineStart.UNDISPATCHED) { gate.await("row") }
        var attempts = 0
        val row = HistoryInsertRetry.run(retryable={true}) {
            attempts++
            assertFalse(writer.isCompleted)
            if (attempts==1) error("synthetic insert failure")
            "persisted"
        }
        assertEquals("persisted",row); assertEquals(2,attempts)
        gate.complete("row",ticket,true); assertTrue(writer.await())
    }
    @Test fun persistentOrNonRetryableFailuresAreBoundedAndCancellationIsNeverRetried() = runBlocking {
        var count=0
        try { HistoryInsertRetry.run(retryable={true}) { count++; error("synthetic") }; fail() }
        catch (_: IllegalStateException) { assertEquals(3,count) }
        count=0
        try { HistoryInsertRetry.run(retryable={false}) { count++; error("invalid") }; fail() }
        catch (_: IllegalStateException) { assertEquals(1,count) }
        count=0
        try { HistoryInsertRetry.run(retryable={true}) { count++; throw CancellationException() }; fail() }
        catch (_: CancellationException) { assertEquals(1,count) }
    }
}
