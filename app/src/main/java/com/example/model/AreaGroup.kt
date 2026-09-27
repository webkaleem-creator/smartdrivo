package com.example.model

import androidx.compose.runtime.Immutable

enum class AreaType {
    GO_TO,
    NO_GO
}

@Immutable
data class AreaEntry(
    val name: String = "",
    val minFare: Float = 0f,
    val maxPickupKm: Float = 0f,
    val maxDropKm: Float = 0f
)

/**
 * Area Group definition for GO_TO or NO_GO filtering.
 *
 * `keywords` and the old group-level limits are intentionally preserved
 * for backward compatibility with existing saved users/data.
 *
 * `areas` is the new per-area configuration.
 */
@Immutable
data class AreaGroup(
    val id: String = "",
    val name: String = "",
    val isEnabled: Boolean = true,
    val type: AreaType = AreaType.GO_TO,

    val keywords: List<String> = emptyList(),

    val filtersEnabled: Boolean = false,
    val minFare: Float = 50f,
    val maxFare: Float = 0f,
    val minPickupKm: Float = 0.5f,
    val maxPickupKm: Float = 3.0f,
    val maxDropKm: Float = 7.5f,

    val areas: List<AreaEntry> = emptyList()
)
