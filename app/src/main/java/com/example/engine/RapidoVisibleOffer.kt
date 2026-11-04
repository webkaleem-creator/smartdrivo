package com.example.engine

import com.example.model.*
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/** Detection ownership is independent of the action lock. Never grants click permission. */
class RapidoVisibleOffer {
    class Session(val id: String, @Volatile var candidate: RideCandidate) {
        @Volatile var attempted = false
        @Volatile var finalized = false
    }
    data class Observation(val session: Session, val created: Boolean, val replaced: Session?)
    private var current: Session? = null
    private val knownBookings = object : LinkedHashMap<String, Session>(128, .75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, Session>?) = size > 128
    }
    @Synchronized fun observe(candidate: RideCandidate): Observation {
        val old = current
        if (old != null && samePresentation(old.candidate, candidate)) {
            old.candidate = candidate.copy(bookingId = candidate.bookingId ?: old.candidate.bookingId)
            return Observation(old, false, null)
        }
        val booking = candidate.bookingId?.takeIf { it.isNotBlank() }
        val known = booking?.let { knownBookings[it] }
        val next = known ?: Session("rapido_offer_${UUID.randomUUID()}", candidate)
        next.candidate = candidate
        if (booking != null) knownBookings[booking] = next
        current = next
        return Observation(next, known == null, old)
    }
    @Synchronized fun finish(id: String) { current?.takeIf { it.id == id }?.finalized = true }
    @Synchronized fun attempted(id: String) { current?.takeIf { it.id == id }?.attempted = true }
    @Synchronized fun owns(id: String): Boolean = current?.id == id
    @Synchronized fun isCurrent(id: String, candidate: RideCandidate): Boolean = current?.let {
        it.id == id && it.candidate.fare == candidate.fare &&
            (candidate.bookingId == null || it.candidate.bookingId == candidate.bookingId) &&
            it.candidate.pickupDistKm == candidate.pickupDistKm &&
            it.candidate.dropDistKm == candidate.dropDistKm &&
            clean(it.candidate.dropAddress) == clean(candidate.dropAddress) &&
            it.candidate.isBundledOrder == candidate.isBundledOrder
    } == true
    @Synchronized fun disappeared(): Session? = current.also { current = null }

    companion object {
        private fun clean(s: String?) = s.orEmpty().trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        fun bookingId(text: String): String? {
            val labelled = Regex(
                """\b(?:CRN|booking\s*(?:id|number|no\.?)|order\s*(?:id|number|no\.?))\s*[:#-]?\s*([A-Z0-9-]{3,32})\b""",
                RegexOption.IGNORE_CASE).findAll(text).map { it.groupValues[1] }
                .firstOrNull { value -> value.any(Char::isDigit) }
            if (labelled != null) return labelled
            // A standalone ID field is supported; Driver/Customer/Vehicle ID prose is not booking evidence.
            return Regex("""^ID\s*[:#-]?\s*([A-Z0-9-]{3,32})$""", RegexOption.IGNORE_CASE)
                .matchEntire(text.trim())?.groupValues?.get(1)?.takeIf { it.any(Char::isDigit) }
        }
        fun samePresentation(a: RideCandidate, b: RideCandidate): Boolean {
            if (a.platform != Platform.RAPIDO || b.platform != Platform.RAPIDO) return false
            val x = a.bookingId?.takeIf { it.isNotBlank() }; val y = b.bookingId?.takeIf { it.isNotBlank() }
            if (x != null && y != null) return x == y
            if (a.isBundledOrder != b.isBundledOrder) return false
            val da = clean(a.dropAddress); val db = clean(b.dropAddress)
            if (da.isNotBlank() && db.isNotBlank() && da != "address unavailable" &&
                db != "address unavailable" && da != db) return false
            fun changed(x: Float?, y: Float?, tolerance: Float) =
                x != null && y != null && x.isFinite() && y.isFinite() && abs(x-y) > tolerance
            return !changed(a.fare, b.fare, 1.01f) && !changed(a.dropDistKm, b.dropDistKm, .11f)
        }
    }
}
