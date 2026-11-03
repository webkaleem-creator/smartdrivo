package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.*
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InstantHistoryRegressionTest {
    private lateinit var db: AppDatabase
    private lateinit var history: RideHistoryRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java).allowMainThreadQueries().build()
        history = RideHistoryRepository(db.rideHistoryDao())
    }
    @After fun close() { db.close() }
    private fun offer(platform: Platform, booking: String) = RideCandidate(45f, 0.9f, 2.2f,
        "Address unavailable", "Destination", "Area", platform, VehicleType.AUTO, booking)

    @Test fun pendingThenFinalFlowUsesOneRowForEveryPlatform() = runBlocking {
        for (platform in Platform.entries) {
            val candidate = offer(platform, "BOOK123")
            val id = history.onOrderDetected(candidate).id
            val pending = withTimeout(5000) { history.allHistory.first { rows -> rows.any { it.id == id } } }
            assertEquals(OrderStatus.PROCESSING.name, pending.first { it.id == id }.status)
            repeat(20) { assertEquals(id, history.onOrderDetected(candidate).id) }
            history.onOrderActionCompleted(id, OrderStatus.ACCEPTED, "CONFIRMED", "Confirmed UI", true)
            repeat(20) { history.onOrderDetected(candidate.copy(detectionTimeMs = candidate.detectionTimeMs - 20_000)) }
            assertEquals(OrderStatus.ACCEPTED.name, history.getById(id)!!.status)
        }
        assertEquals(Platform.entries.size, history.getAllHistoryDirect().size)
    }
    @Test fun oldFinalRecordCannotBeResetAndDifferentBookingsStaySeparate() = runBlocking {
        for (platform in Platform.entries) {
            val candidate = offer(platform, "OLD123").copy(detectionTimeMs = System.currentTimeMillis() - 20_000)
            val old = history.onOrderDetected(candidate)
            history.onOrderDecision(old.id, OrderStatus.IGNORED, "FILTER", "Ignored")
            val terminal = history.getById(old.id)
            history.onOrderDetected(candidate.copy(fare = 80f))
            assertEquals(terminal, history.getById(old.id))
            assertNotEquals(old.id, history.onOrderDetected(offer(platform, "NEW456")).id)
        }
        assertEquals(Platform.entries.size * 2, history.getAllHistoryDirect().size)
    }
    @Test fun legacyBookingRowCanBeFinalizedUsingNewProvisionalKey() = runBlocking {
        val candidate = offer(Platform.RAPIDO, "LEGACY123")
        val newRow = history.onOrderDetected(candidate)
        val legacy = newRow.copy(id = "LEGACY123")
        db.rideHistoryDao().deleteById(newRow.id)
        db.rideHistoryDao().insert(legacy)
        val provisional = HistoryRecordIdentity.id(candidate)
        assertEquals(legacy.id, history.onOrderDetected(candidate).id)
        val finalized = history.onOrderActionCompleted(provisional, OrderStatus.UNCONFIRMED, "UNKNOWN", "Check app", false)
        assertEquals(legacy.id, finalized!!.id)
        assertEquals(1, history.getAllHistoryDirect().size)
    }
    @Test fun recentTerminalRecordCanStillEnrichMissingDropWithoutChangingStatus() = runBlocking {
        for (platform in Platform.entries) {
            val partial = offer(platform, "ENRICH123").copy(dropAddress = "")
            val row = history.onOrderDetected(partial)
            history.onOrderActionCompleted(row.id, OrderStatus.ACCEPTED, "CONFIRMED", "Confirmed UI", true)
            val enriched = history.onOrderDetected(partial.copy(dropAddress = "Destination"))
            assertEquals(row.id, enriched.id)
            assertEquals("Destination", enriched.dropAddress)
            assertEquals(OrderStatus.ACCEPTED.name, enriched.status)
        }
    }

    @Test fun provisionalCallbackResolvesExactDedupedRowBeyondThreeSeconds() = runBlocking {
        val first = offer(Platform.RAPIDO, "").copy(bookingId = null,
            detectionTimeMs = System.currentTimeMillis() - 5_000L)
        val row = history.onOrderDetected(first)
        val second = first.copy(detectionTimeMs = System.currentTimeMillis())
        val provisional = HistoryRecordIdentity.id(second)
        assertEquals(row.id, history.onOrderDetected(second).id)
        val completed = history.onOrderActionCompleted(provisional, OrderStatus.UNCONFIRMED,
            "UNKNOWN", "Check app", false)
        assertEquals(row.id, completed!!.id)
        assertEquals(OrderStatus.UNCONFIRMED.name, completed.status)
        assertEquals(1, history.getAllHistoryDirect().size)
    }

    @Test fun controlledFlowVisibilityP50P95Experiment() = runBlocking {
        // Same real Room flow, old/new UI predicates; deliberately hold finalization 30ms.
        // These timings are a JVM experiment, not phone render or event-to-click timings.
        fun percentile(values: List<Double>, p: Double) = values.sorted()[kotlin.math.ceil(values.size*p).toInt()-1]
        for (platform in Platform.entries) {
            val before = mutableListOf<Double>(); val after = mutableListOf<Double>()
            repeat(30) { sample ->
                val candidate = offer(platform, "PERF${platform.name}$sample")
                val id = HistoryRecordIdentity.id(candidate)
                val start = System.nanoTime()
                val baseline = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(5000) { history.allHistory.first { rows -> rows.any {
                        it.id == id && (platform == Platform.OLA || it.status != OrderStatus.PROCESSING.name)
                    } } }
                    (System.nanoTime()-start)/1e6
                }
                val optimized = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(5000) { history.allHistory.first { rows -> rows.any { it.id == id } } }
                    (System.nanoTime()-start)/1e6
                }
                history.onOrderDetected(candidate)
                after += optimized.await()
                delay(30)
                history.onOrderDecision(id, OrderStatus.IGNORED, "PERF", "Test only")
                before += baseline.await()
            }
            println("HISTORY_JVM platform=$platform n=30 beforeP50=${percentile(before,0.5)} beforeP95=${percentile(before,0.95)} afterP50=${percentile(after,0.5)} afterP95=${percentile(after,0.95)} artificialFinalizationDelayMs=30")
        }
    }
}
