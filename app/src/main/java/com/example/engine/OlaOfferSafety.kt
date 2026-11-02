package com.example.engine

import java.util.Locale
import kotlin.math.abs

/**
 * Ola-only, UI-text safety decisions. No Rapido/Uber state or selectors.
 * Fail closed on ambiguous offers: a UI click is never proof of acceptance.
 */
object OlaOfferSafety {
    // Require an explicit label and an identifier containing a digit. Never parse prose.
    fun bookingId(text: String): String? = Regex(
        """\b(?:CRN|booking\s*(?:id|number|no\.?)|order\s*(?:id|number|no\.?))\s*[:#-]?\s*([A-Z0-9-]{3,32})\b""",
        RegexOption.IGNORE_CASE
    ).findAll(text).map { it.groupValues[1] }
        .firstOrNull { id -> id.any { it.isDigit() } }

    fun routesToOla(eventPackage: String, rootPackages: List<String>): Boolean =
        isDriverPackage(eventPackage) || rootPackages.any { isDriverPackage(it) }

    fun internalId(): String = "ola_offer_" + java.util.UUID.randomUUID().toString()

    /** Missing or changed decision inputs fail closed, even for the same booking. */
    fun sameOffer(old: com.example.model.RideCandidate, live: com.example.model.RideCandidate): Boolean {
        val genuineId = old.bookingId?.takeUnless { it.startsWith("ola_offer_") }
        if (genuineId != null && genuineId != live.bookingId) return false
        fun address(value: String?) = value.orEmpty().trim().lowercase(Locale.ROOT)
        return old.fare != null && old.fare == live.fare &&
            old.pickupDistKm == live.pickupDistKm && old.dropDistKm == live.dropDistKm &&
            address(old.pickupAddress) == address(live.pickupAddress) &&
            address(old.dropAddress) == address(live.dropAddress) && old.vehicleType == live.vehicleType
    }

    /** Replacement requires positive identity evidence, never missing render fields. */
    fun isDifferentOffer(old: com.example.model.RideCandidate, live: com.example.model.RideCandidate): Boolean {
        fun id(v: String?) = v?.takeUnless { it.startsWith("ola_offer_") || it.isBlank() }
        val a = id(old.bookingId); val b = id(live.bookingId)
        if (a != null && b != null) return a != b
        fun address(v: String?): String? = normalize(v).takeUnless {
            it.isBlank() || it == "address unavailable" || it.endsWith(" location")
        }
        val oldPickup = address(old.pickupAddress); val newPickup = address(live.pickupAddress)
        val oldDrop = address(old.dropAddress); val newDrop = address(live.dropAddress)
        return oldPickup != null && newPickup != null && oldDrop != null && newDrop != null &&
            oldPickup != newPickup && oldDrop != newDrop
    }

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
