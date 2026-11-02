package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.RideHistoryRepository
import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.model.RideCandidate
import com.example.model.VehicleType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OlaHistoryOwnershipTest {
    private lateinit var db: AppDatabase
    private lateinit var history: RideHistoryRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        history = RideHistoryRepository(db.rideHistoryDao())
    }

    @After fun tearDown() { db.close() }

    private fun offer() = RideCandidate(120f, 1f, 5f, "Pickup", "Destination", "Area",
        Platform.OLA, VehicleType.AUTO, bookingId = "CRN12345")

    @Test fun delayedOldCompletionCannotFinalizeSameBookingReplacement() = runBlocking {
        val first = history.onOrderDetected(offer(), olaRecordId = "ola_offer_first")
        val next = history.onOrderDetected(offer().copy(fare = 160f), olaRecordId = "ola_offer_next")
        history.onOrderActionCompleted(first.id, OrderStatus.FAILED, "REPLACED", "Replaced", false)
        assertEquals(OrderStatus.PROCESSING.name, history.getById(next.id)!!.status)
        history.onOrderActionCompleted(next.id, OrderStatus.ACCEPTED, "MATCHED", "Accepted", true)
        history.onOrderActionCompleted(first.id, OrderStatus.FAILED, "LATE", "Late completion", false)
        assertEquals(OrderStatus.ACCEPTED.name, history.getById(next.id)!!.status)
        assertEquals("CRN12345", next.bookingId)
        assertEquals(2, history.getAllHistoryDirect().size)
    }

    @Test fun lateDetectionForOldSessionCannotMergeIntoReplacement() = runBlocking {
        val next = history.onOrderDetected(offer().copy(fare = 160f), olaRecordId = "ola_offer_next")
        history.onOrderDetected(offer(), olaRecordId = "ola_offer_first")
        assertEquals(160f, history.getById(next.id)!!.fare, 0.01f)
        assertEquals(2, history.getAllHistoryDirect().size)
    }
}
