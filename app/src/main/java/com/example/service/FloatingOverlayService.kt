package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.R
import java.util.Locale

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        if (action == ACTION_DISMISS) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        val amount = intent.getFloatExtra(EXTRA_AMOUNT, 0f)
        val pickup = intent.getStringExtra(EXTRA_PICKUP) ?: ""
        val pickupDist = intent.getFloatExtra(EXTRA_PICKUP_DIST, 0f)
        val drop = intent.getStringExtra(EXTRA_DROP) ?: ""
        val dropDist = intent.getFloatExtra(EXTRA_DROP_DIST, 0f)
        val dropArea = intent.getStringExtra(EXTRA_DROP_AREA) ?: ""
        val time = intent.getStringExtra(EXTRA_TIME) ?: ""
        val platform = intent.getStringExtra(EXTRA_PLATFORM) ?: ""

        displayOverlay(amount, pickup, pickupDist, drop, dropDist, dropArea, time, platform)
        return START_NOT_STICKY
    }

    private fun displayOverlay(
        amount: Float,
        pickup: String,
        pickupDist: Float,
        drop: String,
        dropDist: Float,
        dropArea: String,
        time: String,
        platform: String
    ) {
        removeOverlay()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        try {
            val tv = TextView(this).apply {
                setBackgroundColor(0xF01E293B.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(32, 24, 32, 24)
                textSize = 14f
                text = "⚡ $platform Order Accepted!\n₹${amount.toInt()} | Pickup: ${String.format(Locale.ENGLISH, "%.1f", pickupDist)}km | Drop: $dropArea (${String.format(Locale.ENGLISH, "%.1f", dropDist)}km)"
                setOnClickListener {
                    removeOverlay()
                    stopSelf()
                }
            }
            overlayView = tv
            windowManager?.addView(overlayView, params)

            dismissRunnable = Runnable {
                removeOverlay()
                stopSelf()
            }
            handler.postDelayed(dismissRunnable!!, 6000)
        } catch (e: Exception) {
            // Permission might have been revoked or overlay cannot be displayed
        }
    }

    private fun removeOverlay() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // View not attached
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    companion object {
        private const val ACTION_DISMISS = "com.example.service.ACTION_DISMISS"
        private const val EXTRA_AMOUNT = "extra_amount"
        private const val EXTRA_PICKUP = "extra_pickup"
        private const val EXTRA_PICKUP_DIST = "extra_pickup_dist"
        private const val EXTRA_DROP = "extra_drop"
        private const val EXTRA_DROP_DIST = "extra_drop_dist"
        private const val EXTRA_DROP_AREA = "extra_drop_area"
        private const val EXTRA_TIME = "extra_time"
        private const val EXTRA_PLATFORM = "extra_platform"

        fun show(
            context: Context,
            amount: Float,
            pickup: String,
            pickupDist: Float,
            drop: String,
            dropDist: Float,
            dropArea: String,
            time: String,
            platform: String
        ) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_PICKUP, pickup)
                putExtra(EXTRA_PICKUP_DIST, pickupDist)
                putExtra(EXTRA_DROP, drop)
                putExtra(EXTRA_DROP_DIST, dropDist)
                putExtra(EXTRA_DROP_AREA, dropArea)
                putExtra(EXTRA_TIME, time)
                putExtra(EXTRA_PLATFORM, platform)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Background start restriction
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_DISMISS
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
