package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PreferencesManager
import com.example.model.OrderHistoryItem
import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.ui.viewmodel.RideHistoryViewModel
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSecondary
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.PlatformRapido
import com.example.ui.theme.PlatformRapidoBg
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusInactiveRed
import com.example.ui.theme.StatusInactiveRedBg
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.StatusWarningYellowBg
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextDarkTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class HistoryTab(val label: String) {
    ALL("All"),
    ACCEPTED("Accepted"),
    IGNORED("Ignored"),
    REJECTED("Rejected")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    prefs: PreferencesManager,
    viewModel: RideHistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToDiagnostics: () -> Unit = {},
    onBack: () -> Unit
) {
    val historyEntities by viewModel.history.collectAsStateWithLifecycle()
    val history = remember(historyEntities) {
        historyEntities.sortedByDescending { it.detectedAt }.map { it.toOrderHistoryItem() }
    }
    val totalAccepted by viewModel.acceptedCount.collectAsStateWithLifecycle()


    var selectedTab by remember { mutableStateOf(HistoryTab.ALL) }
    var selectedPlatformFilter by remember { mutableStateOf<Platform?>(null) }
    var selectedDateRange by remember { mutableStateOf("All") } // "Today", "Yesterday", "Last 7 Days", "All"
    val historyListState = rememberLazyListState()

    val todayStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(Date())
    }
    val yesterdayStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(Date(System.currentTimeMillis() - 86400000L))
    }
    val sevenDaysAgo = remember { System.currentTimeMillis() - 7 * 86400000L }

    val acceptedCount = remember(history) { history.count { it.status == OrderStatus.ACCEPTED } }
    val rejectedCount = remember(history) {
        history.count { it.status == OrderStatus.REJECTED || isNoGoOrder(it) }
    }
    val ignoredCount = remember(history) {
        history.count {
            it.status == OrderStatus.IGNORED ||
            (!isNoGoOrder(it) && it.status != OrderStatus.ACCEPTED && it.status != OrderStatus.REJECTED && it.status != OrderStatus.PROCESSING)
        }
    }
    val allCount by remember(history) { derivedStateOf { history.size } }

    val filteredList by remember(history, selectedTab, selectedPlatformFilter, selectedDateRange, todayStr, yesterdayStr, sevenDaysAgo) {
        derivedStateOf {
            history.filter { item ->
                val matchTab = when (selectedTab) {
                    HistoryTab.ALL -> true
                    HistoryTab.ACCEPTED -> item.status == OrderStatus.ACCEPTED || item.status == OrderStatus.PROCESSING
                    HistoryTab.REJECTED -> (isNoGoOrder(item) || item.status == OrderStatus.REJECTED) && item.status != OrderStatus.PROCESSING
                    HistoryTab.IGNORED -> (item.status == OrderStatus.IGNORED || (!isNoGoOrder(item) && item.status != OrderStatus.ACCEPTED && item.status != OrderStatus.REJECTED)) && item.status != OrderStatus.PROCESSING
                }
                val matchPlatform = selectedPlatformFilter == null || item.platform == selectedPlatformFilter
                val matchDate = when (selectedDateRange) {
                    "Today" -> item.dateStr == todayStr
                    "Yesterday" -> item.dateStr == yesterdayStr
                    "Last 7 Days" -> item.timestamp >= sevenDaysAgo
                    else -> true
                }
                matchTab && matchPlatform && matchDate
            }
        }
    }

    // AUTO-SCROLL TO NEWEST ORDER
    val newestVisibleOrderId = filteredList.firstOrNull()?.id

    LaunchedEffect(newestVisibleOrderId) {
        if (newestVisibleOrderId != null && filteredList.isNotEmpty()) {
            historyListState.scrollToItem(0)
        }
    }
    val totalAcceptedCount by remember(totalAccepted, acceptedCount) {
        derivedStateOf { totalAccepted.coerceAtLeast(acceptedCount) }
    }
    val totalEarnings by remember(filteredList) {
        derivedStateOf {
            filteredList
                .filter { it.status == OrderStatus.ACCEPTED }
                .sumOf { it.amount.toDouble() }
                .toInt()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Order History (${history.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDarkPrimary
                    )
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
                actions = {
                    IconButton(
                        onClick = onNavigateToDiagnostics,
                        modifier = Modifier.testTag("btn_order_history_diagnostics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Ride Diagnostics",
                            tint = BluePrimary
                        )
                    }
                    if (history.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.clearHistory()
                            prefs.clearOrderHistory()
                        }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = StatusInactiveRed
                            )
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Four Tabs: Accepted, Ignored, Rejected, All
            val tabs = HistoryTab.entries
            val selectedTabIndex = tabs.indexOf(selectedTab)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = BluePrimary,
                indicator = { tabPositions ->
                    if (selectedTabIndex in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = BluePrimary,
                            height = 2.5.dp
                        )
                    }
                },
                divider = { HorizontalDivider(color = CardBorderDefault) }
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == tab
                    val tabCount = when (tab) {
                        HistoryTab.ACCEPTED -> acceptedCount
                        HistoryTab.IGNORED -> ignoredCount
                        HistoryTab.REJECTED -> rejectedCount
                        HistoryTab.ALL -> allCount
                    }
                    val badgeColor = when (tab) {
                        HistoryTab.ACCEPTED -> StatusActiveGreen
                        HistoryTab.IGNORED -> StatusWarningYellow
                        HistoryTab.REJECTED -> StatusInactiveRed
                        HistoryTab.ALL -> BlueSecondary
                    }

                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BluePrimary else TextDarkSecondary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) badgeColor.copy(alpha = 0.15f) else Color(0xFFEEEEEE),
                                            CircleShape
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = tabCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) badgeColor else TextDarkSecondary
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Stats Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorderDefault)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stat 1: Total Accepted
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Accepted",
                            fontSize = 11.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(StatusActiveGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = totalAcceptedCount.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(CardBorderDefault)
                    )

                    // Stat 2: Total Fare / Assisted
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Assisted Fare",
                            fontSize = 11.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹$totalEarnings",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(CardBorderDefault)
                    )

                    // Stat 3: Total Logged
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Logged",
                            fontSize = 11.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = history.size.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueSecondary
                        )
                    }
                }
            }

            // Horizontal Filter Chips: Date Range + Platform
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val dates = listOf("All", "Today", "Yesterday")
                items(dates) { d ->
                    FilterChip(
                        selected = selectedDateRange == d,
                        onClick = { selectedDateRange = d },
                        label = { Text(d, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextDarkPrimary
                        )
                    )
                }

                items(Platform.entries) { platform ->
                    FilterChip(
                        selected = selectedPlatformFilter == platform,
                        onClick = {
                            selectedPlatformFilter = if (selectedPlatformFilter == platform) null else platform
                        },
                        label = { Text(platform.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (platform) {
                                Platform.RAPIDO -> PlatformRapido
                                Platform.UBER -> Color(0xFF212121)
                                Platform.OLA -> StatusActiveGreen
                            },
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextDarkPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (history.isEmpty())
                            "No orders recorded yet.\nIncoming orders will appear here automatically."
                        else
                            "No orders match the selected tab or filter.",
                        color = TextDarkSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = historyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        HistoryCard(item)
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// HISTORY COMPACT UI SAFE V3
@Composable
private fun HistoryCard(item: OrderHistoryItem) {
    val formattedTime = remember(item.timestamp) {
        val t = if (item.timestamp > 0L) item.timestamp else System.currentTimeMillis()
        val sdf = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        sdf.format(Date(t))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorderDefault)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isNoGo = isNoGoOrder(item)
            val effectiveStatus = when {
                item.status == OrderStatus.PROCESSING -> OrderStatus.PROCESSING
                item.status == OrderStatus.ACCEPTED -> OrderStatus.ACCEPTED
                item.status == OrderStatus.FAILED -> OrderStatus.FAILED
                item.status == OrderStatus.SKIPPED -> OrderStatus.SKIPPED
                isNoGo || item.status == OrderStatus.REJECTED -> OrderStatus.REJECTED
                item.status == OrderStatus.MISSED -> OrderStatus.MISSED
                else -> OrderStatus.IGNORED
            }

            // Row 1: Platform badge + Vehicle badge + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Platform badge
                    val (platformBg, platformText) = when (item.platform) {
                        Platform.RAPIDO -> Pair(PlatformRapidoBg, PlatformRapido)
                        Platform.UBER -> Pair(Color(0xFFEEEEEE), Color(0xFF212121))
                        Platform.OLA -> Pair(StatusActiveGreenBg, StatusActiveGreen)
                    }
                    Box(
                        modifier = Modifier
                            .background(platformBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            item.platform.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = platformText
                        )
                    }

                    // Vehicle badge
                    Box(
                        modifier = Modifier
                            .background(BlueContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            item.vehicleType.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueSecondary
                        )
                    }
                }

                // Status badge
                val (statusBg, statusTextColor) = when (effectiveStatus) {
                    OrderStatus.PROCESSING -> Pair(Color(0xFFFFF8E1), Color(0xFFE65100))
                    OrderStatus.ACCEPTED -> Pair(StatusActiveGreenBg, StatusActiveGreen)
                    OrderStatus.REJECTED -> Pair(StatusInactiveRedBg, StatusInactiveRed)
                    OrderStatus.IGNORED -> Pair(StatusWarningYellowBg, StatusWarningYellow)
                    OrderStatus.FAILED -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
                    OrderStatus.SKIPPED -> Pair(Color(0xFFEDE7F6), Color(0xFF512DA8))
                    OrderStatus.MISSED -> Pair(Color(0xFFEEEEEE), Color.Gray)
                }
                val isToggleOff = effectiveStatus == OrderStatus.IGNORED && (item.reason.contains("toggle", ignoreCase = true) || item.reason.contains("OFF", ignoreCase = true))
                val statusLabel = when {
                    effectiveStatus == OrderStatus.PROCESSING -> "PROCESSING"
                    isToggleOff -> "IGNORED • TOGGLE OFF"
                    effectiveStatus == OrderStatus.FAILED -> "ACTION FAILED"
                    else -> effectiveStatus.name
                }
                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }
            }

            // Row 2: Formatted timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🕒 $formattedTime",
                    fontSize = 11.sp,
                    color = TextDarkSecondary,
                    fontWeight = FontWeight.Normal
                )
            }

            // Row 3: Bold Fare (14sp) + Base/Tip breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val displayFare = if (item.amount > 0f) "₹${item.amount.toInt()}" else "₹--"
                        Text(
                            text = displayFare,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Text(
                            text = "Total Fare",
                            fontSize = 10.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Base / Tip breakdown
                    val base = when {
                        item.baseFare > 0f -> item.baseFare
                        item.amount > 0f && item.tipAmount > 0f -> item.amount - item.tipAmount
                        item.amount > 0f -> item.amount
                        else -> 0f
                    }
                    val tip = if (item.tipAmount > 0f) item.tipAmount else 0f

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Base: ₹${base.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("•", fontSize = 10.sp, color = CardBorderDefault)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tip: +₹${tip.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tip > 0f) StatusActiveGreen else TextDarkSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (tip > 0f) "Includes rider tip/bonus" else "Standard base fare",
                            fontSize = 10.sp,
                            color = TextDarkSecondary
                        )
                    }
                }
            }

            // Row 4: Pick distance + Trip distance chips (padding 4dp, font 10sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pickup distance chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BlueContainer,
                    border = BorderStroke(1.dp, BlueSecondary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Pickup: ${if (item.pickupDistKm > 0f) "%.1f km".format(item.pickupDistKm) else "Nearby"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueSecondary
                        )
                    }
                }

                // Trip distance chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Trip: ${if (item.dropDistKm > 0f) "%.1f km".format(item.dropDistKm) else "N/A"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkPrimary
                        )
                    }
                }
            }

            HorizontalDivider(color = CardBorderDefault)

            // Row 5: Full pickup and drop addresses (labels 10sp, address 11sp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Full Pickup address
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(8.dp)
                            .background(StatusActiveGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "PICKUP ADDRESS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkSecondary
                        )
                        val cleanPickup = item.pickupAddress.takeIf {
                            it.isNotBlank() && !it.equals("Detected Pickup Location", ignoreCase = true)
                        } ?: "Pickup Location"
                        Text(
                            text = cleanPickup,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextDarkPrimary,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Full Drop address
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(8.dp)
                            .background(StatusInactiveRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DROP ADDRESS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkSecondary
                        )
                        val cleanDrop = item.dropAddress.takeIf {
                            it.isNotBlank() && !it.equals("Detected Drop Location", ignoreCase = true)
                        } ?: "Drop Location"
                        Text(
                            text = cleanDrop,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextDarkPrimary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Status Reason Section for each order:
            when (effectiveStatus) {
                OrderStatus.ACCEPTED -> {
                    val reasonText = formatAcceptedFilterReason(item)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusActiveGreenBg, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, StatusActiveGreen.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Order Accepted",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusActiveGreen
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = reasonText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }
                }
                OrderStatus.IGNORED -> {
                    val ignoredReason = when {
                        item.reason.isNotBlank() && item.reason.contains("fare unavailable", ignoreCase = true) -> "Fare unavailable - order skipped"
                        (item.amount <= 0f && item.baseFare <= 0f) && (item.reason.isBlank() || item.reason.contains("fare", ignoreCase = true) || item.reason.equals("No criteria matched", ignoreCase = true)) -> "Fare unavailable - order skipped"
                        item.reason.isNotBlank() && item.reason.contains("Nearby", ignoreCase = true) -> "Pickup is Nearby - order skipped"
                        item.reason.isNotBlank() && (item.reason.contains("toggle", ignoreCase = true) || item.reason.contains("OFF", ignoreCase = true)) -> "Auto-accept toggle was OFF"
                        item.reason.isNotBlank() && (item.reason.contains("drop distance", ignoreCase = true) || item.reason.contains("Drop ", ignoreCase = true)) -> item.reason
                        item.reason.isNotBlank() && item.reason.contains("pickup distance", ignoreCase = true) -> item.reason
                        item.reason.isNotBlank() && item.reason.contains("fare", ignoreCase = true) -> item.reason
                        item.reason.isNotBlank() && item.reason.contains("exceeds max limit", ignoreCase = true) -> item.reason
                        item.reason.isNotBlank() && item.reason.contains("below min fare", ignoreCase = true) -> item.reason
                        item.reason.isNotBlank() && item.reason.contains("skipped", ignoreCase = true) -> item.reason
                        item.reason.isNotBlank() && !item.reason.equals("No criteria matched", ignoreCase = true) -> item.reason
                        else -> "Filter criteria not matched"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusWarningYellowBg, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, StatusWarningYellow.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ℹ️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Order Ignored",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusWarningYellow
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = ignoredReason,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }
                }
                OrderStatus.REJECTED -> {
                    val areaName = extractAreaName(item)
                    val rejectedReason = if (item.reason.contains("Mode: No-Go", ignoreCase = true)) {
                        item.reason
                    } else if (item.reason.isNotBlank() && item.reason.contains("No-Go", ignoreCase = true)) {
                        if (areaName.isNotBlank() && !item.reason.contains(areaName, ignoreCase = true)) {
                            "${item.reason}: $areaName"
                        } else {
                            item.reason
                        }
                    } else if (areaName.isNotBlank()) {
                        "No-Go area matched: $areaName"
                    } else if (item.reason.isNotBlank()) {
                        item.reason
                    } else {
                        "No-Go area matched"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusInactiveRedBg, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, StatusInactiveRed.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("❌", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Order Rejected",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusInactiveRed
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = rejectedReason,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }
                }
                OrderStatus.PROCESSING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF9C4), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFFFBC02D).copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏳", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Processing Order",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = item.decisionReasonText.ifBlank { item.reason.ifBlank { "Evaluating ride filters..." } },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }
                }
                OrderStatus.FAILED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFFEF9A9A)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Action Failed",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = item.reason.ifBlank { "Accept action could not complete" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }
                }
                OrderStatus.SKIPPED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEDE7F6), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFFD1C4E9)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏭️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Order Skipped",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF512DA8)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = item.reason.ifBlank { "Skipped duplicate or invalid order" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }
                }
                OrderStatus.MISSED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏱️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Order Missed",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = item.reason.ifBlank { "Order timed out before response" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Part J — Performance Latencies (Truthful recorded values)
            val totalMs = when {
                item.totalProcessingMs > 0L -> item.totalProcessingMs
                item.decisionLatencyMs > 0L && effectiveStatus != OrderStatus.ACCEPTED -> item.decisionLatencyMs
                item.clickTimeMs > item.detectionTimeMs && item.detectionTimeMs > 0L -> item.clickTimeMs - item.detectionTimeMs
                else -> 0L
            }

            if (effectiveStatus == OrderStatus.PROCESSING || totalMs > 0L || item.historyInsertLatencyMs > 0L || item.decisionLatencyMs > 0L || item.actionLatencyMs > 0L) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ RESPONSE SPEED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            if (effectiveStatus == OrderStatus.PROCESSING) {
                                Text(
                                    text = "In progress...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            } else if (totalMs > 0L) {
                                Text(
                                    text = "Total: ${totalMs} ms",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (item.historyInsertLatencyMs > 0L) {
                                Text(
                                    text = "Insert: ${item.historyInsertLatencyMs} ms",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary
                                )
                            }
                            if (item.decisionLatencyMs > 0L) {
                                Text(
                                    text = "Decision: ${item.decisionLatencyMs} ms",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary
                                )
                            }
                            if (item.actionLatencyMs > 0L) {
                                Text(
                                    text = "Action: ${item.actionLatencyMs} ms",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format which filter matched for ACCEPTED orders:
 * e.g. "Fare ₹113 matched, Pickup 0.6km matched"
 */
private fun formatAcceptedFilterReason(item: OrderHistoryItem): String {
    val matches = mutableListOf<String>()

    val fare = if (item.amount > 0f) item.amount else item.baseFare
    if (fare > 0f) {
        matches += "Fare ₹${fare.toInt()} matched"
    }

    if (item.pickupDistKm > 0f) {
        matches += "Pickup ${String.format(Locale.ENGLISH, "%.1f km", item.pickupDistKm)} matched"
    }

    if (item.dropDistKm > 0f) {
        matches += "Trip ${String.format(Locale.ENGLISH, "%.1f km", item.dropDistKm)} matched"
    }

    val fullMatch = if (matches.isNotEmpty()) {
        matches.joinToString(" | ")
    } else {
        "Criteria matched"
    }

    // Preserve useful priority-mode information when present, while still
    // showing the real fare/pickup/trip values above.
    val specialReason = item.reason.trim()
    return when {
        specialReason.contains("Go-To", ignoreCase = true) ->
            "$fullMatch\n${specialReason}"
        specialReason.contains("Fastest Mode", ignoreCase = true) ->
            "$fullMatch\n${specialReason}"
        else -> fullMatch
    }
}
/**
 * Extract matched No-Go area name for REJECTED orders:
 * e.g. "No-Go area matched: Koramangala"
 */
private fun extractAreaName(item: OrderHistoryItem): String {
    val r = item.reason.trim()
    // 1. If reason already contains "No-Go area matched: XYZ"
    if (r.contains("No-Go area matched:", ignoreCase = true)) {
        val extracted = r.substringAfter("No-Go area matched:", "").trim().removeSurrounding("[", "]").removeSurrounding("'", "'").trim()
        if (extracted.isNotBlank()) return extracted
    }
    // 2. If single-quoted area name e.g. 'Koramangala'
    if (r.contains("'")) {
        val candidate = r.substringAfter("'").substringBefore("'").trim()
        if (candidate.isNotBlank() && candidate.length < 50) return candidate
    }
    // 3. If double-quoted area name e.g. "Koramangala"
    if (r.contains("\"")) {
        val candidate = r.substringAfter("\"").substringBefore("\"").trim()
        if (candidate.isNotBlank() && candidate.length < 50) return candidate
    }
    // 4. If item.dropArea is specified
    if (item.dropArea.isNotBlank()) {
        return item.dropArea.trim()
    }
    // 5. If item.dropAddress is specified, extract first address segment
    if (item.dropAddress.isNotBlank()) {
        val part = item.dropAddress.split(",").firstOrNull()?.trim().orEmpty()
        if (part.isNotBlank() && part.length < 40) return part
    }
    // 6. If item.pickupAddress is specified, extract first address segment
    if (item.pickupAddress.isNotBlank()) {
        val part = item.pickupAddress.split(",").firstOrNull()?.trim().orEmpty()
        if (part.isNotBlank() && part.length < 40) return part
    }
    // 7. If reason mentions an area after "area "
    if (r.contains("area ", ignoreCase = true)) {
        val after = r.substringAfter("area ", "").trim().take(30)
        if (after.isNotBlank()) return after
    }
    return "Restricted Area"
}

/**
 * Returns true if an order history item was rejected due to matching a No-Go area.
 */
private fun isNoGoOrder(item: OrderHistoryItem): Boolean {
    return item.status == OrderStatus.REJECTED ||
           item.reason.contains("No-Go", ignoreCase = true) ||
           item.reason.contains("nogo", ignoreCase = true) ||
           item.reason.contains("No Go", ignoreCase = true)
}
