package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.PreferencesManager
import com.example.service.SmartDrivoAccessibilityService

/**
 * User-invoked Accessibility OFF workflow. Does not bypass security checks,
 * automate Settings, or try to turn the permission back on silently.
 */
object BankingModeController {
    private const val TAG = "BankingMode"
    private const val PREF_FILE = "smartdrivo_banking_mode"
    private const val KEY_ACTIVE = "banking_mode_active"
    private const val KEY_RESTORE_MASTER = "restore_master_after_banking"
    private const val CHANNEL_ID = "smartdrivo_banking_mode_reminder"
    private const val NOTIFICATION_ID = 28201

    data class StartResult(val saved: Boolean, val requestedServiceDisable: Boolean)

    fun isActive(context: Context): Boolean =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACTIVE, false)

    // Tested independently from Firebase and the live AccessibilityService.
    internal fun persistStart(context: Context, masterWasActive: Boolean): Boolean {
        val storage = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        if (storage.getBoolean(KEY_ACTIVE, false)) return true
        return storage.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_RESTORE_MASTER, masterWasActive)
            .commit()
    }

    internal fun previousMasterWasActive(context: Context): Boolean =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_RESTORE_MASTER, false)

    internal fun persistFinish(context: Context): Boolean =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit().remove(KEY_ACTIVE).remove(KEY_RESTORE_MASTER).commit()

    /** Keep this snapshot even if Android kills the app process. */
    fun enter(context: Context, prefs: PreferencesManager): StartResult {
        if (!persistStart(context, prefs.appSettings.value.isAutoAcceptActive)) {
            return StartResult(false, false)
        }

        prefs.isPendingAutoAcceptActivation = false
        prefs.setAutoAcceptActive(false)
        // The service invokes Android's official disableSelf() method.
        // When it is not connected, user should confirm OFF in Settings.
        val requested = SmartDrivoAccessibilityService.requestBankingModeDisable()
        showReminder(context)
        return StartResult(true, requested)
    }

    /** Explicitly finish banking, then ask user to enable permission manually. */
    fun finishAndOpenSettings(context: Context, prefs: PreferencesManager): Boolean {
        val storage = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        if (!storage.getBoolean(KEY_ACTIVE, false)) {
            PermissionHelper.openSmartDrivoAccessibilitySettings(context)
            return true
        }
        val previouslyOn = previousMasterWasActive(context)
        if (!persistFinish(context)) return false

        cancelReminder(context)
        SmartDrivoAccessibilityService.resumeFromBankingMode()
        val hasAccessibility = PermissionHelper.isAccessibilityPermissionGranted(context)
        val hasOverlay = PermissionHelper.isOverlayPermissionGranted(context)
        if (previouslyOn) {
            if (hasAccessibility && hasOverlay) {
                // The user has ALREADY re-enabled Accessibility in Settings.
                // Restoring the app's master switch is not an OS permission grant.
                prefs.isPendingAutoAcceptActivation = false
                prefs.setAutoAcceptActive(true)
            } else {
                // MainActivity will restore the previous master switch only
                // after the user manually enables the missing permission.
                prefs.isPendingAutoAcceptActivation = true
            }
        }
        if (!hasAccessibility) {
            PermissionHelper.openSmartDrivoAccessibilitySettings(context)
        } else if (!hasOverlay) {
            PermissionHelper.openOverlaySettings(context)
        }
        return true
    }

    private fun showReminder(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Banking Mode reminder",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "Reminder to enable SmartDrivo after banking" }
                )
            }
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pending = PendingIntent.getActivity(
                context, 28201, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("SmartDrivo Banking Mode")
                .setContentText("Monitoring paused. Tap for Accessibility Settings.")
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .build()
            // Notification may not appear on Android 13+ without permission.
            if (PermissionHelper.hasNotificationPermission(context)) {
                nm.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Banking reminder unavailable: " + e.javaClass.simpleName)
        }
    }

    private fun cancelReminder(context: Context) {
        try {
            (context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
