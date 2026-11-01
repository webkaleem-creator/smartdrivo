package com.example.engine

import java.util.Locale
import kotlin.math.abs

/**
 * Ola-only, UI-text safety decisions. No Rapido/Uber state or selectors.
 * Fail closed on ambiguous offers: a UI click is never proof of acceptance.
 */
object OlaOfferSafety {
    private val driverPackages = setOf(
        "com.olacabs.oladriver",
        "com.olacabs.driver",
        "com.ola.partner",
        "ola.cabs"
    )

    private val acceptAction = Regex(
        """^(?:(?:tap|swipe|slide)\s+to\s+)?accept(?:\s+(?:ride|order|booking|trip|duty))?$"""
    )
    private val confirmAction = Regex("""^confirm(?:\s+(?:ride|order|booking|trip))?$""")
    private val acceptCountdown = Regex("""^accept\s+in\s+\d+\s*(?:s|sec|secs|seconds)?$""")
    private val confirmed = Regex("""\b(?:ride|order|booking)\s+accepted\b""")
    private val acceptedNextStep = listOf(
        "navigate to pickup", "go to pickup", "arrived at pickup",
        "arrive at pickup", "start ride", "start trip", "trip started",
        "ongoing ride"
    )
    private val regionalAccept = listOf(
        "स्वीकार", "ಸ್ವೀಕರಿಸಿ", "ஏற்க", "ஏற்றுக்கொள்",
        "అంగీకరించు", "స్వీకరించు", "গ্রহণ"
    )

    private fun normalize(value: String?): String =
        value.orEmpty().trim().lowercase(Locale.ROOT).replace(Regex("""\s+"""), " ")

    fun isDriverPackage(packageName: String?): Boolean =
        normalize(packageName) in driverPackages

    /** Detection may recognize a countdown, but countdown is NOT click-ready. */
    fun isAcceptLabel(text: String?): Boolean {
        val label = normalize(text)
        if (label.isBlank()) return false
        return acceptAction.matches(label) ||
            confirmAction.matches(label) ||
            acceptCountdown.matches(label) ||
            regionalAccept.any { label == it || label == "$it करें" }
    }

    fun isReadyLabel(text: String?): Boolean {
        val label = normalize(text)
        return label.isNotBlank() && !acceptCountdown.matches(label) &&
            (acceptAction.matches(label) || confirmAction.matches(label) ||
                regionalAccept.any { label == it || label == "$it करें" })
    }

    fun hasAcceptedUi(screenText: String): Boolean {
        val text = normalize(screenText)
        if (text.contains("accepted by another") ||
            text.contains("not accepted") ||
            text.contains("unable to accept") ||
            text.contains("no longer available") ||
            text.contains("booking cancelled") ||
            text.contains("ride cancelled") ||
            text.contains("order missed") ||
            text.contains("order expired")
        ) return false
        return confirmed.containsMatchIn(text) ||
            acceptedNextStep.any { text.contains(it) }
    }

    /** Prevent non-offer/profile screens from creating fake Ola history. */
    fun hasUsableOffer(
        fare: Float?,
        pickupKm: Float?,
        dropKm: Float?,
        pickupAddress: String?,
        dropAddress: String?
    ): Boolean {
        if (fare == null || !fare.isFinite() || fare <= 0f) return false
        val distanceSeen = listOf(pickupKm, dropKm)
            .any { it != null && it.isFinite() && it > 0f }
        val addressSeen = listOf(pickupAddress, dropAddress).any {
            val address = normalize(it)
            address.length >= 4 && address != "address unavailable" &&
                address != "pickup location" && address != "drop location"
        }
        return distanceSeen || addressSeen
    }

    /** Never use a previously evaluated offer's fare/distance for a new click. */
    fun hasChanged(
        evaluatedFare: Float?,
        liveFare: Float?,
        evaluatedPickupKm: Float?,
        livePickupKm: Float?
    ): Boolean {
        val fareChanged = evaluatedFare != null && liveFare != null &&
            evaluatedFare.isFinite() && liveFare.isFinite() &&
            evaluatedFare > 0f && liveFare > 0f &&
            abs(evaluatedFare - liveFare) > 0.49f
        val pickupChanged = evaluatedPickupKm != null && livePickupKm != null &&
            evaluatedPickupKm.isFinite() && livePickupKm.isFinite() &&
            evaluatedPickupKm > 0f && livePickupKm > 0f &&
            abs(evaluatedPickupKm - livePickupKm) > 0.20f
        return fareChanged || pickupChanged
    }
}
