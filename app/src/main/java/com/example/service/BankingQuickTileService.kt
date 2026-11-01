package com.example.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.graphics.drawable.Icon
import android.util.Log
import com.example.ui.screens.BankingQuickTileActivity
import com.example.util.BankingModeController

/** Quick Settings alternative to Android's persistent Accessibility floating button. */
class BankingQuickTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        // Banking mode can affect ride acceptance. Require an unlocked device
        // and show an explicit confirmation before changing the state.
        unlockAndRun {
            val launch = Intent(this, BankingQuickTileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pending = PendingIntent.getActivity(
                        this, 5719, launch,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pending)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(launch)
                }
            } catch (e: Exception) {
                Log.w("BankingQuickTile", "Unable to launch tile controls", e)
            }
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val isBanking = BankingModeController.isActive(this)
        tile.label = "SmartDrivo Banking"
        tile.state = if (isBanking) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, android.R.drawable.ic_lock_lock)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isBanking) "Orders paused" else "Tap to pause"
        }
        tile.updateTile()
    }

    companion object {
        fun refresh(context: android.content.Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    requestListeningState(
                        context, ComponentName(context, BankingQuickTileService::class.java)
                    )
                }
            } catch (_: Exception) {
                // Not every launcher actively hosts the tile.
            }
        }
    }
}
