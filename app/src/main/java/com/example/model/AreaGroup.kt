package com.example.model

import androidx.compose.runtime.Immutable

enum class AreaType {
    GO_TO,
    NO_GO
}

/**
 * Area Group definition for GO_TO or NO_GO filtering with per-group customization
 */
@Immutable
data class AreaGroup(
    val id: String = "",
    val name: String = "",
    val isEnabled: Boolean = true,
    val type: AreaType = AreaType.GO_TO,
    val keywords: List<String> = emptyList(), // area name keywords to match in addresses
    // Per-group filter customization
    val filtersEnabled: Boolean = false,
    val minFare: Float = 50f,
    val minPickupKm: Float = 0.5f,
    val maxPickupKm: Float = 3.0f,
    val maxDropKm: Float = 7.5f
)
