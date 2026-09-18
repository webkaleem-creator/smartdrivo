package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.OrderHistoryItem
import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.model.VehicleType

@Entity(
    tableName = "ride_history",
    indices = [
        Index(value = ["bookingFingerprint"]),
        Index(value = ["bookingId"]),
        Index(value = ["detectedAt"]),
        Index(value = ["status"])
    ]
)
data class RideHistoryEntity(
    @PrimaryKey
    val id: String,
    val bookingId: String = "",
    val bookingFingerprint: String = "",
    val platform: String = "RAPIDO", // RAPIDO, UBER, OLA
    val vehicleType: String = "AUTO", // AUTO, BIKE, CAR

    val detectedAt: Long = System.currentTimeMillis(),
    val historyInsertedAt: Long = System.currentTimeMillis(),
    val decisionAt: Long = 0L,
    val actionAttemptAt: Long = 0L,
    val actionCompletedAt: Long = 0L,

    val fare: Float = 0f,
    val pickupDistanceKm: Float = 0f,
    val dropDistanceKm: Float = 0f,

    val pickupAddress: String = "",
    val dropAddress: String = "",
    val dropArea: String = "",

    val status: String = OrderStatus.PROCESSING.name,

    val decisionReasonCode: String = "",
    val decisionReasonText: String = "",

    val matchedGoToGroup: String = "",
    val matchedNoGoGroup: String = "",

    val actionSucceeded: Boolean = false,

    val historyInsertLatencyMs: Long = 0L,
    val decisionLatencyMs: Long = 0L,
    val actionLatencyMs: Long = 0L,
    val totalProcessingMs: Long = 0L,

    val duplicateEventsIgnored: Int = 0,

    val baseFare: Float = 0f,
    val tipAmount: Float = 0f,
    val timesClicked: Int = 0,
    val dateStr: String = "",
    val timeStr: String = ""
) {
    val orderStatus: OrderStatus
        get() = try {
            OrderStatus.valueOf(status)
        } catch (_: Exception) {
            OrderStatus.PROCESSING
        }

    fun toOrderHistoryItem(): OrderHistoryItem {
        val parsedStatus = try {
            OrderStatus.valueOf(status)
        } catch (_: Exception) {
            OrderStatus.PROCESSING
        }
        val parsedPlatform = try {
            Platform.valueOf(platform)
        } catch (_: Exception) {
            Platform.RAPIDO
        }
        val parsedVehicle = try {
            VehicleType.valueOf(vehicleType)
        } catch (_: Exception) {
            VehicleType.AUTO
        }
        return OrderHistoryItem(
            id = id,
            timestamp = detectedAt,
            dateStr = dateStr,
            timeStr = timeStr,
            status = parsedStatus,
            platform = parsedPlatform,
            vehicleType = parsedVehicle,
            pickupDistKm = pickupDistanceKm,
            dropDistKm = dropDistanceKm,
            pickupAddress = pickupAddress,
            dropAddress = dropAddress,
            dropArea = dropArea,
            amount = fare,
            bookingId = bookingId,
            bookingFingerprint = bookingFingerprint,
            detectedAt = detectedAt,
            historyInsertedAt = historyInsertedAt,
            decisionAt = decisionAt,
            actionAttemptAt = actionAttemptAt,
            actionCompletedAt = actionCompletedAt,
            decisionReasonCode = decisionReasonCode,
            decisionReasonText = decisionReasonText,
            matchedGoToGroup = matchedGoToGroup,
            matchedNoGoGroup = matchedNoGoGroup,
            actionSucceeded = actionSucceeded,
            historyInsertLatencyMs = historyInsertLatencyMs,
            decisionLatencyMs = decisionLatencyMs,
            actionLatencyMs = actionLatencyMs,
            totalProcessingMs = totalProcessingMs,
            duplicateEventsIgnored = duplicateEventsIgnored,
            detectionTimeMs = detectedAt,
            clickTimeMs = actionCompletedAt,
            baseFare = baseFare,
            tipAmount = tipAmount,
            timesClicked = timesClicked,
            reason = decisionReasonText.ifEmpty { decisionReasonCode }
        )
    }

    companion object {
        fun fromOrderHistoryItem(item: OrderHistoryItem): RideHistoryEntity {
            return RideHistoryEntity(
                id = item.id,
                bookingId = item.bookingId,
                bookingFingerprint = item.bookingFingerprint,
                platform = item.platform.name,
                vehicleType = item.vehicleType.name,
                detectedAt = if (item.detectedAt > 0L) item.detectedAt else item.timestamp,
                historyInsertedAt = if (item.historyInsertedAt > 0L) item.historyInsertedAt else item.timestamp,
                decisionAt = item.decisionAt,
                actionAttemptAt = item.actionAttemptAt,
                actionCompletedAt = if (item.actionCompletedAt > 0L) item.actionCompletedAt else item.clickTimeMs,
                fare = item.amount,
                pickupDistanceKm = item.pickupDistKm,
                dropDistanceKm = item.dropDistKm,
                pickupAddress = item.pickupAddress,
                dropAddress = item.dropAddress,
                dropArea = item.dropArea,
                status = item.status.name,
                decisionReasonCode = item.decisionReasonCode,
                decisionReasonText = item.decisionReasonText.ifEmpty { item.reason },
                matchedGoToGroup = item.matchedGoToGroup,
                matchedNoGoGroup = item.matchedNoGoGroup,
                actionSucceeded = item.actionSucceeded,
                historyInsertLatencyMs = item.historyInsertLatencyMs,
                decisionLatencyMs = item.decisionLatencyMs,
                actionLatencyMs = item.actionLatencyMs,
                totalProcessingMs = item.totalProcessingMs,
                duplicateEventsIgnored = item.duplicateEventsIgnored,
                baseFare = item.baseFare,
                tipAmount = item.tipAmount,
                timesClicked = item.timesClicked,
                dateStr = item.dateStr,
                timeStr = item.timeStr
            )
        }
    }
}
