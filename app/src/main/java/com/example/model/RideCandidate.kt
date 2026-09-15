package com.example.model

/**
 * Extracted ride request details detected on screen
 */
data class RideCandidate(
    val fare: Float?,
    val pickupDistKm: Float?,
    val dropDistKm: Float?,
    val pickupAddress: String?,
    val dropAddress: String?,
    val dropArea: String?,
    val platform: Platform = Platform.UBER, // RAPIDO, UBER, OLA
    val vehicleType: VehicleType = VehicleType.AUTO, // AUTO, BIKE, CAR
    val bookingId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isBundledOrder: Boolean = false,
    val detectionTimeMs: Long = System.currentTimeMillis(),
    val baseFare: Float? = null,
    val tipAmount: Float? = null
)
