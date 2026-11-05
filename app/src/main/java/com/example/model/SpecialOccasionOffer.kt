package com.example.model

data class SpecialOccasionOffer(
    val id: String = "",
    val enabled: Boolean = false,
    val title: String = "",
    val description: String = "",
    val days: Int = 1,
    val startsAtMillis: Long = 0L
) {
    // Duration is PER USER from their claim timestamp, not from admin ON.
    // Admin ON advertises availability, OFF immediately revokes the campaign.
    fun isLive(now: Long = System.currentTimeMillis()): Boolean =
        enabled && id.isNotBlank() && title.isNotBlank() &&
            days in setOf(0, 1, 3, 7, 30) &&
            startsAtMillis > 0L && startsAtMillis <= now
    fun durationLabel(): String =
        if (days == 0) "Unlimited while offer is ON"
        else days.toString() + if (days == 1) " day free" else " days free"
    companion object {
        fun fromMap(value: Any?): SpecialOccasionOffer {
            val map = value as? Map<*, *> ?: return SpecialOccasionOffer()
            val days = (map["days"] as? Number)?.toInt() ?: -1
            if (days !in setOf(0, 1, 3, 7, 30)) return SpecialOccasionOffer()
            return SpecialOccasionOffer(
                id = (map["id"] as? String).orEmpty().take(120),
                enabled = map["enabled"] == true,
                title = (map["title"] as? String).orEmpty().trim().take(80),
                description = (map["description"] as? String).orEmpty().trim().take(500),
                days = days,
                startsAtMillis = (map["startsAtMillis"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
