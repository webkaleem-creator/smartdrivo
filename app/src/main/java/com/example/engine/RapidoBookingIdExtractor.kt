package com.example.engine

/**
 * Extract only explicitly labeled, plausible Rapido booking IDs.
 *
 * Previously "(?:CRN|ID|#|Booking\\s*ID)" would find "ID" inside "Ride",
 * capturing just "e". All offers could then be persisted under the same
 * Room primary key "e" and later orders silently replaced prior History.
 *
 * No usable ID -> null. The existing unique ride fingerprint/timestamp
 * path handles genuine offers without exposed booking identifiers.
 */
object RapidoBookingIdExtractor {
    private val labeled = Regex(
        """(?i)(?<![a-z0-9])(?:booking\s*id|ride\s*id|order\s*id|crn|id)(?![a-z0-9])\s*(?:[:=#-]\s*|\s+)([a-z0-9][a-z0-9-]{4,63})(?![a-z0-9-])"""
    )
    private val standaloneHash = Regex("""(?i)^#\s*([a-z0-9][a-z0-9-]{4,63})$""")
    private val idFormat = Regex("""(?i)^[a-z0-9]+(?:-[a-z0-9]+)*$""")

    fun isValidId(id: String?): Boolean {
        val clean = id?.trim() ?: return false
        return clean.length in 5..64 && idFormat.matches(clean) && clean.any(Char::isDigit)
    }

    fun fromTexts(texts: Iterable<String>): String? {
        for (text in texts) {
            val value = text.trim()
            val labeledId = labeled.findAll(value).map { it.groupValues[1] }
                .firstOrNull(::isValidId)
            if (labeledId != null) return labeledId
            val hashId = standaloneHash.matchEntire(value)?.groupValues?.get(1)
            if (isValidId(hashId)) return hashId
        }
        return null
    }
}
