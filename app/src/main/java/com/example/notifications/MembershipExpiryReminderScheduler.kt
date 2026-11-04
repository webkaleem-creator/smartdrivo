package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.PreferencesManager
import com.example.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local, one-shot subscription reminders at ~72h, ~24h and ~6h before expiry.
 * AlarmManager reminders are inexact in Doze mode and need no exact-alarm permission.
 * Renewal, logout and account switch cancel the old alarm.
 *
 * Never called from the live accessibility/order-reading path.
 */
object MembershipExpiryReminderScheduler {
    const val ACTION_REMIND = "com.smartdrivo.app.action.PLAN_EXPIRY_REMINDER"
    const val EXTRA_OPEN_RENEWAL = "com.smartdrivo.app.extra.OPEN_PLAN_RENEWAL"

    private const val TAG = "SmartDrivoPlanReminder"
    private const val CHANNEL_ID = "smartdrivo_membership_expiry_v1"
    private const val PREFS_FILE = "smartdrivo_membership_reminder_state_v1"
    private const val REQUEST_CODE = 18231
    private const val NOTIFICATION_ID = 18232
    private const val EXTRA_UID = "reminder_uid"
    private const val EXTRA_EXPIRY = "reminder_expiry"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Subscription reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "SmartDrivo plan renewal reminders with expiry date and time"
                    setShowBadge(true)
                }
            )
        }
    }

    private fun notificationsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return manager?.areNotificationsEnabled() == true
    }

    private fun eligible(prefs: PreferencesManager, p: UserProfile): Boolean =
        prefs.isLoggedIn && p.uid.isNotBlank() && p.isActive &&
            !p.isAdmin && !p.isBlocked && !p.isDeviceBlacklisted &&
            p.isPlanValid

    private fun pending(context: Context, uid: String, expiry: Long): PendingIntent {
        val intent = Intent(context, MembershipExpiryReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_UID, uid)
            putExtra(EXTRA_EXPIRY, expiry)
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelAlarm(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MembershipExpiryReminderReceiver::class.java).apply {
            action = ACTION_REMIND
        }
        val existing = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarm.cancel(existing)
            existing.cancel()
        }
    }

    @Synchronized
    fun refresh(context: Context) {
        val app = context.applicationContext
        val prefs = PreferencesManager.getInstance(app)
        val profile = prefs.userProfile.value
        val store = app.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        cancelAlarm(app)

        if (!eligible(prefs, profile)) {
            (app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                ?.cancel(NOTIFICATION_ID)
            return
        }

        val oldUid = store.getString("uid", "")
        val oldExpiry = store.getLong("expiry", 0L)
        if (oldUid != profile.uid || oldExpiry != profile.planExpireMillis) {
            store.edit()
                .putString("uid", profile.uid)
                .putLong("expiry", profile.planExpireMillis)
                .putInt("last_sent_stage", -1)
                .apply()
            (app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                ?.cancel(NOTIFICATION_ID)
        }
        if (!notificationsEnabled(app)) return

        val last = store.getInt("last_sent_stage", -1)
        val schedule = MembershipExpiryReminderPolicy.next(
            profile.planExpireMillis, System.currentTimeMillis(), last
        ) ?: return

        val alarm = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            val pi = pending(app, profile.uid, profile.planExpireMillis)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, schedule.atMillis, pi)
            } else {
                alarm.set(AlarmManager.RTC_WAKEUP, schedule.atMillis, pi)
            }
            Log.d(TAG, "Renewal reminder armed at stage " + schedule.stage)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to schedule plan reminder: " + e.message)
        }
    }

    /** Called by our non-exported receiver, never accepts an arbitrary UID or expiry. */
    @Synchronized
    fun onAlarm(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_REMIND) return
        val app = context.applicationContext
        val prefs = PreferencesManager.getInstance(app)
        val profile = prefs.userProfile.value
        val intendedUid = intent.getStringExtra(EXTRA_UID)
        val intendedExpiry = intent.getLongExtra(EXTRA_EXPIRY, -1L)
        if (!eligible(prefs, profile) || intendedUid != profile.uid ||
            intendedExpiry != profile.planExpireMillis
        ) {
            refresh(app)
            return
        }

        val now = System.currentTimeMillis()
        val stage = MembershipExpiryReminderPolicy.currentStage(profile.planExpireMillis, now)
        val store = app.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val previous = if (store.getString("uid", "") == profile.uid &&
            store.getLong("expiry", 0L) == profile.planExpireMillis
        ) store.getInt("last_sent_stage", -1) else -1

        if (stage > previous && notificationsEnabled(app)) {
            if (postReminder(app, profile, now)) {
                store.edit()
                    .putString("uid", profile.uid)
                    .putLong("expiry", profile.planExpireMillis)
                    .putInt("last_sent_stage", stage)
                    .commit()
            }
        }
        refresh(app)
    }

    private fun postReminder(context: Context, profile: UserProfile, now: Long): Boolean {
        val timeText = SimpleDateFormat(
            "EEE, dd MMM yyyy 'at' hh:mm a", Locale.ENGLISH
        ).format(Date(profile.planExpireMillis))
        val left = MembershipExpiryReminderPolicy.remainingText(profile.planExpireMillis, now)
        val message = "Your SmartDrivo plan ends in " + left + ". Expires " + timeText + "."

        val renewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_RENEWAL, true)
        }
        val open = PendingIntent.getActivity(
            context, REQUEST_CODE, renewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            createChannel(context)
            val alert = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Subscription expiring soon")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message +
                    "\nTap to open plans and renew your subscription."))
                .setContentIntent(open)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, alert)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Notification was not posted: " + e.message)
            false
        }
    }
}
