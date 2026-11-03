package com.example.model

/** Saved per owner; disabled by default on existing installations. */
data class PickupGuardSettings(val enabled: Boolean = false, val maxPickupKm: Float = 0.6f)
