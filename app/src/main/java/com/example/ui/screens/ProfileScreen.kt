package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.data.PreferencesManager

/**
 * ProfileScreen delegates to MoreScreen (representing the "More" tab).
 */
@Composable
fun ProfileScreen(
    prefs: PreferencesManager,
    onNavigateToPlanSelection: () -> Unit,
    onNavigateToPaymentHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToAdminPanel: () -> Unit = {},
    onLogout: () -> Unit,
    onBack: () -> Unit = {}
) {
    MoreScreen(
        prefs = prefs,
        onNavigateToPlanSelection = onNavigateToPlanSelection,
        onNavigateToPaymentHistory = onNavigateToPaymentHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToCommunity = onNavigateToCommunity,
        onNavigateToAdminPanel = onNavigateToAdminPanel,
        onLogout = onLogout,
        onBack = onBack
    )
}
