package com.example.model

import androidx.compose.runtime.Immutable

/** Per-owner Ola-only Global Filter. Shared safety/area rules are separate. */
@Immutable
data class OlaAdminFilterSettings(
    val enabled: Boolean = false,
    val filterMode: FilterMode = FilterMode.BOTH,
    val minFare: Float = 50f,
    val maxFare: Float = 999f,
    val maxPickupKm: Float = 3f,
    val maxTripKm: Float = 7.5f
) {
    fun valid(): Boolean =
        minFare.isFinite() && minFare >= 0f &&
        maxFare.isFinite() && maxFare >= minFare && maxFare <= 1_000_000f &&
        maxPickupKm.isFinite() && maxPickupKm > 0f && maxPickupKm <= 100f &&
        maxTripKm.isFinite() && maxTripKm > 0f && maxTripKm <= 1000f
}
