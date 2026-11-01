package com.example.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ola-only local diagnostic markers. No screen text, addresses, passenger
 * details, booking IDs or account identifiers. Never affects ride decisions.
 */
object OlaDiagnosticStore {
    enum class Stage {
        SERVICE_CONNECTED, EVENT_RECEIVED, PROCESSOR_ENTERED, ROOT_SELECTED,
        NO_OLA_ROOT, MASTER_OFF, OLA_DISABLED, PLAN_INACTIVE, BUSY,
        BUTTON_MISSING, BUTTON_FOUND, OFFER_PARSED_FARE, OFFER_INCOMPLETE,
        BOOKING_DUPLICATE, HISTORY_DETECTED, DIRECT_FILTER_REJECT,
        DIRECT_FILTER_IGNORE, VEHICLE_MISMATCH, AREA_REJECT,
        FILTER_REJECT, FILTER_IGNORE, FILTER_PASS, WATCH_STARTED,
        WATCH_NO_ROOT, COUNTDOWN_WAIT, READY_BUTTON_MISSING,
        READY_BUTTON_FOUND, OFFER_CHANGED, CLICK_ATTEMPT,
        ACTION_CLICK_OK, ACTION_CLICK_FAILED, GESTURE_DISPATCHED,
        ACCEPT_CONFIRMED, ORDER_ENDED, WATCH_TIMEOUT, WATCH_ERROR
    }

    private const val PREF = "smartdrivo_ola_diagnostics_v1"
    private const val EVENTS = "events"
    private const val MAX_EVENTS = 120
    private const val THROTTLE_MS = 1800L
    private val lock = Any()
    private val lastStageAt = mutableMapOf<Stage, Long>()

    fun record(context: Context, stage: Stage, number: Int? = null) {
        try {
            val now = System.currentTimeMillis()
            synchronized(lock) {
                val previous = lastStageAt[stage] ?: 0L
                if (now - previous in 0 until THROTTLE_MS) return
                lastStageAt[stage] = now
                val safeNumber = number?.coerceIn(-1, 100000)
                val entry = now.toString() + "|" + stage.name + "|" + (safeNumber?.toString() ?: "-")
                val prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                val old = prefs.getString(EVENTS, "").orEmpty()
                val trimmed = (old.lineSequence().filter { it.isNotBlank() }.toList() + entry)
                    .takeLast(MAX_EVENTS).joinToString("\n")
                prefs.edit().putString(EVENTS, trimmed).apply()
            }
        } catch (_: Exception) {
            // Local diagnostics must never interrupt an Ola order.
        }
    }

    fun export(context: Context): String = synchronized(lock) {
        val prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = prefs.getString(EVENTS, "").orEmpty()
        val dates = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val items = raw.lineSequence().filter { it.isNotBlank() }.mapNotNull { row ->
            val columns = row.split('|')
            if (columns.size != 3 || Stage.entries.none { it.name == columns[1] }) null
            else {
                val millis = columns[0].toLongOrNull()
                if (millis == null) null
                else dates.format(Date(millis)) + " | " + columns[1] +
                    (if (columns[2] == "-") "" else " | value=" + columns[2])
            }
        }.toList()
        buildString {
            appendLine("SmartDrivo Ola Diagnostics v1")
            appendLine("Privacy: no addresses, passenger details, booking IDs or screen text.")
            appendLine("Values: ROOT_SELECTED=window score; OFFER_PARSED_FARE=rupees; COUNTDOWN_WAIT=seconds; CLICK_ATTEMPT=attempt number.")
            appendLine("Missing events can mean no Ola accessibility events were delivered.")
            appendLine("Events: " + items.size + " of max " + MAX_EVENTS)
            appendLine("---")
            if (items.isEmpty()) appendLine("No Ola events recorded yet.")
            else items.forEach { appendLine(it) }
        }
    }

    fun summary(context: Context): String = synchronized(lock) {
        val prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val last = prefs.getString(EVENTS, "").orEmpty().lineSequence()
            .lastOrNull { it.isNotBlank() }
        val name = last?.split('|')?.getOrNull(1)
        if (name.isNullOrBlank()) "No Ola events yet" else "Last event: " + name
    }

    fun clear(context: Context) = synchronized(lock) {
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(EVENTS).apply()
        lastStageAt.clear()
    }
}
