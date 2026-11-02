package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.auth.PickupGuardOwnerAccess
import com.example.data.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupGuardScreen(prefs: PreferencesManager, onBack: () -> Unit) {
    val owner by prefs.userProfile.collectAsState()
    val current by prefs.pickupGuardSettings.collectAsState()
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
                                val maximum = if (isValid) parsed!! else current.maxPickupKm
                                message = if (prefs.savePickupGuardSettings(
                                    current.copy(enabled = checked, maxPickupKm = maximum)
                                )) "Saved" else "Owner authorization expired"
                            } else message = "Enter a valid maximum"
                        })
                    }
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        label = { Text("Max Pickup (km)") },
                        singleLine = true, isError = input.isNotBlank() && !isValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        if (isValid) message = if (prefs.savePickupGuardSettings(
                            current.copy(maxPickupKm = parsed!!)
                        )) "Saved" else "Owner authorization expired"
                    }, enabled = isValid, modifier = Modifier.fillMaxWidth()) { Text("SAVE") }
                    Text("Pickup within max and No-Go clear: manual choice, never auto-accept.")
                    Text("Out-of-range or No-Go: verified Reject/X only. Missing data: no action.")
                    Text("Platform offer countdown continues normally.")
                    if (message.isNotBlank()) Text(message)
                }
            }
        }
    }
}
