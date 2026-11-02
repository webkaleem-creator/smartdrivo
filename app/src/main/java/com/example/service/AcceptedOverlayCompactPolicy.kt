package com.example.service

import kotlin.math.roundToInt

/**
 * Compact accepted-ride overlay sizing. UI only: never affects filters,
 * acceptance, history, countdown or the confirmed acceptance time.
 */
internal object AcceptedOverlayCompactPolicy {
    const val MAX_DROP_ADDRESS_LINES = 2
    const val HEADER_BADGE_HEIGHT_DP = 30
    const val FARE_CELL_HEIGHT_DP = 52
    const val ROUTE_CELL_HEIGHT_DP = 52

    fun widthPx(screenWidthPx: Int, density: Float): Int {
        val scale = density.takeIf { it.isFinite() && it > 0f } ?: 1f
        val maxPx = (330f * scale).roundToInt().coerceAtLeast(1)
        val available = (screenWidthPx - (16f * scale).roundToInt()).coerceAtLeast(1)
        val fraction = (screenWidthPx.coerceAtLeast(1) * 0.83f).roundToInt().coerceAtLeast(1)
        return minOf(fraction, maxPx, available)
    }
}
