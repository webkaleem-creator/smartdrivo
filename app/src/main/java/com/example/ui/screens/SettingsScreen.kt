package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSecondary
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.LightBackground
import com.example.ui.theme.PlatformOla
import com.example.ui.theme.PlatformRapido
import com.example.ui.theme.PlatformUber
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextDarkTertiary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: PreferencesManager,
    onNavigateToAdminWeb: () -> Unit = {},
    onBack: () -> Unit
) {
    val settings by prefs.appSettings.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var hasUnsavedChanges by rememberSaveable { mutableStateOf(false) }

    var minFareText by rememberSaveable {
        mutableStateOf(
            if (settings.minFare > 0) {
                if (settings.minFare % 1.0f == 0f) settings.minFare.toInt().toString() else settings.minFare.toString()
            } else "50"
        )
    }
    var maxFareText by rememberSaveable {
        mutableStateOf(
            if (settings.maxFare > 0) {
                if (settings.maxFare % 1.0f == 0f) settings.maxFare.toInt().toString() else settings.maxFare.toString()
            } else "999"
        )
    }
    var maxPickupText by rememberSaveable {
        mutableStateOf(
            if (settings.maxPickupDistanceKm > 0) {
                if (settings.maxPickupDistanceKm % 1.0f == 0f) settings.maxPickupDistanceKm.toInt().toString() else settings.maxPickupDistanceKm.toString()
            } else "3.0"
        )
    }
    var maxDropText by rememberSaveable {
        mutableStateOf(
            if (settings.maxDropDistanceKm > 0) {
                if (settings.maxDropDistanceKm % 1.0f == 0f) settings.maxDropDistanceKm.toInt().toString() else settings.maxDropDistanceKm.toString()
            } else "7.5"
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = "Configure auto-accept preferences",
                            fontSize = 14.sp,
                            color = TextDarkSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDarkPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextDarkPrimary
                )
            )
        },
        containerColor = LightBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "top_spacer", contentType = "spacer") { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. FARE & DISTANCE FILTERS (with Fastest Mode toggle)
            item(key = "fare_distance_section", contentType = "settings_card") {
                SectionHeader("1. FARE & DISTANCE FILTERS")
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Fastest Mode Toggle
                        SettingToggleRow(
                            icon = Icons.Default.ElectricBolt,
                            title = "Fastest Mode",
                            subtitle = "Accept every ride, only check Maximum Pickup Distance",
                            isChecked = settings.isFastestModeEnabled,
                            onCheckedChange = { isFastest ->
                                prefs.saveAppSettings(settings.copy(isFastestModeEnabled = isFastest))
                            }
                        )

                        HorizontalDivider(color = CardBorderDefault)

                        if (settings.isFastestModeEnabled) {
                            // When ON: accept every ride, only check Maximum Pickup Distance km field
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(BlueContainer, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = BlueSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Maximum Pickup Distance",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextDarkPrimary
                                        )
                                        Text(
                                            text = "Fastest mode active: All other filters bypassed",
                                            fontSize = 14.sp,
                                            color = TextDarkSecondary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = maxPickupText,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                            maxPickupText = input
                                            hasUnsavedChanges = true
                                        }
                                    },
                                    label = { Text("Max Pickup Distance (km)", fontSize = 14.sp) },
                                    placeholder = { Text("3.0", color = TextDarkTertiary, fontSize = 14.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BlueSecondary,
                                        focusedLabelColor = BlueSecondary,
                                        unfocusedBorderColor = CardBorderDefault,
                                        focusedTextColor = TextDarkPrimary,
                                        unfocusedTextColor = TextDarkPrimary
                                    )
                                )
                            }
                        } else {
                            // When OFF: show all normal filters (fare, distance etc.)
                            // Fare Criteria Section
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(BlueContainer, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CurrencyRupee,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Fare Criteria",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextDarkPrimary
                                        )
                                        Text(
                                            text = "Set minimum and maximum accepted fares",
                                            fontSize = 14.sp,
                                            color = TextDarkSecondary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = minFareText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                minFareText = input
                                                hasUnsavedChanges = true
                                            }
                                        },
                                        label = { Text("Minimum Fare (₹)", fontSize = 14.sp) },
                                        placeholder = { Text("50", color = TextDarkTertiary, fontSize = 14.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BluePrimary,
                                            focusedLabelColor = BluePrimary,
                                            unfocusedBorderColor = CardBorderDefault,
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = maxFareText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                maxFareText = input
                                                hasUnsavedChanges = true
                                            }
                                        },
                                        label = { Text("Maximum Fare (₹)", fontSize = 14.sp) },
                                        placeholder = { Text("999", color = TextDarkTertiary, fontSize = 14.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BluePrimary,
                                            focusedLabelColor = BluePrimary,
                                            unfocusedBorderColor = CardBorderDefault,
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = CardBorderDefault)

                            // Distance Criteria Section
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(BlueContainer, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = BlueSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Distance Criteria",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextDarkPrimary
                                        )
                                        Text(
                                            text = "Specify pickup and drop thresholds",
                                            fontSize = 14.sp,
                                            color = TextDarkSecondary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = maxPickupText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                maxPickupText = input
                                                hasUnsavedChanges = true
                                            }
                                        },
                                        label = { Text("Max Pickup (km)", fontSize = 14.sp) },
                                        placeholder = { Text("3.0", color = TextDarkTertiary, fontSize = 14.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BlueSecondary,
                                            focusedLabelColor = BlueSecondary,
                                            unfocusedBorderColor = CardBorderDefault,
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = maxDropText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                maxDropText = input
                                                hasUnsavedChanges = true
                                            }
                                        },
                                        label = { Text("Max Drop (km)", fontSize = 14.sp) },
                                        placeholder = { Text("7.5", color = TextDarkTertiary, fontSize = 14.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BlueSecondary,
                                            focusedLabelColor = BlueSecondary,
                                            unfocusedBorderColor = CardBorderDefault,
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Save Settings Button
                        Button(
                            onClick = {
                                val minF = minFareText.toFloatOrNull() ?: settings.minFare
                                val maxF = maxFareText.toFloatOrNull() ?: settings.maxFare
                                val maxP = maxPickupText.toFloatOrNull() ?: settings.maxPickupDistanceKm
                                val maxD = maxDropText.toFloatOrNull() ?: settings.maxDropDistanceKm

                                prefs.saveAppSettings(
                                    settings.copy(
                                        minFare = minF,
                                        maxFare = maxF,
                                        maxPickupDistanceKm = maxP,
                                        maxDropDistanceKm = maxD
                                    )
                                )
                                hasUnsavedChanges = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saved ✓")
                                }
                            },
                            enabled = hasUnsavedChanges,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_settings_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluePrimary,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE0E0E0),
                                disabledContentColor = Color(0xFF9E9E9E)
                            )
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 2. SUPPORTED PLATFORMS (Rapido, Uber, Ola toggles)
            item(key = "platforms_section", contentType = "settings_card") {
                SectionHeader("2. SUPPORTED PLATFORMS")
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Rapido Toggle
                        SettingToggleRow(
                            icon = Icons.Default.ElectricBolt,
                            iconTint = PlatformRapido,
                            title = "Rapido",
                            subtitle = "Auto-accept orders for Rapido Captain",
                            isChecked = settings.rapidoEnabled,
                            onCheckedChange = { isEnabled ->
                                prefs.saveAppSettings(settings.copy(rapidoEnabled = isEnabled))
                            }
                        )

                        HorizontalDivider(color = CardBorderDefault)

                        // Uber Toggle
                        SettingToggleRow(
                            icon = Icons.Default.NearMe,
                            iconTint = PlatformUber,
                            title = "Uber",
                            subtitle = "Auto-accept orders for Uber Driver",
                            isChecked = settings.uberEnabled,
                            onCheckedChange = { isEnabled ->
                                prefs.saveAppSettings(settings.copy(uberEnabled = isEnabled))
                            }
                        )

                        HorizontalDivider(color = CardBorderDefault)

                        // Ola Toggle
                        SettingToggleRow(
                            icon = Icons.Default.Layers,
                            iconTint = PlatformOla,
                            title = "Ola",
                            subtitle = "Auto-accept orders for Ola Driver",
                            isChecked = settings.olaEnabled,
                            onCheckedChange = { isEnabled ->
                                prefs.saveAppSettings(settings.copy(olaEnabled = isEnabled))
                            }
                        )
                    }
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = BluePrimary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconTint: Color = BluePrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(BlueContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDarkPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextDarkSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BluePrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFBDBDBD)
            )
        )
    }
}
