package com.example.service

import android.app.Application
import android.content.Context
import android.content.Intent
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
 * Owner-only Quick Controls coordinator. Uses existing master settings/broadcast:
 * no independent order processor, click route, new permission, or background service.
 */
internal object PickupGuardBubbleRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private lateinit var prefs: PreferencesManager
    private var bubble: PickupGuardBubbleOverlay? = null
    private var bubbleUid: String? = null
    private var checkedOwnerUid: String? = null
    private var lastOwnerCheckAt = 0L
    
    // Per-owner temporary Pause state. The timer is observed on the existing 2-second loop.
    private const val CONTROL_PREFS = "smartdrivo_owner_pg_quick_controls_v1"
    private const val PAUSE_UNTIL = "pause_until_"
    private const val PAUSE_RESTORE_MAIN = "pause_restore_main_"

    fun start(app: Application) {
        if (started) return
        started = true
        prefs = PreferencesManager.getInstance(app)
        scope.launch { prefs.appSettings.collectLatest { sync(app) } }
        scope.launch { prefs.pickupGuardSettings.collectLatest { sync(app) } }
        scope.launch { prefs.userProfile.collectLatest { sync(app) } }
        scope.launch { prefs.isLoggedInFlow.collectLatest { sync(app) } }
        scope.launch { prefs.orderHistory.collectLatest { sync(app) } }
        scope.launch {
            while (isActive) {
                sync(app)
                delay(2_000L)
            }
        }
    }

    private fun controlStore(app: Application) =
        app.getSharedPreferences(CONTROL_PREFS, Context.MODE_PRIVATE)

    private fun pauseUntil(app: Application, uid: String): Long =
        if (uid.isBlank()) 0L else controlStore(app).getLong(PAUSE_UNTIL + uid, 0L)

    private fun clearPause(app: Application, uid: String) {
        if (uid.isBlank()) return
        controlStore(app).edit()
            .remove(PAUSE_UNTIL + uid)
            .remove(PAUSE_RESTORE_MAIN + uid).apply()
    }

    /** A paused session never silently enables automation while still paused. */
    private fun resumeExpiredPause(app: Application, profile: UserProfile) {
        val uid = profile.uid
        if (uid.isBlank()) return
        val until = pauseUntil(app, uid)
        if (until <= 0L) return
        if (PickupGuardQuickControlPolicy.isPaused(until, System.currentTimeMillis())) {
            // A manual Home switch ON overrides/cancels a pause; no forced OFF race.
            if (prefs.appSettings.value.isAutoAcceptActive) clearPause(app, uid)
            return
        }
        val wasMainOn = controlStore(app).getBoolean(PAUSE_RESTORE_MAIN + uid, false)
        clearPause(app, uid) // clear first, so a Flow callback cannot run it twice
        if (PickupGuardQuickControlPolicy.mayRestoreMaster(
                wasOn = wasMainOn,
                currentMasterOn = prefs.appSettings.value.isAutoAcceptActive,
                ownerAuthorized = PickupGuardOwnerAccess.authorized(profile) &&
                    ownerProfileEligible(profile),
                serviceRunning = SmartDrivoAccessibilityService.isServiceRunning,
                bankingActive = BankingModeController.isActive(app),
                overlayAllowed = overlayAllowed(app)
            )) {
            setMaster(app, true)
        }
    }

    private fun serviceReady(app: Application): Boolean =
        SmartDrivoAccessibilityService.isServiceRunning &&
            !BankingModeController.isActive(app)

    private fun overlayAllowed(app: Application): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(app)

    private fun ownerProfileEligible(profile: UserProfile): Boolean =
        profile.uid.isNotBlank() && profile.isAdmin && profile.isActive &&
            !profile.isBlocked && !profile.isDeviceBlacklisted && prefs.isLoggedIn

    private fun canControl(app: Application): Boolean {
        val profile = prefs.userProfile.value
        return serviceReady(app) && overlayAllowed(app) &&
            ownerProfileEligible(profile) && PickupGuardOwnerAccess.authorized(profile)
    }

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
        PickupGuardOwnerAccess.refresh(profile) { sync(app) }
    }

    private fun mostRecentReason(): String {
        val recent = prefs.orderHistory.value.maxByOrNull { it.timestamp }
            ?: return "No recent saved order reason"
        val reason = recent.decisionReasonText.ifBlank { recent.reason }
            .ifBlank { recent.decisionReasonCode }
            .ifBlank { "No reason recorded" }
        return recent.platform.name + " • " + recent.status.name + " • " + reason
    }

    private fun sync(app: Application) {
        if (!started || !::prefs.isInitialized) return
        val profile = prefs.userProfile.value
        if (!ownerProfileEligible(profile) || !serviceReady(app) || !overlayAllowed(app)) {
            hideBubble()
            return
        }

        val verified = PickupGuardOwnerAccess.authorized(profile)
        refreshOwnerClaimIfDue(app, profile, verified)
        if (!PickupGuardBubblePolicy.shouldShow(
                serviceRunning = SmartDrivoAccessibilityService.isServiceRunning,
                loggedIn = prefs.isLoggedIn,
                ownerAuthorized = PickupGuardOwnerAccess.authorized(profile),
                bankingActive = BankingModeController.isActive(app),
                overlayAllowed = overlayAllowed(app)
            )) {
            hideBubble()
            return
        }

        resumeExpiredPause(app, profile)

        if (bubbleUid != profile.uid) {
            hideBubble()
            bubbleUid = profile.uid
            bubble = PickupGuardBubbleOverlay(
                app, profile.uid,
                onToggleGuard = { toggleGuard(app) },
                onLongPressMenu = {
                    if (canControl(app)) {
                        bubble?.openMenu()
                    } else hideBubble()
                },
                onToggleMainAuto = { toggleMain(app) },
                onPickupLimit = { savePickupLimit(app, it) },
                onPause = { pause(app, it) },
                onResume = { resume(app) },
                onEmergencyStop = { emergencyStop(app) }
            )
        }
        val guard = prefs.pickupGuardSettings.value
        bubble?.show(guard.enabled, prefs.appSettings.value.isAutoAcceptActive,
            guard.maxPickupKm, pauseUntil(app, profile.uid), mostRecentReason())
    }

    private fun setMaster(app: Application, enabled: Boolean) {
        prefs.isPendingAutoAcceptActivation = false
        // Same persisted master setting as the Home switch. Don't overwrite filters.
        prefs.setAutoAcceptActive(enabled)
        app.sendBroadcast(Intent("com.example.TOGGLE_CHANGED")
            .setPackage(app.packageName).putExtra("enabled", enabled))
    }

    private fun toggleGuard(app: Application) {
        if (!canControl(app)) { hideBubble(); return }
        val saved = prefs.pickupGuardSettings.value
        val updated = saved.copy(enabled = !saved.enabled)
        if (prefs.savePickupGuardSettings(updated)) {
            sync(app)
            Toast.makeText(app, if (updated.enabled) "Pickup Guard ON" else "Pickup Guard OFF",
                Toast.LENGTH_SHORT).show()
        } else hideBubble()
    }

    private fun toggleMain(app: Application) {
        if (!canControl(app)) { hideBubble(); return }
        val uid = prefs.userProfile.value.uid
        clearPause(app, uid)
        val newValue = !prefs.appSettings.value.isAutoAcceptActive
        setMaster(app, newValue)
        sync(app)
        Toast.makeText(app,
            if (newValue) "Main Auto Accept ON (Guard stays manual)"
            else "Main Auto Accept OFF",
            Toast.LENGTH_SHORT).show()
    }

    private fun savePickupLimit(app: Application, km: Float) {
        if (!canControl(app) || !PickupGuardQuickControlPolicy.validPickup(km)) {
            hideBubble()
            return
        }
        val latest = prefs.pickupGuardSettings.value
        if (prefs.savePickupGuardSettings(latest.copy(maxPickupKm = km))) {
            sync(app)
            Toast.makeText(app, "Pickup Guard max: " + km + " km", Toast.LENGTH_SHORT).show()
        } else hideBubble()
    }

    private fun pause(app: Application, minutes: Int) {
        if (!canControl(app) || minutes !in PickupGuardQuickControlPolicy.PAUSE_MINUTES) {
            hideBubble()
            return
        }
        val uid = prefs.userProfile.value.uid
        val restoreMain = prefs.appSettings.value.isAutoAcceptActive
        val until = System.currentTimeMillis() + minutes.toLong() * 60_000L
        val stored = controlStore(app).edit()
            .putLong(PAUSE_UNTIL + uid, until)
            .putBoolean(PAUSE_RESTORE_MAIN + uid, restoreMain)
            .commit()
        if (!stored) {
            Toast.makeText(app, "Pause could not be saved", Toast.LENGTH_SHORT).show()
            return
        }
        setMaster(app, false) // Existing service immediately stops pending work on master OFF.
        sync(app)
        Toast.makeText(app, "Automation paused for " + minutes + " min",
            Toast.LENGTH_SHORT).show()
    }

    private fun resume(app: Application) {
        if (!canControl(app)) { hideBubble(); return }
        val uid = prefs.userProfile.value.uid
        if (pauseUntil(app, uid) <= 0L) return
        val wasMainOn = controlStore(app).getBoolean(PAUSE_RESTORE_MAIN + uid, false)
        clearPause(app, uid)
        if (wasMainOn) setMaster(app, true)
        sync(app)
        Toast.makeText(app, "Pause ended", Toast.LENGTH_SHORT).show()
    }

    private fun emergencyStop(app: Application) {
        if (!canControl(app)) { hideBubble(); return }
        val uid = prefs.userProfile.value.uid
        clearPause(app, uid) // Never auto-resume after Emergency Stop.
        setMaster(app, false) // Stops in-flight monitoring via existing receiver.
        val guard = prefs.pickupGuardSettings.value
        if (guard.enabled) prefs.savePickupGuardSettings(guard.copy(enabled = false))
        bubble?.hideMenu()
        sync(app)
        Toast.makeText(app, "EMERGENCY STOP: Main Auto OFF, Pickup Guard OFF",
            Toast.LENGTH_LONG).show()
    }

    private fun hideBubble() {
        bubble?.hide()
        bubble = null
        bubbleUid = null
    }
}
