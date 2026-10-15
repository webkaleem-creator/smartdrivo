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
        reason: String,
        pickupKm: Float? = null,
        tripKm: Float? = null,
        destination: String = "",
        tipAmount: Float? = null
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
                reason = reason,
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

        val root =
            LinearLayout(context).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(7),
                    dp(6),
                    dp(8),
                    dp(6)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            18f * density

                        setColor(
                            0xFF09285C.toInt()
                        )

                        setStroke(
                            dp(1),
                            0xFF639DE4.toInt()
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
                        dp(4),
                        dp(38)
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

        // SMARTDRIVO_TASK2_BLUE_V5
        // Only presentation. No order engine or history changes.
        val reasonTitle = compactReason(status, reason)

        val statusLabel = when (status) {
            OrderStatus.ACCEPTED -> "Accepted"
            OrderStatus.IGNORED -> "Ignored"
            OrderStatus.REJECTED -> "Rejected"
            OrderStatus.SKIPPED -> "Skipped"
            OrderStatus.FAILED -> "Failed"
            OrderStatus.MISSED -> "Missed"
            OrderStatus.PROCESSING -> "Checking"
        }

        val titleColor = when (status) {
            OrderStatus.ACCEPTED ->
                0xFF5EF0AF.toInt()

            OrderStatus.IGNORED ->
                0xFFFF9650.toInt()

            OrderStatus.REJECTED,
            OrderStatus.SKIPPED ->
                0xFFFF794B.toInt()

            OrderStatus.FAILED,
            OrderStatus.MISSED ->
                0xFFFFB15B.toInt()

            OrderStatus.PROCESSING ->
                0xFFFFFFFF.toInt()
        }

        val heading = TextView(context).apply {
            text = "$statusLabel: $reasonTitle"

            setTextColor(titleColor)

            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END

            setPadding(
                0,
                0,
                0,
                dp(5)
            )
        }

        content.addView(heading)

        val dropName = destination
            .trim()
            .substringBefore(" - ")
            .substringBefore(",")
            .trim()

        val summaryParts = mutableListOf<String>()

        if (fare > 0f) {
            summaryParts.add("₹${fare.toInt()}")
        }

        // SMARTDRIVO_TASK2_COMPACT_V6
        // Display actual detected +Amount. Do not alter fare.
        tipAmount?.takeIf {
            it.isFinite() && it > 0f
        }?.let { amount ->
            val amountText =
                if (amount % 1f == 0f) {
                    amount.toInt().toString()
                } else {
                    String.format(
                        java.util.Locale.US,
                        "%.1f",
                        amount
                    )
                }

            summaryParts.add("+₹$amountText")
        }

        if (dropName.isNotBlank() &&
            !dropName.equals(
                "Address unavailable",
                ignoreCase = true
            )
        ) {
            summaryParts.add(dropName)
        }

        if (summaryParts.isEmpty()) {
            summaryParts.add(platform.uppercase())
        }

        val summary = TextView(context).apply {
            val label = summaryParts.joinToString(" | ")
            val styled = android.text.SpannableString(label)

            val greenStart = label.indexOf("+₹")

            if (greenStart >= 0) {
                val nextSeparator =
                    label.indexOf(" | ", greenStart)

                val greenEnd =
                    if (nextSeparator >= 0) {
                        nextSeparator
                    } else {
                        label.length
                    }

                styled.setSpan(
                    android.text.style.ForegroundColorSpan(
                        0xFF4ADE80.toInt()
                    ),
                    greenStart,
                    greenEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                styled.setSpan(
                    android.text.style.StyleSpan(
                        Typeface.BOLD
                    ),
                    greenStart,
                    greenEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            text = styled

            setTextColor(0xFFFFFFFF.toInt())

            textSize = 11f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END

            setPadding(
                0,
                0,
                0,
                dp(2)
            )
        }

        content.addView(summary)

        val fullDrop = destination.trim()

        val shortAddress = when {
            fullDrop.contains(" - ") ->
                fullDrop.substringAfter(" - ").trim()

            fullDrop.contains(",") ->
                fullDrop.substringAfter(",").trim()

            else -> ""
        }

        if (shortAddress.isNotBlank()) {
            val addressText = TextView(context).apply {
                text = shortAddress

                setTextColor(0xFFC2D2EC.toInt())

                textSize = 10.5f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }

            content.addView(addressText)
        } else if (pickupKm != null || tripKm != null) {
            val distances = mutableListOf<String>()

            pickupKm?.takeIf {
                it.isFinite() && it > 0f
            }?.let {
                distances.add(
                    "Pickup " +
                        String.format(
                            java.util.Locale.US,
                            "%.1f",
                            it
                        ) + " km"
                )
            }

            tripKm?.takeIf {
                it.isFinite() && it > 0f
            }?.let {
                distances.add(
                    "Trip " +
                        String.format(
                            java.util.Locale.US,
                            "%.1f",
                            it
                        ) + " km"
                )
            }

            if (distances.isNotEmpty()) {
                content.addView(
                    TextView(context).apply {
                        text = distances.joinToString(" • ")
                        setTextColor(0xFFC2D2EC.toInt())
                        textSize = 10.5f
                        maxLines = 1
                    }
                )
            }
        }

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
                minOf(
                    dp(230),
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