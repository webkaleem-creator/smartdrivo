package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.auth.PickupGuardOwnerAccess
import com.example.data.PreferencesManager
import com.example.model.FilterMode
import com.example.model.OlaAdminFilterSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OlaAdminFilterScreen(
    prefs: PreferencesManager,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit
) {
    val profile by prefs.userProfile.collectAsState()
    val current by prefs.olaAdminFilterSettings.collectAsState()
    val pickupGuard by prefs.pickupGuardSettings.collectAsState()
    val context = LocalContext.current
    if (!PickupGuardOwnerAccess.authorized(profile)) return

    var mode by remember(profile.uid) { mutableStateOf(current.filterMode) }
    var minText by remember(profile.uid) { mutableStateOf(current.minFare.toString()) }
    var maxText by remember(profile.uid) { mutableStateOf(current.maxFare.toString()) }
    var pickupText by remember(profile.uid) { mutableStateOf(current.maxPickupKm.toString()) }
    var tripText by remember(profile.uid) { mutableStateOf(current.maxTripKm.toString()) }
    var message by remember { mutableStateOf("") }

    fun draft(enabled: Boolean) = OlaAdminFilterSettings(
        enabled = enabled,
        filterMode = mode,
        minFare = minText.toFloatOrNull() ?: Float.NaN,
        maxFare = maxText.toFloatOrNull() ?: Float.NaN,
        maxPickupKm = pickupText.toFloatOrNull() ?: Float.NaN,
        maxTripKm = tripText.toFloatOrNull() ?: Float.NaN
    )
    fun save(enabled: Boolean, toast: String) {
        val proposed = draft(enabled)
        if (!proposed.valid()) {
            message = "Check fare and distance limits before saving"
        } else {
            val ok = prefs.saveOlaAdminFilterSettings(proposed)
            message = if (ok) "" else "Owner verification expired"
            if (ok) Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(topBar = { TopAppBar(
        title = { Text("Ola Global Filter") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
    ) }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("OLA TESTING  •  OWNER ONLY")
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ola Testing Mode")
                        Switch(
                            checked = current.enabled,
                            onCheckedChange = { save(it,
                                if (it) "Ola Testing ON ✓" else "Ola Testing OFF") }
                        )
                    }
                    if (pickupGuard.enabled) {
                        Text("Pickup Guard is ON. Its existing priority remains active.")
                    }
                    Text("Fare / Distance / Both")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterMode.entries.forEach { item ->
                            FilterChip(
                                selected = mode == item,
                                onClick = { mode = item },
                                label = { Text(when (item) {
                                    FilterMode.FARE_ONLY -> "Fare"
                                    FilterMode.DISTANCE_ONLY -> "Distance"
                                    FilterMode.BOTH -> "Both"
                                }) }
                            )
                        }
                    }
                    @Composable fun decimalField(title: String, value: String, change: (String) -> Unit) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = change,
                            label = { Text(title) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    decimalField("Minimum Fare (₹)", minText) { minText = it }
                    decimalField("Maximum Fare (₹)", maxText) { maxText = it }
                    decimalField("Maximum Pickup (km)", pickupText) { pickupText = it }
                    decimalField("Maximum Trip (km)", tripText) { tripText = it }
                    Button(onClick = { save(current.enabled, "Ola Global Filter saved ✓") },
                        modifier = Modifier.fillMaxWidth()) { Text("Save Ola Filter") }
                    if (message.isNotBlank()) Text(message)
                    Text("Ola-specific limits are independent. Existing No-Go, Go-To, " +
                        "bundle, +Amount and other shared safety checks still apply.")
                    Text("Testing ON: unmatched offers are ignored without Reject/X. " +
                        "Matching offers use Ola's verified final Accept control.")
                }
            }
            Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Text("Ola Testing History")
            }
        }
    }
}
