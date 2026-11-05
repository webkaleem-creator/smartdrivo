package com.example.model

enum class SpecialOfferSyncStatus {
    CHECKING,
    CACHE_ONLY,
    SERVER_LIVE,
    SERVER_NO_OFFER,
    SYNC_ERROR;

    fun description(): String = when (this) {
        CHECKING -> "Checking special offers with the server..."
        CACHE_ONLY -> "Waiting for live server confirmation. Cached offers cannot unlock membership."
        SERVER_LIVE -> "Special occasion offer verified LIVE."
        SERVER_NO_OFFER -> "No active special offer confirmed by the server."
        SYNC_ERROR -> "Cannot verify special offers. Check internet / sign-in and reopen Membership."
    }
}

object SpecialOfferSnapshotPolicy {
    data class Result(
        val offer: SpecialOccasionOffer,
        val status: SpecialOfferSyncStatus
    )

    // Firestore may emit a cached document first. Never grant based on it.
    // INCLUDE metadata updates so identical document data can later be
    // promoted from cache-only to server-verified in the listener.
    fun reconcile(fromCache: Boolean, data: Any?): Result {
        if (fromCache) return Result(SpecialOccasionOffer(), SpecialOfferSyncStatus.CACHE_ONLY)
        val verified = SpecialOccasionOffer.fromMap(data)
        return Result(verified, if (verified.isLive())
            SpecialOfferSyncStatus.SERVER_LIVE else SpecialOfferSyncStatus.SERVER_NO_OFFER)
    }
}
