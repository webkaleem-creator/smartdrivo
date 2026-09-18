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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.auth.PhoneAuthManager
import com.example.data.PreferencesManager
import com.example.model.MembershipPlan
import com.example.model.PaymentStatus
import com.example.model.PaymentSubmission
import com.example.ui.components.QrCodeView
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

    // Auth State - Google Sign-In Only
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        PhoneAuthManager.init(context)
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
                title = "Auto-Accept Orders",
                description = "Captures orders on Rapido, Uber & Ola in 0.1s before other drivers can tap.",
                icon = Icons.Default.ElectricBolt,
                badge = "0.1s Fast"
            ),
            FeatureItem(
                title = "Minimum Fare Filter",
                description = "Auto-skips low-paying trips. Accepts high-value rides matching your target (₹80+).",
                icon = Icons.Default.TrendingUp,
                badge = "High Fare"
            ),
            FeatureItem(
                title = "Smart Area Zones",
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
                title = "Performance Analytics",
                description = "Track daily accepted orders, fuel savings, and earnings growth across platforms.",
                icon = Icons.Default.BarChart,
                badge = "Insights"
            )
        )
    }

    // Helper login success handler
    val handleSuccessfulLogin: (String) -> Unit = { identifier ->
        prefs.isLoggedIn = true
        prefs.hasOpenedBefore = true
        val updated = if (identifier.contains("@")) {
            userProfile.copy(email = identifier)
        } else {
            userProfile.copy(phone = identifier)
        }
        prefs.saveUserProfile(updated)
        onLoginSuccess(identifier)
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

            // 2. Title "Welcome to SmartDrivo": clean display typography
            Text(
                text = "Welcome to SmartDrivo",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Subtitle: clean normal font
            Text(
                text = "Automated Ride Assistant for Rapido, Uber & Ola",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = LightGrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))



            // 4. Google Sign-In Card (Google Only Authentication)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderNavy)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign In with Google",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sign in securely with Google to access auto-accept, fare filters, and smart area automation.",
                        fontSize = 13.sp,
                        color = LightGrayText,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1219)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Google Sign In button: full width, white background
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
                                        errorMessage = "Google Play Services was unavailable or cancelled. Please try again."
                                    }
                                )
                            }
                        },
                        enabled = !isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
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
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "🔒 Secured by Firebase Authentication",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trust & Security Notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Secured by Firebase Authentication • Safe for Drivers",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Key Features Highlights (Compact & responsive without badge overflow)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderNavy)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ Key Features",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(LogoGreenCircle.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = "Auto-Accept 0.1s",
                                fontSize = 11.sp,
                                color = LogoGreenCircle,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = BorderNavy.copy(alpha = 0.7f))

                    features.forEachIndexed { index, feature ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(DarkNavyBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderNavy, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = LogoGreenCircle,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = feature.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (feature.badge != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(LogoGreenCircle.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = feature.badge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LogoGreenCircle,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = feature.description,
                                    fontSize = 11.sp,
                                    color = LightGrayText,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        if (index < features.size - 1) {
                            HorizontalDivider(color = BorderNavy.copy(alpha = 0.35f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
}
