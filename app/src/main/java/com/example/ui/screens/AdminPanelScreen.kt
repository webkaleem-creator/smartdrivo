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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.data.PreferencesManager
import com.example.model.PaymentStatus
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CardBackground
import com.example.ui.theme.LightBackground
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusInactiveRed
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    prefs: PreferencesManager,
    repository: FirebaseRepository,
    onBack: () -> Unit
) {
    val submissions by prefs.paymentSubmissions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Payment Verification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (submissions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No pending payments to review", color = TextDarkSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightBackground)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = submissions,
                    key = { it.paymentId },
                    contentType = { "admin_payment_item" }
                ) { sub ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sub.userName} (₹${sub.amount.toInt()})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDarkPrimary
                                )
                                Text(
                                    text = sub.status.name,
                                    color = when (sub.status) {
                                        PaymentStatus.APPROVED -> StatusActiveGreen
                                        PaymentStatus.REJECTED -> StatusInactiveRed
                                        PaymentStatus.PENDING -> StatusWarningYellow
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "UTR: ${sub.utrNumber}", fontSize = 13.sp, color = TextDarkSecondary)
                            Text(text = "Plan: ${sub.planSelected}", fontSize = 13.sp, color = TextDarkSecondary)

                            if (sub.status == PaymentStatus.PENDING) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val updated = sub.copy(status = PaymentStatus.APPROVED)
                                            repository.submitPayment(updated) {}
                                            val user = prefs.userProfile.value
                                            if (user.uid == sub.uid) {
                                                prefs.saveUserProfile(user.copy(isApproved = true, isActive = true))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusActiveGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Text(" Approve")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val updated = sub.copy(status = PaymentStatus.REJECTED)
                                            repository.submitPayment(updated) {}
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = StatusInactiveRed)
                                        Text(" Reject", color = StatusInactiveRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
