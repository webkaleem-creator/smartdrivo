package com.example.engine

/** One final-gate snapshot only. Grants no click permission and caches across no events. */
class RapidoClickWork(
    canonical: () -> DecisionResult,
    historyReason: () -> String?
) {
    val decision: DecisionResult by lazy(LazyThreadSafetyMode.NONE, canonical)
    private val reason: String? by lazy(LazyThreadSafetyMode.NONE) {
        try { historyReason() } catch (_: Exception) {
            // Presentation must never abort verification after a safe action was dispatched.
            "Saved filters matched (details unavailable)"
        }
    }
    fun afterDispatch(): String? = reason
}
