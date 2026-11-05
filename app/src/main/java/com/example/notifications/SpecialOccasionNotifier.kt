package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.SpecialOccasionOffer

object SpecialOccasionNotifier {
    private const val CHANNEL = "smartdrivo_special_offers"
    private const val ID = 23491
    fun show(context: Context, offer: SpecialOccasionOffer): Boolean {
        if (!offer.isLive()) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "Special occasion offers", NotificationManager.IMPORTANCE_DEFAULT
            ))
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(context, ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try {
            manager.notify(ID, NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(offer.title)
                .setContentText(offer.durationLabel() + " • " + offer.description)
                .setStyle(NotificationCompat.BigTextStyle().bigText(offer.description))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build())
            true
        } catch (_: SecurityException) { false }
    }
}
