package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.LightBackground
import com.example.ui.theme.StatusInactiveRed
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextDarkTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    prefs: PreferencesManager,
    onNavigateToPlanSelection: () -> Unit,
    onNavigateToPaymentHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToAdminPanel: () -> Unit = {},
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by prefs.userProfile.collectAsState()
    var showAccountDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }

    val referralCode by remember(userProfile.referralCode) {
        derivedStateOf { userProfile.referralCode }
    }
    val isAdmin by remember(userProfile.isAdmin) {
        derivedStateOf { userProfile.isAdmin }
    }
    val userDisplayName by remember(userProfile.name) {
        derivedStateOf { userProfile.name.ifEmpty { "Driver" } }
    }
    val userEffectiveMobile by remember(userProfile.effectiveMobile) {
        derivedStateOf { userProfile.effectiveMobile.ifEmpty { "N/A" } }
    }
    val userLocation by remember(userProfile.city, userProfile.state) {
        derivedStateOf {
            listOf(userProfile.city, userProfile.state).filter { it.isNotEmpty() }.joinToString(", ")
        }
    }
    val userPlanDisplay by remember(userProfile.plan) {
        derivedStateOf { userProfile.plan.ifEmpty { "Free User" } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "More Options",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Default,
                            color = TextDarkPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Account, plans, support & settings",
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Default,
                            color = TextDarkSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0xFF2E7D32)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBackground,
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

            // Group 1: Membership & Payment History
            item(key = "group_membership_payment", contentType = "options_group") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column {
                        MoreCardRow(
                            icon = Icons.Default.MenuBook,
                            title = "My Membership",
                            subtitle = "View current plan, remaining days & renewals",
                            onClick = onNavigateToPlanSelection
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = CardBorderDefault,
                            thickness = 1.dp
                        )
                        MoreCardRow(
                            icon = Icons.Default.CreditCard,
                            title = "Payment History",
                            subtitle = "Past UPI transactions & order receipts",
                            onClick = onNavigateToPaymentHistory
                        )
                    }
                }
            }

            // Group 2: My Account & Settings
            item(key = "group_account_settings", contentType = "options_group") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column {
                        MoreCardRow(
                            icon = Icons.Default.Person,
                            title = "My Account",
                            subtitle = "Google profile, device binding & sign out",
                            onClick = { showAccountDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = CardBorderDefault,
                            thickness = 1.dp
                        )
                        MoreCardRow(
                            icon = Icons.Default.Settings,
                            title = "Settings",
                            subtitle = "App preferences, platforms & permissions",
                            onClick = onNavigateToSettings
                        )
                    }
                }
            }

            // Group 3: Contact Support & Driver Tutorial
            item(key = "group_support_tutorial", contentType = "options_group") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column {
                        MoreCardRow(
                            icon = Icons.Default.HelpOutline,
                            title = "Contact SmartDrivo",
                            subtitle = "Telegram community (@SmartDrivoSupport) & support",
                            onClick = onNavigateToCommunity
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = CardBorderDefault,
                            thickness = 1.dp
                        )
                        MoreCardRow(
                            icon = Icons.Default.School,
                            title = "Driver Tutorial",
                            subtitle = "3-step guide to filters & areas",
                            onClick = { showTutorialDialog = true }
                        )
                    }
                }
            }

            // Group 4: Referral & Admin Settings
            item(key = "group_referral_admin", contentType = "options_group") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Driver Referral Code Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Driver Referral Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Default,
                                    color = TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = referralCode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Default,
                                    color = BluePrimary
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Referral", referralCode))
                                    Toast.makeText(context, "Referral Code Copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BluePrimary)
                                }
                                IconButton(onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join SmartDrivo to auto-accept high paying Rapido, Uber & Ola rides! Use my referral code: $referralCode"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Referral Code"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = BluePrimary)
                                }
                            }
                        }

                        HorizontalDivider(color = CardBorderDefault)

                        // Admin Bypass Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Developer Admin Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Default,
                                    color = TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Bypasses plan checks for instant testing",
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Default,
                                    color = TextDarkSecondary
                                )
                            }
                            Switch(
                                checked = isAdmin,
                                onCheckedChange = {
                                    prefs.saveUserProfile(userProfile.copy(isAdmin = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BluePrimary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFBDBDBD)
                                )
                            )
                        }

                        if (isAdmin) {
                            Button(
                                onClick = onNavigateToAdminPanel,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open Admin Control Panel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }
                }
            }

            // Log Out Button
            item(key = "logout_button", contentType = "action_button") {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = StatusInactiveRed
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Log Out",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default
                    )
                }
            }

            // App Brand Footer
            item(key = "footer", contentType = "footer") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SmartDrivo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkSecondary
                    )
                }
            }
        }
    }

    // Account Details Dialog
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Text(
                    text = "My Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Default,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Name: $userDisplayName",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "Mobile: $userEffectiveMobile",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkSecondary
                    )
                    if (userProfile.email.isNotEmpty()) {
                        Text(
                            text = "Email: ${userProfile.email}",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Default,
                            color = TextDarkSecondary
                        )
                    }
                    if (userLocation.isNotEmpty()) {
                        Text(
                            text = "Location: $userLocation",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Default,
                            color = TextDarkSecondary
                        )
                    }
                    Text(
                        text = "Vehicle: ${userProfile.vehicleType.name}",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkSecondary
                    )
                    Text(
                        text = "Plan: $userPlanDisplay",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                }
            }
        )
    }

    // Driver Tutorial Dialog
    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = {
                Text(
                    text = "Driver Tutorial",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Default,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Configure Filters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "Set your minimum fare, pickup, and drop distance or turn on Fastest Mode for high-speed auto-acceptance.",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkSecondary
                    )
                    Text(
                        text = "2. Setup Areas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "Define Go-To areas you want to head towards, or No-Go areas to avoid dangerous or low-traffic zones.",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkSecondary
                    )
                    Text(
                        text = "3. Activate Auto-Accept",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "Turn on the Auto-Accept toggle on the Home screen to let SmartDrivo accept orders automatically.",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = TextDarkSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorialDialog = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                }
            }
        )
    }
}

/**
 * Clean card row component matching Ride Boss styling:
 * - Icon on left in soft rounded container
 * - Title bold
 * - Subtitle below in gray
 * - Trailing chevron
 */
@Composable
fun MoreCardRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Default,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                fontFamily = FontFamily.Default,
                color = TextDarkSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextDarkTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
