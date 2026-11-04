package com.example.engine

import com.example.model.OrderStatus

/**
 * Rapido Pickup Guard has two distinct outcomes:
 * a Skip/X request is not a verified platform rejection.
 * Surface a non-final result immediately, then upgrade only on UI evidence.
 */
object PickupGuardRejectHistoryPolicy {
    enum class Phase { SKIP_SENT, RAPIDO_HOME_CONFIRMED, DISMISSAL_UNVERIFIED }

    data class Decision(
        val status: OrderStatus,
        val reasonCode: String,
        val suffix: String,
        val showPopup: Boolean
    )

    fun forPhase(phase: Phase): Decision = when (phase) {
        Phase.SKIP_SENT -> Decision(
            OrderStatus.IGNORED,
            "PICKUP_GUARD_SKIP_VERIFYING",
            "Skip action sent; verifying dismissal (not yet confirmed)",
            true
        )
        Phase.RAPIDO_HOME_CONFIRMED -> Decision(
            OrderStatus.REJECTED,
            "PICKUP_GUARD_REJECT_UI_DISMISSED",
            "Rapido Skip action sent; home screen observed",
            false
        )
        Phase.DISMISSAL_UNVERIFIED -> Decision(
            OrderStatus.IGNORED,
            "PICKUP_GUARD_REJECT_UNCONFIRMED",
            "Skip action sent, but dismissal not verified",
            false
        )
    }
}
