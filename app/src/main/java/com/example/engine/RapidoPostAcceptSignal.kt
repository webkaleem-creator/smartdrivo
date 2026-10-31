package com.example.engine

import java.util.Locale

/** Classifies only explicit Rapido post-click status text. */
object RapidoPostAcceptSignal {
    enum class Result { CONFIRMED, MISSED, PENDING, UNKNOWN }

    fun classify(raw: String): Result {
        val text = raw.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

        val negative = listOf(
            "you missed the order",
            "order missed",
            "missed this order",
            "order expired",
            "booking expired",
            "no longer available",
            "accepted by another",
            "another captain accepted",
            "order is unavailable",
            "ride is unavailable"
        )
        if (negative.any { text.contains(it) }) return Result.MISSED

        val positive = listOf(
            "go to pickup zone",
            "go to pickup",
            "navigate to pickup",
            "thanks for accepting",
            "ride accepted",
            "booking confirmed",
            "booking accepted",
            "order accepted",
            "ride confirmed",
            "arrived at pickup",
            "arrive at pickup",
            "reached pickup",
            "start trip",
            "start ride",
            "pickup otp",
            "verify otp"
        )
        if (positive.any { text.contains(it) }) return Result.CONFIRMED
        if (text.contains("customer verified location") && text.contains("arrived")) {
            return Result.CONFIRMED
        }
        if (text.contains("checking order status")) return Result.PENDING

        return Result.UNKNOWN
    }
}