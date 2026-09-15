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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MembershipPlan
import com.example.ui.theme.AccentGreen

@Composable
fun PlanSelectionScreen(
    currentPlanId: String = "7DAYS",
    onPlanSelected: (plan: MembershipPlan) -> Unit
) {
    var selectedPlan by remember {
        mutableStateOf(MembershipPlan.DEFAULT_PLANS.firstOrNull { it.id == currentPlanId } ?: MembershipPlan.DEFAULT_PLANS[1])
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Step 4 of 5",
            fontSize = 9.5.sp,
            color = AccentGreen,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Select Membership Plan",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Choose your plan duration to unlock auto-accept on Rapido, Uber & Ola.",
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp, bottom = 11.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(MembershipPlan.DEFAULT_PLANS) { plan ->
                val isSelected = plan.id == selectedPlan.id
                val borderColor = if (isSelected) AccentGreen else MaterialTheme.colorScheme.outline
                val bgColor = if (isSelected) Color(0xFF0D2517) else MaterialTheme.colorScheme.surfaceVariant

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { selectedPlan = plan },
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Column(modifier = Modifier.padding(12.5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = plan.label,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (plan.id == "15DAYS") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("SMART CHOICE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    } else if (plan.id == "7DAYS") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(AccentGreen, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("POPULAR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                                Text(
                                    text = "${plan.days} Days Unlimited Auto-Click",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹${plan.price}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentGreen
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = AccentGreen,
                                        modifier = Modifier.padding(start = 5.5.dp)
                                    )
                                }
                            }
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .background(AccentGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "SELECTED MEMBERSHIP PLAN",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onPlanSelected(selectedPlan) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Pay ₹${selectedPlan.price} for ${selectedPlan.label}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
