package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.auth.PickupGuardOwnerAccess
import com.example.data.PreferencesManager
import com.example.data.db.entity.RideHistoryEntity
import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.ui.viewmodel.RideHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val olaHistoryTabs = listOf(
    OrderStatus.ACCEPTED, OrderStatus.REJECTED,
    OrderStatus.IGNORED, OrderStatus.MISSED
)

/** Do not claim a verified result based merely on an Android click acknowledgement. */
internal fun isVerifiedOlaHistory(entity: RideHistoryEntity): Boolean {
    if (entity.platform != Platform.OLA.name) return false
    return when (entity.orderStatus) {
        OrderStatus.ACCEPTED -> entity.actionSucceeded
        OrderStatus.REJECTED -> entity.decisionReasonCode == "OLA_VERIFIED_REJECT"
        OrderStatus.IGNORED -> true
        OrderStatus.MISSED -> entity.decisionReasonCode.startsWith("OLA_CONFIRMED_MISSED")
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OlaAdminHistoryScreen(
    prefs: PreferencesManager,
    onBack: () -> Unit,
    viewModel: RideHistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val profile by prefs.userProfile.collectAsState()
    val all by viewModel.history.collectAsState()
    if (!PickupGuardOwnerAccess.authorized(profile)) return

    var selected by remember { mutableStateOf(OrderStatus.IGNORED) }
    val ola = all.filter { it.platform == Platform.OLA.name }.sortedByDescending { it.detectedAt }
    val verified = ola.filter(::isVerifiedOlaHistory)
    val unverifiedCount = ola.count { !isVerifiedOlaHistory(it) }
    val visible = verified.filter { it.orderStatus == selected }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Ola Testing History") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Owner-only • Real Ola outcomes")
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                olaHistoryTabs.forEach { status ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selected == status,
                        onClick = { selected = status },
                        label = { Text("${status.name.lowercase().replaceFirstChar { it.uppercase() }} " +
                            verified.count { it.orderStatus == status }) }
                    )
                }
            }
            if (unverifiedCount > 0)
                Text("$unverifiedCount pending / unverified / legacy outcomes excluded from verified counts")
            if (visible.isEmpty()) Text("No verified Ola ${selected.name.lowercase()} offers yet")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("₹${item.fare.toInt()}  •  ${item.status}")
                            Text("Pickup ${item.pickupDistanceKm} km   •   Trip ${item.dropDistanceKm} km")
                            Text(item.dropAddress.ifBlank { "Drop address unavailable" })
                            Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date(item.detectedAt)))
                            Text(item.decisionReasonText.ifBlank { item.decisionReasonCode })
                        }
                    }
                }
            }
        }
    }
}
