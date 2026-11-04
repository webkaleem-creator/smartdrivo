package com.example.service

import android.app.Application
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import com.example.auth.PickupGuardOwnerAccess
import com.example.data.PreferencesManager
import com.example.model.UserProfile
import com.example.util.BankingModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * App-process coordinator for the owner-only bubble. It never reads ride windows,
 * changes Auto Accept, or accesses the Rapido click/decision path.
 *
 * Uses the existing Accessibility Service and overlay permission: no additional
 * foreground service, manifest entry, or battery-draining screen scanner.
 */
internal object PickupGuardBubbleRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private lateinit var prefs: PreferencesManager
    private var bubble: PickupGuardBubbleOverlay? = null
    private var bubbleUid: String? = null
    private var checkedOwnerUid: String? = null
    private var lastOwnerCheckAt = 0L

    fun start(app: Application) {
        if (started) return
        started = true
        prefs = PreferencesManager.getInstance(app)

        scope.launch {
            prefs.appSettings.collectLatest { sync(app) }
        }
        scope.launch {
            prefs.pickupGuardSettings.collectLatest { sync(app) }
        }
        scope.launch {
            prefs.userProfile.collectLatest { sync(app) }
        }
        scope.launch {
            prefs.isLoggedInFlow.collectLatest { sync(app) }
        }
        // An Accessibility Service may be enabled/disabled while the UI is in
        // the background. Recheck its cached lifecycle flag without scanning UI.
        // This also expires owner verification, catches overlay-permission changes
        // and handles Banking Mode without touching order processing.
        scope.launch {
            while (isActive) {
                sync(app)
                delay(2_000L)
            }
        }
    }

    private fun masterReady(app: Application): Boolean =
        prefs.appSettings.value.isAutoAcceptActive &&
            prefs.isAutoAcceptEnabled &&
            SmartDrivoAccessibilityService.isServiceRunning &&
            !BankingModeController.isActive(app)

    private fun overlayAllowed(app: Application): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(app)

    private fun ownerProfileEligible(profile: UserProfile): Boolean =
        profile.uid.isNotBlank() && profile.isAdmin && profile.isActive &&
            !profile.isBlocked && !profile.isDeviceBlacklisted && prefs.isLoggedIn

    private fun refreshOwnerClaimIfDue(app: Application, profile: UserProfile, verified: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (checkedOwnerUid != profile.uid) {
            checkedOwnerUid = profile.uid
            lastOwnerCheckAt = 0L
        }
        val nextCheck = if (verified) 7 * 60_000L else 60_000L
        if (lastOwnerCheckAt != 0L && now >= lastOwnerCheckAt &&
            now - lastOwnerCheckAt < nextCheck) return

        lastOwnerCheckAt = now
        // Firebase refresh is asynchronous. Failure clears the owner authorization
        // and the next sync hides the bubble (fail closed).
        PickupGuardOwnerAccess.refresh(profile) {
            sync(app)
        }
    }

    private fun sync(app: Application) {
        if (!started || !::prefs.isInitialized) return

        val profile = prefs.userProfile.value
        val ready = masterReady(app) && ownerProfileEligible(profile) && overlayAllowed(app)
        if (!ready) {
            hideBubble()
            return
        }

        val verified = PickupGuardOwnerAccess.authorized(profile)
        refreshOwnerClaimIfDue(app, profile, verified)
        if (!PickupGuardBubblePolicy.shouldShow(
                masterOn = masterReady(app),
                loggedIn = prefs.isLoggedIn,
                ownerAuthorized = PickupGuardOwnerAccess.authorized(profile),
                bankingActive = BankingModeController.isActive(app),
                overlayAllowed = overlayAllowed(app)
            )) {
            hideBubble()
            return
        }

        if (bubbleUid != profile.uid) {
            hideBubble()
            bubbleUid = profile.uid
            bubble = PickupGuardBubbleOverlay(app, profile.uid) { toggle(app) }
        }
        bubble?.show(prefs.pickupGuardSettings.value.enabled)
    }

    private fun toggle(app: Application) {
        val profile = prefs.userProfile.value
        if (!masterReady(app) || !ownerProfileEligible(profile) ||
            !overlayAllowed(app) || !PickupGuardOwnerAccess.authorized(profile)) {
            hideBubble()
            return
        }
        val saved = prefs.pickupGuardSettings.value
        val updated = saved.copy(enabled = !saved.enabled)
        if (prefs.savePickupGuardSettings(updated)) {
            bubble?.show(updated.enabled)
            Toast.makeText(app, if (updated.enabled) "Pickup Guard ON" else "Pickup Guard OFF",
                Toast.LENGTH_SHORT).show()
        } else {
            hideBubble()
        }
    }

    private fun hideBubble() {
        bubble?.hide()
        bubble = null
        bubbleUid = null
    }
}
