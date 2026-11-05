package com.example.service

/**
 * Owner-only floating control. The policy is separate from Rapido's live order path.
 * Permission and Firebase owner-claim checks are performed by the Accessibility Service.
 */
internal object PickupGuardBubblePolicy {
    fun shouldShow(
        masterOn: Boolean,
        serviceRunning: Boolean,
        loggedIn: Boolean,
        ownerAuthorized: Boolean,
        bankingActive: Boolean,
        overlayAllowed: Boolean,
        hiddenByOwner: Boolean = false
    ): Boolean = masterOn && serviceRunning && loggedIn && ownerAuthorized && !bankingActive && overlayAllowed && !hiddenByOwner

    fun snapX(x: Int, left: Int, right: Int): Int {
        if (left >= right) return left
        val middle = left + (right - left) / 2
        return if (x <= middle) left else right
    }
}
