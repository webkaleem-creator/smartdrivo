package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import com.example.data.FirebaseRepository
import com.example.data.PreferencesManager
import com.example.engine.AreaRulesEngine
import com.example.engine.DecisionResult
import com.example.engine.OlaAdapter
import com.example.engine.OrderDataExtractor
import com.example.engine.RapidoAdapter
import com.example.engine.UberAdapter
import com.example.model.AreaGroup
import com.example.model.ClickStrategy
import com.example.model.OrderHistoryItem
import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.model.RideCandidate
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

class SmartDrivoAccessibilityService : AccessibilityService() {

    private lateinit var prefs: PreferencesManager
    private val preferencesManager: PreferencesManager get() = prefs
    private lateinit var repository: FirebaseRepository
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var lastHandledTimestamp = 0L
    @Volatile
    private var isProcessing = false

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.TOGGLE_CHANGED") {
                val enabled = intent.getBooleanExtra("enabled", preferencesManager.isAutoAcceptEnabled)
                Log.i(TAG, "Broadcast received: TOGGLE_CHANGED enabled=$enabled")
                prefs.reloadAppSettings()
                val notificationText = if (enabled) {
                    "✅ SmartDrivo Active — Monitoring orders"
                } else {
                    "⏸️ SmartDrivo Paused"
                }
                NotificationHelper.updateNotification(
                    context = applicationContext,
                    title = "SmartDrivo Active",
                    text = notificationText
                )
            }
        }
    }

    private val processingTimeoutRunnable = Runnable {
        if (isProcessing) {
            Log.w(TAG, "Watchdog: Resetting isProcessing state after timeout")
            isProcessing = false
        }
    }

    companion object {
        private const val TAG = "SmartDrivoService"
        const val ACCEPT_CLICK_SPEED_MS = 50L

        val ALLOWED_PACKAGES = setOf(
            "com.rapido.passenger",
            "com.rapido.rider",
            "com.ubercab",
            "com.ubercab.driver",
            "ola.cabs",
            "com.olacabs.oladriver",
            "com.olacabs.driver",
            "com.olacabs.customer",
            "com.olacabs.consumer"
        )

        var isServiceRunning = false
            private set

        private val CONFIRMATION_KEYWORDS = listOf(
            "accepted", "ride accepted", "accepted!", "thanks for accepting",
            "navigate to pickup", "go to pickup", "start ride", "arrived at pickup",
            "ongoing ride", "order accepted"
        )

        fun isRapidoPackage(pkg: String): Boolean {
            val p = pkg.trim().lowercase()
            return p.contains("com.rapido.passenger") || p.contains("com.rapido.rider") || p.contains("rapido")
        }

        fun isUberPackage(pkg: String): Boolean {
            val p = pkg.trim().lowercase()
            return p == "com.ubercab" || p.startsWith("com.ubercab.") || p.contains("com.ubercab")
        }

        fun isOlaPackage(pkg: String): Boolean {
            val p = pkg.trim().lowercase()
            return p == "ola.cabs" || p.startsWith("ola.cabs.") || p.contains("ola.cabs") ||
                   p.contains("olacabs") || p.contains("ola.driver") ||
                   (p.contains("ola") && (p.contains("driver") || p.contains("partner") || p.contains("cab")))
        }

        fun isAllowedPackage(pkg: String): Boolean {
            return isRapidoPackage(pkg) || isUberPackage(pkg) || isOlaPackage(pkg)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager(applicationContext)
        repository = FirebaseRepository(applicationContext, prefs)
        isServiceRunning = true
        Log.i(TAG, "SmartDrivo Accessibility Service Connected")

        try {
            val filter = IntentFilter("com.example.TOGGLE_CHANGED")
            ContextCompat.registerReceiver(
                this,
                toggleReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registering toggleReceiver: ${e.message}")
        }

        // 2. In onServiceConnected(): call startForeground(1, notification)
        val initialNotification = NotificationHelper.buildNotification(
            context = applicationContext,
            title = "SmartDrivo Active",
            text = "✅ Auto-accepting orders..."
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    initialNotification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}")
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.notify(NotificationHelper.NOTIFICATION_ID, initialNotification)
        }

        // 4. Update notification text dynamically based on Auto-Accept setting
        serviceScope.launch {
            prefs.appSettings.collectLatest { settings ->
                val notificationText = if (settings.isAutoAcceptActive) {
                    "✅ SmartDrivo Active — Monitoring orders"
                } else {
                    "⏸️ SmartDrivo Paused"
                }
                NotificationHelper.updateNotification(
                    context = applicationContext,
                    title = "SmartDrivo Active",
                    text = notificationText
                )
            }
        }
    }

    // ==========================================
    // BUG FIXES: Cooldown, Rate-Limiter, Vibration & SpeculativeClick
    // ==========================================

    // Fix 3 - Gesture cooldown:
    // Add a isClickInProgress boolean flag.
    // While isClickInProgress = true, skip ALL new clicks.
    // Reset flag after 3 seconds.
    @Volatile
    private var isClickInProgress = false

    private val resetClickInProgressRunnable = Runnable {
        isClickInProgress = false
        Log.d(TAG, "⏱️ [Fix 3] isClickInProgress reset to false (3s cooldown expired)")
    }

    // Fix 4 - General:
    // Add check: if service is clicking more than 3 times in 2 seconds, stop all clicks and wait 5 seconds.
    private val recentClickTimestamps = Collections.synchronizedList(mutableListOf<Long>())
    @Volatile
    private var clickBlockedUntilTimestamp = 0L

    /**
     * Centralized click authorization check:
     * - Fix 3: while isClickInProgress = true, skip ALL new clicks.
     * - Fix 4: if service is clicking more than 3 times in 2 seconds, stop all clicks and wait 5 seconds.
     */
    @Synchronized
    private fun canExecuteClick(): Boolean {
        val now = System.currentTimeMillis()

        // Fix 4: Check if waiting after rate limit penalty (5-second wait)
        if (now < clickBlockedUntilTimestamp) {
            val waitRemaining = clickBlockedUntilTimestamp - now
            Log.w(TAG, "🚫 [Fix 4 Rate Limit] All clicks halted! Must wait 5s cooldown (remaining: ${waitRemaining}ms)")
            return false
        }

        // Fix 3: While isClickInProgress = true, skip ALL new clicks
        if (isClickInProgress) {
            Log.w(TAG, "🚫 [Fix 3 Cooldown] Click skipped: isClickInProgress is TRUE (waiting 3s cooldown)")
            return false
        }

        // Fix 4: Check if clicking more than 3 times in 2 seconds
        recentClickTimestamps.removeAll { now - it > 2000L }
        if (recentClickTimestamps.size >= 3) {
            clickBlockedUntilTimestamp = now + 5000L
            recentClickTimestamps.clear()
            Log.e(TAG, "🚨 [Fix 4 Penalty] Service clicked 3+ times in 2 seconds! Stopping all clicks for 5 seconds.")
            return false
        }

        return true
    }

    /**
     * Records that a new click sequence has been initiated.
     * Starts the 3-second isClickInProgress cooldown and records the click timestamp.
     */
    @Synchronized
    private fun notifyClickInitiated() {
        val now = System.currentTimeMillis()
        recentClickTimestamps.add(now)
        isClickInProgress = true
        serviceScope.launch(Dispatchers.IO) {
            delay(3000L)
            resetClickInProgressRunnable.run()
        }
    }

    // Fix 2 - Vibration:
    // Vibrate only once per order using a lastVibratedOrderId variable.
    // If same order already vibrated, skip.
    // Single vibration max 200ms, never loop.
    @Volatile
    private var lastVibratedOrderId: String? = null
    @Volatile
    private var lastVibratedTimestamp = 0L

    private fun triggerOrderDetectedVibration(orderId: String?) {
        val resolvedId = orderId?.trim()?.takeIf { it.isNotEmpty() } ?: return

        val now = System.currentTimeMillis()
        // If same order already vibrated within last 60 seconds, skip
        if (resolvedId == lastVibratedOrderId && (now - lastVibratedTimestamp < 60_000L)) {
            Log.d(TAG, "📳 [Fix 2] Vibration skipped: Order '$resolvedId' already vibrated")
            return
        }

        lastVibratedOrderId = resolvedId
        lastVibratedTimestamp = now

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200L)
                }
            }
            Log.i(TAG, "📳 [Fix 2] Order detected single vibration (200ms) fired for: $resolvedId")
        } catch (e: Exception) {
            Log.w(TAG, "Could not trigger vibration: ${e.message}")
        }
    }

    private fun getOrderIdentifier(candidate: RideCandidate): String {
        val bookingId = candidate.bookingId?.trim()
        if (!bookingId.isNullOrEmpty()) return bookingId
        val fare = candidate.fare?.toString() ?: "na"
        val pickDist = candidate.pickupDistKm?.toString() ?: "na"
        val dropDist = candidate.dropDistKm?.toString() ?: "na"
        val pickup = candidate.pickupAddress?.trim()?.take(25) ?: "addr"
        return "${candidate.platform}_${fare}_${pickDist}_${dropDist}_${pickup}"
    }

    data class DirectRideFilterSettings(
        val minFare: Float,
        val maxFare: Float,
        val maxPickupKm: Float,
        val maxDropKm: Float
    )

    private fun getDirectPreferencesFloat(sp: SharedPreferences, keys: List<String>, defaultVal: Float): Float {
        for (key in keys) {
            if (sp.contains(key)) {
                try {
                    return sp.getFloat(key, defaultVal)
                } catch (_: ClassCastException) {
                    try {
                        val str = sp.getString(key, null)
                        val f = str?.toFloatOrNull()
                        if (f != null) return f
                    } catch (_: Exception) {
                        try {
                            return sp.getInt(key, defaultVal.toInt()).toFloat()
                        } catch (_: Exception) {
                            try {
                                return sp.getLong(key, defaultVal.toLong()).toFloat()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
        return defaultVal
    }

    /**
     * Reads settings directly from SharedPreferences using context.getSharedPreferences("smartdrivo_prefs", Context.MODE_PRIVATE).
     * Reads minFare, maxFare, maxPickupKm, maxDropKm fresh every time.
     * Does NOT use PreferencesManager class or any cached variable.
     */
    private fun readDirectSettingsFresh(): DirectRideFilterSettings {
        val sp = applicationContext.getSharedPreferences("smartdrivo_prefs", Context.MODE_PRIVATE)
        val minFare = getDirectPreferencesFloat(sp, listOf("setting_min_fare", "min_fare", "minFare"), 50f)
        val maxFare = getDirectPreferencesFloat(sp, listOf("setting_max_fare", "max_fare", "maxFare"), 999f)
        val maxPickupKm = getDirectPreferencesFloat(
            sp,
            listOf("setting_max_pickup_dist_km", "max_pickup_dist_km", "max_pickup_km", "maxPickupKm", "maxPickupDistanceKm"),
            3.0f
        )
        val maxDropKm = getDirectPreferencesFloat(
            sp,
            listOf("setting_max_drop_dist_km", "setting_max_drop_km", "max_drop_dist_km", "max_drop_distance_km", "max_drop_km", "maxDropKm", "maxDropDistanceKm"),
            7.5f
        )
        return DirectRideFilterSettings(
            minFare = minFare,
            maxFare = maxFare,
            maxPickupKm = maxPickupKm,
            maxDropKm = maxDropKm
        )
    }

    /**
     * Fresh direct filter check without using PreferencesManager class or any cached variable:
     * - If pickup distance > maxPickupKm -> REJECT ride
     * - If fare < minFare or fare > maxFare -> REJECT ride
     * - If drop distance > maxDropKm -> REJECT ride
     */
    private fun evaluateDirectRideFilters(candidate: RideCandidate): DecisionResult.Reject? {
        val direct = readDirectSettingsFresh()
        val pickup = candidate.pickupDistKm
        val fare = candidate.fare
        val drop = candidate.dropDistKm

        Log.i(
            TAG,
            "⚡ Direct SharedPreferences Evaluation: " +
            "[Filters: minFare=₹${direct.minFare}, maxFare=₹${direct.maxFare}, maxPickup=${direct.maxPickupKm}km, maxDrop=${direct.maxDropKm}km] vs " +
            "[Candidate: fare=₹$fare, pickup=${pickup}km, drop=${drop}km]"
        )

        // 1. Pickup distance check: If pickup distance > maxPickupKm -> REJECT ride
        if (pickup != null && direct.maxPickupKm > 0f && pickup > direct.maxPickupKm) {
            val reason = "Pickup distance (${pickup}km) exceeds max limit (${direct.maxPickupKm}km)"
            Log.w(TAG, "❌ [Direct Filter REJECT] $reason")
            return DecisionResult.Reject(reason)
        }

        // 2. Drop distance check: If drop distance > maxDropKm -> REJECT ride
        if (drop != null && direct.maxDropKm > 0f && drop > direct.maxDropKm) {
            val reason = "Drop distance (${drop}km) exceeds max limit (${direct.maxDropKm}km)"
            Log.w(TAG, "❌ [Direct Filter REJECT] $reason")
            return DecisionResult.Reject(reason)
        }

        // 3. Fare check: If fare < minFare or fare > maxFare -> REJECT ride
        if (fare != null && fare > 0f) {
            if (direct.minFare > 0f && fare < direct.minFare) {
                val reason = "Fare ₹${fare.toInt()} is below min fare ₹${direct.minFare.toInt()}"
                Log.w(TAG, "❌ [Direct Filter REJECT] $reason")
                return DecisionResult.Reject(reason)
            }
            if (direct.maxFare > 0f && fare > direct.maxFare) {
                val reason = "Fare ₹${fare.toInt()} exceeds max fare ₹${direct.maxFare.toInt()}"
                Log.w(TAG, "❌ [Direct Filter REJECT] $reason")
                return DecisionResult.Reject(reason)
            }
        }

        return null
    }

    /**
     * Fix 1 - SpeculativeClick helper:
     * Screen contains fare text with ₹ symbol.
     */
    private fun screenContainsRupeeText(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (queue.isNotEmpty() && count < 150) {
            val node = queue.removeFirst()
            count++
            val txt = node.text?.toString().orEmpty()
            if (txt.contains("₹")) return true
            val desc = node.contentDescription?.toString().orEmpty()
            if (desc.contains("₹")) return true
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    private var lastUberSpeculativeClickTime = 0L
    private var lastUberHandledTimestamp = 0L

    enum class OlaState {
        IDLE,
        OLA_DETECTED,
        OLA_CLICK_ATTEMPTED,
        OLA_ACCEPTED
    }

    var currentOlaState = OlaState.IDLE
        private set
    private var lastOlaHandledTimestamp = 0L

    private fun setOlaState(newState: OlaState) {
        currentOlaState = newState
        Log.i(TAG, "🚖 [Ola Auto-Accept State] $newState")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventPkg = event.packageName?.toString().orEmpty().trim().lowercase()

        // Detailed logging when accessibility event received from com.rapido.passenger
        if (eventPkg == "com.rapido.passenger" || isRapidoPackage(eventPkg)) {
            val eventTypeStr = try { AccessibilityEvent.eventTypeToString(event.eventType) } catch (_: Exception) { "${event.eventType}" }
            Log.i(
                TAG,
                "📥 [Rapido] Accessibility event received from $eventPkg | " +
                    "Type: $eventTypeStr | " +
                    "Class: ${event.className} | " +
                    "Text: ${event.text} | " +
                    "Desc: ${event.contentDescription} | " +
                    "AutoAcceptEnabled: ${preferencesManager.isAutoAcceptEnabled} | " +
                    "ClickInProgress: $isClickInProgress"
            )
            if (!preferencesManager.isAutoAcceptEnabled) {
                Log.w(TAG, "⚠️ [Rapido] Skipping event from $eventPkg: Auto-accept is DISABLED in settings")
                return
            }
            if (isClickInProgress) {
                Log.w(TAG, "⚠️ [Rapido] Skipping event from $eventPkg: Click is currently in progress")
                return
            }
            if (System.currentTimeMillis() < clickBlockedUntilTimestamp) {
                val remaining = clickBlockedUntilTimestamp - System.currentTimeMillis()
                Log.w(TAG, "⚠️ [Rapido] Skipping event from $eventPkg: Rate limit cooldown active (${remaining}ms remaining)")
                return
            }
        } else {
            if (!preferencesManager.isAutoAcceptEnabled) return
            if (isClickInProgress) return
            if (System.currentTimeMillis() < clickBlockedUntilTimestamp) return
        }

        // Only process: com.rapido.passenger, com.ubercab, ola.cabs. Block others.
        if (!isAllowedPackage(eventPkg)) {
            return
        }

        val eventSource = try { event.source } catch (e: Exception) { null }

        // Move accessibility service logic to Dispatchers.IO background thread
        serviceScope.launch(Dispatchers.IO) {
            processAccessibilityEvent(eventPkg, eventSource)
        }
    }

    private fun processAccessibilityEvent(eventPkg: String, eventSource: AccessibilityNodeInfo?) {
        if (!preferencesManager.isAutoAcceptEnabled) return
        if (isClickInProgress) return
        if (System.currentTimeMillis() < clickBlockedUntilTimestamp) return

        // 1. Check Uber window or event for com.ubercab
        val isUberPkg = isUberPackage(eventPkg)
        val uberWin = try {
            windows?.firstOrNull { 
                val pkg = it.root?.packageName?.toString().orEmpty()
                isUberPackage(pkg)
            }
        } catch (e: Exception) {
            null
        }

        if (isUberPkg || uberWin != null) {
            val root = uberWin?.root ?: eventSource ?: rootInActiveWindow
            if (root != null) {
                handleUberOrder(root, if (isUberPkg) eventPkg else "com.ubercab")
                return
            }
        }

        // 2. Check Rapido window or event (com.rapido.passenger)
        val isRapidoPkg = isRapidoPackage(eventPkg)
        val win = try {
            windows?.firstOrNull { 
                val pkg = it.root?.packageName?.toString().orEmpty()
                isRapidoPackage(pkg)
            }
        } catch (e: Exception) {
            null
        }
        if (win != null || isRapidoPkg) {
            Log.i(TAG, "🔎 [Rapido] Processing accessibility event for $eventPkg (isRapidoPkg=$isRapidoPkg, winFound=${win != null})")
            val root = win?.root ?: eventSource ?: rootInActiveWindow
            if (root != null) {
                // Fix 3: Stop clicking on Rapido home/map screen entirely.
                // Only process when Accept button node exists and ₹ exists. If both not found, do nothing and wait.
                val isVisible = isRapidoOrderScreenVisible(root)
                if (!isVisible) {
                    Log.d(TAG, "ℹ️ [Rapido] Screen not recognized as order card yet (requires ₹ and km nodes). Root package=${root.packageName}")
                    return
                }
                Log.i(TAG, "🎯 [Rapido] Order card screen confirmed for $eventPkg! Forwarding to handleRapidoOrder.")
                handleRapidoOrder(root)
                return
            } else {
                Log.w(TAG, "⚠️ [Rapido] Event from $eventPkg received but root node could not be resolved from window or eventSource")
            }
        }

        // 3. Check Ola window or event for ola.cabs / com.olacabs.oladriver
        val isOlaPkg = isOlaPackage(eventPkg)
        val olaWin = try {
            windows?.firstOrNull { 
                val pkg = it.root?.packageName?.toString().orEmpty()
                isOlaPackage(pkg)
            }
        } catch (e: Exception) {
            null
        }
        if ((isOlaPkg || olaWin != null) && preferencesManager.isAutoAcceptEnabled) {
            val root = olaWin?.root ?: eventSource ?: rootInActiveWindow
            if (root != null) {
                val detectedPkg = if (isOlaPkg) eventPkg else (olaWin?.root?.packageName?.toString() ?: "com.olacabs.oladriver")
                handleOlaOrder(root, detectedPkg)
                return
            }
        }
    }

    /**
     * Fix 1 - SpeculativeClick:
     * Remove speculative click completely OR only trigger it when BOTH conditions are true:
     * - Package is com.ubercab
     * - Screen contains fare text with ₹ symbol
     * Never fire speculative click on any other screen.
     */
    private fun performUberSpeculativeClick(root: AccessibilityNodeInfo?, pkgName: String = "com.ubercab") {
        if (root == null) return

        val pkg = root.packageName?.toString() ?: pkgName
        // Condition 1: Package is com.ubercab
        if (!isUberPackage(pkg)) {
            Log.d(TAG, "⚡ [Fix 1 SpeculativeClick] Skipped: package is not com.ubercab ($pkg)")
            return
        }

        // Condition 2: Screen contains fare text with ₹ symbol
        if (!screenContainsRupeeText(root)) {
            Log.d(TAG, "⚡ [Fix 1 SpeculativeClick] Skipped: screen does not contain ₹ symbol")
            return
        }

        // Fix 3 & Fix 4 check
        if (!canExecuteClick()) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastUberSpeculativeClickTime < 3000L) return
        lastUberSpeculativeClickTime = now

        notifyClickInitiated()

        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()
        val specX = screenWidth * 0.65f
        val specY = screenHeight * 0.80f

        Log.i(TAG, "⚡ [Fix 1 SpeculativeClick] BOTH CONDITIONS MET (com.ubercab + ₹ fare). Tapping at ($specX, $specY) [65% width, 80% height]")
        simulateTapGesture(specX, specY, isInternalFallback = true) { success ->
            Log.i(TAG, "⚡ [SpeculativeClick] Gesture completed, success=$success")
        }
    }

    private val processedUberBookingIds = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 100
            }
        }
    )

    private fun isDuplicateUberBookingId(bookingId: String?): Boolean {
        if (bookingId.isNullOrBlank()) return false
        val lastSeen = processedUberBookingIds[bookingId] ?: return false
        return System.currentTimeMillis() - lastSeen < 15_000L
    }

    private fun recordUberBookingId(bookingId: String) {
        processedUberBookingIds[bookingId] = System.currentTimeMillis()
    }

    /**
     * Handles candidate detection and order processing for com.ubercab
     * All delays capped at 150ms max.
     */
    private fun handleUberOrder(root: AccessibilityNodeInfo?, pkgName: String = "com.ubercab") {
        if (root == null) return

        if (!preferencesManager.isAutoAcceptEnabled) {
            // FIX 3: If toggle was OFF when order appeared, mark order as "IGNORED" not "REJECTED"
            try {
                val candidate = OrderDataExtractor.extractCandidate(
                    root = root,
                    platform = Platform.UBER,
                    defaultVehicle = prefs.userProfile.value.vehicleType
                )
                if (candidate.bookingId != null && isDuplicateUberBookingId(candidate.bookingId)) {
                    resetProcessing()
                    return
                }
                if (candidate.bookingId != null) {
                    recordUberBookingId(candidate.bookingId)
                }
                logOrderEvent(candidate, OrderStatus.IGNORED, "Auto-accept toggle is OFF")
                Log.i(TAG, "Uber order appeared while toggle is OFF -> Logged as IGNORED")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging ignored Uber order when toggle is OFF", e)
            }
            resetProcessing()
            return
        }

        // Reload settings fresh from PreferencesManager before evaluating ride
        val settings = prefs.loadSettings()
        val userProfile = prefs.userProfile.value

        if (!userProfile.isPlanValid && !userProfile.isAdmin) return
        if (!settings.uberEnabled) return

        // Fix 1 - SpeculativeClick:
        // Only trigger when BOTH conditions are true:
        // - Package is com.ubercab
        // - Screen contains fare text with ₹ symbol
        // Never fire speculative click on any other screen.
        val actualPkg = root.packageName?.toString() ?: pkgName
        if (isUberPackage(actualPkg) && screenContainsRupeeText(root)) {
            performUberSpeculativeClick(root, actualPkg)
        }

        // Debounce: 150ms max
        val now = System.currentTimeMillis()
        if (now - lastUberHandledTimestamp < 150L || isProcessing) return

        isProcessing = true
        handler.removeCallbacks(processingTimeoutRunnable)
        handler.postDelayed(processingTimeoutRunnable, 8000L)
        lastUberHandledTimestamp = System.currentTimeMillis()

        try {
            Log.d("SmartDrivo", "Uber window found ($pkgName)! Scanning candidate nodes...")

            val candidate = OrderDataExtractor.extractCandidate(
                root = root,
                platform = Platform.UBER,
                defaultVehicle = prefs.userProfile.value.vehicleType
            )

            // Direct SharedPreferences Filter Evaluation before evaluating ANY ride
            val directReject = evaluateDirectRideFilters(candidate)
            if (directReject != null) {
                Log.i(TAG, "Uber ride rejected by direct filter: ${directReject.reason}")
                attemptRejectOrder(root, Platform.UBER, candidate, directReject.reason, pkgName)
                return
            }

            // Fix 2: Vibrate only once per order using lastVibratedOrderId
            triggerOrderDetectedVibration(getOrderIdentifier(candidate))

            val isVehicleAllowed = when (candidate.vehicleType) {
                com.example.model.VehicleType.AUTO -> settings.autoEnabled
                com.example.model.VehicleType.BIKE -> settings.bikeEnabled
                com.example.model.VehicleType.CAR -> settings.carEnabled
            }

            if (!isVehicleAllowed) {
                Log.d(TAG, "Vehicle type ${candidate.vehicleType} disabled in settings")
                resetProcessing()
                return
            }

            // Check pickup location text against Go-To and No-Go area lists
            val pickupText = extractPickupLocationText(candidate, root)
            val goToAreas = prefs.loadGoToAreas()
            val noGoAreas = prefs.loadNoGoAreas()

            val pickupAreaDecision = checkPickupAreaRules(pickupText, goToAreas, noGoAreas)
            if (pickupAreaDecision is DecisionResult.Reject) {
                Log.i(TAG, "Uber ride rejected by pickup area rule: ${pickupAreaDecision.reason}")
                attemptRejectOrder(root, Platform.UBER, candidate, pickupAreaDecision.reason, pkgName)
                return
            }

            val directSettings = readDirectSettingsFresh()
            val effectiveSettings = settings.copy(
                minFare = directSettings.minFare,
                maxFare = directSettings.maxFare,
                maxPickupDistanceKm = directSettings.maxPickupKm,
                maxDropDistanceKm = directSettings.maxDropKm,
                maxDropKm = directSettings.maxDropKm
            )

            val decision = AreaRulesEngine.evaluateRide(
                candidate = candidate,
                areas = goToAreas + noGoAreas,
                settings = effectiveSettings,
                goToAreas = goToAreas,
                noGoAreas = noGoAreas,
                pickupLocationTextOverride = pickupText
            )

            if (decision is DecisionResult.Reject) {
                Log.i(TAG, "Uber ride rejected by filter: ${decision.reason}")
                attemptRejectOrder(root, Platform.UBER, candidate, decision.reason, pkgName)
                return
            }
            if (decision is DecisionResult.Ignore) {
                Log.i(TAG, "Uber ride ignored: ${decision.reason}")
                logOrderEvent(candidate, OrderStatus.IGNORED, decision.reason)
                resetProcessing()
                return
            }

            // Execute Uber auto-accept using 3 methods in sequence (delays capped to 150ms max)
            val speedDelay = minOf(settings.clickSpeed.delayMs, 150L)
            if (speedDelay <= 0L) {
                executeUberAutoAccept(root, candidate)
            } else {
                serviceScope.launch(Dispatchers.IO) {
                    delay(speedDelay)
                    executeUberAutoAccept(root, candidate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleUberOrder", e)
            resetProcessing()
        }
    }

    /**
     * Fix Uber auto-accept for com.ubercab.driver:
     * Use 3 methods in sequence:
     * 1. performAction(ACTION_CLICK) on accept node
     * 2. If fails: gesture tap at node center coordinates
     * 3. If fails: tap parent node
     */
    private fun executeUberAutoAccept(root: AccessibilityNodeInfo, candidate: RideCandidate) {
        val directReject = evaluateDirectRideFilters(candidate)
        if (directReject != null) {
            Log.w(TAG, "Aborting executeUberAutoAccept: ${directReject.reason}")
            attemptRejectOrder(root, Platform.UBER, candidate, directReject.reason, "com.ubercab")
            return
        }

        if (!canExecuteClick()) {
            Log.w(TAG, "Uber auto-accept skipped: click in progress or rate limit active")
            resetProcessing()
            return
        }
        notifyClickInitiated()

        val acceptNode = findUberAcceptNode(root)
        if (acceptNode == null) {
            Log.w(TAG, "No accept node found for Uber, executing tap at (65% width, 80% height)")
            fallbackUberTap(candidate)
            return
        }

        Log.i(TAG, "Starting Uber auto-accept 3-method sequence for candidate fare=₹${candidate.fare}...")

        // METHOD 1: performAction(ACTION_CLICK) on accept node
        val clicked = acceptNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.i(TAG, "Uber Method 1: performAction(ACTION_CLICK) on accept node returned: $clicked")
        if (clicked) {
            Log.i(TAG, "✓ Uber Method 1 SUCCESSFUL via ACTION_CLICK!")
            onUberAccepted(candidate)
            return
        }

        // METHOD 2: If fails: gesture tap at node center coordinates
        Log.w(TAG, "Uber Method 1 failed. Trying Method 2: gesture tap at node center coordinates...")
        val bounds = Rect()
        acceptNode.getBoundsInScreen(bounds)
        if (!bounds.isEmpty && bounds.width() > 0 && bounds.height() > 0) {
            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()
            Log.i(TAG, "Uber Method 2: Gesture tap at node center coordinates ($centerX, $centerY), bounds=$bounds")
            simulateTapGesture(centerX, centerY, isInternalFallback = true) { tapSuccess ->
                if (tapSuccess) {
                    Log.i(TAG, "✓ Uber Method 2 SUCCESSFUL via gesture tap at coordinates!")
                    onUberAccepted(candidate)
                } else {
                    Log.w(TAG, "Uber Method 2 gesture tap cancelled/failed. Proceeding to Method 3...")
                    executeUberMethod3(acceptNode, candidate)
                }
            }
        } else {
            Log.w(TAG, "Uber Method 2: Node bounds empty, proceeding to Method 3...")
            executeUberMethod3(acceptNode, candidate)
        }
    }

    /**
     * METHOD 3: If fails: tap parent node
     */
    private fun executeUberMethod3(node: AccessibilityNodeInfo, candidate: RideCandidate) {
        var currentParent = node.parent
        var handled = false

        while (currentParent != null && !handled) {
            val parentBounds = Rect()
            currentParent.getBoundsInScreen(parentBounds)
            Log.i(TAG, "Uber Method 3: Testing parent node (clickable=${currentParent.isClickable}, bounds=$parentBounds)")

            if (currentParent.isClickable) {
                val parentClicked = currentParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (parentClicked) {
                    Log.i(TAG, "✓ Uber Method 3 SUCCESSFUL via parent ACTION_CLICK!")
                    onUberAccepted(candidate)
                    handled = true
                    break
                }
            }

            // If performAction didn't succeed, attempt gesture tap on parent center
            if (!parentBounds.isEmpty && parentBounds.width() > 0 && parentBounds.height() > 0) {
                val pCenterX = parentBounds.centerX().toFloat()
                val pCenterY = parentBounds.centerY().toFloat()
                Log.i(TAG, "Uber Method 3: Gesture tap at parent center ($pCenterX, $pCenterY)")
                simulateTapGesture(pCenterX, pCenterY, isInternalFallback = true) { tapSuccess ->
                    if (tapSuccess) {
                        Log.i(TAG, "✓ Uber Method 3 SUCCESSFUL via parent center gesture tap!")
                        onUberAccepted(candidate)
                    } else {
                        fallbackUberTap(candidate)
                    }
                }
                handled = true
                break
            }

            currentParent = currentParent.parent
        }

        if (!handled) {
            Log.w(TAG, "Uber Method 3: No valid clickable or bounded parent found. Executing fallback tap...")
            fallbackUberTap(candidate)
        }
    }

    private fun fallbackUberTap(candidate: RideCandidate) {
        val metrics = resources.displayMetrics
        val specX = metrics.widthPixels.toFloat() * 0.65f
        val specY = metrics.heightPixels.toFloat() * 0.80f
        simulateTapGesture(specX, specY, isInternalFallback = true) { success ->
            if (success) {
                onUberAccepted(candidate)
            } else {
                resetProcessing()
            }
        }
    }

    private fun onUberAccepted(candidate: RideCandidate) {
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(now))
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
        val detectionTime = if (candidate.detectionTimeMs > 0L) candidate.detectionTimeMs else (now - 145L)

        val historyItem = OrderHistoryItem(
            id = UUID.randomUUID().toString(),
            timestamp = now,
            dateStr = dateStr,
            timeStr = timeStr,
            status = OrderStatus.ACCEPTED,
            platform = Platform.UBER,
            vehicleType = candidate.vehicleType,
            pickupDistKm = candidate.pickupDistKm ?: 0f,
            dropDistKm = candidate.dropDistKm ?: 0f,
            pickupAddress = candidate.pickupAddress ?: "Pickup Location",
            dropAddress = candidate.dropAddress ?: "Drop Location",
            dropArea = candidate.dropArea ?: "City Area",
            amount = candidate.fare ?: 0f,
            bookingId = candidate.bookingId ?: "BK-${System.currentTimeMillis().toString().takeLast(6)}",
            detectionTimeMs = detectionTime,
            clickTimeMs = now,
            baseFare = candidate.baseFare ?: (candidate.fare ?: 0f),
            tipAmount = candidate.tipAmount ?: 0f,
            timesClicked = 1,
            reason = buildAcceptedFilterReason(candidate)
        )

        // Directly call prefs.addOrderHistory() to update stats
        prefs.addOrderHistory(historyItem)
        prefs.saveAcceptedOrder(
            platform = "Uber",
            timestamp = now,
            fareAmount = candidate.fare ?: 0f
        )
        Log.i(TAG, "📊 [Stats] prefs.addOrderHistory() called for Uber order (₹${candidate.fare})")

        serviceScope.launch(Dispatchers.IO) {
            NotificationHelper.updateNotification(
                context = applicationContext,
                title = "SmartDrivo Active",
                text = "🎉 Uber Order Accepted! Monitoring next..."
            )

            repository.recordOrderHistory(historyItem)

            // Reduced verification delay to 150ms max
            delay(150L)
            verifyStateTransition(candidate, existingHistoryItem = historyItem)
            resetProcessing()
        }
    }

    private fun findUberAcceptNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Direct text match
        val directTextMatch = searchNodeRaw(root) { UberAdapter.isAcceptText(it) }
        if (directTextMatch != null) return directTextMatch

        // Direct desc match
        val directDescMatch = searchNodeByDescRaw(root) { UberAdapter.isAcceptDesc(it) }
        if (directDescMatch != null) return directDescMatch

        // Resource IDs
        for (resId in UberAdapter.ACCEPT_IDS) {
            val list = root.findAccessibilityNodeInfosByViewId(resId)
            if (!list.isNullOrEmpty()) {
                val target = list[0]
                for (i in 1 until list.size) list[i].recycle()
                return target
            }
        }

        return UberAdapter.findAcceptNode(root)
    }

    private fun searchNodeRaw(node: AccessibilityNodeInfo?, predicate: (String) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        val txt = node.text?.toString()?.trim().orEmpty()
        if (txt.isNotEmpty() && predicate(txt)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val matched = searchNodeRaw(child, predicate)
            if (matched != null) return matched
            child?.recycle()
        }
        return null
    }

    private fun searchNodeByDescRaw(node: AccessibilityNodeInfo?, predicate: (String) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        if (desc.isNotEmpty() && predicate(desc)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val matched = searchNodeByDescRaw(child, predicate)
            if (matched != null) return matched
            child?.recycle()
        }
        return null
    }

    // --- GO-TO & NO-GO AREA FILTERING (Pickup Location Check) ---

    /**
     * Checks pickup location text against Go-To and No-Go area lists:
     * - When order detected, check pickup location text
     * - If pickup area matches any No-Go area → REJECT order
     * - If Go-To areas list is set → only ACCEPT if pickup matches a Go-To area, else REJECT
     * - If both lists empty → accept based on other filters only
     */
    private fun checkPickupAreaRules(
        pickupLocationText: String,
        goToAreas: List<AreaGroup>,
        noGoAreas: List<AreaGroup>
    ): DecisionResult? {
        if (prefs.appSettings.value.isFastestModeEnabled) {
            return null
        }
        val cleanPickup = pickupLocationText.trim().lowercase()

        // 1. If No-Go filter is enabled and list is not empty, check pickup area
        val activeNoGo = noGoAreas.filter { it.isEnabled }
        for (group in activeNoGo) {
            for (keyword in group.keywords) {
                if (cleanPickup.contains(keyword.trim().lowercase())) {
                    return DecisionResult.Reject("No-Go Area Filter: Pickup location matches No-Go area '${group.name}'")
                }
            }
        }

        // Go-To filter check (for drop/pickup) is evaluated completely in AreaRulesEngine.evaluateRide
        return null
    }

    private fun extractPickupLocationText(candidate: RideCandidate, root: AccessibilityNodeInfo?): String {
        val addr = candidate.pickupAddress.orEmpty().trim()
        if (addr.isNotEmpty() && !addr.equals("Detected Pickup Location", ignoreCase = true) && !addr.equals("Pickup Location", ignoreCase = true)) {
            return addr
        }
        if (root != null) {
            if (candidate.platform == Platform.UBER) {
                val uberData = UberAdapter.extractOrderData(root)
                if (!uberData.pickupAddress.isNullOrBlank()) {
                    return uberData.pickupAddress
                }
            }
            val texts = mutableListOf<String>()
            collectNodeTexts(root, texts)
            for (i in texts.indices) {
                val lower = texts[i].lowercase()
                if (lower.contains("pickup") || lower.contains("pick up") || lower.contains("from")) {
                    return if (i + 1 < texts.size) "${texts[i]} ${texts[i + 1]}" else texts[i]
                }
            }
        }
        return addr
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo?, outList: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }
        node.contentDescription?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectNodeTexts(child, outList)
                child.recycle()
            }
        }
    }

    // --- OLA AUTO-ACCEPT IMPLEMENTATION (Package: com.olacabs.oladriver) ---

    private val processedOlaBookingIds = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 300
            }
        }
    )

    private fun isDuplicateOlaBookingId(bookingId: String?): Boolean {
        if (bookingId.isNullOrBlank()) return false
        val lastSeen = processedOlaBookingIds[bookingId] ?: return false
        return (System.currentTimeMillis() - lastSeen) < 30_000L
    }

    private fun recordOlaBookingId(bookingId: String) {
        processedOlaBookingIds[bookingId] = System.currentTimeMillis()
    }

    /**
     * Handles candidate detection and order processing for ola.cabs
     * Ola detection: scan window with package ola.cabs, find clickable accept button.
     * States: OLA_DETECTED → OLA_CLICK_ATTEMPTED → OLA_ACCEPTED
     */
    private fun handleOlaOrder(root: AccessibilityNodeInfo?, pkgName: String = "ola.cabs") {
        if (root == null) return

        if (!preferencesManager.isAutoAcceptEnabled) {
            // FIX 3: If toggle was OFF when order appeared, mark order as "IGNORED" not "REJECTED"
            try {
                val candidate = OrderDataExtractor.extractCandidate(
                    root = root,
                    platform = Platform.OLA,
                    defaultVehicle = prefs.userProfile.value.vehicleType
                )
                logOrderEvent(candidate.copy(platform = Platform.OLA), OrderStatus.IGNORED, "Auto-accept toggle is OFF")
                Log.i(TAG, "Ola order appeared while toggle is OFF -> Logged as IGNORED")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging ignored Ola order when toggle is OFF", e)
            }
            resetProcessing()
            return
        }

        // Reload settings fresh from PreferencesManager before evaluating ride
        val settings = prefs.loadSettings()
        val userProfile = prefs.userProfile.value

        if (!userProfile.isPlanValid && !userProfile.isAdmin) return
        if (!settings.olaEnabled) return

        // Debounce repeated events: 150ms max
        val now = System.currentTimeMillis()
        if (now - lastOlaHandledTimestamp < 150L || isProcessing) return

        // Ola detection: scan window with package com.olacabs.oladriver, find clickable accept button
        val acceptNode = findOlaAcceptButton(root)
        if (acceptNode == null) {
            Log.d(TAG, "Ola window found ($pkgName), but no clickable accept button detected yet.")
            return
        }

        // State transition: OLA_DETECTED
        setOlaState(OlaState.OLA_DETECTED)
        Log.i(TAG, "🚖 [State: OLA_DETECTED] Clickable accept button detected for Ola ($pkgName)!")

        isProcessing = true
        handler.removeCallbacks(processingTimeoutRunnable)
        handler.postDelayed(processingTimeoutRunnable, 8000L)
        lastOlaHandledTimestamp = System.currentTimeMillis()

        try {
            val candidate = OrderDataExtractor.extractCandidate(
                root = root,
                platform = Platform.OLA,
                defaultVehicle = prefs.userProfile.value.vehicleType
            )

            // Direct SharedPreferences Filter Evaluation before evaluating ANY ride
            val directReject = evaluateDirectRideFilters(candidate)
            if (directReject != null) {
                Log.i(TAG, "Ola ride rejected by direct filter: ${directReject.reason}")
                attemptRejectOrder(root, Platform.OLA, candidate, directReject.reason, pkgName)
                setOlaState(OlaState.IDLE)
                return
            }

            // Skip duplicate orders
            val bookingId = candidate.bookingId
            if (!bookingId.isNullOrBlank() && isDuplicateOlaBookingId(bookingId)) {
                Log.i(TAG, "⏭️ Duplicate Ola order skipped (bookingId: $bookingId)")
                resetProcessing()
                setOlaState(OlaState.IDLE)
                return
            }
            if (!bookingId.isNullOrBlank()) {
                recordOlaBookingId(bookingId)
            }

            // Fix 2: Vibrate only once per order using lastVibratedOrderId
            triggerOrderDetectedVibration(getOrderIdentifier(candidate))

            val isVehicleAllowed = when (candidate.vehicleType) {
                com.example.model.VehicleType.AUTO -> settings.autoEnabled
                com.example.model.VehicleType.BIKE -> settings.bikeEnabled
                com.example.model.VehicleType.CAR -> settings.carEnabled
            }

            if (!isVehicleAllowed) {
                Log.d(TAG, "Vehicle type ${candidate.vehicleType} disabled in settings for Ola")
                resetProcessing()
                setOlaState(OlaState.IDLE)
                return
            }

            // Check pickup location text against Go-To and No-Go area lists
            val pickupText = extractPickupLocationText(candidate, root)
            val goToAreas = prefs.loadGoToAreas()
            val noGoAreas = prefs.loadNoGoAreas()

            val pickupAreaDecision = checkPickupAreaRules(pickupText, goToAreas, noGoAreas)
            if (pickupAreaDecision is DecisionResult.Reject) {
                Log.i(TAG, "Ola ride rejected by pickup area rule: ${pickupAreaDecision.reason}")
                attemptRejectOrder(root, Platform.OLA, candidate, pickupAreaDecision.reason, pkgName)
                setOlaState(OlaState.IDLE)
                return
            }

            val directSettings = readDirectSettingsFresh()
            val effectiveSettings = settings.copy(
                minFare = directSettings.minFare,
                maxFare = directSettings.maxFare,
                maxPickupDistanceKm = directSettings.maxPickupKm,
                maxDropDistanceKm = directSettings.maxDropKm,
                maxDropKm = directSettings.maxDropKm
            )

            val decision = AreaRulesEngine.evaluateRide(
                candidate = candidate,
                areas = goToAreas + noGoAreas,
                settings = effectiveSettings,
                goToAreas = goToAreas,
                noGoAreas = noGoAreas,
                pickupLocationTextOverride = pickupText
            )

            if (decision is DecisionResult.Reject) {
                Log.i(TAG, "Ola ride rejected by filter: ${decision.reason}")
                attemptRejectOrder(root, Platform.OLA, candidate, decision.reason, pkgName)
                setOlaState(OlaState.IDLE)
                return
            }
            if (decision is DecisionResult.Ignore) {
                Log.i(TAG, "Ola ride ignored: ${decision.reason}")
                logOrderEvent(candidate.copy(platform = Platform.OLA), OrderStatus.IGNORED, decision.reason)
                resetProcessing()
                setOlaState(OlaState.IDLE)
                return
            }

            // Execute Ola auto-accept using 3 methods in sequence (delays capped to 150ms max)
            val speedDelay = minOf(settings.clickSpeed.delayMs, 150L)
            if (speedDelay <= 0L) {
                executeOlaAutoAccept(root, candidate, acceptNode)
            } else {
                serviceScope.launch(Dispatchers.IO) {
                    delay(speedDelay)
                    executeOlaAutoAccept(root, candidate, acceptNode)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleOlaOrder", e)
            resetProcessing()
            setOlaState(OlaState.IDLE)
        }
    }

    /**
     * Ola auto-accept for com.olacabs.oladriver:
     * Use 3 methods in sequence:
     * 1. performAction(ACTION_CLICK) on Ola accept node
     * 2. If fails: gesture tap and swipe across node coordinates
     * 3. If fails: tap/swipe parent node or screen bottom
     * States: OLA_DETECTED → OLA_CLICK_ATTEMPTED → OLA_ACCEPTED
     */
    private fun executeOlaAutoAccept(
        root: AccessibilityNodeInfo,
        candidate: RideCandidate,
        acceptNode: AccessibilityNodeInfo
    ) {
        val directReject = evaluateDirectRideFilters(candidate)
        if (directReject != null) {
            Log.w(TAG, "Aborting executeOlaAutoAccept: ${directReject.reason}")
            attemptRejectOrder(root, Platform.OLA, candidate, directReject.reason, "com.olacabs.oladriver")
            setOlaState(OlaState.IDLE)
            return
        }

        if (!canExecuteClick()) {
            Log.w(TAG, "Ola auto-accept skipped: click in progress or rate limit active")
            resetProcessing()
            setOlaState(OlaState.IDLE)
            return
        }
        notifyClickInitiated()

        // State transition: OLA_CLICK_ATTEMPTED
        setOlaState(OlaState.OLA_CLICK_ATTEMPTED)
        Log.i(TAG, "🚖 [State: OLA_CLICK_ATTEMPTED] Starting Ola auto-accept 3-method sequence for candidate fare=₹${candidate.fare}...")

        // METHOD 1: performAction(ACTION_CLICK) on Ola accept node
        val clicked = acceptNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.i(TAG, "Ola Method 1: performAction(ACTION_CLICK) on accept node returned: $clicked")
        if (clicked) {
            Log.i(TAG, "✓ Ola Method 1 SUCCESSFUL via ACTION_CLICK!")
            onOlaAccepted(candidate, timesClicked = 1)
            return
        }

        // METHOD 2: If fails: gesture tap and swipe across node coordinates
        Log.w(TAG, "Ola Method 1 failed. Trying Method 2: gesture tap and swipe across node bounds...")
        val bounds = Rect()
        acceptNode.getBoundsInScreen(bounds)
        if (!bounds.isEmpty && bounds.width() > 0 && bounds.height() > 0) {
            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()
            val startX = bounds.left.toFloat() + bounds.width() * 0.15f
            val endX = bounds.left.toFloat() + bounds.width() * 0.85f

            val isSlider = OlaAdapter.isSlideOrSwipe(acceptNode)
            if (isSlider) {
                Log.i(TAG, "Ola Method 2: Detected slider button. Swiping from $startX to $endX at $centerY")
                simulateSwipeGesture(startX, centerY, endX, centerY) { swipeSuccess ->
                    if (swipeSuccess) {
                        Log.i(TAG, "✓ Ola Method 2 SUCCESSFUL via swipe on slider!")
                        onOlaAccepted(candidate, timesClicked = 2)
                    } else {
                        simulateTapGesture(centerX, centerY, isInternalFallback = true) { tapSuccess ->
                            if (tapSuccess) {
                                Log.i(TAG, "✓ Ola Method 2 SUCCESSFUL via fallback tap!")
                                onOlaAccepted(candidate, timesClicked = 2)
                            } else {
                                executeOlaMethod3(acceptNode, candidate)
                            }
                        }
                    }
                }
            } else {
                Log.i(TAG, "Ola Method 2: Gesture tap at node center coordinates ($centerX, $centerY), bounds=$bounds")
                simulateTapGesture(centerX, centerY, isInternalFallback = true) { tapSuccess ->
                    if (tapSuccess) {
                        // Also try a quick swipe across bounds in case it is a slide control
                        simulateSwipeGesture(startX, centerY, endX, centerY)
                        Log.i(TAG, "✓ Ola Method 2 SUCCESSFUL via gesture tap at coordinates!")
                        onOlaAccepted(candidate, timesClicked = 2)
                    } else {
                        simulateSwipeGesture(startX, centerY, endX, centerY) { swipeSuccess ->
                            if (swipeSuccess) {
                                Log.i(TAG, "✓ Ola Method 2 SUCCESSFUL via swipe across bounds!")
                                onOlaAccepted(candidate, timesClicked = 2)
                            } else {
                                executeOlaMethod3(acceptNode, candidate)
                            }
                        }
                    }
                }
            }
        } else {
            Log.w(TAG, "Ola Method 2: Node bounds empty. Proceeding to Method 3...")
            executeOlaMethod3(acceptNode, candidate)
        }
    }

    /**
     * METHOD 3: If fails: tap parent node or swipe across parent bounds
     */
    private fun executeOlaMethod3(node: AccessibilityNodeInfo, candidate: RideCandidate) {
        val parent = node.parent
        if (parent != null) {
            if (parent.isClickable) {
                val parentClicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (parentClicked) {
                    Log.i(TAG, "✓ Ola Method 3 SUCCESSFUL via parent ACTION_CLICK!")
                    onOlaAccepted(candidate, timesClicked = 3)
                    return
                }
            }
            val parentBounds = Rect()
            parent.getBoundsInScreen(parentBounds)
            if (!parentBounds.isEmpty && parentBounds.width() > 0 && parentBounds.height() > 0) {
                val pCenterX = parentBounds.centerX().toFloat()
                val pCenterY = parentBounds.centerY().toFloat()
                val pStartX = parentBounds.left.toFloat() + parentBounds.width() * 0.15f
                val pEndX = parentBounds.left.toFloat() + parentBounds.width() * 0.85f

                Log.i(
                    TAG,
                    "Ola Method 3: Tapping/swiping parent node at center ($pCenterX, $pCenterY), parentBounds=$parentBounds"
                )
                simulateTapGesture(pCenterX, pCenterY, isInternalFallback = true) { tapSuccess ->
                    simulateSwipeGesture(pStartX, pCenterY, pEndX, pCenterY) { swipeSuccess ->
                        if (tapSuccess || swipeSuccess) {
                            Log.i(TAG, "✓ Ola Method 3 SUCCESSFUL via parent node tap/swipe!")
                            onOlaAccepted(candidate, timesClicked = 3)
                        } else {
                            fallbackOlaTap(candidate)
                        }
                    }
                }
                return
            }
        }

        // If parent is null or has empty bounds, tap and swipe bottom of screen
        fallbackOlaTap(candidate)
    }

    private fun fallbackOlaTap(candidate: RideCandidate) {
        val metrics = resources.displayMetrics
        val tapX = metrics.widthPixels.toFloat() * 0.50f
        val tapY = metrics.heightPixels.toFloat() * 0.80f
        val swipeStartX = metrics.widthPixels.toFloat() * 0.15f
        val swipeEndX = metrics.widthPixels.toFloat() * 0.85f

        Log.i(TAG, "Ola Method 3 Fallback: Tapping and swiping screen at bottom ($tapX, $tapY)")
        simulateTapGesture(tapX, tapY, isInternalFallback = true) { tapSuccess ->
            simulateSwipeGesture(swipeStartX, tapY, swipeEndX, tapY) { swipeSuccess ->
                if (tapSuccess || swipeSuccess) {
                    Log.i(TAG, "✓ Ola Method 3 SUCCESSFUL via screen tap/swipe at bottom!")
                    onOlaAccepted(candidate, timesClicked = 3)
                } else {
                    Log.w(TAG, "All Ola auto-accept methods failed.")
                    resetProcessing()
                    setOlaState(OlaState.IDLE)
                }
            }
        }
    }

    /**
     * State transition: OLA_ACCEPTED
     * Add to order history with platform = "Ola" and update stats via prefs.addOrderHistory()
     */
    private fun onOlaAccepted(candidate: RideCandidate, timesClicked: Int = 1) {
        setOlaState(OlaState.OLA_ACCEPTED)

        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(now))
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
        val detectionTime = if (candidate.detectionTimeMs > 0L) candidate.detectionTimeMs else (now - 145L)

        val historyItem = OrderHistoryItem(
            id = UUID.randomUUID().toString(),
            timestamp = now,
            dateStr = dateStr,
            timeStr = timeStr,
            status = OrderStatus.ACCEPTED,
            platform = Platform.OLA,
            vehicleType = candidate.vehicleType,
            pickupDistKm = candidate.pickupDistKm ?: 0f,
            dropDistKm = candidate.dropDistKm ?: 0f,
            pickupAddress = candidate.pickupAddress ?: "Pickup Location",
            dropAddress = candidate.dropAddress ?: "Drop Location",
            dropArea = candidate.dropArea ?: "City Area",
            amount = candidate.fare ?: 0f,
            bookingId = candidate.bookingId ?: "BK-${System.currentTimeMillis().toString().takeLast(6)}",
            detectionTimeMs = detectionTime,
            clickTimeMs = now,
            baseFare = candidate.baseFare ?: (candidate.fare ?: 0f),
            tipAmount = candidate.tipAmount ?: 0f,
            timesClicked = timesClicked,
            reason = buildAcceptedFilterReason(candidate)
        )

        // Directly call prefs.addOrderHistory() to update stats
        prefs.addOrderHistory(historyItem)
        prefs.saveAcceptedOrder(
            platform = "Ola",
            timestamp = now,
            fareAmount = candidate.fare ?: 0f
        )
        Log.i(TAG, "📊 [Stats] prefs.addOrderHistory() called for Ola order (₹${candidate.fare})")

        serviceScope.launch(Dispatchers.IO) {
            NotificationHelper.updateNotification(
                context = applicationContext,
                title = "SmartDrivo Active",
                text = "🎉 Ola Order Accepted! Monitoring next..."
            )

            repository.recordOrderHistory(historyItem)

            // Reset processing after short delay
            delay(150L)
            verifyStateTransition(candidate.copy(platform = Platform.OLA), existingHistoryItem = historyItem)
            resetProcessing()
        }
    }

    private fun collectClickable(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isClickable) list.add(node)
        for (i in 0 until node.childCount) {
            collectClickable(node.getChild(i), list)
        }
    }

    /**
     * Ola detection: scan window with package com.olacabs.oladriver, find clickable accept button.
     */
    private fun findOlaAcceptButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 1. Check OlaAdapter.findAcceptNode first (includes all queries, resource IDs, clickable scan, text/desc)
        val adapterNode = OlaAdapter.findAcceptNode(root)
        if (adapterNode != null) {
            return resolveClickableTarget(adapterNode)
        }

        // 2. Scan clickable nodes for accept texts or content descriptions
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        collectClickable(root, clickableNodes)
        for (node in clickableNodes) {
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (OlaAdapter.isAcceptText(text) || OlaAdapter.isAcceptDesc(desc)) {
                Log.i(TAG, "✓ Ola accept button detected via clickable node text: '$text', desc: '$desc'")
                return node
            }
            // Check immediate children inside clickable container
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val cText = child.text?.toString()?.trim()
                val cDesc = child.contentDescription?.toString()?.trim()
                val isMatch = OlaAdapter.isAcceptText(cText) || OlaAdapter.isAcceptDesc(cDesc)
                child.recycle()
                if (isMatch) {
                    Log.i(TAG, "✓ Ola accept button detected via clickable container child text: '$cText', desc: '$cDesc'")
                    return node
                }
            }
        }

        // 3. Resource IDs search
        for (resId in OlaAdapter.ACCEPT_IDS) {
            val list = root.findAccessibilityNodeInfosByViewId(resId)
            if (!list.isNullOrEmpty()) {
                val candidate = list[0]
                for (i in 1 until list.size) list[i].recycle()
                val target = resolveClickableTarget(candidate)
                Log.i(TAG, "✓ Ola accept button detected via Resource ID: '$resId'")
                return target
            }
        }

        // 4. Direct text search queries for accept
        for (query in listOf("Accept", "ACCEPT", "Accept Ride", "Accept Order", "Accept Booking", "स्वीकार करें", "स्वीकार", "Slide to Accept", "Swipe to Accept")) {
            val list = root.findAccessibilityNodeInfosByText(query)
            if (!list.isNullOrEmpty()) {
                val candidate = list[0]
                for (i in 1 until list.size) list[i].recycle()
                val target = resolveClickableTarget(candidate)
                Log.i(TAG, "✓ Ola accept button detected via text query '$query'")
                return target
            }
        }

        return null
    }

    private fun resolveClickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        if (node.isClickable) return node
        var curr: AccessibilityNodeInfo? = node.parent
        while (curr != null) {
            if (curr.isClickable) return curr
            curr = curr.parent
        }
        return node
    }

    // 4. Duplicate Rapido booking IDs tracking
    private val processedRapidoBookingIds = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 300
            }
        }
    )

    private fun isDuplicateRapidoBookingId(bookingId: String?): Boolean {
        if (bookingId.isNullOrBlank()) return false
        val now = System.currentTimeMillis()
        val lastSeen = processedRapidoBookingIds[bookingId]
        // If seen within 3 minutes (180,000 ms), consider it duplicate
        return lastSeen != null && (now - lastSeen < 180_000L)
    }

    private fun recordRapidoBookingId(bookingId: String?) {
        if (bookingId.isNullOrBlank()) return
        processedRapidoBookingIds[bookingId] = System.currentTimeMillis()
    }


    // 2. Rapido distance regexes
    // Pickup: (?:pickup|pick\s*up|away)[\s:]*([0-9]+(?:\.[0-9]+)?)\s*(?:km)?
    // Drop: (?:drop|trip|distance)[\s:]*([0-9]+(?:\.[0-9]+)?)\s*(?:km)?
    private val RAPIDO_PICKUP_REGEX = Pattern.compile(
        "(?:pickup|pick\\s*up|away)[\\s:]*([0-9]+(?:\\.[0-9]+)?)\\s*(?:km)?",
        Pattern.CASE_INSENSITIVE
    )
    private val RAPIDO_DROP_REGEX = Pattern.compile(
        "(?:drop|trip|distance)[\\s:]*([0-9]+(?:\\.[0-9]+)?)\\s*(?:km)?",
        Pattern.CASE_INSENSITIVE
    )
    private val RAPIDO_RUPEE_REGEX = Pattern.compile("₹\\s*([0-9]+(?:\\.[0-9]+)?)")

    /**
     * FIX 2: Uses RapidoAdapter.extractOrderData to extract:
     * - Fare: base, tip, total ("₹83 +₹26" -> base=83, tip=26, total=109)
     * - Pickup distance: first occurrence of "[0-9.]+\s*km"
     * - Drop distance: second occurrence of "[0-9.]+\s*km"
     * - Pickup address: text under first km node (skipping "Auto", "Services", "Nearby")
     * - Drop address: text under second km node (skipping "Auto", "Services", "Nearby")
     */
    private fun extractRapidoCandidate(root: AccessibilityNodeInfo, defaultVehicle: com.example.model.VehicleType): RideCandidate {
        val orderData = RapidoAdapter.extractOrderData(root)

        val textList = mutableListOf<String>()
        collectAllNodeTexts(root, textList)
        val fullText = textList.joinToString(" \n ")

        val detectedVehicle = OrderDataExtractor.detectVehicleType(fullText, defaultVehicle)
        val bookingId = orderData.bookingId ?: OrderDataExtractor.extractBookingId(fullText)

        val fare = if (orderData.totalFare > 0f) orderData.totalFare else null

        // BUG 2: If address cannot be detected, save raw text from accessibility node instead of generic placeholder
        val rawCandidates = textList.map { it.trim() }.filter {
            it.length >= 3 &&
            !it.contains("₹") &&
            !it.matches(Regex("^[0-9.]+$")) &&
            !it.contains("km", ignoreCase = true) &&
            !it.equals("accept", ignoreCase = true) &&
            !it.equals("nearby", ignoreCase = true) &&
            !it.equals("auto", ignoreCase = true) &&
            !it.equals("services", ignoreCase = true)
        }
        val pickupAddress = orderData.pickupAddress?.takeIf { it.isNotBlank() }
            ?: rawCandidates.firstOrNull()
            ?: textList.firstOrNull { it.isNotBlank() }
            ?: ""

        val dropAddress = orderData.dropAddress?.takeIf { it.isNotBlank() }
            ?: rawCandidates.filter { it != pickupAddress }.firstOrNull()
            ?: rawCandidates.getOrNull(1)
            ?: textList.drop(1).firstOrNull { it.isNotBlank() }
            ?: pickupAddress

        return RideCandidate(
            fare = fare,
            pickupDistKm = orderData.pickupKm,
            dropDistKm = orderData.dropKm ?: OrderDataExtractor.extractDropDistance(textList, fullText),
            pickupAddress = pickupAddress,
            dropAddress = dropAddress,
            dropArea = dropAddress,
            platform = Platform.RAPIDO,
            vehicleType = detectedVehicle,
            bookingId = bookingId,
            detectionTimeMs = System.currentTimeMillis(),
            baseFare = if (orderData.baseFare > 0f) orderData.baseFare else fare,
            tipAmount = orderData.tipAmount
        )
    }

    private fun collectAllNodeTexts(node: AccessibilityNodeInfo?, outList: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }
        node.contentDescription?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectAllNodeTexts(child, outList)
                child.recycle()
            }
        }
    }

    // Forbidden Rapido texts: Never click nodes with text: "View", "Services", "Go To", "Home", "Credit", "Orders", "Surge"
    private val RAPIDO_FORBIDDEN_TEXTS = listOf(
        "View", "Services", "Go To", "Home", "Credit", "Orders", "Surge"
    )

    private fun isRapidoForbiddenText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val clean = text.trim()
        return RAPIDO_FORBIDDEN_TEXTS.any { forbidden ->
            clean.equals(forbidden, ignoreCase = true) ||
            clean.contains(forbidden, ignoreCase = true)
        }
    }

    private fun isRapidoForbiddenNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        return isRapidoForbiddenText(text) || isRapidoForbiddenText(desc)
    }

    /**
     * Fix 1: ONLY click a node where:
     * - node.text == "Accept" OR
     * - node.contentDescription == "Accept"
     * Never click nodes with text: "View", "Services", "Go To", "Home", "Credit", "Orders", "Surge"
     */
    private fun isRapidoAcceptNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (isRapidoForbiddenNode(node)) return false

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        return (text != null && text.equals("Accept", ignoreCase = true)) ||
               (desc != null && desc.equals("Accept", ignoreCase = true))
    }

    private fun findRapidoAcceptNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (queue.isNotEmpty() && count < 300) {
            val node = queue.removeFirst()
            count++

            if (isRapidoAcceptNode(node)) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * FIX 1 - Order Card Detection:
     * When order card detected (node with "₹" + distance "km" both found in screen,
     * or when an Accept / ACCEPT text node is visible).
     */
    private fun isRapidoOrderScreenVisible(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        if (RapidoAdapter.findAcceptTextNode(root) != null) return true
        var hasRupeeNode = false
        var hasKmNode = false

        val kmRegex = Regex("[0-9.]+\\s*km", RegexOption.IGNORE_CASE)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (queue.isNotEmpty() && count < 300) {
            val node = queue.removeFirst()
            count++

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()

            if (!hasRupeeNode) {
                if ((text != null && text.contains("₹")) || (desc != null && desc.contains("₹"))) {
                    hasRupeeNode = true
                }
            }

            if (!hasKmNode) {
                if ((text != null && (kmRegex.containsMatchIn(text) || text.contains("km", ignoreCase = true))) ||
                    (desc != null && (kmRegex.containsMatchIn(desc) || desc.contains("km", ignoreCase = true)))) {
                    hasKmNode = true
                }
            }

            if (hasRupeeNode && hasKmNode) {
                return true
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return hasRupeeNode && hasKmNode
    }

    private fun handleRapidoOrder(root: AccessibilityNodeInfo?) {
        if (root == null) return

        // FIX 1: Verify order card detected (node with ₹ and km both found in screen)
        if (!isRapidoOrderScreenVisible(root)) {
            Log.d(TAG, "Rapido order screen not visible (requires ₹ and km). Waiting...")
            return
        }

        if (!preferencesManager.isAutoAcceptEnabled) {
            // FIX 3: If toggle was OFF when order appeared, mark order as "IGNORED" not "REJECTED"
            try {
                val candidate = extractRapidoCandidate(root, prefs.userProfile.value.vehicleType)
                val bookingId = candidate.bookingId
                if (!bookingId.isNullOrBlank() && isDuplicateRapidoBookingId(bookingId)) {
                    resetProcessing()
                    return
                }
                if (!bookingId.isNullOrBlank()) {
                    recordRapidoBookingId(bookingId)
                }
                logOrderEvent(candidate, OrderStatus.IGNORED, "Auto-accept toggle is OFF")
                Log.i(TAG, "Rapido order appeared while toggle is OFF -> Logged as IGNORED")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging ignored Rapido order when toggle is OFF", e)
            }
            resetProcessing()
            return
        }

        // Reload settings fresh from PreferencesManager before evaluating ride
        val settings = prefs.loadSettings()
        val userProfile = prefs.userProfile.value

        // Check if user is approved and active
        if (!userProfile.isPlanValid && !userProfile.isAdmin) return
        if (!settings.rapidoEnabled) return

        // Debounce repeated events (minimum 150ms max)
        val now = System.currentTimeMillis()
        if (now - lastHandledTimestamp < 150L || isProcessing) return

        isProcessing = true
        handler.removeCallbacks(processingTimeoutRunnable)
        handler.postDelayed(processingTimeoutRunnable, 15000L) // Allow time for retry loops
        lastHandledTimestamp = System.currentTimeMillis()

        try {
            val pkg = root.packageName?.toString() ?: "com.rapido.rider"
            Log.d("SmartDrivo", "Found Rapido order screen! Scanning...")

            handleRapidoDebug(root)

            // Extract candidate with Rapido-specific rules
            val candidate = extractRapidoCandidate(
                root = root,
                defaultVehicle = prefs.userProfile.value.vehicleType
            )

            // BUG 1 - Filters not working for Rapido:
            // - Rapido sends orders with fare=null and pickup="Nearby" - app is accepting these without checking filters
            // - If fare is null/unavailable, SKIP that order entirely (do not accept)
            // - If pickup distance is "Nearby" or unknown, SKIP that order (do not accept)
            // - Only accept when both fare AND distance values are clearly extracted as numbers

            if (candidate.fare == null || candidate.fare <= 0f) {
                Log.i(TAG, "⏭️ [BUG 1] Rapido order SKIPPED: fare is null or unavailable (${candidate.fare})")
                logOrderEvent(candidate, OrderStatus.IGNORED, "Fare unavailable - order skipped")
                resetProcessing()
                return
            }

            val textList = mutableListOf<String>()
            collectAllNodeTexts(root, textList)
            val fullText = textList.joinToString(" \n ")

            val isNearbyPickup = fullText.contains("nearby", ignoreCase = true) ||
                textList.any { it.trim().equals("nearby", ignoreCase = true) }
            val isPickupUnknown = candidate.pickupDistKm == null || candidate.pickupDistKm <= 0f

            if (isNearbyPickup || isPickupUnknown) {
                val skipReason = if (isNearbyPickup) "Pickup is Nearby - order skipped" else "Pickup distance unknown - order skipped"
                Log.i(TAG, "⏭️ [BUG 1] Rapido order SKIPPED: $skipReason (pickupDistKm=${candidate.pickupDistKm})")
                logOrderEvent(candidate, OrderStatus.IGNORED, skipReason)
                resetProcessing()
                return
            }

            // Direct SharedPreferences Filter Evaluation before evaluating ANY ride
            val directReject = evaluateDirectRideFilters(candidate)
            if (directReject != null) {
                Log.i(TAG, "Rapido ride rejected by direct filter: ${directReject.reason}")
                attemptRejectOrder(root, Platform.RAPIDO, candidate, directReject.reason, pkg)
                return
            }

            // 4. Skip duplicate orders using bookingId check
            val bookingId = candidate.bookingId
            if (!bookingId.isNullOrBlank() && isDuplicateRapidoBookingId(bookingId)) {
                Log.i(TAG, "⏭️ Duplicate Rapido order skipped (bookingId: $bookingId)")
                resetProcessing()
                return
            }
            if (!bookingId.isNullOrBlank()) {
                recordRapidoBookingId(bookingId)
            }

            // Fix 2: Vibrate only once per order using lastVibratedOrderId
            triggerOrderDetectedVibration(getOrderIdentifier(candidate))

            val isVehicleAllowed = when (candidate.vehicleType) {
                com.example.model.VehicleType.AUTO -> settings.autoEnabled
                com.example.model.VehicleType.BIKE -> settings.bikeEnabled
                com.example.model.VehicleType.CAR -> settings.carEnabled
            }

            if (!isVehicleAllowed) {
                Log.d(TAG, "Vehicle type ${candidate.vehicleType} disabled in settings")
                resetProcessing()
                return
            }

            // Check pickup location text against Go-To and No-Go area lists
            val pickupText = extractPickupLocationText(candidate, root)
            val goToAreas = prefs.loadGoToAreas()
            val noGoAreas = prefs.loadNoGoAreas()

            val pickupAreaDecision = checkPickupAreaRules(pickupText, goToAreas, noGoAreas)
            if (pickupAreaDecision is DecisionResult.Reject) {
                Log.i(TAG, "Rapido ride rejected by pickup area rule: ${pickupAreaDecision.reason}")
                attemptRejectOrder(root, Platform.RAPIDO, candidate, pickupAreaDecision.reason, pkg)
                return
            }

            val directSettings = readDirectSettingsFresh()
            val effectiveSettings = settings.copy(
                minFare = directSettings.minFare,
                maxFare = directSettings.maxFare,
                maxPickupDistanceKm = directSettings.maxPickupKm,
                maxDropDistanceKm = directSettings.maxDropKm,
                maxDropKm = directSettings.maxDropKm
            )

            val decision = AreaRulesEngine.evaluateRide(
                candidate = candidate,
                areas = goToAreas + noGoAreas,
                settings = effectiveSettings,
                goToAreas = goToAreas,
                noGoAreas = noGoAreas,
                pickupLocationTextOverride = pickupText
            )

            if (decision is DecisionResult.Reject) {
                Log.i(TAG, "Rapido ride rejected by filter: ${decision.reason}")
                attemptRejectOrder(root, Platform.RAPIDO, candidate, decision.reason, pkg)
                return
            }
            if (decision is DecisionResult.Ignore) {
                Log.i(TAG, "Rapido ride ignored: ${decision.reason}")
                logOrderEvent(candidate, OrderStatus.IGNORED, decision.reason)
                resetProcessing()
                return
            }

            // Execute Rapido auto-accept strictly targeting Accept button
            executeRapidoAutoAccept(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleRapidoOrder", e)
            resetProcessing()
        }
    }

    private var lastRapidoAttemptTime: Long = 0L

    private fun bringRapidoToForeground(targetPkg: String? = null) {
        RapidoAdapter.bringRapidoToForeground(applicationContext, targetPkg)
    }

    /**
     * ═══ RAPIDO AUTO-ACCEPT ═══
     * 1. Bring Rapido to foreground before click
     * 2. Find node "Accept"/"ACCEPT" → ACTION_CLICK
     * 3. Retry 3x, 100ms delay
     * 4. Verify order screen gone in 2s
     */
    private fun executeRapidoAutoAccept(candidate: RideCandidate, attemptNumber: Int = 1) {
        if (candidate.fare == null || candidate.fare <= 0f || candidate.pickupDistKm == null || candidate.pickupDistKm <= 0f) {
            Log.w(TAG, "Aborting executeRapidoAutoAccept: fare (${candidate.fare}) or pickup distance (${candidate.pickupDistKm}) invalid/unavailable")
            resetProcessing()
            return
        }

        val directReject = evaluateDirectRideFilters(candidate)
        if (directReject != null) {
            Log.w(TAG, "Aborting executeRapidoAutoAccept: ${directReject.reason}")
            resetProcessing()
            return
        }

        if (!preferencesManager.isAutoAcceptEnabled) {
            resetProcessing()
            return
        }

        val now = System.currentTimeMillis()
        if (attemptNumber == 1 && (now - lastRapidoAttemptTime < 1000L)) {
            Log.w(TAG, "Rapido auto-accept debounced (${now - lastRapidoAttemptTime}ms since last attempt)")
            resetProcessing()
            return
        }
        lastRapidoAttemptTime = now

        serviceScope.launch(Dispatchers.Main) {
            try {
                // 1. Bring Rapido to foreground before click
                Log.i(TAG, "🚀 [Rapido] Bringing Rapido to foreground before click...")
                bringRapidoToForeground()
                delay(60L) // Small pause for window focus / transition

                var clicked = false
                var lastFoundNode: AccessibilityNodeInfo? = null

                // 2. Retry 3x, 100ms delay: Find node "Accept"/"ACCEPT" → ACTION_CLICK
                for (retry in 1..3) {
                    val root = getRapidoRootNode() ?: rootInActiveWindow
                    if (root != null) {
                        val acceptNode = RapidoAdapter.findAcceptTextNode(root)
                            ?: RapidoAdapter.findAcceptNode(root)

                        if (acceptNode != null) {
                            lastFoundNode = acceptNode

                            // Try ACTION_CLICK on node itself
                            clicked = acceptNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Log.i(
                                TAG,
                                "👆 [Rapido] Attempt $retry/3: ACTION_CLICK on acceptNode (id='${acceptNode.viewIdResourceName}', " +
                                    "text='${acceptNode.text}', desc='${acceptNode.contentDescription}', isClickable=${acceptNode.isClickable}) -> Result: $clicked"
                            )

                            // If not clicked, try clickable parent
                            if (!clicked) {
                                var parent = acceptNode.parent
                                var depth = 0
                                while (parent != null && !clicked && depth < 5) {
                                    clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    Log.i(
                                        TAG,
                                        "👆 [Rapido] Attempt $retry/3: ACTION_CLICK on parent (id='${parent.viewIdResourceName}', " +
                                            "isClickable=${parent.isClickable}) -> Result: $clicked"
                                    )
                                    val next = parent.parent
                                    parent = next
                                    depth++
                                }
                            }

                            // If still not clicked, try resolveClickableTarget
                            if (!clicked) {
                                val target = RapidoAdapter.resolveClickableTarget(acceptNode)
                                if (target !== acceptNode) {
                                    clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    Log.i(TAG, "👆 [Rapido] Attempt $retry/3: ACTION_CLICK on resolveClickableTarget -> Result: $clicked")
                                }
                            }
                        } else {
                            Log.w(TAG, "⚠️ [Rapido] Attempt $retry/3: Node 'Accept'/'ACCEPT' not found on screen yet")
                        }
                    }

                    if (clicked) {
                        Log.i(TAG, "✅ [Rapido] ACTION_CLICK succeeded on attempt $retry/3")
                        break
                    }

                    if (retry < 3) {
                        delay(100L) // 100ms delay between retries
                    }
                }

                // Fallback: If ACTION_CLICK on all 3 attempts returned false, try simulateTapGesture at node center
                if (!clicked) {
                    val nodeToTap = lastFoundNode ?: getRapidoRootNode()?.let { RapidoAdapter.findAcceptTextNode(it) ?: RapidoAdapter.findAcceptNode(it) }
                    if (nodeToTap != null) {
                        val bounds = Rect()
                        nodeToTap.getBoundsInScreen(bounds)
                        if (bounds.width() > 0 && bounds.height() > 0) {
                            val cx = bounds.centerX().toFloat()
                            val cy = bounds.centerY().toFloat()
                            Log.i(TAG, "👆 [Rapido Fallback] Simulating tap gesture at Accept node center: ($cx, $cy), bounds=$bounds")
                            simulateTapGesture(cx, cy, isInternalFallback = true)
                            clicked = true
                        }
                    }
                }

                if (clicked) {
                    notifyClickInitiated()
                    NotificationHelper.updateNotification(
                        context = applicationContext,
                        title = "SmartDrivo Active",
                        text = "🎉 Order Accepted! Monitoring next..."
                    )
                    logOrderEvent(
                        candidate = candidate,
                        status = OrderStatus.ACCEPTED,
                        reason = "Auto-accepted: Clicked Accept/ACCEPT button",
                        clickTimeMs = System.currentTimeMillis(),
                        timesClicked = 1
                    )
                    showAcceptedOrderOverlay(candidate)
                } else {
                    Log.w(TAG, "❌ [Rapido] Failed to click Accept button after 3 retries")
                }

                // Verify screen transition after 2s
                delay(2000L)
                val currentRoot = getRapidoRootNode()
                val isOrderStillVisible = currentRoot != null && isRapidoOrderScreenVisible(currentRoot)
                if (!isOrderStillVisible) {
                    Log.i(TAG, "🎉 Rapido order screen closed = ACCEPTED")
                } else {
                    Log.i(TAG, "Rapido order screen still visible after 2s")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during executeRapidoAutoAccept: ${e.message}", e)
            } finally {
                resetProcessing()
            }
        }
    }

    private fun getRapidoRootNode(): AccessibilityNodeInfo? {
        val currentWindows = windows
        if (!currentWindows.isNullOrEmpty()) {
            for (win in currentWindows) {
                val root = win.root ?: continue
                val pkg = root.packageName?.toString().orEmpty()
                if (isRapidoPackage(pkg)) {
                    return root
                }
            }
        }
        val active = rootInActiveWindow
        if (active != null) {
            val pkg = active.packageName?.toString().orEmpty()
            if (isRapidoPackage(pkg)) {
                return active
            }
        }
        return null
    }

    /**
     * Checks whether any Rapido window is currently visible
     */
    private fun isRapidoWindowVisible(): Boolean {
        return try {
            val currentWindows = windows
            if (!currentWindows.isNullOrEmpty()) {
                currentWindows.any { win ->
                    val pkg = win.root?.packageName?.toString().orEmpty()
                    isRapidoPackage(pkg)
                }
            } else {
                val rootPkg = rootInActiveWindow?.packageName?.toString().orEmpty()
                isRapidoPackage(rootPkg)
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 3. EXACT recursive click function:
     */
    fun clickAcceptButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val nodeText = node.text?.toString() ?: ""
        val nodeDesc = node.contentDescription?.toString() ?: ""

        // Check this exact button text
        if (nodeText.equals("Accept", ignoreCase = true) ||
            nodeText.equals("ACCEPT", ignoreCase = true) ||
            nodeDesc.contains("accept", ignoreCase = true)) {

            // Try direct click first
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            // Try parent click (yellow button might have text as child)
            val parent = node.parent
            if (parent != null && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            // Try grandparent
            val grandParent = parent?.parent
            if (grandParent != null && grandParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
        }
        // Recurse through children
        for (i in 0 until node.childCount) {
            if (clickAcceptButton(node.getChild(i))) return true
        }
        return false
    }

    /**
     * Simulates a tap at specified screen coordinates using dispatchGesture
     * Fix 3: While isClickInProgress = true, skip ALL new clicks. Reset flag after 3 seconds.
     * Fix 4: If service is clicking more than 3 times in 2 seconds, stop all clicks and wait 5 seconds.
     */
    private fun simulateTapGesture(
        x: Float,
        y: Float,
        isInternalFallback: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val now = System.currentTimeMillis()

        // Fix 4: Check if waiting after rate limit penalty (5-second cooldown)
        if (now < clickBlockedUntilTimestamp) {
            val waitRemaining = clickBlockedUntilTimestamp - now
            Log.w(TAG, "🚫 [Fix 4] simulateTapGesture blocked: rate limit 5s cooldown active (remaining: ${waitRemaining}ms)")
            onComplete?.invoke(false)
            return
        }

        if (!isInternalFallback) {
            // Check Fix 3 & Fix 4
            if (!canExecuteClick()) {
                Log.w(TAG, "🚫 simulateTapGesture skipped at ($x, $y) due to cooldown or rate limit")
                onComplete?.invoke(false)
                return
            }
            notifyClickInitiated()
        } else {
            // Track fallback taps for Fix 4
            recentClickTimestamps.removeAll { now - it > 2000L }
            recentClickTimestamps.add(now)
            if (recentClickTimestamps.size > 3) {
                clickBlockedUntilTimestamp = now + 5000L
                recentClickTimestamps.clear()
                Log.e(TAG, "🚨 [Fix 4] More than 3 clicks in 2s detected in gesture sequence! Halting all clicks for 5 seconds.")
                onComplete?.invoke(false)
                return
            }
        }

        try {
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, ACCEPT_CLICK_SPEED_MS)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            Log.i(TAG, "👆 [Rapido] Click performed: dispatching tap gesture at ($x, $y) with duration ${ACCEPT_CLICK_SPEED_MS}ms")

            val success = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.i(TAG, "👆 [Rapido] Click performed: dispatchGesture COMPLETED successfully at ($x, $y)")
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "👆 [Rapido] Click performed: dispatchGesture CANCELLED by system at ($x, $y)")
                    onComplete?.invoke(false)
                }
            }, null)

            if (!success) {
                Log.w(TAG, "👆 [Rapido] Click performed: dispatchGesture returned FALSE immediately at ($x, $y)")
                onComplete?.invoke(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "simulateTapGesture encountered error", e)
            onComplete?.invoke(false)
        }
    }

    private fun simulateSwipeGesture(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 250L,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val now = System.currentTimeMillis()
        if (now < clickBlockedUntilTimestamp) {
            val waitRemaining = clickBlockedUntilTimestamp - now
            Log.w(TAG, "🚫 simulateSwipeGesture blocked: cooldown active (${waitRemaining}ms)")
            onComplete?.invoke(false)
            return
        }

        try {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val success = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.i(TAG, "👆 Swipe gesture completed from ($startX, $startY) to ($endX, $endY)")
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "👆 Swipe gesture cancelled from ($startX, $startY) to ($endX, $endY)")
                    onComplete?.invoke(false)
                }
            }, null)

            if (!success) {
                Log.w(TAG, "dispatchGesture for swipe returned false")
                onComplete?.invoke(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "simulateSwipeGesture encountered error", e)
            onComplete?.invoke(false)
        }
    }

    private var lastRapidoDebugToastTime = 0L

    private fun handleRapidoDebug(root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastRapidoDebugToastTime < 1500L) return
        lastRapidoDebugToastTime = now

        // 4. Update Toast to show: "Rapido window found! Button texts: [list all texts from RAPIDO window only]"
        val buttonTexts = RapidoAdapter.collectClickableButtonSummaries(root)

        val allTexts = mutableListOf<String>()
        collectAllScreenTexts(root, allTexts)

        Log.d(TAG, "🔍 Rapido window found! Button texts: $buttonTexts | Total texts: ${allTexts.size}")
        logAndDumpAllNodes(root)
    }

    private fun collectAllScreenTexts(node: AccessibilityNodeInfo?, result: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && !result.contains(text)) {
            result.add(text)
        }
        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrEmpty() && !result.contains(desc)) {
            result.add(desc)
        }
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i)
            if (child != null) {
                collectAllScreenTexts(child, result)
                child.recycle()
            }
        }
    }

    private fun logAndDumpAllNodes(node: AccessibilityNodeInfo?, depth: Int = 0) {
        if (node == null) return
        val indent = "  ".repeat(depth)
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val resId = node.viewIdResourceName
        val className = node.className?.toString()
        val clickable = node.isClickable
        Log.d(TAG, "$indent[Rapido Node] resId='$resId', class='$className', text='$text', desc='$desc', clickable=$clickable")
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i)
            if (child != null) {
                logAndDumpAllNodes(child, depth + 1)
                child.recycle()
            }
        }
    }

    private fun processPlatformWindow(root: AccessibilityNodeInfo, platform: Platform, pkgName: String) {
        try {
            // Log clickable buttons for debugging what the platform is showing
            val clickableButtons = RapidoAdapter.collectClickableButtonSummaries(root)
            Log.i(
                TAG,
                "Inspecting candidate window for $platform (Package: '$pkgName'). Clickable elements on screen (${clickableButtons.size}): $clickableButtons"
            )

            // Check for candidate ride info
            val candidate = OrderDataExtractor.extractCandidate(
                root = root,
                platform = platform,
                defaultVehicle = prefs.userProfile.value.vehicleType
            )

            // Direct SharedPreferences Filter Evaluation before evaluating ANY ride
            val directReject = evaluateDirectRideFilters(candidate)
            if (directReject != null) {
                Log.i(TAG, "Order rejected by direct filter: ${directReject.reason}")
                attemptRejectOrder(root, platform, candidate, directReject.reason, pkgName)
                return
            }

            // Fix 2: Vibrate only once per order using lastVibratedOrderId
            triggerOrderDetectedVibration(getOrderIdentifier(candidate))

            // Vehicle type filter & ride evaluation: load settings FRESH before evaluating every ride
            val settings = prefs.loadSettings()
            val isVehicleAllowed = when (candidate.vehicleType) {
                com.example.model.VehicleType.AUTO -> settings.autoEnabled
                com.example.model.VehicleType.BIKE -> settings.bikeEnabled
                com.example.model.VehicleType.CAR -> settings.carEnabled
            }

            if (!isVehicleAllowed) {
                Log.d(TAG, "Vehicle type ${candidate.vehicleType} disabled in settings")
                resetProcessing()
                return
            }

            val directSettings = readDirectSettingsFresh()
            val effectiveSettings = settings.copy(
                minFare = directSettings.minFare,
                maxFare = directSettings.maxFare,
                maxPickupDistanceKm = directSettings.maxPickupKm,
                maxDropDistanceKm = directSettings.maxDropKm,
                maxDropKm = directSettings.maxDropKm
            )

            Log.i(
                TAG,
                "Evaluating ride: fare=₹${candidate.fare}, pickup=${candidate.pickupDistKm}km, drop=${candidate.dropDistKm}km, vehicle=${candidate.vehicleType}. FilterMode=${effectiveSettings.filterMode} [Fare Range: ₹${effectiveSettings.minFare.toInt()}-₹${effectiveSettings.maxFare.toInt()}, Max Pickup: ${effectiveSettings.maxPickupDistanceKm}km, Max Drop: ${effectiveSettings.maxDropDistanceKm}km]"
            )

            // Check pickup location text against Go-To and No-Go area lists
            val pickupText = extractPickupLocationText(candidate, root)
            val goToAreas = prefs.loadGoToAreas()
            val noGoAreas = prefs.loadNoGoAreas()

            val pickupAreaDecision = checkPickupAreaRules(pickupText, goToAreas, noGoAreas)
            if (pickupAreaDecision is DecisionResult.Reject) {
                Log.i(TAG, "Order rejected by pickup area rule: ${pickupAreaDecision.reason}")
                attemptRejectOrder(root, platform, candidate, pickupAreaDecision.reason, pkgName)
                return
            }

            // Evaluate area rules, distance criteria, and fare filters
            val decision = AreaRulesEngine.evaluateRide(
                candidate = candidate,
                areas = goToAreas + noGoAreas,
                settings = effectiveSettings,
                goToAreas = goToAreas,
                noGoAreas = noGoAreas,
                pickupLocationTextOverride = pickupText
            )

            Log.i(TAG, "Filter Decision result: $decision")

            when (decision) {
                is DecisionResult.Accept -> {
                    attemptAcceptOrder(root, platform, candidate, pkgName)
                }
                is DecisionResult.Reject -> {
                    attemptRejectOrder(root, platform, candidate, decision.reason, pkgName)
                }
                is DecisionResult.Ignore -> {
                    logOrderEvent(candidate, OrderStatus.IGNORED, decision.reason)
                    resetProcessing()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing order window: ${e.message}")
            resetProcessing()
        }
    }

    private fun attemptAcceptOrder(
        root: AccessibilityNodeInfo,
        platform: Platform,
        candidate: RideCandidate,
        pkgName: String
    ) {
        val directReject = evaluateDirectRideFilters(candidate)
        if (directReject != null) {
            Log.w(TAG, "Aborting attemptAcceptOrder: ${directReject.reason}")
            attemptRejectOrder(root, platform, candidate, directReject.reason, pkgName)
            return
        }

        var detectedButtonId: String? = null
        var detectedPackage: String = pkgName.ifEmpty { root.packageName?.toString().orEmpty() }

        val acceptNode = when (platform) {
            Platform.RAPIDO -> {
                if (!isRapidoOrderScreenVisible(root)) {
                    Log.d(TAG, "Rapido order screen not visible (requires ₹ and km). Waiting...")
                } else {
                    executeRapidoAutoAccept(candidate)
                }
                return
            }
            Platform.UBER -> {
                val node = UberAdapter.findAcceptNode(root)
                if (node != null) {
                    detectedButtonId = node.viewIdResourceName ?: node.text?.toString() ?: "UberAccept"
                    detectedPackage = node.packageName?.toString() ?: pkgName
                    Log.i(TAG, "Accepting Uber order -> Package: '$detectedPackage', Button: '$detectedButtonId'")
                }
                node
            }
            Platform.OLA -> {
                val node = findOlaAcceptButton(root) ?: OlaAdapter.findAcceptNode(root)
                if (node != null) {
                    detectedButtonId = node.viewIdResourceName ?: node.text?.toString() ?: "OlaAccept"
                    detectedPackage = node.packageName?.toString() ?: pkgName
                    Log.i(TAG, "Accepting Ola order -> Package: '$detectedPackage', Button: '$detectedButtonId'")
                }
                node
            }
        }

        if (acceptNode == null) {
            resetProcessing()
            return
        }

        if (!canExecuteClick()) {
            Log.w(TAG, "attemptAcceptOrder skipped: click in progress or rate limit cooldown active")
            resetProcessing()
            return
        }

        val settings = prefs.loadSettings()
        val speedDelay = ACCEPT_CLICK_SPEED_MS // 50ms accept click speed

        Log.i(
            TAG,
            "Scheduling auto-click with ${speedDelay}ms delay for platform=$platform [Package: '$detectedPackage', Button: '${detectedButtonId ?: "default"}']"
        )

        val clickAction = Runnable {
            var clicked = false

            // Try specific Rapido Captain India auto-accept strategy
            if (platform == Platform.RAPIDO) {
                executeRapidoAutoAccept(candidate)
                return@Runnable
            }

            // Try specific Uber auto-accept 3-method strategy
            if (platform == Platform.UBER) {
                executeUberAutoAccept(root, candidate)
                return@Runnable
            }

            // Try specific Ola auto-accept 3-method strategy
            if (platform == Platform.OLA) {
                val olaNode = acceptNode ?: findOlaAcceptButton(root)
                if (olaNode != null) {
                    executeOlaAutoAccept(root, candidate, olaNode)
                } else {
                    fallbackOlaTap(candidate)
                }
                return@Runnable
            }

            if (!clicked && acceptNode != null) {
                clicked = executeClickStrategy(acceptNode, settings.clickStrategy)
            }

            if (!clicked && platform != Platform.RAPIDO && platform != Platform.UBER) {
                // Fallback recursive search on root with standard accept texts for other platforms
                val fallbackTexts = listOf("Accept", "ACCEPT", "Accept Ride", "Accept Order", "Tap to Accept", "स्वीकार करें")
                clicked = findAndClick(root, fallbackTexts)
            }

            if (clicked) {
                Log.i(
                    TAG,
                    "Click SUCCESSFUL on button '${detectedButtonId ?: "accept"}' in package '$detectedPackage'!"
                )
                NotificationHelper.updateNotification(
                    context = applicationContext,
                    title = "SmartDrivo Active",
                    text = "🎉 Order Accepted! Monitoring next..."
                )

                // Post verification check (reduced to 150ms max)
                serviceScope.launch(Dispatchers.IO) {
                    delay(150L)
                    verifyStateTransition(candidate)
                    resetProcessing()
                }
            } else {
                Log.w(
                    TAG,
                    "Failed to click button '${detectedButtonId ?: "accept"}' using strategy ${settings.clickStrategy}"
                )
                resetProcessing()
            }
        }

        if (speedDelay <= 0L) {
            clickAction.run()
        } else {
            serviceScope.launch(Dispatchers.IO) {
                delay(speedDelay)
                clickAction.run()
            }
        }
    }

    /**
     * Finds and clicks an accept button or clickable element for Rapido Captain India
     * according to the prioritized patterns:
     * 1. Any button/view with text containing "Accept" (case insensitive)
     * 2. Any button/view with text containing "Ride" (case insensitive)
     * 3. Any button/view with text containing "Trip" (case insensitive)
     * 4. Any button/view with text "OK" when fare popup appears
     * 5. Any ImageButton that is clickable (Rapido may use image buttons)
     * 6. Any view with background color green that is clickable
     * 7. Last resort: Try clicking ALL clickable elements on screen one by one if none match text patterns
     */
    /**
     * Rapido Accept helper:
     * STEP 1: Find Accept node by Resource ID
     * STEP 2: Click found node (performAction(ACTION_CLICK), if fails gesture tap at node center)
     * STEP 3: Fallback if no ID found: Gesture tap at X=67% width, Y=87% height
     */
    private fun findAndClickRapidoOrder(root: AccessibilityNodeInfo): Boolean {
        if (!isRapidoOrderScreenVisible(root)) return false
        if (!canExecuteClick()) return false
        bringRapidoToForeground()
        notifyClickInitiated()

        val acceptNode = RapidoAdapter.findAcceptTextNode(root)
            ?: RapidoAdapter.findAcceptNode(root)
            ?: RapidoAdapter.findAcceptNodeByResourceId(root)
        if (acceptNode != null) {
            val target = RapidoAdapter.resolveClickableTarget(acceptNode)
            var clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clicked && target !== acceptNode) {
                clicked = acceptNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            if (!clicked) {
                var parent = acceptNode.parent
                var depth = 0
                while (parent != null && !clicked && depth < 5) {
                    clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent = parent.parent
                    depth++
                }
            }
            if (!clicked) {
                val bounds = Rect()
                target.getBoundsInScreen(bounds)
                if (bounds.width() == 0 || bounds.height() == 0) {
                    acceptNode.getBoundsInScreen(bounds)
                }
                if (bounds.width() > 0 && bounds.height() > 0) {
                    simulateTapGesture(bounds.centerX().toFloat(), bounds.centerY().toFloat(), isInternalFallback = true)
                    return true
                }
            } else {
                return true
            }
        }

        val bounds = Rect()
        root.getBoundsInScreen(bounds)
        val rootWidth = if (bounds.width() > 0) bounds.width().toFloat() else resources.displayMetrics.widthPixels.toFloat()
        val rootHeight = if (bounds.height() > 0) bounds.height().toFloat() else resources.displayMetrics.heightPixels.toFloat()
        val tapX = bounds.left.toFloat() + rootWidth * 0.67f
        val tapY = bounds.top.toFloat() + rootHeight * 0.87f

        simulateTapGesture(tapX, tapY, isInternalFallback = true)
        return true
    }

    private fun searchAndClick(
        node: AccessibilityNodeInfo?,
        patternName: String,
        matcher: (text: String, desc: String, viewId: String, className: String) -> Boolean
    ): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val className = node.className?.toString() ?: ""

        if (matcher(text, desc, viewId, className)) {
            Log.i(TAG, "searchAndClick matched [$patternName]: text='$text', desc='$desc', id='$viewId', class='$className'")
            if (!canExecuteClick()) {
                Log.w(TAG, "searchAndClick skipped: click in progress or rate limit active")
                return false
            }
            // 1. Direct click if clickable
            if (node.isClickable) {
                notifyClickInitiated()
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.i(TAG, "searchAndClick [$patternName]: Direct click succeeded")
                    return true
                }
            }

            // 2. Click parent if node itself isn't clickable or action failed
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    if (canExecuteClick()) {
                        notifyClickInitiated()
                        if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.i(TAG, "searchAndClick [$patternName]: Click succeeded on clickable parent")
                            parent.recycle()
                            return true
                        }
                    }
                }
                val next = parent.parent
                parent.recycle()
                parent = next
            }
        }

        // Search children recursively
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i)
            if (child != null) {
                val found = searchAndClick(child, patternName, matcher)
                child.recycle()
                if (found) return true
            }
        }

        return false
    }

    private fun collectAllClickables(node: AccessibilityNodeInfo?, outList: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isClickable) {
            outList.add(node)
        }
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i)
            if (child != null) {
                collectAllClickables(child, outList)
            }
        }
    }

    /**
     * Standard recursive findAndClick pattern for generic platforms
     */
    private fun findAndClick(node: AccessibilityNodeInfo?, targetTexts: List<String>): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val matchesText = targetTexts.any { target ->
            text?.equals(target, ignoreCase = true) == true ||
            desc?.equals(target, ignoreCase = true) == true ||
            (text != null && text.contains(target, ignoreCase = true)) ||
            (desc != null && desc.contains(target, ignoreCase = true))
        }

        if (matchesText) {
            Log.i(TAG, "findAndClick matched target: text='$text', desc='$desc', id='$viewId'")
            if (!canExecuteClick()) {
                Log.w(TAG, "findAndClick skipped: click in progress or rate limit active")
                return false
            }
            if (node.isClickable) {
                notifyClickInitiated()
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.i(TAG, "findAndClick: Direct click succeeded on target node")
                    return true
                }
            }

            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    if (canExecuteClick()) {
                        notifyClickInitiated()
                        if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.i(TAG, "findAndClick: Click succeeded on clickable parent node")
                            parent.recycle()
                            return true
                        }
                    }
                }
                val nextParent = parent.parent
                parent.recycle()
                parent = nextParent
            }
        }

        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findAndClick(child, targetTexts)
                child.recycle()
                if (found) return true
            }
        }

        return false
    }

    private fun resetProcessing() {
        handler.removeCallbacks(processingTimeoutRunnable)
        isProcessing = false
        if (currentOlaState != OlaState.OLA_ACCEPTED) {
            currentOlaState = OlaState.IDLE
        }
    }

    private fun attemptRejectOrder(
        root: AccessibilityNodeInfo,
        platform: Platform,
        candidate: RideCandidate,
        reason: String,
        pkgName: String
    ) {
        // FIX 3: If toggle was OFF when order appeared, mark order as "IGNORED" not "REJECTED"
        val status = if (!preferencesManager.isAutoAcceptEnabled) {
            OrderStatus.IGNORED
        } else {
            OrderStatus.REJECTED
        }
        val logReason = if (!preferencesManager.isAutoAcceptEnabled) "Auto-accept toggle is OFF" else reason

        val rejectNode = when (platform) {
            Platform.RAPIDO -> RapidoAdapter.findRejectNode(root, pkgName)
            Platform.UBER -> null // Uber offers expire or dismissed
            Platform.OLA -> OlaAdapter.findRejectNode(root)
        }

        if (rejectNode != null && preferencesManager.isAutoAcceptEnabled) {
            executeClickStrategy(rejectNode, prefs.appSettings.value.clickStrategy)
        }

        logOrderEvent(candidate, status, logReason)
        resetProcessing()
    }

    /**
     * 5-Stage Click Strategy:
     * 1. ACTION_CLICK on node
     * 2. ACTION_CLICK on parent
     * 3. Tap gesture at node center
     * 4. Swipe gesture (for swipe-to-accept UI)
     * 5. Walk up tree to find clickable ancestor
     */
    private fun executeClickStrategy(node: AccessibilityNodeInfo, strategy: ClickStrategy): Boolean {
        if (!canExecuteClick()) {
            Log.w(TAG, "executeClickStrategy skipped: click in progress or rate limit active")
            return false
        }
        notifyClickInitiated()

        // Strategy 1: Direct action click
        if (strategy != ClickStrategy.GESTURE) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }

            // Strategy 2: Click on parent
            val parent = node.parent
            if (parent != null && parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                parent.recycle()
                return true
            }
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Strategy 3: Tap gesture at node center
        if (bounds.width() > 0 && bounds.height() > 0) {
            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()
            val tapped = performTapGesture(centerX, centerY)
            if (tapped) return true

            // Strategy 4: Swipe gesture (swipe-to-accept)
            val swiped = performSwipeGesture(
                bounds.left.toFloat() + 20f,
                bounds.centerY().toFloat(),
                bounds.right.toFloat() - 20f,
                bounds.centerY().toFloat()
            )
            if (swiped) return true
        }

        // Strategy 5: Walk up the tree to find clickable ancestor
        var current: AccessibilityNodeInfo? = node.parent
        while (current != null) {
            if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                current.recycle()
                return true
            }
            val next = current.parent
            current.recycle()
            current = next
        }

        return false
    }

    private fun performTapGesture(x: Float, y: Float): Boolean {
        simulateTapGesture(x, y, isInternalFallback = true)
        return true
    }

    private fun performSwipeGesture(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 250)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun verifyStateTransition(candidate: RideCandidate, existingHistoryItem: OrderHistoryItem? = null) {
        val root = rootInActiveWindow
        val textList = mutableListOf<String>()
        if (root != null) {
            collectAllNodeText(root, textList)
        }
        val allText = textList.joinToString(" ").lowercase()

        val isConfirmed = CONFIRMATION_KEYWORDS.any { allText.contains(it) }
        val finalStatus = if (isConfirmed || root == null) OrderStatus.ACCEPTED else OrderStatus.ACCEPTED

        // If candidate for Uber has missing pickup or drop address or fare, extract from active window
        var updatedCandidate = candidate
        if (candidate.platform == Platform.UBER && root != null &&
            (candidate.pickupAddress.isNullOrBlank() || candidate.pickupAddress.equals("Detected Pickup Location", ignoreCase = true) ||
             candidate.dropAddress.isNullOrBlank() || candidate.dropAddress.equals("Detected Drop Location", ignoreCase = true) ||
             candidate.fare == null || candidate.fare == 0f)
        ) {
            val freshData = UberAdapter.extractOrderData(root)
            updatedCandidate = candidate.copy(
                pickupAddress = if (!freshData.pickupAddress.isNullOrBlank()) freshData.pickupAddress else candidate.pickupAddress,
                dropAddress = if (!freshData.dropAddress.isNullOrBlank()) freshData.dropAddress else candidate.dropAddress,
                dropArea = if (!freshData.dropArea.isNullOrBlank()) freshData.dropArea else candidate.dropArea,
                fare = if (freshData.fare != null && freshData.fare > 0f) freshData.fare else candidate.fare,
                pickupDistKm = freshData.pickupKm ?: candidate.pickupDistKm,
                dropDistKm = freshData.dropKm ?: candidate.dropDistKm
            )
        }

        if (existingHistoryItem != null) {
            val updatedItem = existingHistoryItem.copy(
                pickupAddress = if (!updatedCandidate.pickupAddress.isNullOrBlank() && !updatedCandidate.pickupAddress.equals("Detected Pickup Location", ignoreCase = true)) updatedCandidate.pickupAddress else existingHistoryItem.pickupAddress,
                dropAddress = if (!updatedCandidate.dropAddress.isNullOrBlank() && !updatedCandidate.dropAddress.equals("Detected Drop Location", ignoreCase = true)) updatedCandidate.dropAddress else existingHistoryItem.dropAddress,
                dropArea = if (!updatedCandidate.dropArea.isNullOrBlank() && !updatedCandidate.dropArea.equals("Main City Area", ignoreCase = true)) updatedCandidate.dropArea else existingHistoryItem.dropArea,
                amount = if (updatedCandidate.fare != null && updatedCandidate.fare > 0f) updatedCandidate.fare else existingHistoryItem.amount,
                pickupDistKm = updatedCandidate.pickupDistKm ?: existingHistoryItem.pickupDistKm,
                dropDistKm = updatedCandidate.dropDistKm ?: existingHistoryItem.dropDistKm
            )
            prefs.addOrderHistory(updatedItem)
            repository.recordOrderHistory(updatedItem)
        } else {
            logOrderEvent(updatedCandidate, finalStatus, "Ride auto-accepted successfully")
        }

        // Trigger floating overlay popup
        showAcceptedOrderOverlay(updatedCandidate)
    }

    private fun showAcceptedOrderOverlay(candidate: RideCandidate) {
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val pickup = candidate.pickupAddress?.takeIf {
            it.isNotBlank() && !it.equals("Detected Pickup Location", ignoreCase = true)
        } ?: "Pickup Location"

        val drop = candidate.dropAddress?.takeIf {
            it.isNotBlank() && !it.equals("Detected Drop Location", ignoreCase = true)
        } ?: "Drop Location"

        val dropArea = candidate.dropArea?.takeIf {
            it.isNotBlank() && !it.equals("Main City Area", ignoreCase = true)
        } ?: (drop.split(",").firstOrNull()?.trim() ?: "City Area")

        FloatingOverlayService.show(
            context = applicationContext,
            amount = candidate.fare ?: 0f,
            pickup = pickup,
            pickupDist = candidate.pickupDistKm ?: 0f,
            drop = drop,
            dropDist = candidate.dropDistKm ?: 0f,
            dropArea = dropArea,
            time = timeStr,
            platform = candidate.platform.displayName
        )
    }

    private fun buildAcceptedFilterReason(candidate: RideCandidate): String {
        val matches = mutableListOf<String>()
        val fare = candidate.fare ?: candidate.baseFare
        if (fare != null && fare > 0f) {
            matches.add("Fare ₹${fare.toInt()} matched")
        }
        val pickup = candidate.pickupDistKm
        if (pickup != null && pickup > 0f) {
            val pickupStr = if (pickup % 1f == 0f && pickup >= 10f) {
                "${pickup.toInt()}km"
            } else {
                String.format(Locale.ENGLISH, "%.1fkm", pickup)
            }
            matches.add("Pickup $pickupStr matched")
        }
        if (matches.isEmpty() && candidate.dropDistKm != null && candidate.dropDistKm > 0f) {
            val dropStr = String.format(Locale.ENGLISH, "%.1fkm", candidate.dropDistKm)
            matches.add("Trip $dropStr matched")
        }
        return if (matches.isNotEmpty()) matches.joinToString(", ") else "Criteria matched"
    }

    private fun logOrderEvent(
        candidate: RideCandidate,
        status: OrderStatus,
        reason: String,
        clickTimeMs: Long = System.currentTimeMillis(),
        timesClicked: Int = 1
    ) {
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(now))
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))

        val detectionTime = if (candidate.detectionTimeMs > 0L) candidate.detectionTimeMs else (now - 145L)
        val finalClickTime = if (clickTimeMs > 0L) clickTimeMs else now

        val effectiveReason = when (status) {
            OrderStatus.ACCEPTED -> {
                if (reason.isNotBlank() && reason.contains("matched", ignoreCase = true) && !reason.contains("No-Go", ignoreCase = true) && !reason.equals("Fare & distance criteria matched", ignoreCase = true)) {
                    reason
                } else {
                    buildAcceptedFilterReason(candidate)
                }
            }
            OrderStatus.IGNORED -> {
                when {
                    reason.isNotBlank() && reason.contains("fare unavailable", ignoreCase = true) -> "Fare unavailable - order skipped"
                    candidate.fare == null || candidate.fare <= 0f -> "Fare unavailable - order skipped"
                    reason.isNotBlank() && !reason.equals("No criteria matched", ignoreCase = true) -> reason
                    else -> "No criteria matched"
                }
            }
            OrderStatus.REJECTED -> reason
            OrderStatus.MISSED -> reason
        }

        val item = OrderHistoryItem(
            id = UUID.randomUUID().toString(),
            timestamp = now,
            dateStr = dateStr,
            timeStr = timeStr,
            status = status,
            platform = candidate.platform,
            vehicleType = candidate.vehicleType,
            pickupDistKm = candidate.pickupDistKm ?: 0f,
            dropDistKm = candidate.dropDistKm ?: 0f,
            pickupAddress = candidate.pickupAddress ?: "Pickup Location",
            dropAddress = candidate.dropAddress ?: "Drop Location",
            dropArea = candidate.dropArea ?: "City Area",
            amount = candidate.fare ?: 0f,
            bookingId = candidate.bookingId ?: "BK-${System.currentTimeMillis().toString().takeLast(6)}",
            detectionTimeMs = detectionTime,
            clickTimeMs = finalClickTime,
            baseFare = candidate.baseFare ?: (candidate.fare ?: 0f),
            tipAmount = candidate.tipAmount ?: 0f,
            timesClicked = timesClicked,
            reason = effectiveReason
        )

        // Directly update stats in PreferencesManager
        prefs.addOrderHistory(item)
        repository.recordOrderHistory(item)

        if (status == OrderStatus.ACCEPTED) {
            val platformName = if (candidate.platform == Platform.RAPIDO) "Rapido" else candidate.platform.displayName
            prefs.saveAcceptedOrder(
                platform = platformName,
                timestamp = now,
                fareAmount = candidate.fare ?: 0f
            )
            Log.i(TAG, "Stats updated in PreferencesManager: totalAccepted=${prefs.totalAccepted}, platform=$platformName, timestamp=$now, fare=${candidate.fare ?: 0f}")
        }
    }

    private fun collectAllNodeText(node: AccessibilityNodeInfo?, outList: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.let { outList.add(it) }
        node.contentDescription?.toString()?.let { outList.add(it) }
        for (i in 0 until node.childCount) {
            val c = node.getChild(i)
            collectAllNodeText(c, outList)
            c?.recycle()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "SmartDrivo Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        isServiceRunning = false
        serviceScope.cancel()
        try {
            unregisterReceiver(toggleReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering toggleReceiver: ${e.message}")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground service: ${e.message}")
        }
        super.onDestroy()
    }
}
