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
import java.util.concurrent.ConcurrentHashMap

/**
 * Short, non-interactive SmartDrivo decision banner.
 *
 * Intentionally different from the accepted-ride detail overlay:
 * - dark compact card
 * - status color strip/badge
 * - only platform + fare + final reason
 * - auto hides after ~2.8 seconds
 */
object OrderDecisionPopup {

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val recentKeys =
        ConcurrentHashMap<String, Long>()

    private var activeView: View? = null
    private var activeWindowManager: WindowManager? = null
    private var dismissRunnable: Runnable? = null

    private const val DISPLAY_MS = 2800L
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
                    accent = 0xFF14B8A6.toInt(),
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
        reason: String
    ) {
        if (status == OrderStatus.PROCESSING) {
            return
        }

        val safeKey =
            "${recordKey.trim()}:${status.name}"

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
            showOnMain(
                context = appContext,
                status = status,
                platform = platform,
                fare = fare,
                reason = reason
            )
        }
    }

    private fun showOnMain(
        context: Context,
        status: OrderStatus,
        platform: String,
        fare: Float,
        reason: String
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

        val root =
            LinearLayout(context).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(8),
                    dp(8),
                    dp(12),
                    dp(8)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            18f * density

                        setColor(
                            0xFF0B1220.toInt()
                        )

                        setStroke(
                            dp(1),
                            0xFF25324A.toInt()
                        )
                    }

                elevation =
                    14f * density
            }

        val strip =
            View(context).apply {
                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            10f * density

                        setColor(style.accent)
                    }

                layoutParams =
                    LinearLayout.LayoutParams(
                        dp(5),
                        dp(48)
                    ).apply {
                        marginEnd =
                            dp(9)
                    }
            }

        root.addView(strip)

        val content =
            LinearLayout(context).apply {
                orientation =
                    LinearLayout.VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams
                            .WRAP_CONTENT,
                        1f
                    )
            }

        val topRow =
            LinearLayout(context).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val platformText =
            TextView(context).apply {
                text =
                    platform
                        .trim()
                        .ifBlank {
                            "SMARTDRIVO"
                        }
                        .uppercase()

                setTextColor(
                    0xFFE2E8F0.toInt()
                )

                textSize = 11f

                typeface =
                    Typeface.DEFAULT_BOLD

                maxLines = 1

                ellipsize =
                    TextUtils.TruncateAt.END

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams
                            .WRAP_CONTENT,
                        1f
                    )
            }

        topRow.addView(platformText)

        val badge =
            TextView(context).apply {
                text = style.label

                setTextColor(
                    style.badgeText
                )

                textSize = 10f

                typeface =
                    Typeface.DEFAULT_BOLD

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(3),
                    dp(8),
                    dp(3)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            20f * density

                        setColor(
                            style.accent
                        )
                    }
            }

        topRow.addView(badge)

        if (fare > 0f) {
            val fareText =
                TextView(context).apply {
                    text =
                        "  ₹${fare.toInt()}"

                    setTextColor(
                        0xFFF8FAFC.toInt()
                    )

                    textSize = 12f

                    typeface =
                        Typeface.DEFAULT_BOLD
                }

            topRow.addView(fareText)
        }

        content.addView(topRow)

        val reasonText =
            TextView(context).apply {
                text =
                    compactReason(
                        status,
                        reason
                    )

                setTextColor(
                    0xFFCBD5E1.toInt()
                )

                textSize = 11.5f

                maxLines = 2

                ellipsize =
                    TextUtils.TruncateAt.END

                setPadding(
                    0,
                    dp(3),
                    0,
                    0
                )
            }

        content.addView(reasonText)

        root.addView(content)

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
                (context.resources
                    .displayMetrics
                    .widthPixels * 0.90f)
                    .toInt(),
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
                        Gravity.CENTER_HORIZONTAL

                y = dp(70)

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
        var reason =
            raw
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
                    "destination matches",
                    "",
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

        val noGoMatch =
            Regex(
                """(?i)No-Go Area:\s*(?:matches\s*)?'([^']+)'"""
            )
                .find(reason)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()

        if (!noGoMatch.isNullOrBlank()) {
            return "No-Go Area: $noGoMatch"
        }

        reason =
            reason
                .replace(
                    Regex(
                        """(?i)Filter 1\s+Fare Only\s+failed"""
                    ),
                    "Filter 1: Fare Only Not Match"
                )
                .replace(
                    Regex(
                        """(?i)Filter 1\s+Distance Only\s+failed"""
                    ),
                    "Filter 1: Distance Only Not Match"
                )
                .replace(
                    Regex(
                        """(?i)Filter 1\s+Both\s+failed"""
                    ),
                    "Filter 1: Both Not Match"
                )
                .replace(
                    Regex(
                        """(?i)Filter 2\s+failed"""
                    ),
                    "Filter 2: Not Match"
                )
                .replace(
                    Regex(
                        """(?i)No condition matched\s*•?\s*"""
                    ),
                    ""
                )
                .trim()

        if (reason.isNotBlank()) {
            return reason
        }

        return when (status) {
            OrderStatus.ACCEPTED ->
                "Saved conditions matched"

            OrderStatus.IGNORED ->
                "Order did not match saved conditions"

            OrderStatus.REJECTED,
            OrderStatus.SKIPPED ->
                "Order blocked by saved conditions"

            OrderStatus.FAILED ->
                "Automatic action could not be completed"

            OrderStatus.MISSED ->
                "Order was missed"

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