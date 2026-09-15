package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PreferencesManager(context)
            if (prefs.appSettings.value.autostartOnBoot) {
                Log.i("BootReceiver", "SmartDrivo autostart triggered on boot")
                // Background setup when device finishes booting
            }
        }
    }
}
