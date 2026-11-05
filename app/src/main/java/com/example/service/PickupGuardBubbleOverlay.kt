package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Owner-only movable quick bubble. Tap still toggles PG; hold opens separate compact controls.
 * Neither bubble nor menu scans orders or creates a new platform click route.
 */
internal class PickupGuardBubbleOverlay(
    private val context: Context,
    private val ownerUid: String,
    private val onToggleGuard: () -> Unit,
    private val onLongPressMenu: () -> Unit,
    private val onHideAllControls: () -> Unit,
    private val onToggleMainAuto: () -> Unit,
    private val onPickupLimit: (Float) -> Unit,
    private val onPause: (Int) -> Unit,
    private val onResume: () -> Unit,
    private val onEmergencyStop: () -> Unit
) {
    private data class MenuState(
        val guardOn: Boolean,
        val mainOn: Boolean,
        val maxPickup: Float,
        val pausedUntil: Long,
        val locked: Boolean
    )
    private val windows = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val positions = context.getSharedPreferences("smartdrivo_owner_pg_bubble_v1", Context.MODE_PRIVATE)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.coerceAtLeast(dp(8))
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var menu: LinearLayout? = null
    private var menuState = MenuState(false, false, 0.6f, 0L, false)
    private var renderedMenuState: MenuState? = null
    private var pressedX = 0f
    private var pressedY = 0f
    private var initialX = 0
    private var initialY = 0
    private var dragged = false
    private var holdFired = false
    private var fingerDown = false
    private var holdRunnable: Runnable? = null

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

    private val windowType: Int get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun isPositionLocked(): Boolean = positions.getBoolean("lock_" + ownerUid, false)

    /** Position lock is separate from PG and master switch. Long press remains available. */
    fun togglePositionLock(): Boolean {
        val next = !isPositionLocked()
        positions.edit().putBoolean("lock_" + ownerUid, next).apply()
        menuState = menuState.copy(locked = next)
        renderMenu()
        return next
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(guardEnabled: Boolean, mainEnabled: Boolean, pickupKm: Float,
             pausedUntil: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            hide()
            return
        }
        val nextState = MenuState(guardEnabled, mainEnabled, pickupKm, pausedUntil,
            isPositionLocked())
        if (menuState != nextState) {
            menuState = nextState
            renderMenu()
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
                alpha = 0.96f
                elevation = dp(4).toFloat()
                isClickable = true
            }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = clampX(positions.getInt("x_" + ownerUid,
                    context.resources.displayMetrics.widthPixels - dp(76)))
                y = clampY(positions.getInt("y_" + ownerUid,
                    context.resources.displayMetrics.heightPixels / 2))
                windowAnimations = 0
            }
            view.setOnClickListener { onToggleGuard() }
            view.setOnLongClickListener { onLongPressMenu(); true }
            view.setOnTouchListener { target: View, event: MotionEvent ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        pressedX = event.rawX
                        pressedY = event.rawY
                        initialX = lp.x
                        initialY = lp.y
                        dragged = false
                        holdFired = false
                        fingerDown = true
                        cancelHold()
                        holdRunnable = Runnable {
                            if (fingerDown && !dragged) {
                                holdFired = true
                                target.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                onLongPressMenu()
                            }
                        }.also { handler.postDelayed(it, PickupGuardQuickControlPolicy.LONG_PRESS_MS) }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - pressedX).toInt()
                        val deltaY = (event.rawY - pressedY).toInt()
                        if (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop) {
                            dragged = true
                            cancelHold()
                        }
                        if (dragged && !isPositionLocked()) {
                            lp.x = clampX(initialX + deltaX)
                            lp.y = clampY(initialY + deltaY)
                            runCatching { windows.updateViewLayout(target, lp) }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        fingerDown = false
                        cancelHold()
                        if (dragged && !isPositionLocked()) {
                            lp.x = PickupGuardBubblePolicy.snapX(lp.x, xBounds().first, xBounds().last)
                            lp.y = clampY(lp.y)
                            runCatching { windows.updateViewLayout(target, lp) }
                            savePosition(lp)
                            hideMenu()
                        } else if (!dragged && !holdFired) {
                            target.performClick()
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        fingerDown = false
                        cancelHold()
                        if (dragged && !isPositionLocked()) savePosition(lp)
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
        val enabledColor = 0xFF166534.toInt()
        val disabledColor = 0xFF475569.toInt()
        bubble?.apply {
            text = if (guardEnabled) "PG ON" else "PG OFF"
            contentDescription = "Pickup Guard " + (if (guardEnabled) "on" else "off") +
                ". Tap to toggle; hold for quick controls; drag to move when unlocked."
            setTextColor(if (guardEnabled) enabledColor else disabledColor)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(if (guardEnabled) 0xFFE0F8E9.toInt() else 0xFFF0F3F7.toInt())
                setStroke(dp(1), if (guardEnabled) 0xFFA7E6BC.toInt() else 0xFFCBD5E1.toInt())
            }
        }
    }

    private fun cancelHold() {
        holdRunnable?.let { handler.removeCallbacks(it) }
        holdRunnable = null
    }

    private fun text(label: String, size: Float = 12f, color: Int = 0xFF0F172A.toInt(),
                     bold: Boolean = false): TextView = TextView(context).apply {
        this.text = label
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), dp(7), dp(8), dp(7))
        includeFontPadding = false
    }

    private fun button(label: String, onClick: () -> Unit, danger: Boolean = false): TextView =
        text(label, 12f, if (danger) 0xFF9F1239.toInt() else 0xFF155E75.toInt(), true).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(if (danger) 0xFFFFE4E6.toInt() else 0xFFEFF6FF.toInt())
            }
            isClickable = true
            setOnClickListener {
                onClick()
            }
        }

    private fun line(): View = View(context).apply {
        setBackgroundColor(0xFFE2E8F0.toInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
    }

    private fun row(vararg views: View): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            views.forEach { addView(it, LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)) }
        }

    /** A green/red circle shows the actual persisted Main Auto Accept state. */
    private fun mainAutoRow(enabled: Boolean): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(0xFFEFF6FF.toInt())
            }
            isClickable = true
            contentDescription = "Main Auto Accept " + if (enabled) "ON" else "OFF"
            setOnClickListener { onToggleMainAuto() }
            addView(
                text("Main Auto Accept", 12f, 0xFF155E75.toInt(), true),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            val status = text(if (enabled) "✓" else "×", 15f, 0xFFFFFFFF.toInt(), true).apply {
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (enabled) 0xFF16A34A.toInt() else 0xFFDC2626.toInt())
                }
            }
            addView(status, LinearLayout.LayoutParams(dp(27), dp(27)).apply {
                marginEnd = dp(10)
            })
        }

    private fun renderMenu() {
        val panel = menu ?: return
        if (renderedMenuState == menuState) return
        renderedMenuState = menuState
        panel.removeAllViews()
        val state = menuState
        // Fixed 34dp close button: title gets the remaining full width, never two lines.
        val heading = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val headingText = text("SmartDrivo Quick Controls", 12f,
            0xFF0F172A.toInt(), true).apply {
            setSingleLine(true)
            maxLines = 1
        }
        heading.addView(headingText, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        // Hide removes BOTH floating overlays without changing any automation setting.
        // X only dismisses the expanded menu and retains the small PG bubble.
        heading.addView(
            button("Hide", { onHideAllControls() }).apply {
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                contentDescription = "Hide Quick Controls and floating PG bubble; keep Auto Accept ON"
            },
            LinearLayout.LayoutParams(dp(50), dp(34)).apply {
                marginEnd = dp(4)
            }
        )
        heading.addView(
            button("×", { hideMenu() }).apply {
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                contentDescription = "Close Quick Controls"
            },
            LinearLayout.LayoutParams(dp(30), dp(34))
        )
        panel.addView(heading)
        panel.addView(line())
        panel.addView(mainAutoRow(state.mainOn))
        panel.addView(line())
        panel.addView(text("Max Pickup: " + state.maxPickup + " km", 12f,
            0xFF475569.toInt(), true))
        panel.addView(row(
            button("0.4 km", { onPickupLimit(0.4f) }),
            button("0.6 km", { onPickupLimit(0.6f) }),
            button("0.8 km", { onPickupLimit(0.8f) })
        ))
        panel.addView(line())
        if (PickupGuardQuickControlPolicy.isPaused(state.pausedUntil,
                System.currentTimeMillis())) {
            panel.addView(row(
                text("Automation paused", 12f, 0xFF9A3412.toInt(), true),
                button("Resume", { onResume() })
            ))
        } else {
            panel.addView(text("Pause automation", 12f, 0xFF475569.toInt(), true))
            panel.addView(row(
                button("5 min", { onPause(5) }),
                button("10 min", { onPause(10) }),
                button("15 min", { onPause(15) })
            ))
        }
        panel.addView(line())
        panel.addView(button(if (state.locked) "🔒 Unlock popup position" else "🔓 Lock popup position", {
            togglePositionLock()
        }))
        panel.addView(line())
        panel.addView(button("⛔ Emergency Stop — Auto OFF + Guard OFF",
            { onEmergencyStop() }, danger = true))
        panel.addView(text("Hide: both controls disappear; show again in Pickup Guard. Main ON keeps Guard manual.",
            10f, 0xFF64748B.toInt()))
    }

    @SuppressLint("ClickableViewAccessibility")
    fun openMenu() {
        if (menu != null) { hideMenu(); return }
        val position = params ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            elevation = dp(9).toFloat()
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(0xFFF8FAFC.toInt())
                setStroke(dp(1), 0xFFCBD5E1.toInt())
            }
        }
        val width = dp(288).coerceAtMost(context.resources.displayMetrics.widthPixels - dp(16))
        val x = (position.x + dp(72)).let { suggested ->
            if (suggested + width > context.resources.displayMetrics.widthPixels - dp(8))
                (position.x - width - dp(8)).coerceAtLeast(dp(8))
            else suggested
        }
        val y = position.y.coerceIn(dp(48),
            (context.resources.displayMetrics.heightPixels - dp(525)).coerceAtLeast(dp(48)))
        val lp = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT, windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            windowAnimations = 0
        }
        panel.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                hideMenu()
                true
            } else false
        }
        menu = panel
        renderedMenuState = null
        renderMenu()
        try {
            windows.addView(panel, lp)
        } catch (_: Exception) {
            menu = null
            renderedMenuState = null
        }
    }

    fun hideMenu() {
        menu?.let { runCatching { windows.removeViewImmediate(it) } }
        menu = null
        renderedMenuState = null
    }

    private fun savePosition(lp: WindowManager.LayoutParams) {
        positions.edit()
            .putInt("x_" + ownerUid, clampX(lp.x))
            .putInt("y_" + ownerUid, clampY(lp.y))
            .apply()
    }

    fun hide() {
        fingerDown = false
        cancelHold()
        hideMenu()
        bubble?.let { runCatching { windows.removeViewImmediate(it) } }
        bubble = null
        params = null
    }
}
