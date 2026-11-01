package com.example.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import androidx.test.core.app.ApplicationProvider
import com.example.service.BankingQuickTileService
import com.example.ui.screens.BankingQuickTileActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BankingQuickTileManifestTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()

    @Test fun tileIsProtectedBySystemBindingPermission() {
        val c = ComponentName(app, BankingQuickTileService::class.java)
        val info = app.packageManager.getServiceInfo(c, PackageManager.GET_META_DATA)
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", info.permission)
        assertTrue(info.exported)
        val tileIntent = Intent(TileService.ACTION_QS_TILE).setPackage(app.packageName)
        val services = app.packageManager.queryIntentServices(tileIntent, 0)
        assertTrue(services.any { it.serviceInfo.name.endsWith("BankingQuickTileService") })
    }

    @Test fun tileConfirmationScreenCannotBeOpenedByOtherApps() {
        val c = ComponentName(app, BankingQuickTileActivity::class.java)
        val info = app.packageManager.getActivityInfo(c, 0)
        assertFalse(info.exported)
    }
}
