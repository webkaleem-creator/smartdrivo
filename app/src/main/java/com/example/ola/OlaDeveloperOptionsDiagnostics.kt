package com.example.ola

import android.content.Context
import android.provider.Settings

/**
 * Android app-visible diagnostics only. Neither app-reported Developer Options
 * nor USB Debugging values can prove the real device state: Android documents
 * that both settings always return 0 to third-party applications.
 *
 * No Shizuku shell calls, Settings writes, security bypasses or engine hooks.
 */
object OlaDeveloperOptionsDiagnostics {
    data class Snapshot(
        val developerDescription: String,
        val usbDescription: String,
        val wirelessDescription: String
    )

    private fun readSetting(context: Context, key: String): Int? =
        try {
            Settings.Global.getInt(context.contentResolver, key, -1).takeIf { it >= 0 }
        } catch (_: Exception) {
            null
        }

    /** Must NOT label a masked 0 as an authoritative OFF state. */
    fun maskedStatus(value: Int?): String = when (value) {
        0 -> "App-reported 0 (masked; actual state unknown)"
        1 -> "App-reported 1 (not independently verified)"
        null -> "Unavailable to this app"
        else -> "Unexpected app-reported value ($value)"
    }

    fun wirelessStatus(value: Int?): String = when (value) {
        1 -> "Reported ON (connection not tested)"
        0 -> "Reported OFF (connection not tested)"
        null -> "Unavailable to this app"
        else -> "Unexpected app-reported value ($value)"
    }

    fun read(context: Context): Snapshot = Snapshot(
        developerDescription = maskedStatus(
            readSetting(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)
        ),
        usbDescription = maskedStatus(
            readSetting(context, Settings.Global.ADB_ENABLED)
        ),
        wirelessDescription = wirelessStatus(
            readSetting(context, "adb_wifi_enabled")
        )
    )
}
