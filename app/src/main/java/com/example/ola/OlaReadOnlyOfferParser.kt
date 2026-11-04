package com.example.ola

/**
 * Pure parser for narrowly selected Ola accessibility resource IDs.
 * NEVER reads or retains pickup address, coordinates, button state or rider identity.
 * Partial data is intentionally allowed for an owner-only diagnostic preview.
 */
object OlaReadOnlyOfferParser {
    enum class FieldType { FARE, PICKUP_KM, TRIP_KM, DROP_ADDRESS }

    data class Field(val viewId: String, val value: String)

    data class Offer(
        val fareRupees: Double?,
        val pickupKm: Double?,
        val tripKm: Double?,
        val dropAddress: String?,
        val matchedFieldCount: Int
    ) {
        val isComplete: Boolean
            get() = fareRupees != null && pickupKm != null &&
                tripKm != null && !dropAddress.isNullOrBlank()
    }

    private val rupee = Regex("""(?:₹\s*|Rs\.?\s*)(\d{1,6}(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE)
    private val distance = Regex("""(?<!\d)(\d{1,4}(?:[.,]\d{1,2})?)\s*kms?\b""",
        RegexOption.IGNORE_CASE)

    /** A positive allowlist; unknown IDs and pickup addresses are never read. */
    fun classify(viewId: String?): FieldType? {
        val name = viewId?.substringAfterLast('/')?.trim()?.lowercase() ?: return null
        return when (name) {
            "tv_compact_price", "tv_order_fare", "tv_total_fare" -> FieldType.FARE
            "tv_pickup_distance_time", "tv_pickup_distance", "tv_pickup_eta" -> FieldType.PICKUP_KM
            "tv_drop_distance_time", "tv_trip_distance", "tv_drop_distance" -> FieldType.TRIP_KM
            "tv_detail_drop_address", "tv_drop_address", "tv_destination_address" ->
                FieldType.DROP_ADDRESS
            else -> null
        }
    }

    private fun parseNumber(value: String): Double? =
        value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

    fun parse(fields: List<Field>): Offer? {
        var fare: Double? = null
        var pickup: Double? = null
        var trip: Double? = null
        var drop: String? = null
        var matched = 0
        // Each field originates from a known UI view ID; no generic text scan.
        for (field in fields.take(16)) {
            val type = classify(field.viewId) ?: continue
            val value = field.value.trim().take(220)
            if (value.isEmpty()) continue
            matched++
            when (type) {
                FieldType.FARE -> {
                    if (fare == null) fare = rupee.find(value)?.groupValues?.getOrNull(1)
                        ?.toDoubleOrNull()?.takeIf { it in 1.0..100000.0 }
                }
                FieldType.PICKUP_KM -> {
                    if (pickup == null) pickup =
                        distance.find(value)?.groupValues?.getOrNull(1)?.let(::parseNumber)
                            ?.takeIf { it <= 300.0 }
                }
                FieldType.TRIP_KM -> {
                    if (trip == null) trip =
                        distance.find(value)?.groupValues?.getOrNull(1)?.let(::parseNumber)
                            ?.takeIf { it <= 300.0 }
                }
                FieldType.DROP_ADDRESS -> {
                    if (drop == null && value.length >= 4) drop = value.take(180)
                }
            }
        }
        if (matched == 0) return null
        return Offer(fare, pickup, trip, drop, matched)
    }
}
