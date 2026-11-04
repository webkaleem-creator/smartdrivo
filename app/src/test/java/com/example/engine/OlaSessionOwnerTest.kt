package com.example.engine

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OlaSessionOwnerTest {
    @org.junit.Test fun accountChangeInvalidatesActionAndVerificationOwnership() {
        val owner = OlaSessionOwner()
        val session = owner.begin("ola_offer_one", "first-user")!!
        org.junit.Assert.assertTrue(owner.isCurrent(session, "first-user"))
        org.junit.Assert.assertFalse(owner.isCurrent(session, "next-user"))
        owner.cancel()
        val next = owner.begin("ola_offer_two", "next-user")!!
        org.junit.Assert.assertFalse(owner.finish(session))
        org.junit.Assert.assertTrue(owner.isCurrent(next, "next-user"))
    }
    @Test fun concurrentEventsHaveExactlyOneOwner() {
        val owner = OlaSessionOwner()
        val pool = Executors.newFixedThreadPool(8)
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        try {
            val tasks = (1..8).map { i -> pool.submit<OlaSessionOwner.Session?> {
                ready.countDown()
                check(start.await(5, TimeUnit.SECONDS))
                owner.begin("offer_$i")
            } }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            val winners = tasks.mapNotNull { it.get(5, TimeUnit.SECONDS) }
            assertEquals(1, winners.size)
            assertTrue(owner.isCurrent(winners.single()))
        } finally { start.countDown(); pool.shutdownNow() }
    }

    @Test fun cancelledWatcherCannotReleaseReplacementOrChangeItsHistoryId() {
        val owner = OlaSessionOwner()
        val old = owner.begin("old_history")!!
        owner.cancel()
        val replacement = owner.begin("new_history")!!
        assertFalse(owner.isCurrent(old))
        assertFalse(owner.finish(old))
        assertTrue(owner.isCurrent(replacement))
        assertEquals("new_history", replacement.recordId)
        assertNull(owner.begin("duplicate"))
    }

    @Test fun handoffReleasesExactlyOnceAndKeepsCapturedRecordId() {
        val owner = OlaSessionOwner()
        val old = owner.begin("first")!!
        assertTrue(owner.finish(old))
        val next = owner.begin("second")!!
        assertFalse(owner.finish(old))
        assertEquals("first", old.recordId)
        assertTrue(owner.isCurrent(next))
    }
}
