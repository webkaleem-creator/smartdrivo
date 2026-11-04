package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

/**
 * Lightweight, movable overlay; completely separate from ride parsing and accepted-order overlays.
 * Only the tiny control consumes touches. The rest of the screen stays interactive.
 */
internal class PickupGuardBubbleOverlay(
    private val context: Context,
    private val ownerUid: String,
    private val onToggle: () -> Unit
) {
    private val windows = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val positions = context.getSharedPreferences("smartdrivo_owner_pg_bubble_v1", Context.MODE_PRIVATE)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.coerceAtLeast(dp(8))
    private var bubble: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var pressedX = 0f
    private var pressedY = 0f
    private var initialX = 0
    private var initialY = 0
    private var dragged = false

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(),
            context.resources.displayMetrics).toInt()

    private fun xBounds(): IntRange {
        val width = context.resources.displayMetrics.widthPixels
        return dp(8)..(width - dp(76)).coerceAtLeast(dp(8))
    }

    private fun yBounds(): IntRange {
        val height = context.resources.displayMetrics.heightPixels
        return dp(60)..(height - dp(108)).coerceAtLeast(dp(60))
    }

    private fun clampX(value: Int): Int = value.coerceIn(xBounds().first, xBounds().last)
    private fun clampY(value: Int): Int = value.coerceIn(yBounds().first, yBounds().last)

    @SuppressLint("ClickableViewAccessibility")
    fun show(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(context)) {
            hide()
            return
        }
        if (bubble == null) {
            val view = TextView(context).apply {
                gravity = Gravity.CENTER
                minWidth = dp(68)
                minHeight = dp(38)
                setPadding(dp(12), dp(7), dp(12), dp(7))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
                alpha = 0.95f
                elevation = dp(4).toFloat()
                isClickable = true
            }
            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = clampX(positions.getInt("x_$ownerUid",
                    context.resources.displayMetrics.widthPixels - dp(76)))
                y = clampY(positions.getInt("y_$ownerUid",
                    context.resources.displayMetrics.heightPixels / 2))
                windowAnimations = 0
            }
            view.setOnClickListener { onToggle() }
            view.setOnTouchListener { target: View, event: MotionEvent ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        pressedX = event.rawX
                        pressedY = event.rawY
                        initialX = lp.x
                        initialY = lp.y
                        dragged = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - pressedX).toInt()
                        val deltaY = (event.rawY - pressedY).toInt()
                        if (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop) {
                            dragged = true
                        }
                        if (dragged) {
                            lp.x = clampX(initialX + deltaX)
                            lp.y = clampY(initialY + deltaY)
                            runCatching { windows.updateViewLayout(target, lp) }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (dragged) {
                            lp.x = PickupGuardBubblePolicy.snapX(lp.x, xBounds().first, xBounds().last)
                            lp.y = clampY(lp.y)
                            runCatching { windows.updateViewLayout(target, lp) }
                            savePosition(lp)
                        } else {
                            target.performClick()
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        if (dragged) savePosition(lp)
                        true
                    }
                    else -> false
                }
            }
            try {
                windows.addView(view, lp)
                bubble = view
                params = lp
            } catch (_: Exception) {
                bubble = null
                params = null
                return
            }
        }
        val lp = params
        if (lp != null && bubble != null) {
            val safeX = clampX(lp.x)
            val safeY = clampY(lp.y)
            if (lp.x != safeX || lp.y != safeY) {
                lp.x = safeX
                lp.y = safeY
                runCatching { windows.updateViewLayout(bubble, lp) }
            }
        }
        val onBackground = 0xFFE0F8E9.toInt()
        val offBackground = 0xFFF0F3F7.toInt()
        val color = if (enabled) 0xFF166534.toInt() else 0xFF475569.toInt()
        bubble?.apply {
            text = if (enabled) "PG ON" else "PG OFF"
            contentDescription = if (enabled)
                "Pickup Guard is on. Tap to turn off; drag to move."
            else "Pickup Guard is off. Tap to turn on; drag to move."
            setTextColor(color)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(if (enabled) onBackground else offBackground)
                setStroke(dp(1), if (enabled) 0xFFA7E6BC.toInt() else 0xFFCBD5E1.toInt())
            }
        }
    }

    private fun savePosition(lp: WindowManager.LayoutParams) {
        positions.edit()
            .putInt("x_$ownerUid", clampX(lp.x))
            .putInt("y_$ownerUid", clampY(lp.y))
            .apply()
    }

    fun hide() {
        bubble?.let { runCatching { windows.removeViewImmediate(it) } }
        bubble = null
        params = null
    }
}
