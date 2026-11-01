package com.example.service

import kotlin.math.roundToInt

/**
 * Compact accepted-ride overlay sizing. UI only: never affects filters,
 * acceptance, history, countdown or the confirmed acceptance time.
 */
internal object AcceptedOverlayCompactPolicy {
    const val MAX_DROP_ADDRESS_LINES = 3
    const val HEADER_BADGE_HEIGHT_DP = 36
    const val FARE_CELL_HEIGHT_DP = 41
    const val ROUTE_CELL_HEIGHT_DP = 46

    fun widthPx(screenWidthPx: Int, density: Float): Int {
        val scale = density.takeIf { it.isFinite() && it > 0f } ?: 1f
        val maxPx = (340f * scale).roundToInt().coerceAtLeast(1)
        val available = (screenWidthPx - (16f * scale).roundToInt()).coerceAtLeast(1)
        val fraction = (screenWidthPx.coerceAtLeast(1) * 0.86f).roundToInt().coerceAtLeast(1)
        return minOf(fraction, maxPx, available)
    }
}
