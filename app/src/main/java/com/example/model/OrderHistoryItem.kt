package com.example.model

import androidx.compose.runtime.Immutable

enum class OrderStatus {
    PROCESSING,
    ACCEPTED,
    REJECTED,
    IGNORED,
    FAILED,
    SKIPPED,
    MISSED
}

@Immutable
data class OrderHistoryItem(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateStr: String = "",
    val timeStr: String = "",
    val status: OrderStatus = OrderStatus.PROCESSING,
    val platform: Platform = Platform.RAPIDO,
    val vehicleType: VehicleType = VehicleType.AUTO,
    val pickupDistKm: Float = 0f,
    val dropDistKm: Float = 0f,
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val dropArea: String = "",
    val amount: Float = 0f,
    val bookingId: String = "",
    val bookingFingerprint: String = "",
    val detectedAt: Long = timestamp,
    val historyInsertedAt: Long = 0L,
    val decisionAt: Long = 0L,
    val actionAttemptAt: Long = 0L,
    val actionCompletedAt: Long = 0L,
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
    val detectionTimeMs: Long = 0L,
    val clickTimeMs: Long = 0L,
    val baseFare: Float = 0f,
    val tipAmount: Float = 0f,
    val timesClicked: Int = 1,
    val reason: String = ""
)
