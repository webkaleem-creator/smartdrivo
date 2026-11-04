package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.PickupGuardOwnerAccess
import com.example.data.PreferencesManager
import com.example.engine.AdminPickupBlockPolicy

/** Admin only. Every valid edit persists instantly, no Save button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPickupBlockScreen(prefs: PreferencesManager, onBack: () -> Unit) {
    val owner by prefs.userProfile.collectAsState()
    val settings by prefs.adminPickupBlockSettings.collectAsState()
    val context = LocalContext.current
    val authorized = owner.isAdmin && PickupGuardOwnerAccess.authorized(owner)
    var draft by remember(owner.uid) {
        mutableStateOf(settings.blockedWords.joinToString(", "))
    }
    var error by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Pickup Block • Admin", fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
    }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF113B60), Color(0xFF6940AD), Color(0xFF087F7A))
                        )
                    ).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("SMARTDRIVO • ADMIN CONTROL", color = Color(0xFFC8F9F2),
                        fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Pickup Shield", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                    Text("Block orders from selected pickup locations only.",
                        color = Color(0xFFF1F5FF), fontSize = 13.sp)
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("AUTO REJECT PICKUP", color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (settings.enabled) "ON • Pickup locations checked"
                                     else "OFF • Existing order engine unchanged",
                                    color = Color(0xFFE3FFF1), fontSize = 11.sp)
                            }
                            Switch(
                                enabled = authorized,
                                checked = settings.enabled,
                                onCheckedChange = { enabled ->
                                    val words = AdminPickupBlockPolicy.parse(draft)
                                    if (enabled && words.isEmpty()) {
                                        Toast.makeText(context,
                                            "Add at least one pickup word first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val saved = prefs.saveAdminPickupBlockSettings(
                                            settings.copy(enabled = enabled, blockedWords = words)
                                        )
                                        error = if (saved) "" else "Admin authorization expired"
                                        Toast.makeText(context,
                                            if (!saved) "Admin access unavailable"
                                            else if (enabled) "Pickup Block ON ✓"
                                            else "Pickup Block OFF",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF16C6A4),
                                    checkedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFF475569),
                                    uncheckedThumbColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FC)),
                border = BorderStroke(1.dp, Color(0xFFE2E6F2))) {
                Column(modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Blocked pickup words", fontSize = 20.sp,
                        color = Color(0xFF17253B), fontWeight = FontWeight.Bold)
                    Text("Comma-separated names or phrases. Match ONLY the pickup address, not the destination.",
                        color = Color(0xFF58677B), fontSize = 12.sp)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft,
                        onValueChange = { input ->
                            if (input.length > 5000) {
                                error = "Text is too long"
                            } else {
                                draft = input
                                val count = input.split(',', '\n', '\r')
                                    .count { it.trim().isNotEmpty() }
                                if (count > AdminPickupBlockPolicy.MAX_WORDS) {
                                    error = "Maximum ${AdminPickupBlockPolicy.MAX_WORDS} pickup entries"
                                } else {
                                    val words = AdminPickupBlockPolicy.parse(input)
                                    val updated = settings.copy(
                                        enabled = settings.enabled && words.isNotEmpty(),
                                        blockedWords = words
                                    )
                                    if (updated != settings) {
                                        val saved = prefs.saveAdminPickupBlockSettings(updated)
                                        error = if (saved) "" else "Admin authorization expired"
                                        if (saved && settings.enabled && !updated.enabled) {
                                            Toast.makeText(context,
                                                "Pickup Block OFF • No words saved", Toast.LENGTH_SHORT).show()
                                        }
                                    } else error = ""
                                }
                            }
                        },
                        placeholder = { Text("Tank Bund, Railway Station, Airport Road") },
                        minLines = 3,
                        maxLines = 6,
                        enabled = authorized,
                        shape = RoundedCornerShape(14.dp),
                        supportingText = {
                            Text("${settings.blockedWords.size} saved • Auto-save ON • No Save button")
                        }
                    )
                    if (error.isNotBlank()) {
                        Text(error, color = Color(0xFFC24134), fontSize = 12.sp)
                    }
                    Text("Example: Tank Bund, Hyderabad Railway Station, Jubilee Hills",
                        color = Color(0xFF6B5DC3), fontSize = 12.sp)
                }
            }
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5E5))) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How Pickup Shield works", fontWeight = FontWeight.Bold,
                        color = Color(0xFF69431B))
                    Text("• Exact word/phrase boundaries, case-insensitive. Pickup ONLY.",
                        fontSize = 12.sp, color = Color(0xFF69431B))
                    Text("• Blocked offer: never auto-accept. Safe Reject/X only when verified.",
                        fontSize = 12.sp, color = Color(0xFF69431B))
                    Text("• Unreadable pickup while ON: no automatic action (safe fallback).",
                        fontSize = 12.sp, color = Color(0xFF69431B))
                    Text("• OFF: no extra pickup scan; Normal and Fastest modes stay unchanged.",
                        fontSize = 12.sp, color = Color(0xFF69431B))
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
