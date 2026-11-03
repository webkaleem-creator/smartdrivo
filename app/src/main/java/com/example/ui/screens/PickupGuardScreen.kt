package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.PickupGuardOwnerAccess
import com.example.data.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupGuardScreen(prefs: PreferencesManager, onBack: () -> Unit) {
    val owner by prefs.userProfile.collectAsState()
    val current by prefs.pickupGuardSettings.collectAsState()
    val context = LocalContext.current
    if (!PickupGuardOwnerAccess.authorized(owner)) return
    var input by remember(owner.uid) { mutableStateOf(current.maxPickupKm.toString()) }
    var message by remember { mutableStateOf("") }
    val parsed = input.toFloatOrNull()
    val isValid = parsed != null && parsed.isFinite() && parsed > 0f && parsed <= 30f

    Scaffold(topBar = {
        TopAppBar(title = { Text("Pickup Guard") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("PICKUP GUARD   •   OWNER ONLY")
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Manual Choice Mode")
                        Switch(checked = current.enabled, onCheckedChange = { checked ->
                            if (!checked || isValid) {
                                // Read the latest persisted settings so a recent
                                // numeric auto-save cannot be overwritten by the toggle.
                                val latest = prefs.pickupGuardSettings.value
                                val maximum = if (isValid) parsed!! else latest.maxPickupKm
                                val saved = prefs.savePickupGuardSettings(
                                    latest.copy(enabled = checked, maxPickupKm = maximum)
                                )
                                message = if (saved) "" else "Owner authorization expired"
                                Toast.makeText(
                                    context,
                                    if (!saved) "Owner authorization expired"
                                    else if (checked) "Pickup Guard ON ✓"
                                    else "Pickup Guard OFF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                message = "Enter a valid maximum"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                            // Owner-only premium switch: teal active / slate inactive,
                            // with a clear in-thumb state symbol for accessibility.
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF087F79),
                                checkedThumbColor = Color.White,
                                checkedBorderColor = Color(0xFF06A99F),
                                uncheckedTrackColor = Color(0xFF475569),
                                uncheckedThumbColor = Color.White,
                                uncheckedBorderColor = Color(0xFF64748B)
                            ),
                            thumbContent = {
                                Text(
                                    text = if (current.enabled) "✓" else "−",
                                    color = if (current.enabled) Color(0xFF087F79)
                                        else Color(0xFF475569),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { draft ->
                            input = draft
                            val maximum = draft.toFloatOrNull()
                            if (maximum != null && maximum.isFinite() &&
                                maximum > 0f && maximum <= 30f
                            ) {
                                // Save every valid edited value immediately. No Save button
                                // or delayed job that could be lost when Back is pressed.
                                val latest = prefs.pickupGuardSettings.value
                                if (maximum != latest.maxPickupKm) {
                                    val saved = prefs.savePickupGuardSettings(
                                        latest.copy(maxPickupKm = maximum)
                                    )
                                    message = if (saved) "" else "Owner authorization expired"
                                    if (!saved) {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    message = ""
                                }
                            } else {
                                // Invalid/partial keyboard drafts never overwrite saved limits.
                                message = if (draft.isBlank()) "" else "Enter a valid pickup maximum"
                            }
                        },
                        label = { Text("Max Pickup (km)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = input.isNotBlank() && !isValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Pickup within max and No-Go clear: manual choice, never auto-accept.")
                    Text("Out-of-range or No-Go: verified Reject/X only. Missing data: no action.")
                    Text("Platform offer countdown continues normally.")
                    if (message.isNotBlank()) Text(message)
                }
            }
        }
    }
}
