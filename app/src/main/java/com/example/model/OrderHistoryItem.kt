package com.example.model

enum class OrderStatus {
    ACCEPTED,
    REJECTED,
    IGNORED,
    MISSED
}

data class OrderHistoryItem(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateStr: String = "",
    val timeStr: String = "",
    val status: OrderStatus = OrderStatus.ACCEPTED,
    val platform: Platform = Platform.RAPIDO,
    val vehicleType: VehicleType = VehicleType.AUTO,
    val pickupDistKm: Float = 0f,
    val dropDistKm: Float = 0f,
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val dropArea: String = "",
    val amount: Float = 0f,
    val bookingId: String = "",
    val detectionTimeMs: Long = 0L,
    val clickTimeMs: Long = 0L,
    val baseFare: Float = 0f,
    val tipAmount: Float = 0f,
    val timesClicked: Int = 1,
    val reason: String = ""
)
