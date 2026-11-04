package com.example.auth

/** Atomic verification and refresh ownership; wall-clock rollback fails closed. */
class OwnerClaimLease {
    data class Request(val uid: String, val generation: Long)
    private var uid: String? = null
    private var verifiedAt = 0L
    private var generation = 0L
    private var request: Request? = null
    private var requestedAt = 0L
    @Synchronized fun valid(currentUid: String?, now: Long): Boolean =
        uid != null && uid == currentUid && verifiedAt > 0 && now >= verifiedAt && now - verifiedAt < 600_000L
    @Synchronized fun begin(currentUid: String, now: Long): Request? {
        if (uid == currentUid && valid(currentUid, now) && now - verifiedAt < 300_000L) return null
        if (request?.uid == currentUid && now >= requestedAt && now-requestedAt < 60_000L) return null
        return Request(currentUid, ++generation).also { request = it; requestedAt = now }
    }
    @Synchronized fun complete(ticket: Request, verified: Boolean, currentUid: String?, now: Long): Boolean {
        if (request != ticket || currentUid != ticket.uid) return false
        request = null
        if (verified) { uid = ticket.uid; verifiedAt = now } else { uid = null; verifiedAt = 0L }
        return verified
    }
    @Synchronized fun clear() { generation++; uid = null; verifiedAt = 0L; request = null }
}
