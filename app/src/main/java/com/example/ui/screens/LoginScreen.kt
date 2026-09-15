package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.SmartDrivoLogo
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BluePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (emailOrPhone: String) -> Unit,
    onGoogleLoginSuccess: ((emailOrUid: String) -> Unit)? = null,
    onExploreAsGuest: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isPhoneLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Resend OTP countdown timer (30s)
    var resendCountdown by remember { mutableIntStateOf(0) }
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000L)
            resendCountdown -= 1
        }
    }

    // Google Sign-In Account Chooser Fallback Dialog (for emulator / environments without Play Store account setup)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1628))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Brand Header with SmartDrivo combination logo (bike, car, auto + SD)
            SmartDrivoLogo(size = 80.dp)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Welcome to SmartDrivo",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Automated Ride Assistant for Rapido, Uber & Ola",
                fontSize = 15.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Admin / Quick Test Login Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable {
                        phoneNumber = PhoneAuthManager.TEST_PHONE_RAW
                        errorMessage = null
                        infoMessage = "Admin phone selected! OTP code is ${PhoneAuthManager.TEST_OTP_CODE} (No SMS required)"
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132A1E)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                .background(AccentGreen.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Test bypass",
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Admin & Test Account (One-Tap)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                            Text(
                                text = "${PhoneAuthManager.TEST_PHONE_NUMBER} • OTP: ${PhoneAuthManager.TEST_OTP_CODE}",
                                fontSize = 8.5.sp,
                                color = Color(0xFFB0DBC1)
                            )
                        }
                    }
                    Text(
                        text = "Auto-Fill",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        modifier = Modifier
                            .background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Main Auth Container Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {

                    // Step 1: Phone Number Input
                    if (!isOtpSent) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign In with Phone Number",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Enter your mobile number to receive a 6-digit OTP",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input ->
                                if (input.length <= 10 && input.all { it.isDigit() }) {
                                    phoneNumber = input
                                    errorMessage = null
                                    infoMessage = null
                                }
                            },
                            label = { Text("10-digit Mobile Number", fontSize = 10.sp) },
                            placeholder = { Text("e.g. 9949957404", color = Color(0xFF64748B), fontSize = 10.sp) },
                            prefix = {
                                Text(
                                    text = "+91  ",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            },
                            trailingIcon = {
                                if (phoneNumber.isNotEmpty()) {
                                    IconButton(onClick = { phoneNumber = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
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
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = AccentGreen,
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                cursorColor = AccentGreen
                            )
                        )

                        if (isTestNumber) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Test bypass recognized: instant OTP 123456",
                                    color = AccentGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (infoMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(infoMessage!!, color = AccentGreen, fontSize = 9.sp)
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFEF4444),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (isPhoneValid) {
                                    if (activity == null) {
                                        errorMessage = "Unable to start verification: Activity context required"
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
                                            onLoginSuccess(verifiedPhone)
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
                                containerColor = Color(0xFF1E88E5),
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Requesting OTP...", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            } else {
                                Text("Send OTP", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    } else {
                        // Step 2: OTP Verification Field
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enter 6-Digit OTP",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Enter code sent to +91 $phoneNumber",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        if (isTestNumber) {
                            Surface(
                                color = Color(0xFF132A1E),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Test Number: enter 123456",
                                        color = AccentGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    otpCode = input
                                    errorMessage = null
                                }
                            },
                            placeholder = { Text(if (isTestNumber) "123456" else "• • • • • •", color = Color(0xFF64748B), fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AccentGreen
                            )
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFEF4444),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (isOtpValid) {
                                    isPhoneLoading = true
                                    errorMessage = null

                                    PhoneAuthManager.verifyOtp(
                                        phone = phoneNumber,
                                        verificationId = verificationId,
                                        otpCode = otpCode,
                                        onSuccess = { phoneOrUid ->
                                            isPhoneLoading = false
                                            onLoginSuccess(phoneOrUid)
                                        },
                                        onError = { err ->
                                            isPhoneLoading = false
                                            errorMessage = err
                                        }
                                    )
                                } else {
                                    errorMessage = "Please enter the complete 6-digit OTP code"
                                }
                            },
                            enabled = !isPhoneLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5),
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
                                Text("Verifying...", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            } else {
                                Text("Verify & Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

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
                                Text("Change Number", color = Color(0xFF94A3B8), fontSize = 9.sp)
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
                                                onLoginSuccess(verifiedPhone)
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
                                    Text("Resend in ${resendCountdown}s", color = Color(0xFF64748B), fontSize = 9.sp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Resend OTP", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Divider with "OR"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                        Text(
                            text = "  OR  ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Official Google Sign-In Button
                    OutlinedButton(
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
                                                if (onGoogleLoginSuccess != null) {
                                                    onGoogleLoginSuccess(emailOrUid)
                                                } else {
                                                    onLoginSuccess(emailOrUid)
                                                }
                                            },
                                            onError = { err ->
                                                isGoogleLoading = false
                                                errorMessage = err
                                            }
                                        )
                                    },
                                    onFallbackPrompt = { note ->
                                        isGoogleLoading = false
                                        // Show Account Chooser Dialog
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

            Spacer(modifier = Modifier.height(14.dp))

            // Explore as Guest / Skip Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "First time here?",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.5.sp
                )
                TextButton(onClick = onExploreAsGuest) {
                    Text(
                        text = "Explore as Guest →",
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trust & Security Notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Secured by Firebase Authentication • Safe for Drivers",
                    fontSize = 8.5.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Google Sign-In Fallback / Account Selection Dialog
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
                    Text("Select Google Account", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose an account to sign in with Google Firebase Auth:",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Default Admin Google Account
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleAccountFallbackDialog = false
                                if (onGoogleLoginSuccess != null) {
                                    onGoogleLoginSuccess(PhoneAuthManager.ADMIN_EMAIL)
                                } else {
                                    onLoginSuccess(PhoneAuthManager.ADMIN_EMAIL)
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(AccentGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("W", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Kaleem (Admin Account)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = PhoneAuthManager.ADMIN_EMAIL,
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Custom Email Input Option
                    OutlinedTextField(
                        value = fallbackCustomEmail,
                        onValueChange = { fallbackCustomEmail = it },
                        label = { Text("Or enter another Google Email", fontSize = 9.sp) },
                        placeholder = { Text("driver@gmail.com", fontSize = 9.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = fallbackCustomEmail.trim()
                        if (email.contains("@")) {
                            showGoogleAccountFallbackDialog = false
                            if (onGoogleLoginSuccess != null) {
                                onGoogleLoginSuccess(email)
                            } else {
                                onLoginSuccess(email)
                            }
                        } else {
                            errorMessage = "Please enter a valid Google email address"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleAccountFallbackDialog = false }) {
                    Text("Cancel", fontSize = 10.sp)
                }
            }
        )
    }
}
