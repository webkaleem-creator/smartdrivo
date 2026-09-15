package com.example.model

enum class VehicleType(val displayName: String) {
    AUTO("Auto"),
    BIKE("Bike"),
    CAR("Car");

    companion object {
        fun fromString(value: String?): VehicleType {
            return when (value?.uppercase()) {
                "BIKE" -> BIKE
                "CAR" -> CAR
                else -> AUTO
            }
        }
    }
}
