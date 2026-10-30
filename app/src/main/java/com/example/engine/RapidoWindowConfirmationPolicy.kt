package com.example.engine

/** Truthful, side-effect-free reduction of live Rapido window signals. */
object RapidoWindowConfirmationPolicy {
    fun resolve(results: List<RapidoPostAcceptSignal.Result>): RapidoPostAcceptSignal.Result {

        val positive = results.any { it == RapidoPostAcceptSignal.Result.CONFIRMED }
        val negative = results.any { it == RapidoPostAcceptSignal.Result.MISSED }
        return when {
            positive && negative -> RapidoPostAcceptSignal.Result.UNKNOWN // contradictory windows are not proof
            positive -> RapidoPostAcceptSignal.Result.CONFIRMED
            negative -> RapidoPostAcceptSignal.Result.MISSED
            results.any { it == RapidoPostAcceptSignal.Result.PENDING } -> RapidoPostAcceptSignal.Result.PENDING
            else -> RapidoPostAcceptSignal.Result.UNKNOWN
        }
    }
}
