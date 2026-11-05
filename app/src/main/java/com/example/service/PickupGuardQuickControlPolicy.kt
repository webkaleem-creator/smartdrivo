package com.example.service

/** Pure policy for owner-only Quick Controls. No platform order parsing or clicks. */
internal object PickupGuardQuickControlPolicy {
    const val LONG_PRESS_MS = 500L
    val QUICK_PICKUP_LIMITS = listOf(0.4f, 0.6f, 0.8f)
    val PAUSE_MINUTES = listOf(5, 10, 15)

    fun validPickup(value: Float): Boolean =
        value.isFinite() && value > 0f && value <= 30f

    fun isPaused(untilMillis: Long, nowMillis: Long): Boolean =
        untilMillis > 0L && nowMillis < untilMillis

    /** Never restart automatically after logout, lost owner claim, banking or removed permissions. */
    fun mayRestoreMaster(
        wasOn: Boolean,
        currentMasterOn: Boolean,
        ownerAuthorized: Boolean,
        serviceRunning: Boolean,
        bankingActive: Boolean,
        overlayAllowed: Boolean
    ): Boolean = wasOn && !currentMasterOn && ownerAuthorized &&
        serviceRunning && !bankingActive && overlayAllowed
}
