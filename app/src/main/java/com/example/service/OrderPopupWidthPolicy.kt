package com.example.service

import kotlin.math.roundToInt

/**
 * Caps the short, non-touchable order-status popup to screen bounds.
 * Uses measured label width, not a permanent empty right-side slot.
 * Does not change the full accepted-order floating overlay.
 */
internal object OrderPopupWidthPolicy {
    fun resolveWidthPx(
        screenWidthPx: Int,
        density: Float,
        measuredContentWidthPx: Int
    ): Int {
        val safeDensity = density.takeIf { it.isFinite() && it > 0f } ?: 1f
        fun dp(value: Int) = (value * safeDensity).roundToInt()
        val available = (screenWidthPx - dp(24)).coerceAtLeast(1)
        val maximum = minOf(dp(260), available)
        val minimum = minOf(dp(150), maximum)
        return (measuredContentWidthPx.coerceAtLeast(0) + dp(24))
            .coerceIn(minimum, maximum)
    }
}
