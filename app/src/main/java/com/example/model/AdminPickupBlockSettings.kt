package com.example.model

/** Owner-only pickup-specific veto, OFF by default; stored per Firebase UID. */
data class AdminPickupBlockSettings(
    val enabled: Boolean = false,
    val blockedWords: List<String> = emptyList()
)
