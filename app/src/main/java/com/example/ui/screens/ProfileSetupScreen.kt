package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VehicleType
import com.example.ui.theme.AccentGreen

@Composable
fun ProfileSetupScreen(
    initialName: String = "",
    initialPhone: String = "",
    initialCity: String = "",
    initialState: String = "",
    initialVehicle: VehicleType = VehicleType.AUTO,
    onSaveProfile: (name: String, mobileNumber: String, city: String, state: String, vehicleType: VehicleType) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf(initialName) }
    var mobileNumber by remember {
        mutableStateOf(
            initialPhone.replace("+91", "").trim().takeLast(10)
        )
    }
    var city by remember { mutableStateOf(initialCity) }
    var state by remember { mutableStateOf(initialState) }
    var selectedVehicle by remember { mutableStateOf(initialVehicle) }

    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1628))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Account Setup",
                fontSize = 12.sp,
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Complete Your Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Please provide your mobile number, city and state for admin verification and dashboard synchronization.",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D44))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Full Name
                    Text(
                        text = "Full Name *",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = null
                        },
                        placeholder = { Text("e.g. Ramesh Kumar", color = Color(0xFF64748B), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AccentGreen) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = AccentGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mobile Number (Required for Admin Dashboard)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mobile Number (Required) *",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "For Admin view",
                            fontSize = 11.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 10) {
                                mobileNumber = digitsOnly
                                errorText = null
                            }
                        },
                        prefix = {
                            Text(
                                text = "+91 ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        },
                        placeholder = { Text("10-digit mobile number", color = Color(0xFF64748B), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AccentGreen) },
                        trailingIcon = {
                            if (mobileNumber.isNotEmpty()) {
                                IconButton(onClick = { mobileNumber = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = AccentGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // City & State Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // City Field
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "City *",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = city,
                                onValueChange = {
                                    city = it
                                    errorText = null
                                },
                                placeholder = { Text("e.g. Hyderabad", color = Color(0xFF64748B), fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = AccentGreen,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = AccentGreen
                                )
                            )
                        }

                        // State Field
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "State *",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = state,
                                onValueChange = {
                                    state = it
                                    errorText = null
                                },
                                placeholder = { Text("e.g. Telangana", color = Color(0xFF64748B), fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = AccentGreen,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = AccentGreen
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Vehicle Type Selection
                    Text(
                        text = "Select Vehicle Type (Choose One)",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VehicleOptionCard(
                            title = "Auto Rickshaw",
                            emoji = "🛺",
                            isSelected = selectedVehicle == VehicleType.AUTO,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedVehicle = VehicleType.AUTO }
                        )
                        VehicleOptionCard(
                            title = "Bike",
                            emoji = "🏍️",
                            isSelected = selectedVehicle == VehicleType.BIKE,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedVehicle = VehicleType.BIKE }
                        )
                        VehicleOptionCard(
                            title = "Car",
                            emoji = "🚗",
                            isSelected = selectedVehicle == VehicleType.CAR,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedVehicle = VehicleType.CAR }
                        )
                    }

                    if (errorText != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = errorText!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    val cleanName = name.trim()
                    val cleanPhone = mobileNumber.trim()
                    val cleanCity = city.trim()
                    val cleanState = state.trim()

                    when {
                        cleanName.isEmpty() -> {
                            errorText = "Please enter your full name"
                        }
                        cleanPhone.length != 10 -> {
                            errorText = "Please enter a valid 10-digit mobile number"
                        }
                        cleanCity.isEmpty() -> {
                            errorText = "Please enter your city (e.g. Hyderabad)"
                        }
                        cleanState.isEmpty() -> {
                            errorText = "Please enter your state (e.g. Telangana)"
                        }
                        else -> {
                            errorText = null
                            onSaveProfile(cleanName, cleanPhone, cleanCity, cleanState, selectedVehicle)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF00391A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Save Profile & Continue →",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun VehicleOptionCard(
    title: String,
    emoji: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentGreen else Color(0xFF243347)
    val bgColor = if (isSelected) Color(0xFF00391A) else Color(0xFF0F172A)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            fontSize = 26.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
