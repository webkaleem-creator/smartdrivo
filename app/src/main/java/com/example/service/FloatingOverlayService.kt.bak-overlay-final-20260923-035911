package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.util.Locale

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var remainingSeconds = 300 // 5 minutes

    companion object {
        private const val TAG = "FloatingOverlayService"
        const val ACTION_SHOW_OVERLAY = "com.example.smartdrivo.ACTION_SHOW_OVERLAY"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_PICKUP = "extra_pickup"
        const val EXTRA_PICKUP_DIST = "extra_pickup_dist"
        const val EXTRA_DROP = "extra_drop"
        const val EXTRA_DROP_DIST = "extra_drop_dist"
        const val EXTRA_DROP_AREA = "extra_drop_area"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_PLATFORM = "extra_platform"

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
                action = ACTION_SHOW_OVERLAY
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start FloatingOverlayService: ${e.message}", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    private fun startForegroundNotification() {
        val channelId = "smartdrivo_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SmartDrivo Active Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live ride details overlay on screen"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SmartDrivo Order Overlay")
            .setContentText("Active ride details overlay is displayed")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1001,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "startForeground error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_OVERLAY) {
            val amount = intent.getFloatExtra(EXTRA_AMOUNT, 0f)
            val pickup = intent.getStringExtra(EXTRA_PICKUP) ?: "Pickup Location"
            val pickupDist = intent.getFloatExtra(EXTRA_PICKUP_DIST, 0f)
            val drop = intent.getStringExtra(EXTRA_DROP) ?: "Drop Location"
            val dropDist = intent.getFloatExtra(EXTRA_DROP_DIST, 0f)
            val dropArea = intent.getStringExtra(EXTRA_DROP_AREA) ?: "City Area"
            val time = intent.getStringExtra(EXTRA_TIME) ?: "Just Now"
            val platform = intent.getStringExtra(EXTRA_PLATFORM) ?: "Rapido"

            showPopup(amount, pickup, pickupDist, drop, dropDist, dropArea, time, platform)
        }
        return START_NOT_STICKY
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    private fun dpToPxf(dp: Float): Float = dp * resources.displayMetrics.density

    private data class PlatformStyle(
        val displayName: String,
        val headerBg: Int,
        val badgeBg: Int,
        val badgeText: Int,
        val accentColor: Int,
        val emoji: String
    )

    private fun getPlatformStyle(platform: String): PlatformStyle {
        val p = platform.lowercase()
        return when {
            p.contains("rapido") -> PlatformStyle(
                displayName = "RAPIDO",
                headerBg = 0xFF1C1917.toInt(), // Warm dark Charcoal
                badgeBg = 0xFFF59E0B.toInt(),  // Vibrant Amber Gold
                badgeText = 0xFF000000.toInt(),
                accentColor = 0xFFF59E0B.toInt(),
                emoji = "⚡"
            )
            p.contains("uber") -> PlatformStyle(
                displayName = "UBER",
                headerBg = 0xFF09090B.toInt(), // Deep Jet Black
                badgeBg = 0xFF27272A.toInt(),  // Zinc Badge
                badgeText = 0xFFFFFFFF.toInt(),
                accentColor = 0xFF3B82F6.toInt(),
                emoji = "🚕"
            )
            p.contains("ola") -> PlatformStyle(
                displayName = "OLA",
                headerBg = 0xFF052E16.toInt(), // Deep Forest Green
                badgeBg = 0xFF10B981.toInt(),  // Electric Emerald Green
                badgeText = 0xFF000000.toInt(),
                accentColor = 0xFF10B981.toInt(),
                emoji = "🚖"
            )
            else -> PlatformStyle(
                displayName = platform.uppercase(),
                headerBg = 0xFF0F172A.toInt(),
                badgeBg = 0xFF3B82F6.toInt(),
                badgeText = 0xFFFFFFFF.toInt(),
                accentColor = 0xFF3B82F6.toInt(),
                emoji = "🚗"
            )
        }
    }

    private fun showPopup(
        amount: Float,
        pickup: String,
        pickupDist: Float,
        drop: String,
        dropDist: Float,
        dropArea: String,
        time: String,
        platform: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot show overlay: 'Display over other apps' permission not granted.")
            removeCurrentOverlay()
            return
        }

        detachOverlayViewOnly()

        val style = getPlatformStyle(platform)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayWidth = resources.displayMetrics.widthPixels
        val cardWidth = (displayWidth * 0.82f).toInt()
            .coerceAtLeast(dpToPx(280))
            .coerceAtMost(displayWidth - dpToPx(20))

        val params = WindowManager.LayoutParams(
            cardWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
                        gravity = Gravity.TOP or Gravity.START
            x = ((displayWidth - cardWidth) / 2).coerceAtLeast(0)
            y = dpToPx(78)
            windowAnimations = android.R.style.Animation_Translucent
        }        // Compact white floating card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPxf(16f)
                setColor(Color.WHITE)
                setStroke(dpToPx(1), 0xFF90CAF9.toInt())
            }
            background = bgDrawable
            elevation = dpToPxf(10f)
            clipToOutline = true
            setPadding(0, 0, 0, dpToPx(8))
        }
        // --- COMPACT BLUE HEADER ---
        val headerView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFF1E88E5.toInt())
            }

            setPadding(dpToPx(10), dpToPx(4), dpToPx(7), dpToPx(4))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(46)
            )
        }

        val platformBadge = TextView(this).apply {
            text = "✅ ${style.displayName} ACCEPTED"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine(true)
        }
        headerView.addView(platformBadge)

        headerView.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })

        val countdownBadge = TextView(this).apply {
            text = "⏳ 05:00"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

            background = GradientDrawable().apply {
                cornerRadius = dpToPxf(18f)
                setColor(0x22FFFFFF)
            }
        }
        headerView.addView(countdownBadge)

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            contentDescription = "Dismiss Overlay"

            val size = dpToPx(38)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = dpToPx(5)
            }

            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x22FFFFFF)
            }

            background = RippleDrawable(
                ColorStateList.valueOf(0x44FFFFFF),
                circle,
                null
            )

            isClickable = true
            isFocusable = true
            setOnClickListener { removeCurrentOverlay() }
        }

        headerView.addView(closeBtn)
        card.addView(headerView)
        // --- COMPACT FARE / PICKUP / DROP ---
        val metrics = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(6))
        }

        fun metricItem(
            icon: String,
            label: String,
            value: String,
            weight: Float
        ): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    weight
                )

                addView(TextView(this@FloatingOverlayService).apply {
                    text = icon
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, 0, dpToPx(5), 0)
                })

                addView(LinearLayout(this@FloatingOverlayService).apply {
                    orientation = LinearLayout.VERTICAL

                    if(label.isNotBlank()){
                        addView(TextView(this@FloatingOverlayService).apply {
                            text = label
                            textSize = 10f
                            setTextColor(0xFF4B5563.toInt())
                            setSingleLine(true)
                        })
                    }

                    addView(TextView(this@FloatingOverlayService).apply {
                        text = value
                        textSize = if(label.isBlank()) 16f else 13.5f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(0xFF111827.toInt())
                        setSingleLine(true)
                    })
                })
            }
        }

        val fareValue =
            if(amount > 0f) "₹${amount.toInt()}" else "₹--"

        val pickupValue =
            if(pickupDist > 0f)
                String.format(Locale.US, "%.1f km", pickupDist)
            else
                "Nearby"

        val dropValue =
            if(dropDist > 0f)
                String.format(Locale.US, "%.1f km", dropDist)
            else
                "N/A"

        metrics.addView(metricItem("₹", "", fareValue, 0.9f))

        fun dividerView(): View {
            return View(this).apply {
                setBackgroundColor(0xFFE5E7EB.toInt())

                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(1),
                    dpToPx(34)
                ).apply {
                    marginStart = dpToPx(4)
                    marginEnd = dpToPx(4)
                }
            }
        }

        metrics.addView(dividerView())
        metrics.addView(metricItem("📍", "Pickup", pickupValue, 1.1f))
        metrics.addView(dividerView())
        metrics.addView(metricItem("🔴", "Drop", dropValue, 1.05f))

        card.addView(metrics)

        card.addView(View(this).apply {
            setBackgroundColor(0xFFE5E7EB.toInt())

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                marginStart = dpToPx(10)
                marginEnd = dpToPx(10)
            }
        })

        val cleanDrop = drop.trim()

        val finalDrop = when {
            cleanDrop.isBlank() ||
            cleanDrop.equals("Drop Location", true) ||
            cleanDrop.equals("Detected Drop Location", true) ->
                "Location unavailable"

            cleanDrop.startsWith("Drop:", true) ->
                cleanDrop.substringAfter(":").trim()

            cleanDrop.startsWith("Drop -", true) ->
                cleanDrop.substringAfter("-").trim()

            else -> cleanDrop
        }

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dpToPx(10), dpToPx(7), dpToPx(10), dpToPx(2))
        }

        addressRow.addView(TextView(this).apply {
            text = "📍"
            textSize = 14f
            setPadding(0, 0, dpToPx(6), 0)
        })

        addressRow.addView(TextView(this).apply {
            text = "Drop: $finalDrop"
            textSize = 11.5f
            setTextColor(0xFF374151.toInt())
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })

        card.addView(addressRow)

        // Drag listener so driver can reposition the card anywhere on screen (both X and Y axes)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val dragListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    params.x = (initialX + deltaX).coerceAtLeast(0)
                    params.y = (initialY + deltaY).coerceAtLeast(0)
                    try {
                        windowManager?.updateViewLayout(overlayView, params)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error moving overlay: ${e.message}")
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    true
                }
                else -> false
            }
        }
        card.setOnTouchListener(dragListener)

        // Container
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            addView(card)
        }

        overlayView = container

        try {
            windowManager?.addView(container, params)
            Log.i(TAG, "Floating overlay displayed for $platform ($pickupDist km -> $dropDist km)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view to WindowManager: ${e.message}", e)
            removeCurrentOverlay()
            return
        }

        // --- 3. 5-MINUTE LIVE COUNTDOWN TIMER (300 seconds) ---
        remainingSeconds = 300
        countdownRunnable?.let { handler.removeCallbacks(it) }

        countdownRunnable = object : Runnable {
            override fun run() {
                remainingSeconds--
                if (remainingSeconds > 0) {
                    val m = remainingSeconds / 60
                    val s = remainingSeconds % 60
                    val timeText = "%02d:%02d".format(m, s)
                    countdownBadge.text = "⏳ $timeText"
                    handler.postDelayed(this, 1000L)
                } else {
                    countdownBadge.text = "⏳ 00:00"
                    removeCurrentOverlay()
                }
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000L)
    }

    private fun detachOverlayViewOnly() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null

        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing overlay view: ${e.message}")
            }
            overlayView = null
        }
    }

    private fun removeCurrentOverlay() {
        detachOverlayViewOnly()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        removeCurrentOverlay()
        super.onDestroy()
    }
}
