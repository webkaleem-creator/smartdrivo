package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.ui.components.QrCodeView
import com.example.model.MembershipPlan
import com.example.model.PaymentStatus
import com.example.model.PaymentSubmission
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.LightBackground
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusInactiveRed
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    plan: MembershipPlan,
    prefs: PreferencesManager,
    onSubmitPayment: (PaymentSubmission) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val userProfile by prefs.userProfile.collectAsState()
    val upiId by prefs.upiId.collectAsState()

    var utrNumber by remember { mutableStateOf("") }
    var utrError by remember { mutableStateOf<String?>(null) }

    // UTR_KEYBOARD_VISIBILITY_FIX_V1
    val paymentListState = rememberLazyListState()
    val paymentScope = rememberCoroutineScope()

    val upiDeepLink = remember(
        upiId,
        plan.id,
        plan.price
    ) {
        Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId.trim())
            .appendQueryParameter("pn", "SmartDrivo Admin")
            .appendQueryParameter(
                "am",
                plan.price.toString()
            )
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter(
                "tn",
                "SmartDrivo ${plan.label}"
            )
            .build()
            .toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Complete Payment",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = TextDarkPrimary
                    )
            )
        },
        containerColor = LightBackground
    ) { padding ->

        LazyColumn(
            state = paymentListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .imePadding(),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            item {
                Spacer(
                    modifier = Modifier.height(4.dp)
                )
            }


            // -----------------------------------------------
            // SELECTED PLAN
            // -----------------------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = BlueContainer
                        )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = plan.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text = "Amount: ₹${plan.price}",
                            fontSize = 25.sp,
                            fontWeight =
                                FontWeight.ExtraBold,
                            color = TextDarkPrimary
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "${plan.durationDays} Days Access",
                            fontSize = 14.sp,
                            color = TextDarkSecondary
                        )
                    }
                }
            }


            // -----------------------------------------------
            // ADMIN QR + ADMIN UPI
            // -----------------------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = CardBackground
                        )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(18.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Pay via UPI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary,
                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Scan the admin QR or pay directly using any installed UPI app.",
                            fontSize = 12.5.sp,
                            color = TextDarkSecondary,
                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        if (upiId.isNotBlank()) {

                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(12.dp),
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                QrCodeView(
                                    content = upiDeepLink,
                                    size = 190.dp,
                                    darkColor = Color.Black,
                                    lightColor = Color.White
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    "Scan to pay ₹${plan.price}",
                                fontSize = 12.sp,
                                fontWeight =
                                    FontWeight.SemiBold,
                                color = BluePrimary
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(14.dp)
                            )


                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        LightBackground,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Admin UPI ID",
                                        fontSize = 10.5.sp,
                                        color =
                                            TextDarkSecondary
                                    )

                                    Text(
                                        text = upiId,
                                        fontSize = 14.sp,
                                        fontWeight =
                                            FontWeight.Bold,
                                        color =
                                            TextDarkPrimary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager
                                            .setText(
                                                AnnotatedString(
                                                    upiId
                                                )
                                            )

                                        Toast.makeText(
                                            context,
                                            "UPI ID copied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription =
                                            "Copy UPI ID",
                                        tint = BluePrimary
                                    )
                                }
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(14.dp)
                            )


                            Button(
                                onClick = {

                                    try {
                                        val intent =
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(
                                                    upiDeepLink
                                                )
                                            )

                                        val chooser =
                                            Intent.createChooser(
                                                intent,
                                                "Pay ₹${plan.price} with UPI"
                                            )

                                        context.startActivity(
                                            chooser
                                        )

                                    } catch (e: Exception) {

                                        Toast.makeText(
                                            context,
                                            "No UPI payment app found. Install Google Pay, PhonePe, Paytm or BHIM.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),
                                colors =
                                    ButtonDefaults
                                        .buttonColors(
                                            containerColor =
                                                BluePrimary
                                        )
                            ) {
                                Text(
                                    text =
                                        "Pay ₹${plan.price} with UPI Apps",
                                    fontSize = 15.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color = Color.White
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    "Google Pay • PhonePe • Paytm • BHIM • other UPI apps",
                                fontSize = 10.5.sp,
                                color = TextDarkSecondary
                            )

                        } else {

                            Text(
                                text =
                                    "Admin UPI ID is not configured yet.",
                                color = StatusInactiveRed,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }
            }


            // -----------------------------------------------
            // UTR VERIFICATION
            // -----------------------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                CardBackground
                        )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(18.dp)
                    ) {

                        Text(
                            text =
                                "Enter 12-Digit UTR / Transaction Ref",
                            fontSize = 15.sp,
                            fontWeight =
                                FontWeight.SemiBold,
                            color = TextDarkPrimary
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "After payment, copy the UTR / transaction reference from your UPI app and enter it below.",
                            fontSize = 11.5.sp,
                            color = TextDarkSecondary
                        )

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        OutlinedTextField(
                            value = utrNumber,
                            onValueChange = {
                                utrNumber =
                                    it.filter {
                                            char ->
                                        char.isDigit()
                                    }.take(12)

                                utrError = null
                            },
                            placeholder = {
                                Text(
                                    "e.g. 425619847231"
                                )
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            paymentScope.launch {
                                                delay(180)
                                                // UTR card is item #3 in this LazyColumn.
                                                paymentListState.animateScrollToItem(3)
                                            }
                                        }
                                    },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            isError = utrError != null,
                            singleLine = true,
                            shape =
                                RoundedCornerShape(
                                    12.dp
                                )
                        )

                        if (utrError != null) {
                            Text(
                                text = utrError!!,
                                color =
                                    StatusInactiveRed,
                                fontSize = 12.sp,
                                modifier =
                                    Modifier.padding(
                                        top = 4.dp
                                    )
                            )
                        }
                    }
                }
            }


            // -----------------------------------------------
            // SEND REQUEST TO ADMIN
            // -----------------------------------------------
            item {
                Button(
                    onClick = {

                        if (utrNumber.length != 12) {

                            utrError =
                                "Please enter full 12-digit UTR number"

                        } else {

                            val submission =
                                PaymentSubmission(
                                    paymentId =
                                        "PAY-${
                                            UUID
                                                .randomUUID()
                                                .toString()
                                                .take(8)
                                                .uppercase()
                                        }",
                                    uid =
                                        userProfile.uid,
                                    userName =
                                        userProfile.name,
                                    utrNumber =
                                        utrNumber,
                                    planSelected =
                                        plan.id,
                                    amount =
                                        plan.price,
                                    timestamp =
                                        System
                                            .currentTimeMillis(),
                                    status =
                                        PaymentStatus.PENDING
                                )

                            onSubmitPayment(
                                submission
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape =
                        RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                BluePrimary
                        )
                ) {
                    Text(
                        text =
                            "Submit Payment for Admin Approval",
                        fontSize = 15.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )
            }
        }
    }
}
@Composable
fun PaymentProcessingScreen(onFinishedChecking: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500)
        onFinishedChecking()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(color = BluePrimary, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Verifying Payment...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connecting to payment gateway to check status of your transaction",
                fontSize = 14.sp,
                color = TextDarkSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PaymentPendingScreen(
    submission: PaymentSubmission,
    onCheckStatus: () -> Unit,
    onGoToHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.HourglassTop,
                contentDescription = "Pending",
                tint = StatusWarningYellow,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Payment Under Review", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your UTR (${submission.utrNumber}) has been submitted. Verification usually completes within 5-15 minutes.",
                fontSize = 14.sp,
                color = TextDarkSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onCheckStatus,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text(text = "Check Status", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGoToHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Go to Home", color = BluePrimary)
            }
        }
    }
}

@Composable
fun PaymentSuccessScreen(onContinueToHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = StatusActiveGreen,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Subscription Activated! 🎉", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your payment was confirmed. You now have full access to SmartDrivo auto-accept features.",
                fontSize = 14.sp,
                color = TextDarkSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onContinueToHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusActiveGreen)
            ) {
                Text(text = "Start Driving", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun PaymentFailedScreen(onRetry: () -> Unit, onContactAdmin: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Failed",
                tint = StatusInactiveRed,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Payment Verification Failed", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We could not verify your transaction ref. Please check your bank UTR or contact admin.",
                fontSize = 14.sp,
                color = TextDarkSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text(text = "Try Again", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onContactAdmin,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Contact Support", color = TextDarkPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(prefs: PreferencesManager, onBack: () -> Unit) {
    val submissions by prefs.paymentSubmissions.collectAsState()
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (submissions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No payment submissions found", color = TextDarkSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(LightBackground).padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = submissions,
                    key = { it.paymentId },
                    contentType = { "payment_submission_item" }
                ) { sub ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "₹${sub.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDarkPrimary)
                                val (statusText, color) = when (sub.status) {
                                    PaymentStatus.APPROVED -> "Approved" to StatusActiveGreen
                                    PaymentStatus.REJECTED -> "Rejected" to StatusInactiveRed
                                    PaymentStatus.PENDING -> "Pending" to StatusWarningYellow
                                }
                                Text(text = statusText, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "UTR: ${sub.utrNumber}", fontSize = 13.sp, color = TextDarkSecondary)
                            Text(text = sdf.format(Date(sub.timestamp)), fontSize = 12.sp, color = TextDarkSecondary)
                        }
                    }
                }
            }
        }
    }
}
