package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.AccountBalance
import com.example.util.BankingModeController
import com.example.util.PermissionHelper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.model.VehicleType
import com.example.model.MembershipDisplay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.PreferencesManager
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.premiumPageBrush
import com.example.ui.theme.LightBackground
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusInactiveRed
import com.example.ui.theme.StatusInactiveRedBg
import com.example.ui.theme.StatusInactiveRedBorder
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextDarkTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    prefs: PreferencesManager,
    onNavigateToPlanSelection: () -> Unit,
    onNavigateToPaymentHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToReadOnlyProfile: () -> Unit = {},
    onNavigateToAdminPanel: () -> Unit = {},
    onLogout: () -> Unit,
    onBack: () -> Unit = {}
) {
    val userProfile by prefs.userProfile.collectAsState()
    val settings by prefs.appSettings.collectAsState()
    var showTutorialDialog by remember { mutableStateOf(false) }
    var showBankingDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var bankingModeActive by remember { mutableStateOf(BankingModeController.isActive(context)) }
    val links by prefs.communityLinks.collectAsState()

    fun openSupportLink(url: String) {
        val address = url.trim()
        if (!address.startsWith("https://", ignoreCase = true) &&
            !address.startsWith("http://", ignoreCase = true)) {
            Toast.makeText(
                context,
                "Support link unavailable",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(address))
            )
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Unable to open support link",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        // SMARTDRIVO_ALL_PAGES_UP_V3
        // Parent layout already handles system bars.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(BlueContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "More Options",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Account, membership & settings",
                                fontSize = 12.sp,
                                color = Color(0xFFBBD6FF)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF17264D),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(premiumPageBrush())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // SMARTDRIVO_SUPPORT_CENTER_V1
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCommunity() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF142B50)
                    ),
                    border = BorderStroke(
                        1.dp, Color(0xFF36547D)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(43.dp)
                                    .background(
                                        Color(0xFF28599C),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HeadsetMic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "SMARTDRIVO SUPPORT CENTER",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Help, updates & driver community",
                                    fontSize = 10.sp,
                                    color = Color(0xFFBDD0EA)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open support center",
                                tint = Color(0xFFBDD0EA)
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFF37547B)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MoreSupportShortcut(
                                title = "Telegram",
                                icon = Icons.Default.Send,
                                tint = Color(0xFF229ED9),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    openSupportLink(links.supportTelegramUrl)
                                }
                            )

                            MoreSupportShortcut(
                                title = "WhatsApp",
                                icon = Icons.Default.Campaign,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    openSupportLink(links.whatsappUrl)
                                }
                            )

                            MoreSupportShortcut(
                                title = "Instagram",
                                icon = Icons.Default.PhotoCamera,
                                tint = Color(0xFFE1306C),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    openSupportLink(links.instagramUrl)
                                }
                            )
                        }
                    }
                }
            }

            // READ_ONLY_MY_PROFILE_V1
            item {
                val vehicleLabel = when (userProfile.vehicleType) {
                    VehicleType.AUTO -> "Auto Rickshaw"
                    VehicleType.BIKE -> "Bike"
                    VehicleType.CAR -> "Car"
                }
                val planActive = userProfile.isPlanValid || userProfile.isAdmin

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReadOnlyProfile() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    ),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        BlueContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(25.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "My Profile",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Text(
                                    text = "View account details",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Profile",
                                tint = BluePrimary
                            )
                        }

                        HorizontalDivider(color = CardBorderDefault)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = vehicleLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )

                            Text(
                                text = if (planActive)
                                    "● Active Plan"
                                else
                                    "● No Active Plan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (planActive)
                                    StatusActiveGreen
                                else
                                    StatusInactiveRed
                            )
                        }
                    }
                }
            }

            // 2. MEMBERSHIP & BILLING (Unique options: Plans, Payment Receipts)
            item {
                SectionHeader("MEMBERSHIP & BILLING")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column {
                        MoreCardRow(
                            icon = Icons.Default.WorkspacePremium,
                            title = "My Membership",
                            subtitle = "View current plan, remaining days & renewals",
                            onClick = onNavigateToPlanSelection
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = CardBorderDefault,
                            thickness = 1.dp
                        )
                        MoreCardRow(
                            icon = Icons.Default.ReceiptLong,
                            title = "Payment History",
                            subtitle = "Past UPI transactions & order receipts",
                            onClick = onNavigateToPaymentHistory
                        )
                    }
                }
            }

            // BANKING_MODE_SAFE_V1
            item {
                SectionHeader("BANKING & PRIVACY")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (bankingModeActive) Color(0xFFFFF7E8)
                        else CardBackground
                    ),
                    border = BorderStroke(1.dp,
                        if (bankingModeActive) Color(0xFFE6AF4F) else CardBorderDefault)
                ) {
                    MoreCardRow(
                        icon = Icons.Default.AccountBalance,
                        title = if (bankingModeActive) "Banking Mode is ON"
                            else "Banking Mode",
                        subtitle = if (bankingModeActive)
                            "Orders paused. Finish banking to restore SmartDrivo."
                        else "Safely turn OFF Accessibility for banking or UPI",
                        onClick = {
                            if (bankingModeActive) {
                                val done = BankingModeController.finishAndOpenSettings(context, prefs)
                                if (done) {
                                    bankingModeActive = false
                                } else {
                                    Toast.makeText(context,
                                        "Unable to save Banking Mode state",
                                        Toast.LENGTH_LONG).show()
                                }
                            } else showBankingDialog = true
                        }
                    )
                    if (bankingModeActive) {
                        Text(
                            "After tapping, enable SmartDrivo manually in Android Accessibility Settings. " +
                                "You may need to return to SmartDrivo to resume orders.",
                            modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 12.dp),
                            fontSize = 12.sp,
                            color = TextDarkSecondary
                        )
                    }
                }
            }

            // 3. NOTIFICATIONS
            item {
                SectionHeader("NOTIFICATIONS")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    ),
                    border = BorderStroke(
                        1.dp,
                        CardBorderDefault
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.saveAppSettings(
                                    settings.copy(
                                        showOrderReasonPopups =
                                            !settings.showOrderReasonPopups
                                    )
                                )
                            }
                            .padding(
                                horizontal = 12.dp,
                                vertical = 10.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    Color(0xFFEDE9FE),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier =
                                    Modifier.size(18.dp)
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                text =
                                    "Order Reason Popups",
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color =
                                    TextDarkPrimary
                            )

                            Text(
                                text =
                                    "Show short accepted, ignored and rejected reasons over ride apps",
                                fontSize = 12.sp,
                                color =
                                    TextDarkSecondary
                            )
                        }

                        Switch(
                            checked =
                                settings
                                    .showOrderReasonPopups,
                            onCheckedChange = {
                                enabled ->
                                prefs.saveAppSettings(
                                    settings.copy(
                                        showOrderReasonPopups =
                                            enabled
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // 4. DRIVER GUIDE
            item {
                SectionHeader("DRIVER GUIDE")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    ),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    MoreCardRow(
                        icon = Icons.Default.School,
                        title = "SmartDrivo Driver Guide",
                        subtitle = "Complete guide to SmartDrivo features",
                        onClick = { showTutorialDialog = true }
                    )
                }
            }

            // 5. ADMINISTRATIVE (Only if user has Admin privileges)
            if (userProfile.isAdmin) {
                item {
                    SectionHeader("ADMINISTRATIVE")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, CardBorderDefault)
                    ) {
                        MoreCardRow(
                            icon = Icons.Default.AdminPanelSettings,
                            title = "Admin Control Panel",
                            subtitle = "Manage users, subscriptions & system settings",
                            onClick = onNavigateToAdminPanel
                        )
                    }
                }
            }

            // 6. Log Out Button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusInactiveRedBg,
                        contentColor = StatusInactiveRed
                    ),
                    border = BorderStroke(1.dp, StatusInactiveRedBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log Out",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // 7. Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SmartDrivo • v${BuildConfig.VERSION_NAME}",
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = TextDarkTertiary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    if (showBankingDialog) {
        AlertDialog(
            onDismissRequest = { showBankingDialog = false },
            title = {
                Text("Turn ON Banking Mode?",
                    fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            },
            text = {
                Text(
                    "SmartDrivo will stop monitoring and clicking ride orders, hide its " +
                        "floating popups and request Android to disable its Accessibility Service. " +
                        "After banking, you must manually re-enable Accessibility in Settings. " +
                        "Your saved filters, History and membership will remain.",
                    color = TextDarkSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBankingDialog = false
                    val result = BankingModeController.enter(context, prefs)
                    if (result.saved) {
                        bankingModeActive = true
                        Toast.makeText(context,
                            if (result.requestedServiceDisable)
                                "Banking Mode started. Check Accessibility is OFF before paying."
                            else
                                "Orders paused. Confirm Accessibility is OFF in Settings.",
                            Toast.LENGTH_LONG).show()
                        if (!result.requestedServiceDisable &&
                            PermissionHelper.isAccessibilityPermissionGranted(context)) {
                            PermissionHelper.openAccessibilitySettings(context)
                        }
                    } else {
                        Toast.makeText(context,
                            "Banking Mode could not save state", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("Turn ON Banking Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBankingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Complete SmartDrivo Driver Guide
    if (showTutorialDialog) {

        AlertDialog(
            onDismissRequest = {
                showTutorialDialog = false
            },

            title = {
                Column {
                    Text(
                        text = "SmartDrivo Driver Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDarkPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Complete guide to features & controls",
                        fontSize = 12.sp,
                        color = TextDarkSecondary
                    )
                }
            },

            text = {

                val guideItems = listOf(

                    "1. Auto Accept — Master Switch" to
                        "Auto Accept is the main SmartDrivo switch. When ON, SmartDrivo can monitor eligible ride offers and use your saved settings. When OFF, live ride processing, Auto Reject, Bundle processing, area rules, automatic actions and new ride-history processing stay paused.",

                    "2. Auto Reject — No Condition Match" to
                        "When Auto Accept is ON and Auto Reject is also ON, an order that fails your active conditions can be skipped or rejected using the verified platform reject/skip control. If Auto Reject is OFF, unmatched orders are left for manual action.",

                    "3. Fare / Distance / Both Filters" to
                        "Choose how SmartDrivo evaluates normal ride offers. Fare mode checks your saved fare range. Distance mode checks pickup and drop distance limits. Both mode requires the active fare and distance conditions to match together.",

                    "4. Fastest Mode" to
                        "Fastest Mode is designed for quicker eligible-order processing. It prioritizes the Maximum Pickup Distance rule so nearby eligible rides can be handled with minimum delay. Your main fare and distance values are managed from the Home screen.",

                    "5. Bundle Order" to
                        "Bundle Order has its own ON/OFF control. When ON, detected bundle rides continue through your normal Go-To, No-Go, Fare, Distance or Both rules. When OFF, SmartDrivo will never auto-accept the bundle. If Auto Reject is ON it may be skipped; if Auto Reject is OFF it stays for manual action.",

                    "6. Supported Platforms" to
                        "Use the Filters screen to enable or disable Rapido, Uber and Ola individually. SmartDrivo processes only the platforms you have enabled.",

                    "7. Go-To Areas" to
                        "Create Go-To areas for locations you prefer to travel towards. When Go-To filtering is enabled, SmartDrivo compares the detected ride destination with your saved area rules and conditions.",

                    "8. No-Go Areas" to
                        "Create No-Go areas for destinations you want to avoid. Enabled No-Go rules can block an otherwise matching ride when its detected destination matches one of your saved restricted areas.",

                    "9. Ride History" to
                        "History keeps the processed ride result along with useful details such as platform, fare, pickup/drop distance, status and decision reason. It helps you understand why a ride was accepted, rejected or ignored.",

                    "10. Accepted Ride Overlay" to
                        "After a successful supported ride acceptance, SmartDrivo can show the compact floating overlay with platform, fare, pickup distance, drop distance and destination information. The overlay can also be closed manually.",

                    "11. SmartDrivo Notification" to
                        "When Auto Accept is ON, the notification panel shows SmartDrivo Active — Monitoring orders. When Auto Accept is OFF, SmartDrivo live automation is paused and the persistent active notification is removed.",

                    "12. Membership & Renewal" to
                        "My Membership shows your current plan, remaining days and expiry date. Available renewal plans and prices are loaded from SmartDrivo admin settings, so updated plan pricing can appear in the app automatically.",

                    "13. UPI & QR Payment" to
                        "After selecting a membership plan, SmartDrivo shows the admin UPI payment QR and a Pay with UPI Apps button. The selected plan amount is included in the payment request. After payment, enter the 12-digit UTR and submit it for admin approval.",

                    "14. Payment History" to
                        "Payment History shows your submitted membership payments and their current status, such as Pending, Approved or Rejected. Membership days are activated or extended after payment approval.",
                )


                Column(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(
                            rememberScrollState()
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "SmartDrivo helps drivers apply their own ride preferences consistently across supported driver apps.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    guideItems.forEach { item ->

                        Text(
                            text = item.first,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDarkPrimary
                        )

                        Text(
                            text = item.second,
                            fontSize = 12.5.sp,
                            color = TextDarkSecondary
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Note: SmartDrivo Accessibility Service must be enabled for supported live ride monitoring. Auto Accept remains the master ON/OFF control for SmartDrivo's live order processing.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkSecondary
                    )
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showTutorialDialog = false
                    }
                ) {
                    Text(
                        text = "Got it",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BluePrimary
                    )
                }
            }
        )
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

/**
 * Standard card row matching HomeScreen and SettingsScreen styling:
 * - BlueContainer box with BluePrimary icon
 * - Bold 15sp title
 * - Regular 13sp subtitle
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(BlueContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = TextDarkSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextDarkTertiary,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun MoreSupportShortcut(
    title: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF1F6FD)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        tint.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF152744)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadOnlyDriverProfileScreen(
    prefs: PreferencesManager,
    onBack: () -> Unit
) {
    val profile by prefs.userProfile.collectAsState()

    val vehicleLabel = when (profile.vehicleType) {
        VehicleType.AUTO -> "Auto Rickshaw"
        VehicleType.BIKE -> "Bike"
        VehicleType.CAR -> "Car"
    }

    val activePlan = profile.isAdmin || profile.isPlanValid

    // READ_ONLY_PROFILE_DETAILS_V1
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDarkPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDarkPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ACCOUNT INFORMATION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        ReadOnlyProfileRow(
                            "Full Name",
                            profile.name
                        )
                        HorizontalDivider(color = CardBorderDefault)

                        ReadOnlyProfileRow(
                            "Mobile Number",
                            profile.effectiveMobile
                        )
                        HorizontalDivider(color = CardBorderDefault)

                        ReadOnlyProfileRow(
                            "Email ID",
                            profile.email
                        )
                        HorizontalDivider(color = CardBorderDefault)

                        ReadOnlyProfileRow(
                            "City",
                            profile.city
                        )
                        HorizontalDivider(color = CardBorderDefault)

                        ReadOnlyProfileRow(
                            "State",
                            profile.state
                        )
                        HorizontalDivider(color = CardBorderDefault)

                        ReadOnlyProfileRow(
                            "Vehicle Type",
                            vehicleLabel
                        )
                        HorizontalDivider(color = CardBorderDefault)

                        ReadOnlyProfileRow(
                            "Account Type",
                            if (profile.isAdmin) "Admin" else "Driver"
                        )
                    }
                }
            }

            item {
                Text(
                    text = "MEMBERSHIP INFORMATION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        ReadOnlyProfileRow(
                            "Membership Status",
                            if (activePlan) "Active" else "Inactive"
                        )
                        HorizontalDivider(color = CardBorderDefault)

                        ReadOnlyProfileRow(
                            "Current Plan",
                            MembershipDisplay.of(profile).plan
                        )
                    }
                }
            }

            item {
                Text(
                    text = "These account details are read-only.",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ReadOnlyProfileRow(
    label: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextDarkSecondary
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = value.ifBlank { "Not provided" },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDarkPrimary
        )
    }
}
