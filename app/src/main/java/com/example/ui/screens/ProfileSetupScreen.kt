package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VehicleType
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.LightBackground
import com.example.ui.theme.StatusInactiveRed
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

@Composable
fun ProfileSetupScreen(
    initialName: String = "",
    initialMobile: String = "",
    initialCity: String = "",
    initialState: String = "",
    initialVehicleType: VehicleType = VehicleType.AUTO,
    onSaveProfile: (name: String, mobile: String, city: String, state: String, vehicleType: VehicleType) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var mobile by remember { mutableStateOf(initialMobile) }
    var city by remember { mutableStateOf(initialCity) }
    var state by remember { mutableStateOf(initialState) }
    var selectedVehicle by remember { mutableStateOf(initialVehicleType) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Setup Driver Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Please enter your details to continue to SmartDrivo",
                fontSize = 14.sp,
                color = TextDarkSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = StatusInactiveRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Full Name
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Ramesh Kumar") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mobile Number
            OutlinedTextField(
                value = mobile,
                onValueChange = { 
                    mobile = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Mobile Number") },
                placeholder = { Text("e.g. 9876543210") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Mobile") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // City
            OutlinedTextField(
                value = city,
                onValueChange = { 
                    city = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("City") },
                placeholder = { Text("e.g. Hyderabad, Bengaluru") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = "City") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // State
            OutlinedTextField(
                value = state,
                onValueChange = { 
                    state = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("State") },
                placeholder = { Text("e.g. Telangana, Karnataka") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = "State") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Vehicle Type",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDarkPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            VehicleType.values().forEach { vehicle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedVehicle == vehicle),
                        onClick = { selectedVehicle = vehicle },
                        colors = RadioButtonDefaults.colors(selectedColor = BluePrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = vehicle.displayName,
                        fontSize = 15.sp,
                        color = TextDarkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val trimmedName = name.trim().ifEmpty { "Driver" }
                    val trimmedMobile = mobile.trim()
                    val trimmedCity = city.trim()
                    val trimmedState = state.trim()

                    if (trimmedMobile.isEmpty()) {
                        errorMessage = "Please enter your Mobile number"
                        return@Button
                    }
                    if (trimmedCity.isEmpty()) {
                        errorMessage = "Please enter your City"
                        return@Button
                    }
                    if (trimmedState.isEmpty()) {
                        errorMessage = "Please enter your State"
                        return@Button
                    }

                    onSaveProfile(trimmedName, trimmedMobile, trimmedCity, trimmedState, selectedVehicle)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text(
                    text = "Save & Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
