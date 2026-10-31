package com.example.engine

/**
 * READ-ONLY verification after Rapido Accept was already dispatched.
 * More frequent polling must never shorten the previous ~1540ms evidence
 * window or trigger a second Accept click.
 */
object RapidoPostAcceptCadence {
    const val INITIAL_DELAY_MS = 40L
    const val POLL_INTERVAL_MS = 100L
    const val POLL_SAMPLES = 17
    const val POLL_SPAN_MS = (POLL_SAMPLES - 1) * POLL_INTERVAL_MS
}
