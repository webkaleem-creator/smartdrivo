package com.example.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ola.OlaShizukuPolicy
import com.example.ola.OlaDeveloperOptionsDiagnostics
import com.example.ola.OlaReadOnlyPreviewStore
import androidx.compose.runtime.collectAsState
import java.util.Locale
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

/**
 * Owner-only Ola preparation screen, no privileged input, no Ola order processing.
 * Permission is requested ONLY when the owner taps the button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OlaShizukuSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preview by OlaReadOnlyPreviewStore.state.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var showDeveloperOptionsWarning by remember { mutableStateOf(false) }
    val binderListener = remember {
        Shizuku.OnBinderReceivedListener { tick++ }
    }
    val deadListener = remember {
        Shizuku.OnBinderDeadListener { tick++ }
    }
    val permissionListener = remember {
        Shizuku.OnRequestPermissionResultListener { requestCode, result ->
            if (requestCode == 21071) {
                message = if (result == PackageManager.PERMISSION_GRANTED)
                    "SmartDrivo Shizuku permission granted"
                else "Permission denied. Open Shizuku and authorize SmartDrivo."
                tick++
            }
        }
    }
    DisposableEffect(Unit) {
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(deadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        onDispose {
            Shizuku.removeBinderReceivedListener(binderListener)
            Shizuku.removeBinderDeadListener(deadListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        }
    }

    // Only while the setup screen is visible, never in Accessibility order events.
    LaunchedEffect(Unit) {
        while (true) {
            delay(3_000L)
            tick++
        }
    }
    @Suppress("UNUSED_VARIABLE")
    val observedTick = tick
    val serviceRunning = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    val authorized = serviceRunning && runCatching {
        !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
    val status = OlaShizukuPolicy.status(serviceRunning, authorized)
    val stateText = when (status) {
        OlaShizukuPolicy.Status.SERVICE_MISSING -> "Not running / binder unavailable"
        OlaShizukuPolicy.Status.AUTHORIZATION_REQUIRED -> "Running • SmartDrivo permission required"
        OlaShizukuPolicy.Status.READY -> "Running • SmartDrivo authorized"
    }
    val versionText = if (serviceRunning) runCatching {
        "Shizuku server API: " + Shizuku.getVersion()
    }.getOrDefault("Shizuku service detected") else ""
    // Only this owner/admin setup screen reads system-reported indicators.
    // Existing 3s screen refresh triggers a fresh read; no background polling.
    val deviceDiagnostic = remember(tick) {
        OlaDeveloperOptionsDiagnostics.read(context)
    }

    // A real Android Settings workflow, never a stealth setting override.
    if (showDeveloperOptionsWarning) {
        AlertDialog(
            onDismissRequest = { showDeveloperOptionsWarning = false },
            title = { Text("Developer Options • system setting") },
            text = { Text(
                "This opens Android's real Developer Options. Turning them OFF " +
                    "can stop Wireless Debugging, Shizuku, and admin diagnostics. " +
                    "SmartDrivo will not hide or spoof this state from Ola. " +
                    "To restore, turn Developer Options ON again and restart Shizuku. " +
                    "Ola online access is not guaranteed."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    showDeveloperOptionsWarning = false
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                        )
                    } catch (_: Exception) {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }.onFailure {
                            message = "Android Settings could not be opened."
                        }
                    }
                }) { Text("Open system settings") }
            },
            dismissButton = {
                TextButton(onClick = { showDeveloperOptionsWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ola • Shizuku Setup") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("OLA ONLY • ADMIN PREVIEW", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Shizuku service: $stateText")
                    if (versionText.isNotEmpty()) Text(versionText)
                    if (message.isNotBlank()) Text(message)
                    Button(
                        onClick = { tick++ },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Recheck Shizuku") }
                    if (serviceRunning && !authorized) {
                        Button(
                            onClick = {
                                try {
                                    if (Shizuku.shouldShowRequestPermissionRationale()) {
                                        message = "Open Shizuku's Authorized Apps and allow SmartDrivo."
                                    } else {
                                        Shizuku.requestPermission(21071)
                                        message = "Review the permission request from Shizuku."
                                    }
                                } catch (_: Exception) {
                                    message = "Shizuku stopped or permission request unavailable."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Grant SmartDrivo Shizuku access") }
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("How to prepare", fontWeight = FontWeight.Bold)
                    Text("1. Install the official Shizuku application.")
                    Text("2. On Android 11+, use Developer Options → Wireless debugging to pair and start Shizuku.")
                    Text("3. Open SmartDrivo again, then approve its access through Shizuku.")
                    Text("4. On each phone restart, Shizuku may require restarting.")
                    Button(
                        onClick = {
                            val url = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://shizuku.rikka.app/download/"))
                            runCatching { context.startActivity(url) }
                                .onFailure { message = "No browser found for Shizuku website." }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Official Shizuku download & instructions") }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Device diagnostics • READ ONLY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Shizuku service: " + if (serviceRunning) "Running" else "Not running")
                    Text("SmartDrivo Shizuku authorization: " +
                        if (authorized) "Granted" else "Not granted")
                    Text("Developer Options: " + deviceDiagnostic.developerDescription)
                    Text("USB Debugging: " + deviceDiagnostic.usbDescription)
                    Text("Wireless Debugging: " + deviceDiagnostic.wirelessDescription)
                    Text(
                        "Android masks Developer Options and USB Debugging flags from " +
                            "third-party apps. A reported 0 does NOT prove these are OFF. " +
                            "Wireless Debugging is an app-reported indicator, not an ADB " +
                            "connection test. No setting is changed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { tick++ },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Refresh device diagnostics") }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Developer Options • Ola compatibility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(
                        "Open Android's genuine Developer Options to turn them OFF or " +
                            "restore them manually. This is not a hidden bypass."
                    )
                    Text(
                        "Turning OFF may disconnect Wireless Debugging and Shizuku; " +
                            "you may need to turn them ON and restart Shizuku before " +
                            "SmartDrivo can use its Shizuku permission again."
                    )
                    Text("Shizuku now: " + if (serviceRunning) "Running" else "Not running")
                    Button(
                        onClick = { showDeveloperOptionsWarning = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open Developer Options • OFF / Restore") }
                    Text(
                        "No automatic change, security-check spoofing, or ADB " +
                            "state modification is performed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Ola live offer • READ ONLY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Only fare, pickup km, trip km and drop address are captured. " +
                        "Pickup address, rider details and taps are excluded.")
                    val ageMillis = System.currentTimeMillis() - preview.seenAtMillis
                    if (preview.seenAtMillis == 0L || ageMillis > 120_000L) {
                        Text("No recent Ola offer. Keep SmartDrivo Accessibility ON, " +
                            "open an Ola Driver offer, then return here within 2 minutes.")
                    } else {
                        Text(preview.status)
                        val offer = preview.offer
                        if (offer != null) {
                            fun number(value: Double?): String =
                                value?.let { String.format(Locale.US, "%.1f", it) } ?: "Not read"
                            Text("Fare: " + (offer.fareRupees?.let {
                                "₹" + String.format(Locale.US, "%.2f", it)
                            } ?: "Not read"))
                            Text("Pickup: " + number(offer.pickupKm) + " km")
                            Text("Trip: " + number(offer.tripKm) + " km")
                            Text("Drop address: " + (offer.dropAddress ?: "Not read"))
                            Text(if (offer.isComplete) "Complete diagnostic snapshot"
                                 else "Partial: Ola UI IDs require live verification")
                        }
                    }
                }
            }
            Text(
                "Ola auto-accept is NOT enabled by this setup. Permission alone cannot read a valid " +
                    "Ola offer or safely confirm acceptance. Ola engine remains disabled until " +
                    "Ola-specific offer recognition and tap testing are verified. " +
                    "Rapido, Uber and Pickup Guard are unchanged.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
