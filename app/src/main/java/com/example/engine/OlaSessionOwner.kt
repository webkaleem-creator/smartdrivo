package com.example.engine

/** Ola-only ownership. Old callbacks cannot release a replacement session. */
class OlaSessionOwner {
    class Session internal constructor(val recordId: String, val userId: String?)
    private var active: Session? = null

    @Synchronized fun begin(recordId: String, userId: String? = null): Session? {
        if (active != null) return null
        return Session(recordId, userId).also { active = it }
    }
    @Synchronized fun isCurrent(session: Session, userId: String? = null): Boolean =
        active === session && (userId == null || session.userId == userId)
    @Synchronized fun busy(): Boolean = active != null
    @Synchronized fun finish(session: Session): Boolean {
        if (active !== session) return false
        active = null
        return true
    }
    @Synchronized fun cancel() { active = null }
}
