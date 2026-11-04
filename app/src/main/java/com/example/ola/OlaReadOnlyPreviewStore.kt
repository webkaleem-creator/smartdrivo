package com.example.ola

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Private, process-local diagnostic snapshot. No preference, history, network,
 * Firebase, logcat or user-visible notification writes.
 */
object OlaReadOnlyPreviewStore {
    data class State(
        val offer: OlaReadOnlyOfferParser.Offer? = null,
        val seenAtMillis: Long = 0L,
        val status: String = "No live Ola offer captured"
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun record(knownFields: List<OlaReadOnlyOfferParser.Field>, nowMillis: Long) {
        val parsed = OlaReadOnlyOfferParser.parse(knownFields)
        // Don't discard a complete snapshot on a later partial content event.
        if (parsed == null) {
            if (_state.value.offer == null) {
                _state.value = State(
                    seenAtMillis = nowMillis,
                    status = "Ola window seen; expected resource IDs not found"
                )
            }
            return
        }
        if (_state.value.offer?.isComplete == true && !parsed.isComplete &&
            nowMillis - _state.value.seenAtMillis < 3_000L) return
        _state.value = State(
            offer = parsed,
            seenAtMillis = nowMillis,
            status = if (parsed.isComplete) "Fields captured (read-only)"
                     else "Partial Ola data; missing fields shown below"
        )
    }

    fun clear() { _state.value = State() }
}
