package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.FirebaseRepository
import com.example.data.PreferencesManager
import com.example.model.FilterMode
import com.example.model.OrderHistoryItem
import com.example.model.OrderStatus
import com.example.model.Platform
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.RideHistoryViewModel
import com.example.service.FloatingOverlayService
import com.example.service.SmartDrivoAccessibilityService
import com.example.ui.components.SmartDrivoLogo
import com.example.util.PermissionHelper
import kotlinx.coroutines.launch
import com.example.ui.theme.BlueBorder
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSecondary
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusActiveGreenBorder
import com.example.ui.theme.StatusInactiveRed
import com.example.ui.theme.StatusInactiveRedBg
import com.example.ui.theme.StatusInactiveRedBorder
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.StatusWarningYellowBg
import com.example.ui.theme.StatusWarningYellowBorder
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextDarkTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: PreferencesManager,
    repository: FirebaseRepository,
    historyViewModel: RideHistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAreaManager: () -> Unit,
    onNavigateToCommunity: () -> Unit
) {
    val context = LocalContext.current
    val settings by prefs.appSettings.collectAsState()
    val userProfile by prefs.userProfile.collectAsState()
    val historyEntities by historyViewModel.history.collectAsStateWithLifecycle()
    // Room is the single UI source of truth for ride history.
    // Avoid collecting the legacy SharedPreferences JSON history in parallel.
    val historyItems = remember(historyEntities) {
        historyEntities.map { it.toOrderHistoryItem() }
    }
    val latestHistoryOrder = remember(historyItems) {
        historyItems.firstOrNull()
    }
    val lastAccepted by prefs.lastAcceptedRide.collectAsState()
    val goToAreas by prefs.goToAreas.collectAsState()
    val noGoAreas by prefs.noGoAreas.collectAsState()
    val activeAreaFilterCount by remember(goToAreas, noGoAreas) {
        derivedStateOf { goToAreas.size + noGoAreas.size }
    }

    var showSimulateDialog by remember { mutableStateOf(false) }
    var simPlatform by remember { mutableStateOf(Platform.RAPIDO) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showOverlayDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityGranted by remember {
        mutableStateOf(PermissionHelper.isAccessibilityPermissionGranted(context))
    }
    var isOverlayGranted by remember {
        mutableStateOf(PermissionHelper.isOverlayPermissionGranted(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasAccessibility = PermissionHelper.isAccessibilityPermissionGranted(context)
                val hasOverlay = PermissionHelper.isOverlayPermissionGranted(context)
                isAccessibilityGranted = hasAccessibility
                isOverlayGranted = hasOverlay

                if (prefs.isPendingAutoAcceptActivation) {
                    if (hasAccessibility && hasOverlay) {
                        prefs.isPendingAutoAcceptActivation = false
                        showAccessibilityDialog = false
                        showOverlayDialog = false
                        if (!settings.isAutoAcceptActive) {
                            prefs.saveAppSettings(settings.copy(isAutoAcceptActive = true))
                            context.sendBroadcast(
                                Intent("com.example.TOGGLE_CHANGED")
                                    .setPackage(context.packageName)
                                    .putExtra("enabled", true)
                            )
                        }
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Auto-Accept is now Active! ✓",
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else if (hasAccessibility && !hasOverlay) {
                        showAccessibilityDialog = false
                        showOverlayDialog = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val handleToggleAutoAccept: (Boolean) -> Unit = { shouldEnable ->
        if (!shouldEnable) {
            prefs.isPendingAutoAcceptActivation = false
            prefs.saveAppSettings(settings.copy(isAutoAcceptActive = false))
            context.sendBroadcast(
                Intent("com.example.TOGGLE_CHANGED")
                    .setPackage(context.packageName)
                    .putExtra("enabled", false)
            )
        } else {
            val hasAccessibility = PermissionHelper.isAccessibilityPermissionGranted(context)
            val hasOverlay = PermissionHelper.isOverlayPermissionGranted(context)
            isAccessibilityGranted = hasAccessibility
            isOverlayGranted = hasOverlay

            if (!hasAccessibility) {
                prefs.isPendingAutoAcceptActivation = true
                showAccessibilityDialog = true
            } else if (!hasOverlay) {
                prefs.isPendingAutoAcceptActivation = true
                showOverlayDialog = true
            } else {
                prefs.isPendingAutoAcceptActivation = false
                prefs.saveAppSettings(settings.copy(isAutoAcceptActive = true))
                context.sendBroadcast(
                    Intent("com.example.TOGGLE_CHANGED")
                        .setPackage(context.packageName)
                        .putExtra("enabled", true)
                )
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Auto-Accept is now Active! ✓",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Fare and Distance inputs with rememberSaveable and hasUnsavedChanges
    var hasUnsavedChanges by rememberSaveable { mutableStateOf(false) }

    var minFareInput by rememberSaveable {
        mutableStateOf(if (settings.minFare > 0) {
            if (settings.minFare % 1.0f == 0f) settings.minFare.toInt().toString() else settings.minFare.toString()
        } else "50")
    }
    var maxFareInput by rememberSaveable {
        mutableStateOf(if (settings.maxFare > 0) {
            if (settings.maxFare % 1.0f == 0f) settings.maxFare.toInt().toString() else settings.maxFare.toString()
        } else "999")
    }
    var maxPickupInput by rememberSaveable {
        mutableStateOf(if (settings.maxPickupDistanceKm > 0) {
            if (settings.maxPickupDistanceKm % 1.0f == 0f) settings.maxPickupDistanceKm.toInt().toString() else settings.maxPickupDistanceKm.toString()
        } else "3.0")
    }
    var maxDropInput by rememberSaveable {
        mutableStateOf(if (settings.maxDropDistanceKm > 0) {
            if (settings.maxDropDistanceKm % 1.0f == 0f) settings.maxDropDistanceKm.toInt().toString() else settings.maxDropDistanceKm.toString()
        } else "7.5")
    }


    // Calculate real-time stats for today
    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val todayOrders by remember(historyItems, todayStr) {
        derivedStateOf { historyItems.filter { it.dateStr == todayStr } }
    }
    val acceptedCount by remember(todayOrders) {
        derivedStateOf { todayOrders.count { it.status == OrderStatus.ACCEPTED } }
    }
    val rejectedCount by remember(todayOrders) {
        derivedStateOf {
            todayOrders.count {
                it.reason.contains("No-Go", ignoreCase = true) ||
                it.reason.contains("nogo", ignoreCase = true) ||
                it.reason.contains("No Go", ignoreCase = true)
            }
        }
    }
    val ignoredCount by remember(todayOrders) {
        derivedStateOf {
            todayOrders.count {
                it.status != OrderStatus.ACCEPTED &&
                !(it.reason.contains("No-Go", ignoreCase = true) ||
                  it.reason.contains("nogo", ignoreCase = true) ||
                  it.reason.contains("No Go", ignoreCase = true))
            }
        }
    }
    val earningsAssisted by remember(todayOrders) {
        derivedStateOf {
            todayOrders
                .filter { it.status == OrderStatus.ACCEPTED }
                .sumOf { it.amount.toDouble() }
                .toInt()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmartDrivoLogo(size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SmartDrivo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextDarkPrimary
                            )
                            Text(
                                text = if (userProfile.isPlanValid || userProfile.isAdmin) "● Membership Active" else "○ Membership Inactive",
                                fontSize = 14.sp,
                                color = if (userProfile.isPlanValid || userProfile.isAdmin) StatusActiveGreen else StatusInactiveRed
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCommunity) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = "Community",
                            tint = TextDarkSecondary
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Accessibility Service Warning (Colorful Warning Card)
            if (!isAccessibilityGranted) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusWarningYellowBg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, StatusWarningYellowBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.isPendingAutoAcceptActivation = true
                                showAccessibilityDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFFFECB3), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusWarningYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Accessibility Service Disabled",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Tap to enable SmartDrivo in Accessibility Settings. Auto-Accept will activate automatically.",
                                    color = TextDarkSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = StatusWarningYellow
                            )
                        }
                    }
                }
            } else if (!isOverlayGranted) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BlueContainer),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, BlueBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.isPendingAutoAcceptActivation = true
                                showOverlayDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = BlueSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Display Overlay Required",
                                    fontWeight = FontWeight.Bold,
                                    color = BlueSecondary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Tap to allow SmartDrivo to show popup ride cards over Rapido, Uber, and Ola.",
                                    color = TextDarkSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = BlueSecondary
                            )
                        }
                    }
                }
            }

            // Automation Health Status Card
            item {
                AutomationHealthCard(
                    isAccessibilityActive = isAccessibilityGranted,
                    isOverlayActive = isOverlayGranted,
                    isAutoAcceptActive = settings.isAutoAcceptActive,
                    onClickAccessibility = {
                        prefs.isPendingAutoAcceptActivation = true
                        showAccessibilityDialog = true
                    },
                    onClickOverlay = {
                        prefs.isPendingAutoAcceptActivation = true
                        showOverlayDialog = true
                    }
                )
            }

            // 2. AUTO-ACCEPT ORDERS toggle card (MOVED TO TOP)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            handleToggleAutoAccept(!settings.isAutoAcceptActive)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.isAutoAcceptActive) StatusActiveGreenBg else StatusInactiveRedBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (settings.isAutoAcceptActive) StatusActiveGreenBorder else StatusInactiveRedBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (settings.isAutoAcceptActive) Color(0xFFDCFCE7) else Color(0xFFFFEBEE),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = if (settings.isAutoAcceptActive) StatusActiveGreen else StatusInactiveRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Auto-Accept Orders",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (settings.isAutoAcceptActive)
                                        "ACTIVE — Instant click & filter enabled"
                                    else
                                        "INACTIVE — Auto click is paused",
                                    fontSize = 14.sp,
                                    color = TextDarkSecondary
                                )
                            }
                        }

                        Switch(
                            checked = settings.isAutoAcceptActive,
                            onCheckedChange = { shouldEnable ->
                                handleToggleAutoAccept(shouldEnable)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BluePrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFBDBDBD)
                            )
                        )
                    }
                }
            }

            // 3. Filter Mode buttons + Fare Criteria card + Distance Criteria card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top: Filter Mode Buttons ("Fare Only" | "Distance Only" | "Both")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FILTER MODE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDarkSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Active: ${settings.filterMode.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = when (settings.filterMode) {
                                        FilterMode.FARE_ONLY -> BluePrimary
                                        FilterMode.DISTANCE_ONLY -> BlueSecondary
                                        FilterMode.BOTH -> StatusActiveGreen
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterMode.values().forEach { mode ->
                                    val isSelected = settings.filterMode == mode
                                    val selectedBg = when (mode) {
                                        FilterMode.FARE_ONLY -> BluePrimary
                                        FilterMode.DISTANCE_ONLY -> BlueSecondary
                                        FilterMode.BOTH -> Color(0xFF10B981)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isSelected) selectedBg else Color.Transparent)
                                            .clickable {
                                                prefs.saveAppSettings(settings.copy(filterMode = mode))
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (isSelected) Color.White else TextDarkPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Fare Criteria Section
                        val fareActive by remember(settings.filterMode) {
                            derivedStateOf { settings.filterMode == FilterMode.FARE_ONLY || settings.filterMode == FilterMode.BOTH }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (fareActive) Color.White else Color(0xFFFAFAFA)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (fareActive) BlueBorder else CardBorderDefault)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(if (fareActive) BlueContainer else Color(0xFFEEEEEE), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CurrencyRupee,
                                                contentDescription = null,
                                                tint = if (fareActive) BluePrimary else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Fare Criteria",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (fareActive) TextDarkPrimary else Color.Gray
                                            )
                                            Text(
                                                text = "Accepting: ₹${settings.minFare.toInt()} - ₹${settings.maxFare.toInt()}",
                                                fontSize = 14.sp,
                                                color = if (fareActive) BluePrimary else TextDarkSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (fareActive) "ACTIVE" else "OFF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (fareActive) BluePrimary else Color.Gray
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = minFareInput,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                minFareInput = input
                                                hasUnsavedChanges = true
                                            }
                                        },
                                        label = { Text("Minimum Fare (₹)", fontSize = 14.sp) },
                                        placeholder = { Text("50", fontSize = 14.sp, color = TextDarkTertiary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BluePrimary,
                                            focusedLabelColor = BluePrimary,
                                            unfocusedBorderColor = CardBorderDefault,
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = maxFareInput,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                maxFareInput = input
                                                hasUnsavedChanges = true
                                            }
                                        },
                                        label = { Text("Maximum Fare (₹)", fontSize = 14.sp) },
                                        placeholder = { Text("999", fontSize = 14.sp, color = TextDarkTertiary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BluePrimary,
                                            focusedLabelColor = BluePrimary,
                                            unfocusedBorderColor = CardBorderDefault,
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Distance Criteria Section
                        val distActive by remember(settings.filterMode) {
                            derivedStateOf { settings.filterMode == FilterMode.DISTANCE_ONLY || settings.filterMode == FilterMode.BOTH }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (distActive) Color.White else Color(0xFFFAFAFA)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (distActive) BlueBorder else CardBorderDefault)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(if (distActive) BlueContainer else Color(0xFFEEEEEE), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.NearMe,
                                                contentDescription = null,
                                                tint = if (distActive) BlueSecondary else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Distance Criteria",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (distActive) TextDarkPrimary else Color.Gray
                                            )
                                            Text(
                                                text = "Max Pickup: ${if (settings.maxPickupDistanceKm % 1.0f == 0f) settings.maxPickupDistanceKm.toInt().toString() else settings.maxPickupDistanceKm.toString()} km • Max Drop: ${if (settings.maxDropDistanceKm % 1.0f == 0f) settings.maxDropDistanceKm.toInt().toString() else settings.maxDropDistanceKm.toString()} km",
                                                fontSize = 14.sp,
                                                color = if (distActive) BlueSecondary else TextDarkSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (distActive) "ACTIVE" else "OFF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (distActive) BlueSecondary else Color.Gray
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = maxPickupInput,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                maxPickupInput = input
                                                hasUnsavedChanges = true
                                            }
                                        },
                                        label = { Text("Max Pickup (km)", fontSize = 14.sp) },
                                        placeholder = { Text("3.0", fontSize = 14.sp, color = TextDarkTertiary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BlueSecondary,
                                            focusedLabelColor = BlueSecondary,
                                            unfocusedBorderColor = CardBorderDefault,
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = maxDropInput,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                maxDropInput = input
                                                hasUnsavedChanges = true
                                            }
                                        },
                                        label = { Text("Max Drop Distance (km)", fontSize = 14.sp) },
                                        placeholder = { Text("7.5", fontSize = 14.sp, color = TextDarkTertiary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BlueSecondary,
                                            focusedLabelColor = BlueSecondary,
                                            unfocusedBorderColor = CardBorderDefault,
                                            focusedTextColor = TextDarkPrimary,
                                            unfocusedTextColor = TextDarkPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Save Settings Button
                        Button(
                            onClick = {
                                val minF = minFareInput.toFloatOrNull() ?: settings.minFare
                                val maxF = maxFareInput.toFloatOrNull() ?: settings.maxFare
                                val maxP = maxPickupInput.toFloatOrNull() ?: settings.maxPickupDistanceKm
                                val maxD = maxDropInput.toFloatOrNull() ?: settings.maxDropDistanceKm

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
                            enabled = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_settings_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluePrimary,
                                contentColor = Color.White
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

            // 4. Today's Performance Stats
            item {
                Text(
                    text = "Today's Performance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Accepted",
                        value = acceptedCount.toString(),
                        color = StatusActiveGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Rejected",
                        value = rejectedCount.toString(),
                        color = StatusInactiveRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Ignored",
                        value = ignoredCount.toString(),
                        color = StatusWarningYellow,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Earnings Assisted (Today)",
                            fontSize = 14.sp,
                            color = TextDarkSecondary
                        )
                        Text(
                            text = "₹$earningsAssisted",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }
                }
            }

            // Last Accepted Ride Info Card
            if (lastAccepted != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatusActiveGreenBg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, StatusActiveGreenBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LAST ACCEPTED RIDE (${lastAccepted!!.platform.name}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusActiveGreen
                                )
                                Text(
                                    text = lastAccepted!!.timeStr,
                                    fontSize = 14.sp,
                                    color = TextDarkSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "₹${lastAccepted!!.amount.toInt()}  →  ${lastAccepted!!.dropArea.ifEmpty { "Drop Point" }}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            if (lastAccepted!!.pickupAddress.isNotEmpty()) {
                                Text(
                                    text = "Pickup: ${lastAccepted!!.pickupAddress}",
                                    fontSize = 14.sp,
                                    color = TextDarkSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Last Order Card from Room History (Reactive & Instant, Ride Boss Style)
            if (latestHistoryOrder != null) {
                item {
                    val statusColor = when (latestHistoryOrder.status) {
                        OrderStatus.PROCESSING -> Color(0xFFE65100)
                        OrderStatus.ACCEPTED -> StatusActiveGreen
                        OrderStatus.REJECTED -> StatusInactiveRed
                        OrderStatus.IGNORED -> StatusWarningYellow
                        OrderStatus.FAILED -> Color(0xFFC62828)
                        OrderStatus.SKIPPED -> Color(0xFF512DA8)
                        OrderStatus.MISSED -> Color.Gray
                    }
                    val statusBg = when (latestHistoryOrder.status) {
                        OrderStatus.PROCESSING -> Color(0xFFFFF8E1)
                        OrderStatus.ACCEPTED -> StatusActiveGreenBg
                        OrderStatus.REJECTED -> StatusInactiveRedBg
                        OrderStatus.IGNORED -> StatusWarningYellowBg
                        OrderStatus.FAILED -> Color(0xFFFFEBEE)
                        OrderStatus.SKIPPED -> Color(0xFFEDE7F6)
                        OrderStatus.MISSED -> Color(0xFFEEEEEE)
                    }

                    val totalMs = when {
                        latestHistoryOrder.totalProcessingMs > 0L -> latestHistoryOrder.totalProcessingMs
                        latestHistoryOrder.decisionLatencyMs > 0L && latestHistoryOrder.status != OrderStatus.ACCEPTED -> latestHistoryOrder.decisionLatencyMs
                        latestHistoryOrder.clickTimeMs > latestHistoryOrder.detectionTimeMs && latestHistoryOrder.detectionTimeMs > 0L -> latestHistoryOrder.clickTimeMs - latestHistoryOrder.detectionTimeMs
                        else -> 0L
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToHistory() },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1: "LAST ORDER" Header + Platform/Vehicle + Status Badge + Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "LAST ORDER",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueSecondary,
                                        letterSpacing = 0.5.sp
                                    )

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (latestHistoryOrder.platform) {
                                                    Platform.RAPIDO -> Color(0xFFFFF9C4)
                                                    Platform.UBER -> Color(0xFFEEEEEE)
                                                    Platform.OLA -> StatusActiveGreenBg
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = latestHistoryOrder.platform.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (latestHistoryOrder.platform) {
                                                Platform.RAPIDO -> Color(0xFFF57F17)
                                                Platform.UBER -> Color(0xFF212121)
                                                Platform.OLA -> StatusActiveGreen
                                            }
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(statusBg, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (latestHistoryOrder.status == OrderStatus.PROCESSING) "PROCESSING ⏳" else latestHistoryOrder.status.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                }

                                Text(
                                    text = latestHistoryOrder.timeStr,
                                    fontSize = 12.sp,
                                    color = TextDarkSecondary
                                )
                            }

                            // Row 2: Fare & Route
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (latestHistoryOrder.amount > 0f)
                                        "₹${latestHistoryOrder.amount.toInt()}  →  ${latestHistoryOrder.dropArea.ifEmpty { "Drop Point" }}"
                                    else
                                        latestHistoryOrder.dropArea.ifEmpty { "Order evaluating..." },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                if (latestHistoryOrder.pickupDistKm > 0f || latestHistoryOrder.dropDistKm > 0f) {
                                    Text(
                                        text = "${if (latestHistoryOrder.pickupDistKm > 0f) "%.1f km".format(latestHistoryOrder.pickupDistKm) else "--"} pickup",
                                        fontSize = 12.sp,
                                        color = TextDarkSecondary
                                    )
                                }
                            }

                            // Row 3: Exact Reason
                            val displayReason = latestHistoryOrder.reason.ifBlank {
                                if (latestHistoryOrder.status == OrderStatus.PROCESSING) "Evaluating ride filters..." else "Filter evaluated"
                            }
                            Text(
                                text = displayReason,
                                fontSize = 13.sp,
                                color = TextDarkPrimary,
                                maxLines = 2
                            )

                            // Row 4: Real Response Speed ms + View History prompt
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (latestHistoryOrder.status == OrderStatus.PROCESSING) {
                                    Text(
                                        text = "⚡ Speed: In progress...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                } else if (totalMs > 0L) {
                                    Text(
                                        text = "⚡ Speed: ${totalMs} ms",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                } else if (latestHistoryOrder.historyInsertLatencyMs > 0L) {
                                    Text(
                                        text = "⚡ Speed: ${latestHistoryOrder.historyInsertLatencyMs} ms",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Text(
                                    text = "View History →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BluePrimary
                                )
                            }
                        }
                    }
                }
            }

            // 7. Area Rules Engine card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAreaManager() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(BlueContainer, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = BlueSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Area Rules Engine",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeAreaFilterCount > 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (activeAreaFilterCount > 0) Color(0xFF86EFAC) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "$activeAreaFilterCount Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeAreaFilterCount > 0) Color(0xFF15803D) else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (activeAreaFilterCount > 0)
                                    "${goToAreas.size} Go-To (Green) • ${noGoAreas.size} No-Go (Red)"
                                else
                                    "Configure GO TO & NO GO filter groups",
                                color = TextDarkSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextDarkSecondary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Simulation Dialog
    if (showSimulateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSimulateDialog = false },
            title = {
                Text(
                    text = "Test Order Acceptance Overlay",
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose platform to test the live overlay (5-minute countdown timer, pickup & drop distances in km, full address, and X dismiss button):",
                        fontSize = 11.5.sp,
                        color = TextDarkSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Platform.RAPIDO, Platform.UBER, Platform.OLA).forEach { plat ->
                            val isSelected = simPlatform == plat
                            Surface(
                                onClick = { simPlatform = plat },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) when (plat) {
                                    Platform.RAPIDO -> Color(0xFFF59E0B)
                                    Platform.UBER -> Color(0xFF18181B)
                                    Platform.OLA -> Color(0xFF10B981)
                                    else -> BluePrimary
                                } else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.Transparent else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (plat) {
                                            Platform.RAPIDO -> "⚡ Rapido"
                                            Platform.UBER -> "🚕 Uber"
                                            Platform.OLA -> "🚖 Ola"
                                            else -> plat.displayName
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    // Preview of data
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val (pickupDist, dropDist, fare) = when (simPlatform) {
                                Platform.RAPIDO -> Triple(0.8f, 7.2f, 145f)
                                Platform.UBER -> Triple(1.4f, 14.5f, 275f)
                                Platform.OLA -> Triple(1.1f, 9.8f, 195f)
                                else -> Triple(1.0f, 8.0f, 150f)
                            }
                            Text(
                                text = "• Platform: ${simPlatform.displayName}  |  Fare: ₹${fare.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Text(
                                text = "• Pickup: $pickupDist km  |  Drop: $dropDist km",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• 5-min countdown timer with live auto-dismiss.",
                                fontSize = 10.5.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "• Full pickup & drop addresses with X button to close.",
                                fontSize = 10.5.sp,
                                color = TextDarkSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isOverlayGranted) {
                            showSimulateDialog = false
                            showOverlayDialog = true
                            return@Button
                        }
                        showSimulateDialog = false
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                        val now = System.currentTimeMillis()
                        data class SimRideData(
                            val pickupDistKm: Float,
                            val dropDistKm: Float,
                            val fare: Float,
                            val pickupAddress: String,
                            val dropAddress: String,
                            val dropArea: String
                        )

                        val simData = when (simPlatform) {
                            Platform.RAPIDO -> SimRideData(
                                pickupDistKm = 0.8f,
                                dropDistKm = 7.2f,
                                fare = 145f,
                                pickupAddress = "Sector 14 Metro Station Gate 2, Old DLF Colony, MG Road, Gurgaon",
                                dropAddress = "Building 10, DLF Cyber City Phase 2, DLF City, Gurgaon, Haryana 122002",
                                dropArea = "DLF Cyber City"
                            )
                            Platform.UBER -> SimRideData(
                                pickupDistKm = 1.4f,
                                dropDistKm = 14.5f,
                                fare = 275f,
                                pickupAddress = "Terminal 3 Departure Ramp, Indira Gandhi International Airport, New Delhi",
                                dropAddress = "Block A, Connaught Place, Inner Circle, Near Rajiv Chowk Metro Gate 5, New Delhi",
                                dropArea = "Connaught Place"
                            )
                            Platform.OLA -> SimRideData(
                                pickupDistKm = 1.1f,
                                dropDistKm = 9.8f,
                                fare = 195f,
                                pickupAddress = "H-No 42, 100 Feet Road, Near Sony Center, Indiranagar, Bengaluru",
                                dropAddress = "RMZ Ecospace, Outer Ring Road, Bellandur, Bengaluru, Karnataka 560103",
                                dropArea = "Bellandur Outer Ring Road"
                            )
                            else -> SimRideData(
                                pickupDistKm = 1.0f,
                                dropDistKm = 8.0f,
                                fare = 150f,
                                pickupAddress = "Main Market, Sector 18, Noida, Uttar Pradesh 201301",
                                dropAddress = "Tower B, Advant Navis Business Park, Sector 142, Noida, Uttar Pradesh",
                                dropArea = "Sector 142"
                            )
                        }

                        val simulated = OrderHistoryItem(
                            id = UUID.randomUUID().toString(),
                            timestamp = now,
                            dateStr = dateStr,
                            timeStr = timeStr,
                            status = OrderStatus.ACCEPTED,
                            platform = simPlatform,
                            vehicleType = userProfile.vehicleType,
                            pickupDistKm = simData.pickupDistKm,
                            dropDistKm = simData.dropDistKm,
                            pickupAddress = simData.pickupAddress,
                            dropAddress = simData.dropAddress,
                            dropArea = simData.dropArea,
                            amount = simData.fare,
                            bookingId = "CRN-${(10000..99999).random()}",
                            detectionTimeMs = now - 142L,
                            clickTimeMs = now,
                            baseFare = (simData.fare * 0.8f),
                            tipAmount = (simData.fare * 0.2f),
                            timesClicked = 1,
                            reason = "Fare ₹${simData.fare.toInt()} matched, Pickup ${String.format(Locale.ENGLISH, "%.1fkm", simData.pickupDistKm)} matched"
                        )
                        repository.recordOrderHistory(simulated)

                        // Trigger overlay
                        FloatingOverlayService.show(
                            context = context,
                            amount = simulated.amount,
                            pickup = simulated.pickupAddress,
                            pickupDist = simulated.pickupDistKm,
                            drop = simulated.dropAddress,
                            dropDist = simulated.dropDistKm,
                            dropArea = simulated.dropArea,
                            time = simulated.timeStr,
                            platform = simulated.platform.displayName
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Launch ${simPlatform.displayName} Overlay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimulateDialog = false }) {
                    Text("Cancel", color = TextDarkSecondary)
                }
            }
        )
    }

    // Accessibility Permission Explanation Dialog
    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = {
                showAccessibilityDialog = false
                prefs.isPendingAutoAcceptActivation = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(BlueContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Accessibility,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Accessibility Service Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SmartDrivo requires Accessibility permission to detect incoming ride orders from Rapido, Uber, and Ola, and auto-click the Accept button according to your fare and distance filters.",
                        fontSize = 11.sp,
                        color = TextDarkSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CardBorderDefault)
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("1. Tap 'Enable in Settings' below", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                            Text("2. Locate and tap 'SmartDrivo'", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                            Text("3. Switch the toggle ON and allow", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                            Text("4. Return here — Auto-Accept activates automatically!", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDialog = false
                        prefs.isPendingAutoAcceptActivation = true
                        PermissionHelper.openAccessibilitySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enable in Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAccessibilityDialog = false
                        prefs.isPendingAutoAcceptActivation = false
                    }
                ) {
                    Text("Cancel", color = TextDarkSecondary)
                }
            }
        )
    }

    // Display Over Other Apps Permission Dialog
    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = {
                showOverlayDialog = false
                prefs.isPendingAutoAcceptActivation = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(BlueContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = BlueSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Display Over Other Apps",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SmartDrivo needs 'Display over other apps' permission to show the floating ride details popup and accept overlay on top of Rapido, Uber, and Ola.",
                        fontSize = 11.sp,
                        color = TextDarkSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CardBorderDefault)
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("1. Tap 'Enable in Settings' below", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                            Text("2. Find SmartDrivo and allow permission", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                            Text("3. Return here — Auto-Accept activates automatically!", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = BlueSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayDialog = false
                        prefs.isPendingAutoAcceptActivation = true
                        PermissionHelper.openOverlaySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueSecondary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enable in Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOverlayDialog = false
                        prefs.isPendingAutoAcceptActivation = false
                    }
                ) {
                    Text("Cancel", color = TextDarkSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardBorderDefault)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 14.sp, color = TextDarkSecondary)
        }
    }
}

@Composable
private fun AutomationHealthCard(
    isAccessibilityActive: Boolean,
    isOverlayActive: Boolean,
    isAutoAcceptActive: Boolean,
    onClickAccessibility: () -> Unit,
    onClickOverlay: () -> Unit
) {
    val allHealthy = isAccessibilityActive && isOverlayActive && isAutoAcceptActive
    val bg = if (allHealthy) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
    val borderCol = if (allHealthy) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
    val titleCol = if (allHealthy) Color(0xFF15803D) else Color(0xFFB45309)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (allHealthy) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = if (allHealthy) StatusActiveGreen else Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Automation Health",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = titleCol
                        )
                        Text(
                            text = if (allHealthy) "All systems ready & monitoring" else "Action required for full automation",
                            fontSize = 12.sp,
                            color = TextDarkSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (allHealthy) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (allHealthy) Color(0xFF86EFAC) else Color(0xFFFCD34D))
                ) {
                    Text(
                        text = if (allHealthy) "OPTIMAL" else "ATTENTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (allHealthy) Color(0xFF15803D) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Status items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HealthStatusChip(
                    label = "Accessibility",
                    isActive = isAccessibilityActive,
                    onClick = if (!isAccessibilityActive) onClickAccessibility else null,
                    modifier = Modifier.weight(1f)
                )
                HealthStatusChip(
                    label = "Overlay",
                    isActive = isOverlayActive,
                    onClick = if (!isOverlayActive) onClickOverlay else null,
                    modifier = Modifier.weight(1f)
                )
                HealthStatusChip(
                    label = "Auto-Accept",
                    isActive = isAutoAcceptActive,
                    onClick = null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HealthStatusChip(
    label: String,
    isActive: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val activeColor = StatusActiveGreen
    val inactiveColor = StatusInactiveRed
    val activeBg = Color(0xFFDCFCE7)
    val inactiveBg = Color(0xFFFEE2E2)

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) activeBg else inactiveBg,
        border = BorderStroke(1.dp, if (isActive) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isActive) "● $label" else "○ $label",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) activeColor else inactiveColor,
                maxLines = 1
            )
            Text(
                text = if (isActive) "Active" else if (onClick != null) "Enable" else "Off",
                fontSize = 10.sp,
                color = if (isActive) activeColor else inactiveColor
            )
        }
    }
}
