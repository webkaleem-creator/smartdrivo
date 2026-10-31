package com.example.engine

import com.example.model.OrderStatus

/**
 * Only a verified Accepted result should be shown as ACCEPTED.
 * Later/queued uncertain callbacks must never visually downgrade it.
 */
object OrderPopupStatusPolicy {
    fun mayReplace(previous: OrderStatus?, next: OrderStatus): Boolean {
        if (previous == OrderStatus.ACCEPTED && next != OrderStatus.ACCEPTED) return false
        if (previous == OrderStatus.MISSED && next == OrderStatus.UNCONFIRMED) return false
        return true
    }

    // Presentation wording is intentionally not the persisted OrderStatus.
    // "Could not verify yet" does not mean that Rapido rejected the order.
    fun statusTitle(status: OrderStatus): String = when (status) {
        OrderStatus.ACCEPTED -> "ACCEPTED"
        OrderStatus.UNCONFIRMED -> "COULD NOT VERIFY YET"
        OrderStatus.PROCESSING -> "VERIFYING"
        OrderStatus.IGNORED -> "IGNORED"
        OrderStatus.REJECTED -> "REJECTED"
        OrderStatus.SKIPPED -> "SKIPPED"
        OrderStatus.FAILED -> "ACTION FAILED"
        OrderStatus.MISSED -> "MISSED"
    }
}
