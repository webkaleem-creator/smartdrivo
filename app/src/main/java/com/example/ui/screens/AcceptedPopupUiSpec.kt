package com.example.ui.screens

/** Pure display contract; does not change acceptance behavior or stored orders. */
internal object AcceptedPopupUiSpec {
    data class EventTime(val caption: String, val timestampMs: Long?)
    fun eventTime(confirmedAtMs: Long, clickAtMs: Long): EventTime = when {
        confirmedAtMs > 0L -> EventTime("Accepted at", confirmedAtMs)
        clickAtMs > 0L -> EventTime("Accept tapped at", clickAtMs)
        else -> EventTime("Time unavailable", null)
    }
    fun widthFraction(widthDp: Int): Float = if (widthDp >= 600) 0.78f else 0.90f
    fun maxHeightDp(heightDp: Int): Int =
        (heightDp.coerceAtLeast(320) * 58 / 100).coerceIn(200, 440)
}
