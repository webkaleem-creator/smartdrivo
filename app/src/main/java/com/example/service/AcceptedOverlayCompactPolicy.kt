package com.example.service

import kotlin.math.roundToInt

/**
 * Compact accepted-ride overlay sizing. UI only: never affects filters,
 * acceptance, history, countdown or the confirmed acceptance time.
 */
internal object AcceptedOverlayCompactPolicy {
    const val MAX_DROP_ADDRESS_LINES = 2
    const val HEADER_BADGE_HEIGHT_DP = 28
    const val FARE_CELL_HEIGHT_DP = 46
    const val ROUTE_CELL_HEIGHT_DP = 46

    fun widthPx(screenWidthPx: Int, density: Float): Int {
        val scale = density.takeIf { it.isFinite() && it > 0f } ?: 1f
        val maxPx = (420f * scale).roundToInt().coerceAtLeast(1)
        val available = (screenWidthPx - (8f * scale).roundToInt()).coerceAtLeast(1)
        val fraction = (screenWidthPx.coerceAtLeast(1) * 0.96f).roundToInt().coerceAtLeast(1)
        return minOf(fraction, maxPx, available)
    }
}
