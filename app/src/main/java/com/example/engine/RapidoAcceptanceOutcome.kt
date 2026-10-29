package com.example.engine

import com.example.model.OrderStatus

/** A click or vanished popup alone never proves Rapido acceptance. */
object RapidoAcceptanceOutcome {
    fun statusFor(signal: RapidoPostAcceptSignal.Result): OrderStatus = when (signal) {
        RapidoPostAcceptSignal.Result.CONFIRMED -> OrderStatus.ACCEPTED
        RapidoPostAcceptSignal.Result.MISSED -> OrderStatus.MISSED
        RapidoPostAcceptSignal.Result.PENDING,
        RapidoPostAcceptSignal.Result.UNKNOWN -> OrderStatus.UNCONFIRMED
    }
}
