package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.auth.PhoneAuthManager
import com.example.data.PreferencesManager
import com.example.model.MembershipPlan
import com.example.model.PaymentStatus
import com.example.model.PaymentSubmission
import com.example.ui.components.SmartDrivoLogo
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BluePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// Colors specifically requested for WelcomeScreen
private val DarkNavyBackground = Color(0xFF0A1628)
private val BrightBlue = Color(0xFF1E88E5)
private val LogoGreenCircle = Color(0xFF10B981)
private val LightGrayText = Color(0xFF94A3B8)
private val CardNavy = Color(0xFF132238)
private val BorderNavy = Color(0xFF1E324F)

@Immutable
private data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String? = null
)

@Composable
fun WelcomeScreen(
    prefs: PreferencesManager,
    onNavigateToHome: () -> Unit,
    onPlanSelectedForFullPayment: (MembershipPlan) -> Unit = {},
    onSubmitDirectPayment: (PaymentSubmission) -> Unit = {},
    onLoginSuccess: (emailOrPhone: String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val userProfile by prefs.userProfile.collectAsState()
    val upiId by prefs.upiId.collectAsState()

    // Auth State
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isPhoneLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Resend countdown timer (30s)
    var resendCountdown by remember { mutableIntStateOf(0) }
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000L)
            resendCountdown -= 1
        }
    }

    var showGoogleAccountFallbackDialog by remember { mutableStateOf(false) }
    var fallbackCustomEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        PhoneAuthManager.init(context)
    }

    val isTestNumber by remember(phoneNumber) {
        derivedStateOf { PhoneAuthManager.isTestPhoneNumber(phoneNumber) }
    }

    val isPhoneValid by remember(phoneNumber) {
        derivedStateOf { phoneNumber.length == 10 && phoneNumber.all { it.isDigit() } }
    }

    val isOtpValid by remember(otpCode) {
        derivedStateOf { otpCode.length == 6 && otpCode.all { it.isDigit() } }
    }

    // Plans & Payment
    val welcomePlans = remember {
        listOf(
            MembershipPlan("3DAYS", 3, 59, "3 Days Pass", "Quick trial for new drivers"),
            MembershipPlan("7DAYS", 7, 129, "7 Days Pass", "Most popular weekly plan"),
            MembershipPlan("15DAYS", 15, 199, "15 Days Pass", "Best bi-weekly discount pack"),
            MembershipPlan("1MONTH", 30, 329, "1 Month Pass", "Best value unlimited auto-accept")
        )
    }

    var selectedPlan by remember { mutableStateOf(welcomePlans[1]) }
    var utrNumber by remember { mutableStateOf("") }
    var isSubmittingPayment by remember { mutableStateOf(false) }
    var utrError by remember { mutableStateOf<String?>(null) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    val upiDeepLink by remember(upiId, selectedPlan.price) {
        derivedStateOf {
            "upi://pay?pa=$upiId&pn=SmartDrivo&am=${selectedPlan.price}&cu=INR&tn=SmartDrivo_${selectedPlan.id}"
        }
    }

    val features = remember {
        listOf(
            FeatureItem(
                title = "High-Speed Auto-Accept",
                description = "Captures orders on Rapido, Uber & Ola in 0.1s before other drivers can tap.",
                icon = Icons.Default.ElectricBolt,
                badge = "0.1s Fast"
            ),
            FeatureItem(
                title = "Minimum Fare Filter",
                description = "Auto-skips low-paying trips. Only accepts high-value rides matching your target (₹80+).",
                icon = Icons.Default.TrendingUp,
                badge = "High Profit"
            ),
            FeatureItem(
                title = "Smart Area & No-Go Zones",
                description = "Target busy delivery corridors and block remote drop-off areas automatically.",
                icon = Icons.Default.LocationOn,
                badge = "Custom Areas"
            ),
            FeatureItem(
                title = "Floating Control Head",
                description = "Overlay widget stays on top of driver apps so you can toggle auto-accept with 1 tap.",
                icon = Icons.Default.Layers,
                badge = "Overlay"
            ),
            FeatureItem(
                title = "Anti-Ban Safety Shield",
                description = "Humanized random tap intervals prevent detection and protect your driver accounts.",
                icon = Icons.Default.Shield,
                badge = "100% Safe"
            ),
            FeatureItem(
                title = "Live Performance Analytics",
                description = "Track daily accepted orders, fuel savings, and earnings growth across all platforms.",
                icon = Icons.Default.BarChart,
                badge = "Insights"
            )
        )
    }

    // Helper login success handler
    val handleSuccessfulLogin: (String) -> Unit = { identifier ->
        prefs.isLoggedIn = true
        prefs.hasOpenedBefore = true
        val isAdmin = PhoneAuthManager.isAdminAccount(identifier)
        if (isAdmin) {
            val adminProfile = userProfile.copy(
                uid = if (userProfile.uid.isNotEmpty()) userProfile.uid else "admin_9949957404",
                name = if (userProfile.name.isNotEmpty()) userProfile.name else "Admin Driver",
                phone = if (identifier.contains("@")) userProfile.phone.ifEmpty { "+919949957404" } else identifier,
                email = if (identifier.contains("@")) identifier else userProfile.email.ifEmpty { PhoneAuthManager.ADMIN_EMAIL },
                plan = "LIFETIME_ADMIN",
                planPrice = 0,
                planExpireMillis = System.currentTimeMillis() + (3650L * 24 * 60 * 60 * 1000L),
                isApproved = true,
                isAdmin = true,
                isActive = true
            )
            prefs.saveUserProfile(adminProfile)
        } else {
            val updated = if (identifier.contains("@")) {
                userProfile.copy(email = identifier)
            } else {
                userProfile.copy(phone = identifier)
            }
            prefs.saveUserProfile(updated)
        }
        onLoginSuccess(identifier)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Logo Icon: 84dp combination logo (bike, car, auto + SD on blue circle)
            SmartDrivoLogo(size = 84.dp)

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Title "Welcome to SmartDrivo": 26sp bold, white color
            Text(
                text = "Welcome to SmartDrivo",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Subtitle: 15sp, light gray color
            Text(
                text = "Automated Ride Assistant for Rapido, Uber & Ola",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = LightGrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4. One-Tap Admin / Test Login Quick Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        phoneNumber = PhoneAuthManager.TEST_PHONE_RAW
                        errorMessage = null
                        infoMessage = "Admin phone selected! OTP is ${PhoneAuthManager.TEST_OTP_CODE}"
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E281E)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LogoGreenCircle.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(LogoGreenCircle.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Test Account",
                                tint = LogoGreenCircle,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin & Test Account (One-Tap)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LogoGreenCircle
                            )
                            Text(
                                text = "${PhoneAuthManager.TEST_PHONE_NUMBER} • OTP: ${PhoneAuthManager.TEST_OTP_CODE}",
                                fontSize = 11.sp,
                                color = Color(0xFFB0DBC1)
                            )
                        }
                    }
                    Text(
                        text = "Auto-Fill",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LogoGreenCircle,
                        modifier = Modifier
                            .background(LogoGreenCircle.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Auth Card with Phone Number + Send OTP Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderNavy)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isOtpSent) {
                        // Phone Number Input
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input ->
                                if (input.length <= 10 && input.all { it.isDigit() }) {
                                    phoneNumber = input
                                    errorMessage = null
                                    infoMessage = null
                                }
                            },
                            label = { Text("10-digit Mobile Number", fontSize = 13.sp) },
                            placeholder = { Text("e.g. 9949957404", color = Color(0xFF64748B), fontSize = 13.sp) },
                            prefix = {
                                Text(
                                    text = "+91  ",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            },
                            trailingIcon = {
                                if (phoneNumber.isNotEmpty()) {
                                    IconButton(onClick = { phoneNumber = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = LightGrayText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0A1628),
                                unfocusedContainerColor = Color(0xFF0A1628),
                                focusedBorderColor = BrightBlue,
                                unfocusedBorderColor = BorderNavy,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = BrightBlue,
                                unfocusedLabelColor = LightGrayText,
                                cursorColor = BrightBlue
                            )
                        )

                        if (isTestNumber) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LogoGreenCircle,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Test bypass recognized: instant OTP 123456",
                                    color = LogoGreenCircle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (infoMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = infoMessage!!,
                                color = LogoGreenCircle,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Send OTP button: full width, bright blue (#1E88E5)
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (isPhoneValid) {
                                    if (activity == null) {
                                        errorMessage = "Activity context required"
                                        return@Button
                                    }
                                    isPhoneLoading = true
                                    errorMessage = null
                                    infoMessage = null

                                    PhoneAuthManager.sendOtp(
                                        activity = activity,
                                        phoneNumber = phoneNumber,
                                        onCodeSent = { vId ->
                                            isPhoneLoading = false
                                            verificationId = vId
                                            isOtpSent = true
                                            resendCountdown = 30
                                            if (isTestNumber) {
                                                otpCode = PhoneAuthManager.TEST_OTP_CODE
                                            }
                                        },
                                        onAutoVerified = { verifiedPhone ->
                                            isPhoneLoading = false
                                            handleSuccessfulLogin(verifiedPhone)
                                        },
                                        onError = { error ->
                                            isPhoneLoading = false
                                            errorMessage = error
                                        }
                                    )
                                } else {
                                    errorMessage = "Please enter a valid 10-digit mobile number"
                                }
                            },
                            enabled = !isPhoneLoading && !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrightBlue,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF1E324F),
                                disabledContentColor = Color(0xFF64748B)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isPhoneLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sending OTP...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Send OTP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // OTP Verification View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BrightBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enter 6-Digit OTP",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    otpCode = input
                                    errorMessage = null
                                }
                            },
                            placeholder = { Text("• • • • • •", color = Color(0xFF64748B), fontSize = 18.sp) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0A1628),
                                unfocusedContainerColor = Color(0xFF0A1628),
                                focusedBorderColor = BrightBlue,
                                unfocusedBorderColor = BorderNavy,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Verify Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                val cleanOtp = otpCode.trim()
                                if (cleanOtp.length == 6) {
                                    isPhoneLoading = true
                                    errorMessage = null

                                    PhoneAuthManager.verifyOtp(
                                        phone = phoneNumber,
                                        verificationId = verificationId,
                                        otpCode = cleanOtp,
                                        onSuccess = { verifiedPhone ->
                                            isPhoneLoading = false
                                            handleSuccessfulLogin(verifiedPhone)
                                        },
                                        onError = { err ->
                                            isPhoneLoading = false
                                            errorMessage = err
                                        }
                                    )
                                } else {
                                    errorMessage = "Please enter 6-digit OTP code"
                                }
                            },
                            enabled = !isPhoneLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrightBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isPhoneLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            } else {
                                Text("Verify & Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    isOtpSent = false
                                    otpCode = ""
                                    errorMessage = null
                                }
                            ) {
                                Text("Change Number", color = LightGrayText, fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    if (resendCountdown <= 0 && activity != null) {
                                        isPhoneLoading = true
                                        errorMessage = null
                                        PhoneAuthManager.sendOtp(
                                            activity = activity,
                                            phoneNumber = phoneNumber,
                                            onCodeSent = { vId ->
                                                isPhoneLoading = false
                                                verificationId = vId
                                                resendCountdown = 30
                                                infoMessage = "New OTP sent!"
                                            },
                                            onAutoVerified = { verifiedPhone ->
                                                isPhoneLoading = false
                                                handleSuccessfulLogin(verifiedPhone)
                                            },
                                            onError = { error ->
                                                isPhoneLoading = false
                                                errorMessage = error
                                            }
                                        )
                                    }
                                },
                                enabled = resendCountdown <= 0 && !isPhoneLoading
                            ) {
                                if (resendCountdown > 0) {
                                    Text("Resend in ${resendCountdown}s", color = Color(0xFF64748B), fontSize = 12.sp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = BrightBlue, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Resend OTP", color = BrightBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Divider with "OR"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderNavy)
                        Text(
                            text = "  OR  ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderNavy)
                    }

                    // 6. Google Sign in button: full width, white background
                    Button(
                        onClick = {
                            isGoogleLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                GoogleAuthHelper.initiateGoogleSignIn(
                                    context = context,
                                    onTokenReceived = { idToken ->
                                        PhoneAuthManager.signInWithGoogleToken(
                                            idToken = idToken,
                                            onSuccess = { emailOrUid ->
                                                isGoogleLoading = false
                                                handleSuccessfulLogin(emailOrUid)
                                            },
                                            onError = { err ->
                                                isGoogleLoading = false
                                                errorMessage = err
                                            }
                                        )
                                    },
                                    onFallbackPrompt = { _ ->
                                        isGoogleLoading = false
                                        showGoogleAccountFallbackDialog = true
                                    }
                                )
                            }
                        },
                        enabled = !isGoogleLoading && !isPhoneLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F2937),
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF1F2937),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Connecting Google...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF1F2937)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7. Explore as Guest / Skip Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "First time here?",
                    color = LightGrayText,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(onClick = onNavigateToHome) {
                    Text(
                        text = "Explore as Guest →",
                        color = BrightBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trust & Security Notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Secured by Firebase Authentication • Safe for Drivers",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 8. Key Features Highlights
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderNavy)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ Key SmartDrivo Features",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-Accept 0.1s",
                            fontSize = 11.sp,
                            color = LogoGreenCircle,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider(color = BorderNavy)

                    features.forEachIndexed { index, feature ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(DarkNavyBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderNavy, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = LogoGreenCircle,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = feature.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (feature.badge != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(LogoGreenCircle.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = feature.badge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LogoGreenCircle
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = feature.description,
                                    fontSize = 11.sp,
                                    color = LightGrayText,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        if (index < features.size - 1) {
                            HorizontalDivider(color = BorderNavy.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 9. Membership Plans Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderNavy)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💳 Membership Passes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Select Plan",
                            fontSize = 11.sp,
                            color = BrightBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        welcomePlans.forEach { plan ->
                            val isSelected = plan.id == selectedPlan.id
                            val borderColor = if (isSelected) LogoGreenCircle else BorderNavy
                            val containerColor = if (isSelected) Color(0xFF0E281E) else DarkNavyBackground

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                                    .clickable { selectedPlan = plan },
                                colors = CardDefaults.cardColors(containerColor = containerColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (plan.id == "7DAYS") {
                                        Box(
                                            modifier = Modifier
                                                .background(BrightBlue, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("POPULAR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    } else if (plan.id == "1MONTH") {
                                        Box(
                                            modifier = Modifier
                                                .background(LogoGreenCircle, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("BEST VALUE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(BorderNavy, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("TRIAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Text(
                                        text = plan.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "₹${plan.price}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) LogoGreenCircle else Color.White,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )

                                    Text(
                                        text = "${plan.days} Days Pass",
                                        fontSize = 10.sp,
                                        color = LightGrayText
                                    )

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = LogoGreenCircle,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Direct UPI button for plan
                    Button(
                        onClick = { onPlanSelectedForFullPayment(selectedPlan) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrightBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay ₹${selectedPlan.price} for ${selectedPlan.label}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Google Sign-In Fallback Account Chooser Dialog
    if (showGoogleAccountFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleAccountFallbackDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Google Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose an account to sign in with Google Firebase Auth:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    val demoAccounts = listOf(
                        "driver.smartdrivo@gmail.com",
                        "admin.drivo@gmail.com",
                        "kkaleem7u@gmail.com"
                    )

                    demoAccounts.forEach { email ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleAccountFallbackDialog = false
                                    handleSuccessfulLogin(email)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = CardNavy,
                            border = BorderStroke(1.dp, BorderNavy)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(email, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fallbackCustomEmail,
                        onValueChange = { fallbackCustomEmail = it },
                        placeholder = { Text("Or type your Google email", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = fallbackCustomEmail.trim().ifEmpty { "driver.smartdrivo@gmail.com" }
                        showGoogleAccountFallbackDialog = false
                        handleSuccessfulLogin(chosen)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightBlue)
                ) {
                    Text("Continue", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleAccountFallbackDialog = false }) {
                    Text("Cancel", color = LightGrayText)
                }
            },
            containerColor = CardNavy,
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }
}
