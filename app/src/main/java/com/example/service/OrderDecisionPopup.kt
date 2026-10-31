package com.example.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.model.OrderStatus
import com.example.engine.OrderPopupStatusPolicy
import java.util.concurrent.ConcurrentHashMap

/**
 * Short, non-interactive SmartDrivo decision banner.
 *
 * Intentionally different from the accepted-ride detail overlay:
 * - dark compact card
 * - status color strip/badge
 * - only platform + fare + final reason
 * - auto hides after 8 seconds; a new order immediately replaces the old one
 */
object OrderDecisionPopup {

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val recentKeys =
        ConcurrentHashMap<String, Long>()

    private var activeView: View? = null
    private var activeWindowManager: WindowManager? = null
    private var dismissRunnable: Runnable? = null
    // Accessed on the UI thread only. Preserve confirmed status over queued uncertainty.
    private val shownStates = mutableMapOf<String, Pair<OrderStatus, Long>>()

    // SHORT_POPUP_V7
    @Volatile
    private var latestOrderKey: String = ""

    fun onNewOrderDetected(recordKey: String) {
        val key = recordKey.trim()
        if (key.isBlank() || key == latestOrderKey) return

        latestOrderKey = key

        // Async UI work only. Never block Rapido order processing.
        mainHandler.post {
            if (latestOrderKey == key) {
                removeActive()
            }
        }
    }

    private const val DISPLAY_MS = 8000L
    private const val DEDUPE_MS = 5000L

    private data class Style(
        val label: String,
        val accent: Int,
        val badgeText: Int
    )

    private fun styleFor(
        status: OrderStatus
    ): Style {
        return when (status) {
            OrderStatus.ACCEPTED ->
                Style(
                    label = "ACCEPTED",
                    accent = 0xFF4ADE80.toInt(),
                    badgeText = Color.WHITE
                )

            OrderStatus.IGNORED ->
                Style(
                    label = "IGNORED",
                    accent = 0xFF7C3AED.toInt(),
                    badgeText = Color.WHITE
                )

            OrderStatus.REJECTED,
            OrderStatus.SKIPPED ->
                Style(
                    label =
                        if (status == OrderStatus.SKIPPED)
                            "SKIPPED"
                        else
                            "REJECTED",
                    accent = 0xFFF97316.toInt(),
                    badgeText = Color.WHITE
                )

            OrderStatus.FAILED,
            OrderStatus.MISSED ->
                Style(
                    label =
                        if (status == OrderStatus.MISSED)
                            "MISSED"
                        else
                            "FAILED",
                    accent = 0xFFE11D48.toInt(),
                    badgeText = Color.WHITE
                )

            OrderStatus.UNCONFIRMED ->
                Style(
                    label = "NOT VERIFIED",
                    accent = 0xFFFBBF24.toInt(),
                    badgeText = Color.WHITE
                )

            OrderStatus.PROCESSING ->
                Style(
                    label = "CHECKING",
                    accent = 0xFF64748B.toInt(),
                    badgeText = Color.WHITE
                )
        }
    }

    fun show(
        context: Context,
        recordKey: String,
        status: OrderStatus,
        platform: String,
        fare: Float,
        reason: String,
        pickupKm: Float? = null,
        tripKm: Float? = null,
        destination: String = "",
        tipAmount: Float? = null
    ) {
        if (status == OrderStatus.PROCESSING) {
            return
        }

        val key = recordKey.trim()

        // A previous order must never overwrite a newer popup.
        if (
            key.isNotBlank() &&
            latestOrderKey.isNotBlank() &&
            key != latestOrderKey
        ) return

        val safeKey = "$key:${status.name}"

        val now =
            System.currentTimeMillis()

        val last =
            recentKeys[safeKey]

        if (
            last != null &&
            now - last < DEDUPE_MS
        ) {
            return
        }

        recentKeys[safeKey] = now

        recentKeys.entries.removeIf {
            now - it.value > 30_000L
        }

        val appContext =
            context.applicationContext

        mainHandler.post {
            if (
                key.isNotBlank() &&
                latestOrderKey.isNotBlank() &&
                key != latestOrderKey
            ) return@post

            // The repository may upgrade UNCONFIRMED -> ACCEPTED later.
            // Pending callbacks must not downgrade an already-confirmed card.
            val previous = shownStates[key]?.first
            if (!OrderPopupStatusPolicy.mayReplace(previous, status)) return@post
            if (key.isNotBlank()) {
                val shownAt = System.currentTimeMillis()
                shownStates[key] = status to shownAt
                shownStates.entries.removeAll { shownAt - it.value.second > 120_000L }
            }

            showOnMain(
                context = appContext,
                status = status,
                platform = platform,
                fare = fare,
                reason = if (status == OrderStatus.UNCONFIRMED)
                    "Rapido confirmation not found. Check trip screen."
                else reason,
                pickupKm = pickupKm,
                tripKm = tripKm,
                destination = destination,
                tipAmount = tipAmount
            )
        }
    }

    private fun showOnMain(
        context: Context,
        status: OrderStatus,
        platform: String,
        fare: Float,
        reason: String,
        pickupKm: Float?,
        tripKm: Float?,
        destination: String,
        tipAmount: Float?
    ) {
        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(context)
        ) {
            return
        }

        removeActive()

        val wm =
            context.getSystemService(
                Context.WINDOW_SERVICE
            ) as? WindowManager
                ?: return

        val density =
            context.resources
                .displayMetrics
                .density

        fun dp(value: Int): Int =
            (value * density).toInt()

        val style =
            styleFor(status)

        // Compact Driver Card V8. Non-touchable so Rapido remains usable.
        // Large fare, short reason and legible area on a calm navy surface.
        fun rounded(color: Int, radiusDp: Int = 15): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusDp * density
                setColor(color)
            }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 17f * density
                setColor(0xFF152238.toInt())
                setStroke(dp(1), style.accent)
            }
            elevation = 10f * density
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(context).apply {
            text = platform.uppercase(java.util.Locale.ROOT) +
                "  •  " + OrderPopupStatusPolicy.statusTitle(status)
            setTextColor(style.accent)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(context).apply {
            text = "●"
            setTextColor(style.accent)
            textSize = 12f
            gravity = Gravity.CENTER
        })
        root.addView(header)

        val reasonTitle = compactReason(status, reason)
        val friendlyReason = if (status == OrderStatus.UNCONFIRMED) {
            "Checking Rapido trip screen is recommended"
        } else reasonTitle
        root.addView(TextView(context).apply {
            text = friendlyReason
            setTextColor(0xFFF4F6FB.toInt())
            textSize = 12f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(4), 0, dp(4))
        })

        val location = destination.trim()
            .substringBefore(" - ")
            .substringBefore(",")
            .trim()
            .takeIf { it.isNotBlank() && !it.equals("Address unavailable", true) }
            ?: platform

        val fareRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        fareRow.addView(TextView(context).apply {
            text = if (fare.isFinite() && fare > 0f) "₹" + fare.toInt() else "₹--"
            setTextColor(0xFF7AF0A3.toInt())
            typeface = Typeface.DEFAULT_BOLD
            textSize = 23f
            maxLines = 1
        })
        fareRow.addView(TextView(context).apply {
            text = "  |  " + location
            setTextColor(0xFFE4EAF5.toInt())
            textSize = 12.5f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        root.addView(fareRow)

        val distances = mutableListOf<String>()
        pickupKm?.takeIf { it.isFinite() && it >= 0f }?.let {
            distances.add("Pickup " + String.format(java.util.Locale.US, "%.1f km", it))
        }
        tripKm?.takeIf { it.isFinite() && it > 0f }?.let {
            distances.add("Trip " + String.format(java.util.Locale.US, "%.1f km", it))
        }
        val details = if (distances.isNotEmpty()) {
            distances.joinToString("  •  ")
        } else {
            destination.trim().substringAfter(",", "").trim().take(65)
        }
        if (details.isNotBlank()) {
            root.addView(TextView(context).apply {
                text = details
                setTextColor(0xFFB8C8E3.toInt())
                textSize = 11f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(4), 0, 0)
            })
        }

        val type =
            if (
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O
            ) {
                WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams
                    .TYPE_PHONE
            }

        val params =
            WindowManager.LayoutParams(
                minOf(
                    dp(288),
                    context.resources.displayMetrics.widthPixels - dp(24)
                ),
                WindowManager.LayoutParams
                    .WRAP_CONTENT,
                type,
                WindowManager.LayoutParams
                    .FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams
                        .FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams
                        .FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity =
                    Gravity.TOP or
                        Gravity.END

                x = dp(12)
                y = dp(52)

                windowAnimations = 0
            }

        try {
            wm.addView(
                root,
                params
            )

            activeView =
                root

            activeWindowManager =
                wm

            val runnable =
                Runnable {
                    removeActive()
                }

            dismissRunnable =
                runnable

            mainHandler.postDelayed(
                runnable,
                DISPLAY_MS
            )
        } catch (_: Exception) {
            removeActive()
        }
    }

    private fun compactReason(
        status: OrderStatus,
        raw: String
    ): String {
        val cleaned = raw
            .replace(
                "Auto Reject:",
                "",
                ignoreCase = true
            )
            .replace(
                "Separate Both",
                "Filter 2",
                ignoreCase = true
            )
            .replace(
                Regex("""\s*\|\s*"""),
                " • "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()

        val noGo = Regex(
            """(?i)No-Go Area:\s*(?:destination matches\s*)?'([^']+)'"""
        ).find(cleaned)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()

        if (!noGo.isNullOrBlank()) {
            return "No-Go Area blocked: $noGo"
        }

        if (status == OrderStatus.ACCEPTED) {
            when {
                cleaned.contains(
                    "+Amount",
                    ignoreCase = true
                ) || cleaned.contains(
                    "+ Amount",
                    ignoreCase = true
                ) -> return "+Amount filter matched"

                cleaned.contains(
                    "Order Filter Unlimited",
                    ignoreCase = true
                ) -> return "Order Filter Unlimited matched"

                cleaned.contains(
                    "Go-To",
                    ignoreCase = true
                ) -> return "Go-To Area matched"

                cleaned.contains(
                    "Fastest",
                    ignoreCase = true
                ) -> return "Fastest Mode matched"
            }
        }

        val offPattern = Regex(
            """(?i)(?:Order\s+)?Filter\s+\d+\s+OFF"""
        )

        val failedPattern = Regex(
            """(?i)(?:Order\s+)?Filter\s+(\d+)\s+failed"""
        )

        val notMatchPattern = Regex(
            """(?i)(?:Order\s+)?Filter\s+(\d+):?\s*Not Match(?:ed)?"""
        )

        val parts = cleaned
            .split('•')
            .map { it.trim() }
            .mapNotNull { part ->
                val failed =
                    failedPattern.matchEntire(part)

                val notMatch =
                    notMatchPattern.matchEntire(part)

                when {
                    part.isBlank() -> null

                    offPattern.matches(part) -> null

                    part.equals(
                        "No condition matched",
                        ignoreCase = true
                    ) -> null

                    failed != null ->
                        "Order Filter ${failed.groupValues[1]} not matched"

                    notMatch != null ->
                        "Order Filter ${notMatch.groupValues[1]} not matched"

                    part.matches(
                        Regex(
                            """(?i)Filter 1:\s*(?:Fare Only|Distance Only|Both)\s+Not Match"""
                        )
                    ) -> "Order Filter 1 not matched"

                    else -> part
                }
            }
            .distinct()

        if (parts.isNotEmpty()) {
            return parts.take(2).joinToString(" • ")
        }

        return when (status) {
            OrderStatus.ACCEPTED ->
                "Saved conditions matched"

            OrderStatus.IGNORED ->
                "No saved filter matched"

            OrderStatus.REJECTED,
            OrderStatus.SKIPPED ->
                "Order blocked by saved conditions"

            OrderStatus.FAILED ->
                "Automatic action could not be completed"

            OrderStatus.MISSED ->
                "Order was missed"

            OrderStatus.UNCONFIRMED ->
                "Rapido result not confirmed"

            OrderStatus.PROCESSING ->
                "Checking order"
        }
    }

    private fun removeActive() {
        dismissRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        dismissRunnable = null

        val view =
            activeView

        val wm =
            activeWindowManager

        activeView = null
        activeWindowManager = null

        if (
            view != null &&
            wm != null
        ) {
            try {
                wm.removeViewImmediate(view)
            } catch (_: Exception) {
            }
        }
    }
}