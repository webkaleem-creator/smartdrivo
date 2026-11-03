package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.PickupGuardOwnerAccess
import com.example.data.PreferencesManager
import com.example.model.FilterMode
import com.example.model.OlaAdminFilterSettings

// Deliberately distinct from Home's Global Filter: Ola has its own permanent owner UI.
private val olaInk = Color(0xFF1A163A)
private val olaViolet = Color(0xFF6A46DA)
private val olaTeal = Color(0xFF069D91)
private val olaAmber = Color(0xFFF4A340)
private val olaMuted = Color(0xFF66647C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OlaAdminFilterScreen(
    prefs: PreferencesManager,
    onBack: () -> Unit
) {
    val profile by prefs.userProfile.collectAsState()
    val saved by prefs.olaAdminFilterSettings.collectAsState()
    val pickupGuard by prefs.pickupGuardSettings.collectAsState()
    val context = LocalContext.current
    if (!PickupGuardOwnerAccess.authorized(profile)) return

    // Draft fields only change with Save; flipping ON/OFF never commits unsaved limits.
    var mode by remember(profile.uid) { mutableStateOf(saved.filterMode) }
    var minText by remember(profile.uid) { mutableStateOf(saved.minFare.toString()) }
    var maxText by remember(profile.uid) { mutableStateOf(saved.maxFare.toString()) }
    var pickupText by remember(profile.uid) { mutableStateOf(saved.maxPickupKm.toString()) }
    var tripText by remember(profile.uid) { mutableStateOf(saved.maxTripKm.toString()) }
    var message by remember(profile.uid) { mutableStateOf("") }

    fun draft() = OlaAdminFilterSettings(
        enabled = saved.enabled,
        filterMode = mode,
        minFare = minText.toFloatOrNull() ?: Float.NaN,
        maxFare = maxText.toFloatOrNull() ?: Float.NaN,
        maxPickupKm = pickupText.toFloatOrNull() ?: Float.NaN,
        maxTripKm = tripText.toFloatOrNull() ?: Float.NaN
    )

    fun toggle(enabled: Boolean) {
        // Use the last saved configuration, not any unsaved form text.
        val next = OlaAdminFilterUiState.switched(saved, enabled)
        if (prefs.saveOlaAdminFilterSettings(next)) {
            message = ""
            Toast.makeText(context, if (enabled) "Ola Global Filter ON" else "Ola Global Filter OFF", Toast.LENGTH_SHORT).show()
        } else {
            message = "Could not update Ola Global Filter. Check admin access."
        }
    }

    fun saveLimits() {
        val next = draft()
        if (!next.valid()) {
            message = "Settings not saved. Check the entered fare and distance limits."
        } else if (prefs.saveOlaAdminFilterSettings(next)) {
            message = ""
            Toast.makeText(context, "Ola Global Filter saved", Toast.LENGTH_SHORT).show()
        } else {
            message = "Could not save Ola Global Filter. Check admin access."
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6F5FC),
        topBar = {
            TopAppBar(
                title = { Text("Ola Global Filter", fontWeight = FontWeight.Bold, color = olaInk) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back", color = olaViolet) } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A vivid, separate Ola identity rather than duplicating Home's blue card.
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF201D53), Color(0xFF5934A4), Color(0xFF087B81))))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("OLA • ADMIN CONTROL", color = Color(0xFFBFEFEA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Your Ola, your rules.", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text("Independent, permanent Ola filters", color = Color(0xFFE0D9FA), fontSize = 12.sp)
                    }
                    Text("✦", color = olaAmber, fontSize = 34.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("OLA GLOBAL FILTER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (saved.enabled) "ACTIVE • Checking Ola orders" else "PAUSED • Ola rules are OFF",
                            color = if (saved.enabled) Color(0xFF9CF4DF) else Color(0xFFE2DAF2),
                            fontSize = 12.sp)
                    }
                    Switch(
                        checked = saved.enabled,
                        onCheckedChange = ::toggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFF19C6AA),
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF625B80),
                            uncheckedThumbColor = Color.White,
                            uncheckedBorderColor = Color(0xFFA5A0BB)
                        )
                    )
                }
                if (pickupGuard.enabled) {
                    Text("Pickup Guard is ON • Its existing priority is preserved.",
                        color = Color(0xFFFFE2AB), fontSize = 12.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Choose filter type", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = olaInk)
                    Text("Only the selected condition type applies to Ola's extra filter.",
                        color = olaMuted, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OlaModeOption("Fare", "₹", FilterMode.FARE_ONLY, mode, olaViolet, { mode = it }, Modifier.weight(1f))
                        OlaModeOption("Distance", "↗", FilterMode.DISTANCE_ONLY, mode, olaAmber, { mode = it }, Modifier.weight(1f))
                        OlaModeOption("Both", "✓", FilterMode.BOTH, mode, olaTeal, { mode = it }, Modifier.weight(1f))
                    }
                    if (OlaAdminFilterUiState.showFare(mode)) {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("FARE LIMITS", color = olaViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OlaLimitInput("Min fare ₹", minText, { minText = it }, olaViolet, Modifier.weight(1f))
                                OlaLimitInput("Max fare ₹", maxText, { maxText = it }, olaViolet, Modifier.weight(1f))
                            }
                        }
                    }
                    if (OlaAdminFilterUiState.showDistance(mode)) {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("DISTANCE LIMITS", color = olaTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OlaLimitInput("Max pickup km", pickupText, { pickupText = it }, olaTeal, Modifier.weight(1f))
                                OlaLimitInput("Max trip km", tripText, { tripText = it }, olaTeal, Modifier.weight(1f))
                            }
                        }
                    }
                    Button(
                        onClick = ::saveLimits,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = olaViolet)
                    ) {
                        Text("Save Ola Filter", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
                    }
                    if (message.isNotBlank()) Text(message, color = Color(0xFFB44136), fontSize = 12.sp)
                    Text("ON/OFF saves immediately. Use Save Ola Filter to store mode and limits.",
                        color = olaMuted, fontSize = 12.sp)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F8F5)),
                border = BorderStroke(1.dp, Color(0xFFB6E6DA)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Protected by existing rules", color = Color(0xFF087A73), fontWeight = FontWeight.Bold)
                    Text(
                        "Ola's separate limits work alongside existing No-Go, Go-To, Bundle, +Amount and other shared checks. Rapido, Uber and Pickup Guard settings stay independent.",
                        color = olaInk, fontSize = 12.sp
                    )
                    Text("Check Ola orders in the main History → Ola filter.",
                        color = Color(0xFF087A73), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun OlaModeOption(
    label: String,
    glyph: String,
    value: FilterMode,
    selected: FilterMode,
    tint: Color,
    onSelect: (FilterMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = value == selected
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) tint else Color(0xFFF5F3FA))
            .clickable { onSelect(value) }
            .padding(vertical = 13.dp, horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(glyph, color = if (active) Color.White else tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (active) Color.White else olaInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OlaLimitInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            focusedLabelColor = accent,
            unfocusedBorderColor = Color(0xFFDAD8E6),
            unfocusedContainerColor = Color(0xFFFCFBFF),
            focusedContainerColor = Color.White
        )
    )
}
