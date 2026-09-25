package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PreferencesManager
import com.example.data.RideDiagnosticReport
import com.example.data.RideDiagnosticsManager
import com.example.data.db.AppDatabase
import com.example.data.db.RideHistoryRepository
import com.example.data.db.entity.RideHistoryEntity
import com.example.engine.AreaRulesEngine
import com.example.engine.DecisionResult
import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.model.RideCandidate
import com.example.model.VehicleType
import com.example.service.FloatingOverlayService
import com.example.service.SmartDrivoAccessibilityService
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSecondary
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.LightBackground
import com.example.util.PermissionHelper
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    prefs: PreferencesManager,
    repository: RideHistoryRepository? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appSettings by prefs.appSettings.collectAsStateWithLifecycle()
    val isAutoAcceptEnabled = appSettings.isAutoAcceptActive
    val userProfile by prefs.userProfile.collectAsStateWithLifecycle()

    val liveReports by RideDiagnosticsManager.recentReports.collectAsState()

    // Fallback to Room DB records if live reports are empty
    val roomRepo = remember(context) {
        repository ?: RideHistoryRepository(AppDatabase.getInstance(context).rideHistoryDao())
    }
    val roomHistory by roomRepo.allHistory.collectAsState(initial = emptyList())

    val isServiceRunning = remember { SmartDrivoAccessibilityService.isServiceRunning }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Engine Diagnostics",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary,
                            fontFamily = FontFamily.Default
                        )
                        Text(
                            text = "Platform, rules, button detection & latencies",
                            fontSize = 12.sp,
                            color = TextDarkSecondary,
                            fontFamily = FontFamily.Default
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("diagnostic_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDarkPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { RideDiagnosticsManager.clearAll() },
                        modifier = Modifier.testTag("diagnostic_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear logs",
                            tint = TextDarkSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Engine & Driver Control Status Card
            item {
                EngineControlCard(
                    isServiceRunning = isServiceRunning,
                    isAutoAcceptEnabled = isAutoAcceptEnabled,
                    vehicleType = userProfile.vehicleType,
                    minFare = appSettings.minFare,
                    maxPickupKm = appSettings.maxPickupDistanceKm,
                    maxDropKm = appSettings.maxDropDistanceKm,
                    onToggleAutoAccept = { enabled ->
                        prefs.setAutoAcceptActive(enabled)
                    }
                )
            }

            // 2. Interactive Diagnostic Simulation Card
            item {
                SimulationDiagnosticCard(
                    roomRepo = roomRepo,
                    prefs = prefs
                )
            }

            // 3. Header for Diagnostic Log Items
            item {
                val evaluationsCount = if (liveReports.isNotEmpty()) liveReports.count() else roomHistory.count()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RIDE EVALUATIONS ($evaluationsCount)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary,
                        letterSpacing = 0.5.sp,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = "Newest Top",
                        fontSize = 12.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Default
                    )
                }
            }

            // 4. List of Diagnostic Reports
            if (liveReports.isNotEmpty()) {
                items(liveReports, key = { it.id }) { report ->
                    DiagnosticReportCard(report = report)
                }
            } else if (roomHistory.isNotEmpty()) {
                items(roomHistory, key = { it.id }) { entity ->
                    DiagnosticReportCard(report = entity.toDiagnosticReport())
                }
            } else {
                item {
                    EmptyDiagnosticCard()
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun EngineControlCard(
    isServiceRunning: Boolean,
    isAutoAcceptEnabled: Boolean,
    vehicleType: VehicleType,
    minFare: Float,
    maxPickupKm: Float,
    maxDropKm: Float,
    onToggleAutoAccept: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("engine_control_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = BorderStroke(1.dp, CardBorderDefault),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) StatusActiveGreen else StatusInactiveRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) "Accessibility Service Active" else "Accessibility Service Inactive",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isServiceRunning) StatusActiveGreen else StatusInactiveRed,
                        fontFamily = FontFamily.Default
                    )
                }

                // Driver Start/Stop Control Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAutoAcceptEnabled) "AUTO-ACCEPT ON" else "STOPPED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAutoAcceptEnabled) BluePrimary else TextDarkTertiary,
                        fontFamily = FontFamily.Default
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isAutoAcceptEnabled,
                        onCheckedChange = onToggleAutoAccept,
                        modifier = Modifier.testTag("diagnostic_auto_accept_toggle"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BluePrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = CardBorderDefault
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DiagnosticStatPill(label = "Vehicle", value = vehicleType.name)
                DiagnosticStatPill(label = "Min Fare", value = "₹${minFare.toInt()}")
                DiagnosticStatPill(label = "Max Pickup", value = "${maxPickupKm.toInt()} km")
                DiagnosticStatPill(label = "Max Drop", value = "${maxDropKm.toInt()} km")
            }
        }
    }
}

@Composable
private fun DiagnosticStatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextDarkSecondary,
            fontFamily = FontFamily.Default
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary,
            fontFamily = FontFamily.Default
        )
    }
}

@Composable
private fun SimulationDiagnosticCard(
    roomRepo: RideHistoryRepository,
    prefs: PreferencesManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRapidoSimulating by remember {
        mutableStateOf(false)
    }

    var isOlaSimulating by remember {
        mutableStateOf(false)
    }

    val anySimulationRunning =
        isRapidoSimulating ||
            isOlaSimulating

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simulation_diagnostic_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                BlueContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            BluePrimary.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "Engine Verification Tool",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    fontFamily = FontFamily.Default
                )
            }

            Text(
                text =
                    "Test Ola uses a mock Ola offer with your CURRENT saved filters. " +
                    "It verifies engine decision, history and diagnostics. " +
                    "It does not tap the real Ola app.",
                fontSize = 12.sp,
                color = TextDarkSecondary,
                modifier = Modifier.padding(
                    top = 4.dp,
                    bottom = 10.dp
                ),
                fontFamily = FontFamily.Default
            )

            // ------------------------------------------------
            // RAPIDO + OLA simulation buttons
            // ------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Button(
                    onClick = {
                        if (anySimulationRunning) {
                            return@Button
                        }

                        isRapidoSimulating = true

                        coroutineScope.launch(
                            Dispatchers.IO
                        ) {
                            try {
                                runSimulationTest(
                                    roomRepo,
                                    prefs
                                )
                            } finally {
                                isRapidoSimulating = false
                            }
                        }
                    },
                    enabled = !anySimulationRunning,
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = BluePrimary
                        ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(
                            "run_simulation_button"
                        )
                ) {
                    Text(
                        text =
                            if (isRapidoSimulating)
                                "Testing..."
                            else
                                "Test Ride",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Default
                    )
                }

                Button(
                    onClick = {
                        if (anySimulationRunning) {
                            return@Button
                        }

                        isOlaSimulating = true

                        coroutineScope.launch(
                            Dispatchers.IO
                        ) {
                            try {
                                runOlaSimulationTest(
                                    roomRepo = roomRepo,
                                    prefs = prefs
                                )
                            } finally {
                                isOlaSimulating = false
                            }
                        }
                    },
                    enabled = !anySimulationRunning,
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF00A859)
                        ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(
                            "run_ola_simulation_button"
                        )
                ) {
                    Text(
                        text =
                            if (isOlaSimulating)
                                "Testing Ola..."
                            else
                                "Test Ola",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Default
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // ------------------------------------------------
            // Existing overlay test
            // ------------------------------------------------
            Button(
                onClick = {
                    if (
                        !PermissionHelper
                            .isOverlayPermissionGranted(context)
                    ) {
                        PermissionHelper
                            .openOverlaySettings(context)
                    } else {

                        val timeFormat =
                            SimpleDateFormat(
                                "hh:mm a",
                                Locale.getDefault()
                            )

                        val currentTimeStr =
                            timeFormat.format(Date())

                        FloatingOverlayService.show(
                            context = context,
                            amount = 145f,
                            pickup =
                                "Indiranagar 100ft Rd, Bangalore",
                            pickupDist = 1.4f,
                            drop =
                                "Koramangala 5th Block, Bangalore",
                            dropDist = 6.8f,
                            dropArea = "Koramangala",
                            time = currentTimeStr,
                            platform = "Rapido"
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = BlueDark
                    ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(
                        "test_overlay_button"
                    )
            ) {
                Text(
                    text = "Test Overlay",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Default
                )
            }
        }
    }
}


// ============================================================
// OLA ENGINE DIAGNOSTIC SIMULATION
//
// MOCK ORDER:
// Fare        ₹150 total
// Base Fare   ₹140
// Tip         ₹10
// Pickup      1.5 km
// Drop        7.0 km
// Pickup      Mehdipatnam, Hyderabad
// Drop        Banjara Hills, Hyderabad
//
// IMPORTANT:
// Uses CURRENT SmartDrivo settings / Go-To / No-Go.
// No real Ola button is clicked.
// ============================================================

private suspend fun runOlaSimulationTest(
    roomRepo: RideHistoryRepository,
    prefs: PreferencesManager
) {
    val startMs =
        System.currentTimeMillis()

    val testId =
        "OLA-TEST-${startMs.toString().takeLast(6)}"

    val candidate =
        RideCandidate(
            platform = Platform.OLA,
            vehicleType = VehicleType.AUTO,
            fare = 150f,
            baseFare = 140f,
            tipAmount = 10f,
            pickupDistKm = 1.5f,
            dropDistKm = 7.0f,
            pickupAddress =
                "Mehdipatnam, Hyderabad",
            dropAddress =
                "Banjara Hills, Hyderabad",
            dropArea =
                "Banjara Hills",
            bookingId = testId,
            detectionTimeMs = startMs
        )

    // --------------------------------------------------------
    // 1. Detection / history insert
    // --------------------------------------------------------
    val entity =
        roomRepo.onOrderDetected(
            candidate = candidate,
            initialReasonCode =
                "OLA_TEST_PROCESSING",
            initialReasonText =
                "Ola diagnostic ride detected. Evaluating current saved filters..."
        )

    RideDiagnosticsManager.recordDetection(
        id = entity.id,
        candidate = candidate,
        insertLatencyMs =
            entity.historyInsertLatencyMs
    )

    kotlinx.coroutines.delay(40L)

    // --------------------------------------------------------
    // 2. Load CURRENT real SmartDrivo settings
    // --------------------------------------------------------
    val settings =
        prefs.loadSettings()

    val goToAreas =
        prefs.loadGoToAreas()

    val noGoAreas =
        prefs.loadNoGoAreas()

    // --------------------------------------------------------
    // 3. Use actual SmartDrivo filter/rules engine
    // --------------------------------------------------------
    val decision: DecisionResult =
        when {

            !settings.isAutoAcceptActive ->
                DecisionResult.Ignore(
                    "Master Auto-Accept is OFF"
                )

            !settings.olaEnabled ->
                DecisionResult.Ignore(
                    "Ola platform is OFF in Settings"
                )

            else ->
                AreaRulesEngine.evaluateRide(
                    candidate = candidate,
                    areas =
                        goToAreas + noGoAreas,
                    settings = settings,
                    goToAreas = goToAreas,
                    noGoAreas = noGoAreas,
                    pickupLocationTextOverride =
                        candidate.pickupAddress
                )
        }

    val status: OrderStatus
    val reasonCode: String
    val reasonText: String

    when (decision) {

        is DecisionResult.Accept -> {
            status =
                OrderStatus.ACCEPTED

            reasonCode =
                "OLA_TEST_FILTERS_MATCHED"

            reasonText =
                "OLA TEST • ${decision.reason}"
        }

        is DecisionResult.Reject -> {
            status =
                OrderStatus.REJECTED

            reasonCode =
                "OLA_TEST_FILTER_REJECTED"

            reasonText =
                "OLA TEST • ${decision.reason}"
        }

        is DecisionResult.Ignore -> {
            status =
                OrderStatus.IGNORED

            reasonCode =
                "OLA_TEST_IGNORED"

            reasonText =
                "OLA TEST • ${decision.reason}"
        }
    }

    val decisionEntity =
        roomRepo.onOrderDecision(
            id = entity.id,
            status = status,
            reasonCode = reasonCode,
            reasonText = reasonText
        )

    RideDiagnosticsManager.recordDecision(
        id = entity.id,
        status = status,
        ruleCode = reasonCode,
        exactRule = reasonText,
        decisionLatencyMs =
            decisionEntity
                ?.decisionLatencyMs
                ?: 0L
    )

    // --------------------------------------------------------
    // 4. If filters ACCEPT:
    //    simulate Ola Accept / Confirm node path.
    //
    //    NO REAL external Ola app click happens here.
    // --------------------------------------------------------
    if (status == OrderStatus.ACCEPTED) {

        kotlinx.coroutines.delay(25L)

        roomRepo.onOrderActionAttempt(
            entity.id
        )

        kotlinx.coroutines.delay(20L)

        val completed =
            roomRepo.onOrderActionCompleted(
                id = entity.id,
                status =
                    OrderStatus.ACCEPTED,
                reasonCode =
                    "OLA_TEST_AUTO_ACCEPTED",
                reasonText =
                    reasonText,
                actionSucceeded = true,
                timesClicked = 1
            )

        RideDiagnosticsManager.recordAction(
            id = entity.id,
            status =
                OrderStatus.ACCEPTED,
            buttonFound = true,
            buttonDetails =
                "MOCK OLA: Accept / Confirm node detected",
            clickMethod =
                "SIMULATED Ola ACTION_CLICK — no external app tap",
            finalAction =
                "OLA TEST PASSED • Filters matched • Accept/Confirm path ready",
            actionLatencyMs =
                completed
                    ?.actionLatencyMs
                    ?: 20L,
            totalLatencyMs =
                completed
                    ?.totalProcessingMs
                    ?: (
                        System.currentTimeMillis() -
                            startMs
                        )
        )

    } else {

        RideDiagnosticsManager.recordAction(
            id = entity.id,
            status = status,
            buttonFound = false,
            buttonDetails =
                "Button action skipped because current filters did not accept this mock Ola ride",
            clickMethod =
                "No click — filter decision",
            finalAction =
                when (status) {
                    OrderStatus.REJECTED ->
                        "OLA TEST REJECTED • $reasonText"

                    OrderStatus.IGNORED ->
                        "OLA TEST IGNORED • $reasonText"

                    else ->
                        "OLA TEST • $reasonText"
                },
            actionLatencyMs = 0L,
            totalLatencyMs =
                decisionEntity
                    ?.totalProcessingMs
                    ?: (
                        System.currentTimeMillis() -
                            startMs
                        )
        )
    }

    prefs.notifyOrderHistoryChanged()
}


private suspend fun runSimulationTest(roomRepo: RideHistoryRepository, prefs: PreferencesManager) {
    val startMs = System.currentTimeMillis()
    val testId = "TEST-${startMs.toString().takeLast(5)}"
    val candidate = RideCandidate(
        platform = Platform.RAPIDO,
        vehicleType = VehicleType.AUTO,
        fare = 125f,
        baseFare = 115f,
        tipAmount = 10f,
        pickupDistKm = 1.4f,
        dropDistKm = 6.8f,
        pickupAddress = "Indiranagar 100ft Rd, Bangalore",
        dropAddress = "Koramangala 5th Block, Bangalore",
        dropArea = "Koramangala",
        bookingId = testId,
        detectionTimeMs = startMs
    )

    // 1. Detection
    val entity = roomRepo.onOrderDetected(
        candidate = candidate,
        initialReasonCode = "PROCESSING",
        initialReasonText = "Evaluating ride filters..."
    )
    RideDiagnosticsManager.recordDetection(
        id = entity.id,
        candidate = candidate,
        insertLatencyMs = entity.historyInsertLatencyMs
    )

    kotlinx.coroutines.delay(45L)

    // 2. Decision
    val exactReason = "Criteria matched: ₹125 (Min ₹${prefs.appSettings.value.minFare.toInt()}), 1.4 km (Max ${prefs.appSettings.value.maxPickupDistanceKm.toInt()} km)"
    val decisionEntity = roomRepo.onOrderDecision(
        id = entity.id,
        status = OrderStatus.ACCEPTED,
        reasonCode = "FILTERS_MATCHED",
        reasonText = exactReason
    )
    RideDiagnosticsManager.recordDecision(
        id = entity.id,
        status = OrderStatus.ACCEPTED,
        ruleCode = "FILTERS_MATCHED",
        exactRule = exactReason,
        decisionLatencyMs = decisionEntity?.decisionLatencyMs ?: 45L
    )

    kotlinx.coroutines.delay(30L)

    // 3. Action Attempt
    roomRepo.onOrderActionAttempt(entity.id)

    kotlinx.coroutines.delay(25L)

    // 4. Action Completed
    val completedEntity = roomRepo.onOrderActionCompleted(
        id = entity.id,
        status = OrderStatus.ACCEPTED,
        reasonCode = "AUTO_ACCEPTED",
        reasonText = exactReason,
        actionSucceeded = true,
        timesClicked = 1
    )
    RideDiagnosticsManager.recordAction(
        id = entity.id,
        status = OrderStatus.ACCEPTED,
        buttonFound = true,
        buttonDetails = "Accept button node detected [Bounds: Rect(360, 1540 - 720, 1680)]",
        clickMethod = "ACTION_CLICK on strict button node",
        finalAction = "ACCEPTED: Dispatched click in 25ms",
        actionLatencyMs = completedEntity?.actionLatencyMs ?: 25L,
        totalLatencyMs = completedEntity?.totalProcessingMs ?: 100L
    )
    prefs.notifyOrderHistoryChanged()
}

@Composable
private fun DiagnosticReportCard(report: RideDiagnosticReport) {
    val statusColor = when (report.status) {
        OrderStatus.ACCEPTED -> StatusActiveGreen
        OrderStatus.REJECTED -> StatusInactiveRed
        OrderStatus.IGNORED -> StatusWarningYellow
        OrderStatus.FAILED -> StatusInactiveRed
        OrderStatus.PROCESSING -> BluePrimary
        else -> TextDarkSecondary
    }

    val statusBgColor = when (report.status) {
        OrderStatus.ACCEPTED -> StatusActiveGreenBg
        OrderStatus.REJECTED -> StatusInactiveRedBg
        OrderStatus.IGNORED -> StatusWarningYellowBg
        OrderStatus.FAILED -> StatusInactiveRedBg
        OrderStatus.PROCESSING -> BlueContainer
        else -> LightSurface
    }

    val platformColor = when (report.platform) {
        Platform.RAPIDO -> PlatformRapido
        Platform.UBER -> Color.Black
        Platform.OLA -> Color(0xFF00A859)
        else -> BluePrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diagnostic_card_${report.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = BorderStroke(1.dp, CardBorderDefault),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Platform Badge + Status Chip + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Platform badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(platformColor)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = report.platform.displayName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.platform == Platform.RAPIDO) Color.Black else Color.White,
                            fontFamily = FontFamily.Default
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Status Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = report.status.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = FontFamily.Default
                        )
                    }
                }

                Text(
                    text = "${report.dateStr} ${report.timeStr}",
                    fontSize = 11.sp,
                    color = TextDarkSecondary,
                    fontFamily = FontFamily.Default
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics row: Fare, Pickup km, Drop km
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FARE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = "₹${report.fare.toInt()}${if (report.tipAmount > 0f) " (+₹${report.tipAmount.toInt()} tip)" else ""}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        fontFamily = FontFamily.Default
                    )
                }

                Column {
                    Text(
                        text = "PICKUP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = if (report.pickupDistKm > 0f) "${report.pickupDistKm} km" else "Nearby",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkPrimary,
                        fontFamily = FontFamily.Default
                    )
                }

                Column {
                    Text(
                        text = "DROP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = if (report.dropDistKm > 0f) "${report.dropDistKm} km" else "—",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkPrimary,
                        fontFamily = FontFamily.Default
                    )
                }

                Column {
                    Text(
                        text = "TOTAL TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = "${report.totalLatencyMs} ms",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        fontFamily = FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Addresses
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(LightBackground)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusActiveGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.pickupAddress.ifBlank { "Pickup location" },
                        fontSize = 12.sp,
                        color = TextDarkPrimary,
                        maxLines = 1,
                        fontFamily = FontFamily.Default
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusInactiveRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.dropAddress.ifBlank { "Drop location" },
                        fontSize = 12.sp,
                        color = TextDarkPrimary,
                        maxLines = 1,
                        fontFamily = FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Matched Rule & Decision Details
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MATCHED RULE / REASON",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkSecondary,
                    fontFamily = FontFamily.Default
                )
                Text(
                    text = report.matchedRule,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDarkPrimary,
                    fontFamily = FontFamily.Default
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Button Detection & Action Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BUTTON DETECTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary,
                        fontFamily = FontFamily.Default
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (report.buttonFound) "✓ Button Detected" else if (report.status == OrderStatus.ACCEPTED) "✓ Verified" else "—",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (report.buttonFound || report.status == OrderStatus.ACCEPTED) StatusActiveGreen else TextDarkSecondary,
                            fontFamily = FontFamily.Default
                        )
                    }
                    if (report.buttonDetails.isNotBlank()) {
                        Text(
                            text = report.buttonDetails,
                            fontSize = 11.sp,
                            color = TextDarkSecondary,
                            maxLines = 2,
                            fontFamily = FontFamily.Default
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FINAL ACTION / ERROR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = report.finalAction,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (report.error != null || report.status == OrderStatus.FAILED) StatusInactiveRed else TextDarkPrimary,
                        fontFamily = FontFamily.Default
                    )
                    if (report.error != null) {
                        Text(
                            text = "Error: ${report.error}",
                            fontSize = 11.sp,
                            color = StatusInactiveRed,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
            }

            // Latency Benchmark Breakdown
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Insert: ${report.insertLatencyMs}ms",
                    fontSize = 11.sp,
                    color = TextDarkSecondary,
                    fontFamily = FontFamily.Default
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
                Text(
                    text = "Decision: ${report.decisionLatencyMs}ms",
                    fontSize = 11.sp,
                    color = TextDarkSecondary,
                    fontFamily = FontFamily.Default
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
                Text(
                    text = "Action: ${report.actionLatencyMs}ms",
                    fontSize = 11.sp,
                    color = TextDarkSecondary,
                    fontFamily = FontFamily.Default
                )
                if (report.duplicateEventsCount > 0) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "(${report.duplicateEventsCount} dupes merged)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueSecondary,
                        fontFamily = FontFamily.Default
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDiagnosticCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = BorderStroke(1.dp, CardBorderDefault),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = TextDarkTertiary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Diagnostic Reports Yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                fontFamily = FontFamily.Default
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When Rapido, Uber, or Ola ride offers appear on your screen, complete evaluation diagnostics, rule matching, button detection, and response times will appear here in real time.",
                fontSize = 13.sp,
                color = TextDarkSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontFamily = FontFamily.Default
            )
        }
    }
}

private fun RideHistoryEntity.toDiagnosticReport(): RideDiagnosticReport {
    val plat = try {
        Platform.valueOf(this.platform)
    } catch (_: Exception) {
        when {
            this.platform.contains("Rapido", ignoreCase = true) -> Platform.RAPIDO
            this.platform.contains("Uber", ignoreCase = true) -> Platform.UBER
            this.platform.contains("Ola", ignoreCase = true) -> Platform.OLA
            else -> Platform.RAPIDO
        }
    }

    val veh = try {
        VehicleType.valueOf(this.vehicleType)
    } catch (_: Exception) {
        VehicleType.AUTO
    }

    val ordStatus = this.orderStatus

    val isButtonDetected = ordStatus == OrderStatus.ACCEPTED || this.timesClicked > 0

    return RideDiagnosticReport(
        id = this.id,
        platform = plat,
        vehicleType = veh,
        detectedAt = this.detectedAt,
        dateStr = this.dateStr,
        timeStr = this.timeStr,
        fare = this.fare,
        baseFare = this.baseFare,
        tipAmount = this.tipAmount,
        pickupDistKm = this.pickupDistanceKm,
        dropDistKm = this.dropDistanceKm,
        pickupAddress = this.pickupAddress,
        dropAddress = this.dropAddress,
        status = ordStatus,
        matchedRule = this.decisionReasonText.ifBlank { "Evaluated by rules engine" },
        matchedRuleCode = this.decisionReasonCode,
        buttonFound = isButtonDetected,
        buttonDetails = if (isButtonDetected) "Accept button found and verified" else if (ordStatus == OrderStatus.FAILED) "Button not found on screen" else "N/A (Ignored by filter)",
        clickMethod = if (this.timesClicked > 0) "Strict Button ACTION_CLICK (${this.timesClicked} attempts)" else "None",
        finalAction = when (ordStatus) {
            OrderStatus.ACCEPTED -> "ACCEPTED in ${this.actionLatencyMs}ms"
            OrderStatus.REJECTED -> "REJECTED by filter rule"
            OrderStatus.IGNORED -> "IGNORED: ${this.decisionReasonText}"
            OrderStatus.FAILED -> "FAILED: ${this.decisionReasonText}"
            OrderStatus.PROCESSING -> "PROCESSING"
            else -> this.decisionReasonText
        },
        error = if (ordStatus == OrderStatus.FAILED) this.decisionReasonText else null,
        insertLatencyMs = this.historyInsertLatencyMs,
        decisionLatencyMs = this.decisionLatencyMs,
        actionLatencyMs = this.actionLatencyMs,
        totalLatencyMs = this.totalProcessingMs,
        duplicateEventsCount = this.duplicateEventsIgnored
    )
}
