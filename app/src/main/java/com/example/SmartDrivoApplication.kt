package com.example

import android.app.Application
import com.example.util.NotificationHelper

class SmartDrivoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Create the notification channel on Application start
        NotificationHelper.createNotificationChannel(this)
    }
}
