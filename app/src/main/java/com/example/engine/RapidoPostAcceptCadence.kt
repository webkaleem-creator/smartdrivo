package com.example.engine

/**
 * READ-ONLY verification after Rapido Accept was already dispatched.
 * UI confirmation can arrive later than the old ~1.6-second poll window.
 * Observe read-only for ~4 seconds; never trigger another Accept click.
 */
object RapidoPostAcceptCadence {
    const val INITIAL_DELAY_MS = 40L
    const val POLL_INTERVAL_MS = 150L
    const val POLL_SAMPLES = 27
    const val POLL_SPAN_MS = (POLL_SAMPLES - 1) * POLL_INTERVAL_MS
}
