package com.example.model

/** One immutable, server-timestamped claim per Firebase UID per campaign. */
data class SpecialOccasionClaim(
    val uid: String = "",
    val campaignId: String = "",
    val days: Int = -1,
    val claimedAtMillis: Long = 0L
) {
    fun belongsTo(uid: String, offer: SpecialOccasionOffer, now: Long = System.currentTimeMillis()): Boolean =
        uid.isNotBlank() && this.uid == uid && campaignId.isNotBlank() &&
            campaignId == offer.id && days == offer.days &&
            claimedAtMillis >= offer.startsAtMillis &&
            claimedAtMillis > 0L && claimedAtMillis <= now + 30_000L

    fun expiryMillis(): Long = when (days) {
        0 -> Long.MAX_VALUE
        1, 3, 7, 30 -> claimedAtMillis.takeIf { it > 0L }
            ?.plus(days.toLong() * 86_400_000L) ?: 0L
        else -> 0L
    }

    fun activeFor(uid: String, offer: SpecialOccasionOffer, now: Long = System.currentTimeMillis()): Boolean =
        offer.isLive(now) && belongsTo(uid, offer, now) && expiryMillis() > now
}
