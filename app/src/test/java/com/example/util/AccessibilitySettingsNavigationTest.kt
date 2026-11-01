package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccessibilitySettingsNavigationTest {
    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun detailRequestTargetsOnlyOurOwnAccessibilityService() {
        val detail = PermissionHelper.preferredAccessibilityDetailIntent(app)
        assertEquals("android.settings.ACCESSIBILITY_DETAILS_SETTINGS", detail.action)
        assertEquals(
            "com.smartdrivo.app/com.example.service.SmartDrivoAccessibilityService",
            detail.getStringExtra(Intent.EXTRA_COMPONENT_NAME)
        )
        assertTrue(detail.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun publicFallbackKeepsCorrectServiceHintButNeverTogglesPermission() {
        val fallback = PermissionHelper.generalAccessibilityIntent(app)
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, fallback.action)
        assertEquals(
            "com.smartdrivo.app/com.example.service.SmartDrivoAccessibilityService",
            fallback.getStringExtra(Intent.EXTRA_COMPONENT_NAME)
        )
    }

    @Test
    fun protectedDetailPermissionDeniedSafelyOpensPublicAccessibilitySettings() {
        val launched = mutableListOf<Intent>()
        val context = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent) {
                launched += intent
                if (intent.action == "android.settings.ACCESSIBILITY_DETAILS_SETTINGS") {
                    throw SecurityException("Android protects service detail activity")
                }
            }
        }
        PermissionHelper.openSmartDrivoAccessibilitySettings(context)
        assertEquals(2, launched.size)
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, launched[1].action)
    }
}
