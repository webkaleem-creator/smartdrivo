package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.LightBackground
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusActiveGreenBorder
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.StatusWarningYellowBg
import com.example.ui.theme.StatusWarningYellowBorder
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextDarkTertiary
import com.example.util.PermissionHelper

@Composable
fun FinishSetupScreen(
    onGoToHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityGranted by remember {
        mutableStateOf(PermissionHelper.isAccessibilityPermissionGranted(context))
    }
    var isOverlayGranted by remember {
        mutableStateOf(PermissionHelper.isOverlayPermissionGranted(context))
    }
    var isNotificationGranted by remember {
        mutableStateOf(PermissionHelper.hasNotificationPermission(context))
    }

    // If all permissions are already granted on screen launch, skip setup and navigate directly Home
    val initialAllGranted = remember {
        PermissionHelper.isAccessibilityPermissionGranted(context) &&
            PermissionHelper.isOverlayPermissionGranted(context) &&
            PermissionHelper.hasNotificationPermission(context)
    }

    LaunchedEffect(initialAllGranted) {
        if (initialAllGranted) {
            onGoToHome()
        }
    }

    var hasAttemptedStep1 by remember { mutableStateOf(false) }
    var hasAttemptedStep2 by remember { mutableStateOf(false) }
    var hasAttemptedStep3 by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted || PermissionHelper.hasNotificationPermission(context)
        if (!isNotificationGranted) {
            hasAttemptedStep3 = true
        }
    }

    // Re-check all permission states on ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val acc = PermissionHelper.isAccessibilityPermissionGranted(context)
                val ovl = PermissionHelper.isOverlayPermissionGranted(context)
                val notif = PermissionHelper.hasNotificationPermission(context)

                isAccessibilityGranted = acc
                isOverlayGranted = ovl
                isNotificationGranted = notif

                if (acc) hasAttemptedStep1 = false
                if (ovl) hasAttemptedStep2 = false
                if (notif) hasAttemptedStep3 = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allCompleted = isAccessibilityGranted && isOverlayGranted && isNotificationGranted
    val currentStep = when {
        !isAccessibilityGranted -> 1
        !isOverlayGranted -> 2
        !isNotificationGranted -> 3
        else -> 4
    }

    val isCurrentStepFailedAttempt = when (currentStep) {
        1 -> hasAttemptedStep1 && !isAccessibilityGranted
        2 -> hasAttemptedStep2 && !isOverlayGranted
        3 -> hasAttemptedStep3 && !isNotificationGranted
        else -> false
    }

    val handlePrimaryAction = {
        when (currentStep) {
            1 -> {
                hasAttemptedStep1 = true
                PermissionHelper.openAccessibilitySettings(context)
            }
            2 -> {
                hasAttemptedStep2 = true
                PermissionHelper.openOverlaySettings(context)
            }
            3 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    isNotificationGranted = true
                }
            }
            else -> {
                onGoToHome()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .testTag("finish_setup_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (allCompleted) StatusActiveGreenBg else BlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (allCompleted) Icons.Default.CheckCircle else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (allCompleted) StatusActiveGreen else BluePrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (allCompleted) "Setup Complete ✓" else "Finish Setup",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (allCompleted) {
                    "SmartDrivo is ready"
                } else {
                    "Complete 3 quick steps to enable automatic ride monitoring and auto-acceptance."
                },
                fontSize = 14.sp,
                color = if (allCompleted) StatusActiveGreen else TextDarkSecondary,
                fontWeight = if (allCompleted) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Step 1 Card: Accessibility Service
            SetupStepCard(
                stepNumber = 1,
                title = "Accessibility Service",
                description = "Required to detect incoming ride orders and read ride details automatically.",
                subNote = if (currentStep == 1 && isCurrentStepFailedAttempt) {
                    "Tap 'SmartDrivo' under Installed Services / Apps and toggle it ON."
                } else null,
                icon = Icons.Default.Accessibility,
                isGranted = isAccessibilityGranted,
                isActive = currentStep == 1,
                isRetryState = currentStep == 1 && isCurrentStepFailedAttempt,
                testTag = "step_accessibility",
                onClickAction = {
                    hasAttemptedStep1 = true
                    PermissionHelper.openAccessibilitySettings(context)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 2 Card: Display Over Other Apps
            SetupStepCard(
                stepNumber = 2,
                title = "Display Over Other Apps",
                description = "Required to show order overlay cards and trip controls over driver partner apps.",
                subNote = if (currentStep == 2 && isCurrentStepFailedAttempt) {
                    "Enable 'Allow display over other apps' for SmartDrivo."
                } else null,
                icon = Icons.Default.Layers,
                isGranted = isOverlayGranted,
                isActive = currentStep == 2,
                isRetryState = currentStep == 2 && isCurrentStepFailedAttempt,
                testTag = "step_overlay",
                onClickAction = {
                    hasAttemptedStep2 = true
                    PermissionHelper.openOverlaySettings(context)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 3 Card: Notifications
            SetupStepCard(
                stepNumber = 3,
                title = "Notifications",
                description = "Required for ride match alerts, background automation status, and driver updates.",
                subNote = if (currentStep == 3 && isCurrentStepFailedAttempt) {
                    "Please allow notifications in the prompt to receive real-time ride alerts."
                } else null,
                icon = Icons.Default.Notifications,
                isGranted = isNotificationGranted,
                isActive = currentStep == 3,
                isRetryState = currentStep == 3 && isCurrentStepFailedAttempt,
                testTag = "step_notification",
                onClickAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        isNotificationGranted = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Action Button
            if (!allCompleted) {
                Button(
                    onClick = handlePrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_setup_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentStepFailedAttempt) BlueDark else BluePrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isCurrentStepFailedAttempt) Icons.Default.Refresh else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCurrentStepFailedAttempt) "Retry Setup" else "Continue Setup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = onGoToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("go_to_home_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusActiveGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Go to Home",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    subNote: String?,
    icon: ImageVector,
    isGranted: Boolean,
    isActive: Boolean,
    isRetryState: Boolean,
    testTag: String,
    onClickAction: () -> Unit
) {
    val borderColor = when {
        isGranted -> StatusActiveGreenBorder
        isActive -> BluePrimary
        else -> CardBorderDefault
    }

    val cardBg = Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (isActive) 1.8.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator / Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isGranted -> StatusActiveGreenBg
                                isActive -> BlueContainer
                                else -> Color(0xFFF1F5F9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Enabled",
                            tint = StatusActiveGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) BluePrimary else TextDarkTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$stepNumber. $title",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted || isActive) TextDarkPrimary else TextDarkSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Pill
                if (isGranted) {
                    Surface(
                        color = StatusActiveGreenBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StatusActiveGreenBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusActiveGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Enabled",
                                color = StatusActiveGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isRetryState) {
                    Surface(
                        color = StatusWarningYellowBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StatusWarningYellowBorder)
                    ) {
                        Text(
                            text = "Not Detected",
                            color = StatusWarningYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = if (isActive) BlueContainer else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Required",
                            color = if (isActive) BluePrimary else TextDarkTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = TextDarkSecondary,
                lineHeight = 18.sp
            )

            if (subNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subNote,
                    fontSize = 12.sp,
                    color = BlueDark,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
