package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android wakes the app for this explicit, non-exported reminder broadcast.
 * Reads only local membership state; no network or accessibility UI scanning.
 */
class MembershipExpiryReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        MembershipExpiryReminderScheduler.onAlarm(context, intent)
    }
}
