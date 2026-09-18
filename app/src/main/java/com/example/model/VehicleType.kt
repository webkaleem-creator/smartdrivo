package com.example.model

enum class VehicleType(val displayName: String, val icon: String) {
    AUTO("Auto Rickshaw", "🛺"),
    BIKE("Bike", "🏍️"),
    CAR("Car", "🚗");

    companion object {
        fun fromString(value: String?): VehicleType {
            val upper = value?.uppercase()?.trim() ?: return AUTO
            return when {
                upper == "BIKE" -> BIKE
                upper == "CAR" -> CAR
                upper.contains("AUTO") || upper.contains("RICKSHAW") -> AUTO
                else -> AUTO
            }
        }
    }
}
