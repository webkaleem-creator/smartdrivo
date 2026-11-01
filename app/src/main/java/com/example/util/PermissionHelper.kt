package com.example.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.service.SmartDrivoAccessibilityService
import android.util.Log

object PermissionHelper {

    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val packageName = context.packageName
        return enabledServices.any { it.resolveInfo?.serviceInfo?.packageName == packageName }
    }

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private const val TAG = "PermissionHelper"

    // This action is a restricted @SystemApi in AOSP. Some OEMs may honor it,
    // but ordinary apps generally receive SecurityException. Always fall back.
    private const val ACCESSIBILITY_DETAILS_ACTION =
        "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"

    internal fun serviceComponent(context: Context): ComponentName =
        ComponentName(context, SmartDrivoAccessibilityService::class.java)

    internal fun preferredAccessibilityDetailIntent(context: Context): Intent =
        Intent(ACCESSIBILITY_DETAILS_ACTION).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // AOSP expects the flattened accessibility service component.
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent(context).flattenToString())
        }

    internal fun generalAccessibilityIntent(context: Context): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Non-authoritative hint: a supporting OEM may open the service
            // entry; other phones show the normal Accessibility landing page.
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent(context).flattenToString())
        }

    /** Best-effort service page; never bypasses system permission requirements. */
    fun openSmartDrivoAccessibilitySettings(context: Context) {
        try {
            context.startActivity(preferredAccessibilityDetailIntent(context))
            return
        } catch (e: SecurityException) {
            Log.i(TAG, "Direct accessibility details require system permission")
        } catch (e: android.content.ActivityNotFoundException) {
            Log.i(TAG, "Direct accessibility details unavailable on this phone")
        } catch (e: Exception) {
            Log.w(TAG, "Direct accessibility details unavailable: " + e.javaClass.simpleName)
        }
        openAccessibilitySettings(context)
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            context.startActivity(generalAccessibilityIntent(context))
        } catch (e: Exception) {
            Log.w(TAG, "Accessibility Settings unavailable: " + e.javaClass.simpleName)
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (secondary: Exception) {
                Log.e(TAG, "Unable to open Android Settings", secondary)
            }
        }
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
    }
}
