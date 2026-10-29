package com.example.engine

/** Distance validity rules for Rapido offers. A measured pickup of 0 km is valid; a trip/drop must be > 0 km. */
object RapidoDistanceValidity {
    fun isValidPickup(value: Float?): Boolean =
        value != null && value.isFinite() && value >= 0f

    fun isValidDrop(value: Float?): Boolean =
        value != null && value.isFinite() && value > 0f
}