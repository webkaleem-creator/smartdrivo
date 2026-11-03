package com.example.data.db

import com.example.model.RideCandidate
import java.util.Locale

/** Pure local identity generation; safe before the asynchronous Room insert. */
object HistoryRecordIdentity {
    fun id(candidate: RideCandidate): String {
        val booking = candidate.bookingId?.takeIf { it.isNotBlank() }
        if (booking != null) return "booking_${candidate.platform.name}_$booking"
        val detected = candidate.detectionTimeMs.takeIf { it > 0L } ?: candidate.timestamp
        return "ride_${fingerprint(candidate)}_$detected"
    }
    fun fingerprint(candidate: RideCandidate): String {
        if (!candidate.bookingId.isNullOrBlank()) {
            return "${candidate.platform}_${candidate.bookingId}"
        }
        val fareInt = (candidate.fare ?: 0f).toInt()
        val pickup10 = ((candidate.pickupDistKm ?: 0f) * 10).toInt()
        val drop10 = ((candidate.dropDistKm ?: 0f) * 10).toInt()
        val pickupClean = (candidate.pickupAddress ?: "").take(12).replace(Regex("[^A-Za-z0-9]"), "")
        // A 12-character drop prefix collides for many Charminar offers.
        val fullDrop = candidate.dropAddress.orEmpty().trim().lowercase(Locale.ROOT)
        val dropClean = fullDrop.replace(Regex("[^a-z0-9]"), "").take(48)
        val dropSignature = fullDrop.hashCode().toUInt().toString(16)
        return "${candidate.platform}_${fareInt}_${pickup10}_${drop10}_${pickupClean}_${dropClean}_${dropSignature}"
    }


}
