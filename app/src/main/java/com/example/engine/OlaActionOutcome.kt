package com.example.engine

import com.example.model.OrderStatus

/** A successful action request is not a booking. No retry permission is implied. */
object OlaActionOutcome {
    fun status(actionAcknowledged: Boolean, bookingConfirmed: Boolean): OrderStatus = when {
        bookingConfirmed -> OrderStatus.ACCEPTED
        actionAcknowledged -> OrderStatus.UNCONFIRMED
        else -> OrderStatus.FAILED
    }
}
