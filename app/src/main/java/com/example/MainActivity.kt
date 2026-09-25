package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.PhoneAuthManager
import com.example.data.FirebaseRepository
import com.example.data.PreferencesManager
import com.google.firebase.FirebaseApp
import com.example.util.PermissionHelper
import com.example.model.MembershipPlan
import com.example.model.PaymentSubmission
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AreaManagerScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.DiagnosticScreen
import com.example.ui.screens.FinishSetupScreen
import com.example.ui.screens.GuestScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.OrderHistoryScreen
import com.example.ui.screens.PaymentFailedScreen
import com.example.ui.screens.PaymentHistoryScreen
import com.example.ui.screens.PaymentPendingScreen
import com.example.ui.screens.PaymentProcessingScreen
import com.example.ui.screens.PaymentScreen
import com.example.ui.screens.PaymentSuccessScreen
import com.example.ui.screens.PlanSelectionScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CardBorderDefault
import com.example.ui.theme.SmartDrivoTheme
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextDarkTertiary

object Routes {
    const val SPLASH = "splash"
    const val GUEST = "guest"
    const val LOGIN = "login"
    const val WELCOME = "welcome"
    const val PROFILE_SETUP = "profile_setup"
    const val PLAN_SELECTION = "plan_selection"
    const val PAYMENT = "payment"
    const val PAYMENT_PROCESSING = "payment_processing"
    const val PAYMENT_PENDING = "payment_pending"
    const val PAYMENT_SUCCESS = "payment_success"
    const val PAYMENT_FAILED = "payment_failed"
    const val PAYMENT_HISTORY = "payment_history"
    const val HOME = "home"
    const val AREA_MANAGER = "area_manager"
    const val ORDER_HISTORY = "order_history"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val COMMUNITY = "community"
    const val ADMIN_PANEL = "admin_panel"
    const val DIAGNOSTICS = "diagnostics"
    const val FINISH_SETUP = "finish_setup"
}

private data class NavItemData(
    val label: String,
    val route: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {



    private lateinit var preferencesManager: PreferencesManager
    private lateinit var firebaseRepository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        try {
            preferencesManager = PreferencesManager.getInstance(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error creating PreferencesManager: ${e.message}", e)
        }

        try {
            PhoneAuthManager.init(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing PhoneAuthManager: ${e.message}", e)
        }

        try {
            firebaseRepository = FirebaseRepository(applicationContext, preferencesManager)
            firebaseRepository.fetchGlobalSettings()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing FirebaseRepository: ${e.message}", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != 
                    PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error requesting notification permission: ${e.message}")
            }
        }

        setContent {
            val appSettings by preferencesManager.appSettings.collectAsState()

            SmartDrivoTheme(themeMode = appSettings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartDrivoApp(
                        prefs = preferencesManager,
                        repository = firebaseRepository
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndApplyAutoAcceptOnResume()
    }

    private fun checkAndApplyAutoAcceptOnResume() {
        try {
            if (::preferencesManager.isInitialized && preferencesManager.isPendingAutoAcceptActivation) {
                val hasAccessibility = PermissionHelper.isAccessibilityPermissionGranted(applicationContext)
                val hasOverlay = PermissionHelper.isOverlayPermissionGranted(applicationContext)

                if (hasAccessibility && hasOverlay) {
                    preferencesManager.isPendingAutoAcceptActivation = false
                    val current = preferencesManager.appSettings.value
                    preferencesManager.saveAppSettings(current.copy(isAutoAcceptActive = true))
                    android.widget.Toast.makeText(
                        applicationContext,
                        "Auto-Accept is now Active! ✓",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in checkAndApplyAutoAcceptOnResume: ${e.message}", e)
        }
    }
}

@Composable
fun SmartDrivoApp(
    prefs: PreferencesManager,
    repository: FirebaseRepository
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val checkAllPermissionsGranted = {
        PermissionHelper.isAccessibilityPermissionGranted(appContext) &&
            PermissionHelper.isOverlayPermissionGranted(appContext) &&
            PermissionHelper.hasNotificationPermission(appContext)
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userProfile by prefs.userProfile.collectAsState()

    val membershipPlans by
        prefs.membershipPlans.collectAsState()

    val paymentSubmissions by
        prefs.paymentSubmissions.collectAsState()
    val isUserAdmin = userProfile.isAdmin
    val firebaseUser = PhoneAuthManager.getAuthInstance()?.currentUser
    val isLoggedIn = prefs.isLoggedIn && (firebaseUser != null || userProfile.email.isNotBlank() || userProfile.phone.isNotBlank())
    val isProfileComplete = userProfile.phone.isNotBlank() && userProfile.city.isNotBlank() && userProfile.state.isNotBlank()
    val isMembershipActive = (isUserAdmin || userProfile.isPlanValid) && userProfile.isActive

    // Verify profile with Firestore for existing users on startup
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            // GLOBAL_MEMBERSHIP_LIVE_SYNC_V2
            repository.startOwnMembershipSync()
            // Refresh global pricing after login
            repository.fetchGlobalSettings()
            val fUser = PhoneAuthManager.getAuthInstance()?.currentUser
            val uid = fUser?.uid ?: userProfile.uid
            val email = fUser?.email ?: userProfile.email
            if (uid.isNotBlank() || email.isNotBlank()) {
                repository.checkUserProfileFromFirestore(uid, email) { fsProfile ->
                    if (fsProfile != null) {
                        val updated = userProfile.copy(
                            phone = fsProfile.phone.ifEmpty { userProfile.phone },
                            city = fsProfile.city.ifEmpty { userProfile.city },
                            state = fsProfile.state.ifEmpty { userProfile.state },
                            name = fsProfile.name.ifEmpty { userProfile.name },
                            vehicleType = fsProfile.vehicleType,
                            plan = fsProfile.plan,
                            planPrice = fsProfile.planPrice,
                            planExpireMillis = fsProfile.planExpireMillis,
                            isApproved = fsProfile.isApproved,
                            isAdmin = fsProfile.isAdmin,
                            isActive = fsProfile.isActive
                        )
                        prefs.saveUserProfile(updated)
                        if (!updated.isPlanValid && !updated.isAdmin) {
                            prefs.setAutoAcceptActive(false)
                        }
                    } else {
                        if (userProfile.isAdmin) {
                            val downgraded = userProfile.copy(isAdmin = false)
                            prefs.saveUserProfile(downgraded)
                        }
                    }
                }
            }
        }
    }

    val initialRoute = remember {
        val fUser = PhoneAuthManager.getAuthInstance()?.currentUser
        val loggedIn = prefs.isLoggedIn && (fUser != null || userProfile.email.isNotBlank() || userProfile.phone.isNotBlank())
        val admin = userProfile.isAdmin
        val hasAllPermissions = checkAllPermissionsGranted()

        if (!loggedIn) {
            Routes.WELCOME
        } else if (!admin && (userProfile.phone.isBlank() || userProfile.city.isBlank() || userProfile.state.isBlank())) {
            Routes.PROFILE_SETUP
        } else if (!admin && !userProfile.isPlanValid) {
            Routes.PLAN_SELECTION
        } else if (!hasAllPermissions) {
            Routes.FINISH_SETUP
        } else {
            Routes.HOME
        }
    }

    var selectedPlanForPayment by remember {
        mutableStateOf(MembershipPlan.DEFAULT_PLANS[1])
    }

    var activePaymentSubmission by remember {
        mutableStateOf<PaymentSubmission?>(null)
    }

    val bottomNavItems = remember {
        listOf(
            NavItemData("Home", Routes.HOME, Icons.Default.Home),
            NavItemData("Filters", Routes.SETTINGS, Icons.Default.Tune),
            NavItemData("Areas", Routes.AREA_MANAGER, Icons.Default.Place),
            NavItemData("History", Routes.ORDER_HISTORY, Icons.Default.History),
            NavItemData("More", Routes.PROFILE, Icons.Default.MoreHoriz)
        )
    }
    val mainRoutes = remember {
        setOf(Routes.HOME, Routes.SETTINGS, Routes.AREA_MANAGER, Routes.ORDER_HISTORY, Routes.PROFILE)
    }
    val showBottomBar = currentRoute in mainRoutes && isLoggedIn && (isUserAdmin || (isProfileComplete && isMembershipActive))

    val isDarkNavyScreen = currentRoute == Routes.WELCOME

    val authRoutes = remember { setOf(Routes.WELCOME, Routes.SPLASH, Routes.LOGIN) }
    val profileRoutes = remember { setOf(Routes.PROFILE_SETUP) }
    val paymentRoutes = remember {
        setOf(
            Routes.PLAN_SELECTION,
            Routes.PAYMENT,
            Routes.PAYMENT_PROCESSING,
            Routes.PAYMENT_PENDING,
            Routes.PAYMENT_SUCCESS,
            Routes.PAYMENT_FAILED,
            Routes.PAYMENT_HISTORY,
            Routes.COMMUNITY
        )
    }

    // Gate Check Enforcer:
    // Step 1: Authentication -> WelcomeScreen
    // Step 2: Profile Setup (mobile, city, state) -> ProfileSetupScreen
    // Step 3: Active membership/payment -> PlanSelectionScreen
    // Only after all 3 steps -> HomeScreen (full app access)
    LaunchedEffect(currentRoute, isLoggedIn, isProfileComplete, isMembershipActive) {
        if (currentRoute == null) return@LaunchedEffect

        if (!isLoggedIn) {
            if (currentRoute !in authRoutes) {
                navController.navigate(Routes.WELCOME) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (!isProfileComplete && !isUserAdmin) {
            if (currentRoute !in profileRoutes) {
                navController.navigate(Routes.PROFILE_SETUP) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (!isMembershipActive) {
            if (currentRoute !in paymentRoutes) {
                navController.navigate(Routes.PLAN_SELECTION) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(
                        color = if (isDarkNavyScreen) Color(0xFF1E324F) else CardBorderDefault,
                        thickness = 1.dp
                    )
                    NavigationBar(
                        containerColor = if (isDarkNavyScreen) Color(0xFF0A1628) else Color.White,
                        contentColor = if (isDarkNavyScreen) Color.White else TextDarkPrimary,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route || (item.route == Routes.HOME && currentRoute == Routes.WELCOME)
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (isSelected) BluePrimary else if (isDarkNavyScreen) Color(0xFF94A3B8) else TextDarkTertiary
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(Routes.HOME) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BluePrimary,
                                    selectedTextColor = BluePrimary,
                                    indicatorColor = if (isDarkNavyScreen) Color(0xFF132238) else BlueContainer,
                                    unselectedIconColor = if (isDarkNavyScreen) Color(0xFF94A3B8) else TextDarkTertiary,
                                    unselectedTextColor = if (isDarkNavyScreen) Color(0xFF94A3B8) else TextDarkTertiary
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = if (isDarkNavyScreen) Color(0xFF0A1628) else Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = initialRoute,
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) }
            ) {
        // 1. Splash Screen
        composable(Routes.SPLASH) {
            SplashScreen(
                onTimeout = {
                    val fUser = PhoneAuthManager.getAuthInstance()?.currentUser
                    val loggedIn = prefs.isLoggedIn && (fUser != null || userProfile.phone.isNotEmpty() || userProfile.email.isNotEmpty())
                    val admin = userProfile.isAdmin
                    val hasAllPermissions = checkAllPermissionsGranted()
                    prefs.hasOpenedBefore = true
                    if (!loggedIn) {
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else if (!admin && (userProfile.phone.isBlank() || userProfile.city.isBlank() || userProfile.state.isBlank())) {
                        navController.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else if (!admin && !userProfile.isPlanValid) {
                        navController.navigate(Routes.PLAN_SELECTION) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else if (!hasAllPermissions) {
                        navController.navigate(Routes.FINISH_SETUP) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 2. Guest Screen (Disabled - app strictly locked)
        composable(Routes.GUEST) {
            LaunchedEffect(Unit) {
                navController.navigate(Routes.WELCOME) {
                    popUpTo(Routes.GUEST) { inclusive = true }
                }
            }
        }

        // 3. Welcome Screen (Merged Auth + Features list + Membership plans)
        composable(Routes.WELCOME) {
            WelcomeScreen(
                prefs = prefs,
                onNavigateToHome = {
                    if (!isLoggedIn) {
                        // Stay on welcome
                    } else if (!isProfileComplete && !isUserAdmin) {
                        navController.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    } else if (!isMembershipActive) {
                        navController.navigate(Routes.PLAN_SELECTION) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    } else {
                        val hasAllPermissions = checkAllPermissionsGranted()
                        val dest = if (hasAllPermissions) Routes.HOME else Routes.FINISH_SETUP
                        navController.navigate(dest) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                },
                onPlanSelectedForFullPayment = { plan ->
                    selectedPlanForPayment = plan
                    navController.navigate(Routes.PAYMENT)
                },
                onSubmitDirectPayment = { submission ->
                    activePaymentSubmission = submission
                    repository.submitPayment(submission) {
                        navController.navigate(Routes.PAYMENT_PROCESSING)
                    }
                },
                onLoginSuccess = { phoneOrEmail ->
                    prefs.isLoggedIn = true
                    prefs.hasOpenedBefore = true
                    val email = if (phoneOrEmail.contains("@")) phoneOrEmail else userProfile.email
                    val currentUid = repository.getCurrentUid().ifEmpty { userProfile.uid }

                    repository.checkUserProfileFromFirestore(currentUid, email) { firestoreProfile ->
                        val merged = if (firestoreProfile != null) {
                            userProfile.copy(
                                uid = firestoreProfile.uid.ifEmpty { currentUid },
                                name = firestoreProfile.name.ifEmpty { userProfile.name },
                                email = if (email.isNotBlank()) email else firestoreProfile.email,
                                phone = firestoreProfile.phone.ifEmpty { if (!phoneOrEmail.contains("@")) phoneOrEmail else userProfile.phone },
                                city = firestoreProfile.city,
                                state = firestoreProfile.state,
                                vehicleType = firestoreProfile.vehicleType,
                                plan = firestoreProfile.plan,
                                planPrice = firestoreProfile.planPrice,
                                planExpireMillis = firestoreProfile.planExpireMillis,
                                isApproved = firestoreProfile.isApproved,
                                isAdmin = firestoreProfile.isAdmin,
                                isActive = firestoreProfile.isActive
                            )
                        } else {
                            val updatedEmail = if (phoneOrEmail.contains("@")) phoneOrEmail else userProfile.email
                            val updatedPhone = if (!phoneOrEmail.contains("@")) phoneOrEmail else userProfile.phone
                            userProfile.copy(uid = currentUid, email = updatedEmail, phone = updatedPhone, isAdmin = false)
                        }
                        prefs.saveUserProfile(merged)
                        if (!merged.isPlanValid && !merged.isAdmin) {
                            prefs.setAutoAcceptActive(false)
                        }

                        // If empty → go to ProfileSetupScreen. If filled → continue to next gate check
                        val isProfileEmpty = merged.phone.isBlank() || merged.city.isBlank() || merged.state.isBlank()
                        if (isProfileEmpty && !merged.isAdmin) {
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        } else {
                            val isMembershipValid = (merged.isAdmin || merged.isPlanValid) && merged.isActive
                            if (isMembershipValid) {
                                val hasAllPermissions = checkAllPermissionsGranted()
                                val dest = if (hasAllPermissions) Routes.HOME else Routes.FINISH_SETUP
                                navController.navigate(dest) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Routes.PLAN_SELECTION) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            )
        }

        // 4. Profile Setup Screen
        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                initialName = userProfile.name,
                initialPhone = userProfile.phone,
                initialCity = userProfile.city,
                initialState = userProfile.state,
                initialVehicle = userProfile.vehicleType,
                onSaveProfile = { name, mobileNumber, city, state, vehicleType ->
                    val cleanPhone = if (mobileNumber.startsWith("+")) mobileNumber else "+91$mobileNumber"
                    val updated = userProfile.copy(
                        name = name,
                        phone = cleanPhone,
                        city = city,
                        state = state,
                        vehicleType = vehicleType
                    )
                    prefs.saveUserProfile(updated)
                    repository.saveUserProfile(updated)
                    if (updated.isAdmin || updated.isPlanValid) {
                        val hasAllPermissions = checkAllPermissionsGranted()
                        val dest = if (hasAllPermissions) Routes.HOME else Routes.FINISH_SETUP
                        navController.navigate(dest) {
                            popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.PLAN_SELECTION) {
                            popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 5. Membership / Plan Selection Screen
        composable(Routes.PLAN_SELECTION) {
            PlanSelectionScreen(
                plans = membershipPlans,
                currentPlanId = userProfile.plan.ifEmpty { "7DAYS" },
                currentPlanPrice = userProfile.planPrice,
                planExpireMillis = userProfile.planExpireMillis,
                isCurrentPlanActive =
                    userProfile.isPlanValid &&
                    userProfile.isActive,
                isAdmin = userProfile.isAdmin,

                onPlanSelected = { plan ->
                    selectedPlanForPayment = plan
                    navController.navigate(Routes.PAYMENT)
                },

                onBack =
                    if (
                        userProfile.isAdmin ||
                        userProfile.isPlanValid
                    ) {
                        {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.PROFILE) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    } else {
                        null
                    }
            )
        }


        // 6. Payment Screen
        composable(Routes.PAYMENT) {
            PaymentScreen(
                plan = selectedPlanForPayment,
                prefs = prefs,
                onSubmitPayment = { submission ->
                    activePaymentSubmission = submission
                    repository.submitPayment(submission) {
                        navController.navigate(Routes.PAYMENT_PROCESSING)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 7. Payment Processing Screen
        composable(Routes.PAYMENT_PROCESSING) {
            PaymentProcessingScreen(
                onFinishedChecking = {
                    navController.navigate(Routes.PAYMENT_PENDING) {
                        popUpTo(Routes.PAYMENT) { inclusive = true }
                    }
                }
            )
        }

        // 8. Payment Pending Screen
        composable(Routes.PAYMENT_PENDING) {

            // PAYMENT_PENDING_LIVE_SYNC_V2
            // Start Firestore live listeners immediately when this screen opens.
            // Admin approval/rejection should update this screen automatically.
            LaunchedEffect(Unit) {
                repository.startOwnMembershipSync()
            }

            val sub = activePaymentSubmission ?: PaymentSubmission(
                paymentId = "PAY-SAMPLE",
                uid = userProfile.uid,
                userName = userProfile.name,
                utrNumber = "425619847231",
                planSelected = selectedPlanForPayment.id,
                amount = selectedPlanForPayment.price
            )

            val liveSub =
                paymentSubmissions.firstOrNull {
                    it.paymentId == sub.paymentId
                } ?: sub

            LaunchedEffect(liveSub.status, isMembershipActive) {
                when {
                    liveSub.status ==
                        com.example.model.PaymentStatus.APPROVED ||
                        isMembershipActive -> {
                        navController.navigate(Routes.PAYMENT_SUCCESS) {
                            launchSingleTop = true
                        }
                    }

                    liveSub.status ==
                        com.example.model.PaymentStatus.REJECTED -> {
                        navController.navigate(Routes.PAYMENT_FAILED) {
                            launchSingleTop = true
                        }
                    }
                }
            }

            PaymentPendingScreen(
                submission = liveSub,
                onCheckStatus = {
                    repository.startOwnMembershipSync()

                    val currentSub =
                        prefs.paymentSubmissions.value.firstOrNull {
                            it.paymentId == sub.paymentId
                        }

                    when {
                        currentSub?.status ==
                            com.example.model.PaymentStatus.APPROVED ||
                            isMembershipActive ->
                            navController.navigate(Routes.PAYMENT_SUCCESS)

                        currentSub?.status ==
                            com.example.model.PaymentStatus.REJECTED ->
                            navController.navigate(Routes.PAYMENT_FAILED)

                        else ->
                            navController.navigate(Routes.PAYMENT_PROCESSING)
                    }
                },

                onGoToHome = {
                    if (isMembershipActive) {
                        val dest =
                            if (checkAllPermissionsGranted())
                                Routes.HOME
                            else
                                Routes.FINISH_SETUP

                        navController.navigate(dest) {
                            popUpTo(Routes.PAYMENT_PENDING) {
                                inclusive = true
                            }
                        }
                    } else {
                        navController.navigate(Routes.PLAN_SELECTION) {
                            popUpTo(Routes.PAYMENT_PENDING) {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }
        // 9. Payment Success Screen
        composable(Routes.PAYMENT_SUCCESS) {
            PaymentSuccessScreen(
                onContinueToHome = {
                    val hasAllPermissions = checkAllPermissionsGranted()
                    val dest = if (hasAllPermissions) Routes.HOME else Routes.FINISH_SETUP
                    navController.navigate(dest) {
                        popUpTo(Routes.PAYMENT_SUCCESS) { inclusive = true }
                    }
                }
            )
        }

        // 10. Payment Failed Screen
        composable(Routes.PAYMENT_FAILED) {
            PaymentFailedScreen(
                onRetry = {
                    navController.navigate(Routes.PLAN_SELECTION) {
                        popUpTo(Routes.PAYMENT_FAILED) { inclusive = true }
                    }
                },
                onContactAdmin = {
                    navController.navigate(Routes.COMMUNITY)
                }
            )
        }

        // 11. Payment History Screen
        composable(Routes.PAYMENT_HISTORY) {
            PaymentHistoryScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() }
            )
        }

        // 12. Main Home Dashboard
        composable(Routes.HOME) {
            HomeScreen(
                prefs = prefs,
                repository = repository,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToHistory = { navController.navigate(Routes.ORDER_HISTORY) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToAreaManager = { navController.navigate(Routes.AREA_MANAGER) },
                onNavigateToCommunity = { navController.navigate(Routes.COMMUNITY) }
            )
        }

        // 13. Area Rules Manager Screen
        composable(Routes.AREA_MANAGER) {
            AreaManagerScreen(
                prefs = prefs,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        // 14. Order History Screen
        composable(Routes.ORDER_HISTORY) {
            OrderHistoryScreen(
                prefs = prefs,
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onBack = { navController.popBackStack() }
            )
        }

        // 15. Settings Screen
        composable(Routes.SETTINGS) {
            SettingsScreen(
                prefs = prefs,
                onNavigateToAdminWeb = { navController.navigate(Routes.ADMIN_PANEL) },
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onBack = { navController.popBackStack() }
            )
        }

        // 16. More Screen
        composable(Routes.PROFILE) {
            MoreScreen(
                prefs = prefs,
                onNavigateToPlanSelection = { navController.navigate(Routes.PLAN_SELECTION) },
                onNavigateToPaymentHistory = { navController.navigate(Routes.PAYMENT_HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onNavigateToCommunity = { navController.navigate(Routes.COMMUNITY) },
                onNavigateToAdminPanel = { navController.navigate(Routes.ADMIN_PANEL) },
                onLogout = {
                    PhoneAuthManager.signOut()
                    prefs.isLoggedIn = false
                    prefs.saveUserProfile(userProfile.copy(name = "", email = "", phone = ""))
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 17. Community Screen
        composable(Routes.COMMUNITY) {
            CommunityScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() }
            )
        }

        // 18. Admin Panel Screen
        composable(Routes.ADMIN_PANEL) {
            if (!userProfile.isAdmin) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ADMIN_PANEL) { inclusive = true }
                    }
                }
            } else {
                AdminPanelScreen(
                    prefs = prefs,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // 19. Ride Engine Diagnostic Screen
        composable(Routes.DIAGNOSTICS) {
            DiagnosticScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() }
            )
        }

        // 20. First-Install Permission Finish Setup Screen
        composable(Routes.FINISH_SETUP) {
            FinishSetupScreen(
                onGoToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.FINISH_SETUP) { inclusive = true }
                    }
                }
            )
        }
            }
        }
    }
}
