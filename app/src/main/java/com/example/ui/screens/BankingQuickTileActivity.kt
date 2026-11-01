package com.example.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.PreferencesManager
import com.example.service.BankingQuickTileService
import com.example.ui.theme.SmartDrivoTheme
import com.example.util.BankingModeController
import com.example.util.PermissionHelper

/**
 * Explicit confirmation for a Quick Settings tile tap.
 * Accessibility is disabled with disableSelf(), but never enabled programmatically.
 * This screen has no login, payment fields, or permission bypass.
 */
class BankingQuickTileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferencesManager.getInstance(applicationContext)
        val bankingActive = BankingModeController.isActive(this)
        setContent {
            SmartDrivoTheme {
                Surface(Modifier.fillMaxSize()) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = {
                            Text(if (bankingActive) "Finish Banking Mode?"
                                 else "Pause SmartDrivo for banking?")
                        },
                        text = {
                            Text(if (bankingActive)
                                "SmartDrivo can only resume when you manually switch on its " +
                                "Accessibility permission in Android Settings. The floating " +
                                "Accessibility shortcut is not needed."
                            else
                                "SmartDrivo will pause all ride monitoring and clicks, hide " +
                                "its order popups, and request Accessibility OFF. " +
                                "Confirm it is OFF before opening your bank app."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (bankingActive) {
                                    val okay = BankingModeController.finishAndOpenSettings(
                                        this@BankingQuickTileActivity, prefs
                                    )
                                    if (okay) {
                                        BankingQuickTileService.refresh(this@BankingQuickTileActivity)
                                        finish()
                                    } else {
                                        Toast.makeText(this@BankingQuickTileActivity,
                                            "Unable to finish Banking Mode", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    val result = BankingModeController.enter(
                                        this@BankingQuickTileActivity, prefs
                                    )
                                    if (result.saved) {
                                        BankingQuickTileService.refresh(this@BankingQuickTileActivity)
                                        if (!result.requestedServiceDisable &&
                                            PermissionHelper.isAccessibilityPermissionGranted(
                                                this@BankingQuickTileActivity
                                            )
                                        ) {
                                            PermissionHelper.openAccessibilitySettings(
                                                this@BankingQuickTileActivity
                                            )
                                        }
                                        Toast.makeText(this@BankingQuickTileActivity,
                                            "Banking Mode ON. Verify Accessibility is OFF.",
                                            Toast.LENGTH_LONG).show()
                                        finish()
                                    } else {
                                        Toast.makeText(this@BankingQuickTileActivity,
                                            "Unable to save Banking Mode", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }) {
                                Text(if (bankingActive) "Finish & open Settings" else "Pause orders")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}
