package com.example.data.db

import android.content.Context
import android.util.Log
import com.example.data.db.dao.RideHistoryDao
import com.example.data.db.entity.RideHistoryEntity
import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.model.RideCandidate
import com.example.model.VehicleType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class RideHistoryRepository(
    private val dao: RideHistoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val allHistory: Flow<List<RideHistoryEntity>> = dao.getAllHistory()
    val latestRide: Flow<RideHistoryEntity?> = dao.getLatestRide()
    val acceptedCount: Flow<Int> = dao.getAcceptedCount()

    private val mutex = Mutex()

    /**
     * Step 1: THE MOMENT an order candidate is detected:
     * Immediately inserts a single record with status = PROCESSING into Room.
     * If a duplicate accessibility event arrives for the same booking, updates existing
     * record and increments duplicateEventsIgnored instead of creating duplicate cards.
     */
    suspend fun onOrderDetected(
        candidate: RideCandidate,
        initialReasonCode: String = "DETECTING",
        initialReasonText: String = "Evaluating ride filters..."
    ): RideHistoryEntity = withContext(ioDispatcher) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val detectedTime = if (candidate.detectionTimeMs > 0L) candidate.detectionTimeMs else now
            val fingerprint = generateFingerprint(candidate)

            // Check duplicate by bookingId (if available)
            val byBookingId = if (!candidate.bookingId.isNullOrBlank()) {
                dao.getByBookingId(candidate.bookingId)
            } else null

            // Check duplicate by fingerprint
            val byFingerprint = if (byBookingId == null) {
                dao.getByFingerprint(fingerprint)
            } else null

            val existing = byBookingId ?: byFingerprint

            if (existing != null) {
                // If existing record was detected within 20 seconds, treat as duplicate event
                val isRecent = (now - existing.detectedAt) < 8_000L
                if (isRecent) {
                    val merged = existing.copy(
                        fare = if ((candidate.fare ?: 0f) > 0f) (candidate.fare ?: 0f) else existing.fare,
                        pickupDistanceKm = if ((candidate.pickupDistKm ?: 0f) > 0f) (candidate.pickupDistKm ?: 0f) else existing.pickupDistanceKm,
                        dropDistanceKm = if ((candidate.dropDistKm ?: 0f) > 0f) (candidate.dropDistKm ?: 0f) else existing.dropDistanceKm,
                        pickupAddress = if (!(candidate.pickupAddress.isNullOrBlank())) (candidate.pickupAddress ?: "") else existing.pickupAddress,
                        dropAddress = if (!(candidate.dropAddress.isNullOrBlank())) (candidate.dropAddress ?: "") else existing.dropAddress,
                        dropArea = if (!(candidate.dropArea.isNullOrBlank())) (candidate.dropArea ?: "") else existing.dropArea,
                        duplicateEventsIgnored = existing.duplicateEventsIgnored + 1
                    )
                    dao.update(merged)
                    Log.d("RideHistoryRepo", "Merged duplicate event into active record: ${merged.id} (duplicates: ${merged.duplicateEventsIgnored})")
                    return@withContext merged
                }
            }

            // Create new record
            val recordId = if (!candidate.bookingId.isNullOrBlank()) {
                candidate.bookingId
            } else {
                "ride_${fingerprint}"
            }

            val (dateStr, timeStr) = formatDateTime(detectedTime)
            val insertLatency = now - detectedTime

            val newEntity = RideHistoryEntity(
                id = recordId,
                bookingId = candidate.bookingId ?: "",
                bookingFingerprint = fingerprint,
                platform = candidate.platform.name,
                vehicleType = candidate.vehicleType.name,
                detectedAt = detectedTime,
                historyInsertedAt = now,
                decisionAt = 0L,
                actionAttemptAt = 0L,
                actionCompletedAt = 0L,
                fare = candidate.fare ?: 0f,
                pickupDistanceKm = candidate.pickupDistKm ?: 0f,
                dropDistanceKm = candidate.dropDistKm ?: 0f,
                pickupAddress = candidate.pickupAddress ?: "",
                dropAddress = candidate.dropAddress ?: "",
                dropArea = candidate.dropArea ?: "",
                status = OrderStatus.PROCESSING.name,
                decisionReasonCode = initialReasonCode,
                decisionReasonText = initialReasonText,
                matchedGoToGroup = "",
                matchedNoGoGroup = "",
                actionSucceeded = false,
                historyInsertLatencyMs = insertLatency.coerceAtLeast(0L),
                decisionLatencyMs = 0L,
                actionLatencyMs = 0L,
                totalProcessingMs = 0L,
                duplicateEventsIgnored = 0,
                baseFare = candidate.baseFare ?: (candidate.fare ?: 0f),
                tipAmount = candidate.tipAmount ?: 0f,
                timesClicked = 0,
                dateStr = dateStr,
                timeStr = timeStr
            )

            dao.insert(newEntity)
            Log.i("RideHistoryRepo", "Inserted detected ride: ${newEntity.id} [PROCESSING] platform=${newEntity.platform}, fare=₹${newEntity.fare}")
            newEntity
        }
    }

    /**
     * Step 2: Evaluation completed.
     * Updates the SAME record with decision result (ACCEPTED, IGNORED, REJECTED, SKIPPED).
     */
    suspend fun onOrderDecision(
        id: String,
        status: OrderStatus,
        reasonCode: String,
        reasonText: String,
        matchedGoTo: String = "",
        matchedNoGo: String = ""
    ): RideHistoryEntity? = withContext(ioDispatcher) {
        mutex.withLock {
            val existing = dao.getById(id) ?: dao.getByBookingId(id) ?: dao.getByFingerprint(id) ?: return@withContext null
            val now = System.currentTimeMillis()
            val decisionLatency = (now - existing.detectedAt).coerceAtLeast(0L)
            val totalProcessing = if (status != OrderStatus.ACCEPTED) decisionLatency else existing.totalProcessingMs

            val updated = existing.copy(
                status = status.name,
                decisionAt = now,
                decisionLatencyMs = decisionLatency,
                decisionReasonCode = reasonCode,
                decisionReasonText = reasonText,
                matchedGoToGroup = matchedGoTo,
                matchedNoGoGroup = matchedNoGo,
                totalProcessingMs = totalProcessing
            )
            dao.update(updated)
            Log.i("RideHistoryRepo", "Updated decision for ${updated.id}: ${updated.status} - $reasonCode ($reasonText)")
            updated
        }
    }

    /**
     * Step 3: Performing accept/reject action attempt.
     */
    suspend fun onOrderActionAttempt(id: String): RideHistoryEntity? = withContext(ioDispatcher) {
        mutex.withLock {
            val existing = dao.getById(id) ?: dao.getByBookingId(id) ?: dao.getByFingerprint(id) ?: return@withContext null
            val now = System.currentTimeMillis()
            val updated = existing.copy(actionAttemptAt = now)
            dao.update(updated)
            updated
        }
    }

    /**
     * Step 4: Action completed (e.g. click performed or failed).
     * Updates SAME record with final outcome, click times, and real measured latencies.
     */
    suspend fun onOrderActionCompleted(
        id: String,
        status: OrderStatus,
        reasonCode: String,
        reasonText: String,
        actionSucceeded: Boolean,
        timesClicked: Int = 1
    ): RideHistoryEntity? = withContext(ioDispatcher) {
        mutex.withLock {
            val existing = dao.getById(id) ?: dao.getByBookingId(id) ?: dao.getByFingerprint(id) ?: return@withContext null
            val now = System.currentTimeMillis()
            val attemptTime = if (existing.actionAttemptAt > 0L) existing.actionAttemptAt else if (existing.decisionAt > 0L) existing.decisionAt else existing.detectedAt
            val actionLatency = if (attemptTime > 0L) (now - attemptTime).coerceAtLeast(0L) else 0L
            val totalProcessing = (now - existing.detectedAt).coerceAtLeast(0L)

            val updated = existing.copy(
                status = status.name,
                actionCompletedAt = now,
                actionSucceeded = actionSucceeded,
                actionLatencyMs = actionLatency,
                totalProcessingMs = totalProcessing,
                decisionReasonCode = reasonCode,
                decisionReasonText = reasonText,
                timesClicked = timesClicked
            )
            dao.update(updated)
            Log.i("RideHistoryRepo", "Action completed for ${updated.id}: ${updated.status} total=${totalProcessing}ms, action=${actionLatency}ms")
            updated
        }
    }

    suspend fun getById(id: String): RideHistoryEntity? = withContext(ioDispatcher) {
        dao.getById(id) ?: dao.getByBookingId(id) ?: dao.getByFingerprint(id)
    }

    suspend fun clearHistory() = withContext(ioDispatcher) {
        dao.clearAll()
    }

    suspend fun getAllHistoryDirect(): List<RideHistoryEntity> = withContext(ioDispatcher) {
        dao.getAllHistoryList()
    }

    fun generateFingerprint(candidate: RideCandidate): String {
        if (!candidate.bookingId.isNullOrBlank()) {
            return "${candidate.platform}_${candidate.bookingId}"
        }
        val fareInt = (candidate.fare ?: 0f).toInt()
        val pickup10 = ((candidate.pickupDistKm ?: 0f) * 10).toInt()
        val drop10 = ((candidate.dropDistKm ?: 0f) * 10).toInt()
        val pickupClean = (candidate.pickupAddress ?: "").take(12).replace(Regex("[^A-Za-z0-9]"), "")
        val dropClean = (candidate.dropAddress ?: "").take(12).replace(Regex("[^A-Za-z0-9]"), "")
        return "${candidate.platform}_${fareInt}_${pickup10}_${drop10}_${pickupClean}_${dropClean}"
    }

    private fun formatDateTime(timestamp: Long): Pair<String, String> {
        val date = Date(if (timestamp > 0L) timestamp else System.currentTimeMillis())
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        return Pair(sdfDate.format(date), sdfTime.format(date))
    }

    companion object {
        @Volatile
        private var INSTANCE: RideHistoryRepository? = null

        fun getInstance(context: Context): RideHistoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RideHistoryRepository(
                    AppDatabase.getInstance(context).rideHistoryDao()
                ).also { INSTANCE = it }
            }
        }

        fun createForTest(dao: RideHistoryDao, dispatcher: CoroutineDispatcher = Dispatchers.Unconfined): RideHistoryRepository {
            return RideHistoryRepository(dao, dispatcher)
        }
    }
}
