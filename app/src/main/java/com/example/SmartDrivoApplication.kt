package com.example

import android.app.Application
import com.example.util.NotificationHelper

class SmartDrivoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Create the notification channel on Application start
        NotificationHelper.createNotificationChannel(this)
        com.example.notifications.MembershipExpiryReminderScheduler.createChannel(this)
        com.example.notifications.MembershipExpiryReminderScheduler.refresh(this)
        // Owner-only Pickup Guard bubble; independent of live ride parsing.
        com.example.service.PickupGuardBubbleRuntime.start(this)
    }
}
