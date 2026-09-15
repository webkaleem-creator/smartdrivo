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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.PreferencesManager
import com.example.model.OrderHistoryItem
import com.example.model.OrderStatus
import com.example.model.Platform
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
    ACCEPTED("Accepted"),
    IGNORED("Ignored"),
    REJECTED("Rejected"),
    ALL("All")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    prefs: PreferencesManager,
    onBack: () -> Unit
) {
    val history by prefs.orderHistory.collectAsState()
    val totalAccepted by prefs.totalAcceptedFlow.collectAsState()

    fun loadStats() {
        prefs.loadStats()
    }

    LaunchedEffect(Unit) {
        loadStats()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedTab by remember { mutableStateOf(HistoryTab.ACCEPTED) }
    var selectedPlatformFilter by remember { mutableStateOf<Platform?>(null) }
    var selectedDateRange by remember { mutableStateOf("All") } // "Today", "Yesterday", "Last 7 Days", "All"

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
    val ignoredCount = remember(history) { history.count { it.status == OrderStatus.IGNORED } }
    val rejectedCount = remember(history) {
        history.count { it.status == OrderStatus.REJECTED || it.status == OrderStatus.MISSED }
    }
    val allCount by remember(history) { derivedStateOf { history.size } }

    val filteredList by remember(history, selectedTab, selectedPlatformFilter, selectedDateRange, todayStr, yesterdayStr, sevenDaysAgo) {
        derivedStateOf {
            history.filter { item ->
                val matchTab = when (selectedTab) {
                    HistoryTab.ACCEPTED -> item.status == OrderStatus.ACCEPTED
                    HistoryTab.IGNORED -> item.status == OrderStatus.IGNORED
                    HistoryTab.REJECTED -> item.status == OrderStatus.REJECTED || item.status == OrderStatus.MISSED
                    HistoryTab.ALL -> true
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
                        fontSize = 20.sp,
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
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { prefs.clearOrderHistory() }) {
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
                            height = 3.dp
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
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BluePrimary else TextDarkSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) badgeColor.copy(alpha = 0.15f) else Color(0xFFEEEEEE),
                                            CircleShape
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tabCount.toString(),
                                        fontSize = 12.sp,
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
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorderDefault)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                            fontSize = 14.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(StatusActiveGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = totalAcceptedCount.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(CardBorderDefault)
                    )

                    // Stat 2: Total Fare / Assisted
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Assisted Fare",
                            fontSize = 14.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹$totalEarnings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(CardBorderDefault)
                    )

                    // Stat 3: Total Logged
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Logged",
                            fontSize = 14.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = history.size.toString(),
                            fontSize = 20.sp,
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
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dates = listOf("All", "Today", "Yesterday", "Last 7 Days")
                items(dates) { d ->
                    FilterChip(
                        selected = selectedDateRange == d,
                        onClick = { selectedDateRange = d },
                        label = { Text(d, fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextDarkPrimary
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedPlatformFilter == null,
                        onClick = { selectedPlatformFilter = null },
                        label = { Text("All Platforms", fontSize = 14.sp) },
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
                        label = { Text(platform.displayName, fontSize = 14.sp) },
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

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (history.isEmpty())
                            "No orders recorded yet.\nIncoming orders will appear here automatically."
                        else
                            "No orders match the selected tab or filter.",
                        color = TextDarkSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        HistoryCard(item)
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorderDefault)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Platform badge + Vehicle badge + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Platform badge
                    val (platformBg, platformText) = when (item.platform) {
                        Platform.RAPIDO -> Pair(PlatformRapidoBg, PlatformRapido)
                        Platform.UBER -> Pair(Color(0xFFEEEEEE), Color(0xFF212121))
                        Platform.OLA -> Pair(StatusActiveGreenBg, StatusActiveGreen)
                    }
                    Box(
                        modifier = Modifier
                            .background(platformBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            item.platform.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = platformText
                        )
                    }

                    // Vehicle badge
                    Box(
                        modifier = Modifier
                            .background(BlueContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            item.vehicleType.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueSecondary
                        )
                    }
                }

                // Status badge
                val (statusBg, statusTextColor) = when (item.status) {
                    OrderStatus.ACCEPTED -> Pair(StatusActiveGreenBg, StatusActiveGreen)
                    OrderStatus.REJECTED -> Pair(StatusInactiveRedBg, StatusInactiveRed)
                    OrderStatus.IGNORED -> Pair(StatusWarningYellowBg, StatusWarningYellow)
                    OrderStatus.MISSED -> Pair(Color(0xFFEEEEEE), Color.Gray)
                }
                val isToggleOff = item.status == OrderStatus.IGNORED && (item.reason.contains("toggle", ignoreCase = true) || item.reason.contains("OFF", ignoreCase = true))
                val statusLabel = if (isToggleOff) "IGNORED • TOGGLE OFF" else item.status.name
                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
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
                    fontSize = 14.sp,
                    color = TextDarkSecondary,
                    fontWeight = FontWeight.Normal
                )
            }

            // Row 3: Bold large Fare + Base/Tip breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val displayFare = if (item.amount > 0f) "₹${item.amount.toInt()}" else "₹--"
                        Text(
                            text = displayFare,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Text(
                            text = "Total Fare",
                            fontSize = 14.sp,
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("•", color = CardBorderDefault)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tip: +₹${tip.toInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tip > 0f) StatusActiveGreen else TextDarkSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (tip > 0f) "Includes rider tip/bonus" else "Standard base fare",
                            fontSize = 14.sp,
                            color = TextDarkSecondary
                        )
                    }
                }
            }

            // Row 4: Pick distance + Trip distance chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pickup distance chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BlueContainer,
                    border = BorderStroke(1.dp, BlueSecondary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pickup: ${if (item.pickupDistKm > 0f) "%.1f km".format(item.pickupDistKm) else "Nearby"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueSecondary
                        )
                    }
                }

                // Trip distance chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, CardBorderDefault)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Trip: ${if (item.dropDistKm > 0f) "%.1f km".format(item.dropDistKm) else "N/A"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkPrimary
                        )
                    }
                }
            }

            HorizontalDivider(color = CardBorderDefault)

            // Row 5: Full pickup and drop addresses (Card row style: icon on left, title bold, subtitle below in gray)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Full Pickup address
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(12.dp)
                            .background(StatusActiveGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "PICKUP ADDRESS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkSecondary
                        )
                        val cleanPickup = item.pickupAddress.takeIf {
                            it.isNotBlank() && !it.equals("Detected Pickup Location", ignoreCase = true)
                        } ?: "Pickup Location"
                        Text(
                            text = cleanPickup,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextDarkPrimary,
                            lineHeight = 20.sp
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
                            .padding(top = 4.dp)
                            .size(12.dp)
                            .background(StatusInactiveRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DROP ADDRESS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkSecondary
                        )
                        val cleanDrop = item.dropAddress.takeIf {
                            it.isNotBlank() && !it.equals("Detected Drop Location", ignoreCase = true)
                        } ?: "Drop Location"
                        Text(
                            text = cleanDrop,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextDarkPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Status Reason Section for each order:
            // - ACCEPTED: show which filter matched (e.g. "Fare ₹113 matched, Pickup 0.6km matched")
            // - IGNORED: show "No criteria matched" or "Fare unavailable - order skipped"
            // - REJECTED: show "No-Go area matched: [area name]"
            when (item.status) {
                OrderStatus.ACCEPTED -> {
                    val reasonText = formatAcceptedFilterReason(item)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusActiveGreenBg, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, StatusActiveGreen.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Order Accepted",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusActiveGreen
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = reasonText,
                                    fontSize = 14.sp,
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
                        item.reason.isNotBlank() && item.reason.contains("skipped", ignoreCase = true) -> item.reason
                        item.reason.isNotBlank() && !item.reason.equals("No criteria matched", ignoreCase = true) -> item.reason
                        else -> "No criteria matched"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusWarningYellowBg, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, StatusWarningYellow.copy(alpha = 0.6f)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ℹ️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Order Ignored",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusWarningYellow
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ignoredReason,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }
                }
                OrderStatus.REJECTED -> {
                    val rejectedReason = when {
                        item.reason.isNotBlank() && (item.reason.contains("drop distance", ignoreCase = true) || item.reason.contains("exceeds max limit", ignoreCase = true) || item.reason.contains("Drop ", ignoreCase = true)) -> {
                            item.reason
                        }
                        item.reason.isNotBlank() && item.reason.contains("pickup distance", ignoreCase = true) -> {
                            item.reason
                        }
                        item.reason.isNotBlank() && item.reason.contains("fare", ignoreCase = true) -> {
                            item.reason
                        }
                        item.reason.isNotBlank() && item.reason.contains("No-Go", ignoreCase = true) -> {
                            val areaName = extractAreaName(item)
                            "No-Go area matched: $areaName"
                        }
                        item.reason.isNotBlank() && !item.reason.equals("rejected", ignoreCase = true) -> {
                            item.reason
                        }
                        else -> {
                            val areaName = extractAreaName(item)
                            "No-Go area matched: $areaName"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusInactiveRedBg, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, StatusInactiveRed.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("❌", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Order Rejected",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusInactiveRed
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = rejectedReason,
                                    fontSize = 14.sp,
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
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏱️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Order Missed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.reason.ifBlank { "Order timed out before response" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
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
    // If the item reason already has a formatted matched filter string with actual numbers
    if (item.reason.isNotBlank() &&
        item.reason.contains("matched", ignoreCase = true) &&
        !item.reason.contains("No-Go", ignoreCase = true) &&
        !item.reason.equals("Fare & distance criteria matched", ignoreCase = true) &&
        (item.reason.contains("₹") || item.reason.contains("km"))
    ) {
        return item.reason
    }

    val matches = mutableListOf<String>()
    val fare = if (item.amount > 0f) item.amount else item.baseFare
    if (fare > 0f) {
        matches.add("Fare ₹${fare.toInt()} matched")
    }

    if (item.pickupDistKm > 0f) {
        val pickupStr = if (item.pickupDistKm % 1f == 0f && item.pickupDistKm >= 10f) {
            "${item.pickupDistKm.toInt()}km"
        } else {
            String.format(Locale.ENGLISH, "%.1fkm", item.pickupDistKm)
        }
        matches.add("Pickup $pickupStr matched")
    }

    if (matches.isEmpty() && item.dropDistKm > 0f) {
        val dropStr = String.format(Locale.ENGLISH, "%.1fkm", item.dropDistKm)
        matches.add("Trip $dropStr matched")
    }

    return if (matches.isNotEmpty()) {
        matches.joinToString(", ")
    } else {
        "Criteria matched"
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
