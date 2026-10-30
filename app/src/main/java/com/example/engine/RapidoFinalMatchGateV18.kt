package com.example.engine

/** The final Rapido click cannot bypass every saved eligibility route. */
object RapidoFinalMatchGateV18 {
    fun isEligible(
        fastestEnabled: Boolean,
        normalMatch: Boolean,
        fixAreaMatch: Boolean,
        plusAmountMatch: Boolean,
        unlimitedPlusMatch: Boolean
    ): Boolean {
        // Fastest is explicitly isolated: ONLY the pickup-rule decision.
        if (fastestEnabled) return normalMatch
        // Normal-mode rules are independent OR alternatives by design.
        return normalMatch || fixAreaMatch || plusAmountMatch || unlimitedPlusMatch
    }
}
