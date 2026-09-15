package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.data.PreferencesManager
import com.example.model.AreaGroup
import com.example.model.AreaType
import com.example.ui.theme.BluePrimary
import kotlinx.coroutines.launch
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AreaManagerScreen(
    prefs: PreferencesManager,
    repository: FirebaseRepository,
    onBack: () -> Unit
) {
    val savedGoTo by prefs.goToAreas.collectAsState()
    val savedNoGo by prefs.noGoAreas.collectAsState()

    var goToGroups by remember(savedGoTo) { mutableStateOf(savedGoTo) }
    var noGoGroups by remember(savedNoGo) { mutableStateOf(savedNoGo) }

    var goToGroupNameInput by remember { mutableStateOf("") }
    var noGoGroupNameInput by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun addGoToGroup() {
        val trimmed = goToGroupNameInput.trim()
        if (trimmed.isNotBlank()) {
            if (!goToGroups.any { it.name.equals(trimmed, ignoreCase = true) }) {
                val newGroup = AreaGroup(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    type = AreaType.GO_TO,
                    isEnabled = true,
                    keywords = emptyList(),
                    filtersEnabled = false
                )
                goToGroups = goToGroups + newGroup
                goToGroupNameInput = ""
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("'$trimmed' group already exists")
                }
            }
        }
    }

    fun addNoGoGroup() {
        val trimmed = noGoGroupNameInput.trim()
        if (trimmed.isNotBlank()) {
            if (!noGoGroups.any { it.name.equals(trimmed, ignoreCase = true) }) {
                val newGroup = AreaGroup(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    type = AreaType.NO_GO,
                    isEnabled = true,
                    keywords = emptyList(),
                    filtersEnabled = false
                )
                noGoGroups = noGoGroups + newGroup
                noGoGroupNameInput = ""
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("'$trimmed' group already exists")
                }
            }
        }
    }

    val saveButtonText by remember(goToGroups, noGoGroups) {
        derivedStateOf { "Save Area Groups (${goToGroups.size} Go-To • ${noGoGroups.size} No-Go)" }
    }

    fun saveAllGroups() {
        focusManager.clearFocus()
        prefs.saveAreaGroups(goToGroups, noGoGroups)
        coroutineScope.launch {
            val goToStr = "${goToGroups.size} Go-To groups"
            val noGoStr = "${noGoGroups.size} No-Go groups"
            snackbarHostState.showSnackbar("✓ Saved: $goToStr • $noGoStr")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Area Rules Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Area Groups with Per-Group Filters",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Button(
                            onClick = { saveAllGroups() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = saveButtonText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Rules Overview Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFE0F2FE), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Unlimited Area Groups",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Create unlimited area groups with custom names.\n• Each group has individual ON/OFF toggle and optional per-group fare/distance filters.\n• Go-To: Accept only from these areas. No-Go: Auto-reject these areas.",
                                fontSize = 14.sp,
                                color = Color(0xFF475569),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // SECTION 1: GO-TO GROUPS (Green)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, GoToGreenBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(GoToGreenBg, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = GoToGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Go-To Area Groups",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GoToGreenDark
                                )
                                Text(
                                    text = "Accept only if pickup is in these area groups",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoToGreenBg,
                                border = BorderStroke(1.dp, GoToGreenBorder)
                            ) {
                                Text(
                                    text = "${goToGroups.size} Groups",
                                    color = GoToGreenDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Add Group Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = goToGroupNameInput,
                                onValueChange = { goToGroupNameInput = it },
                                placeholder = {
                                    Text(
                                        "Enter group name (e.g. Downtown)",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    addGoToGroup()
                                    focusManager.clearFocus()
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoToGreenPrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    cursorColor = GoToGreenPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    addGoToGroup()
                                    focusManager.clearFocus()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoToGreenPrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Group", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        if (goToGroups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                goToGroups.forEach { group ->
                                    AreaGroupCard(
                                        group = group,
                                        onToggle = {
                                             goToGroups = goToGroups.map { g ->
                                                if (g.id == group.id) g.copy(isEnabled = !g.isEnabled) else g
                                            }
                                        },
                                        onFiltersToggle = {
                                            goToGroups = goToGroups.map { g ->
                                                if (g.id == group.id) g.copy(filtersEnabled = !g.filtersEnabled) else g
                                            }
                                        },
                                        onMinFareChange = { newValue ->
                                            goToGroups = goToGroups.map { g ->
                                                if (g.id == group.id) g.copy(minFare = newValue) else g
                                            }
                                        },
                                        onMaxPickupChange = { newValue ->
                                            goToGroups = goToGroups.map { g ->
                                                if (g.id == group.id) g.copy(maxPickupKm = newValue) else g
                                            }
                                        },
                                        onMaxDropChange = { newValue ->
                                            goToGroups = goToGroups.map { g ->
                                                if (g.id == group.id) g.copy(maxDropKm = newValue) else g
                                            }
                                        },
                                        onDelete = {
                                            goToGroups = goToGroups.filterNot { it.id == group.id }
                                        }
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No Go-To groups yet. Create one to start filtering by area.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: NO-GO GROUPS (Red)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, NoGoRedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(NoGoRedBg, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = NoGoRedPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No-Go Area Groups",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NoGoRedDark
                                )
                                Text(
                                    text = "Auto-reject if pickup matches these area groups",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NoGoRedBg,
                                border = BorderStroke(1.dp, NoGoRedBorder)
                            ) {
                                Text(
                                    text = "${noGoGroups.size} Groups",
                                    color = NoGoRedDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Add Group Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = noGoGroupNameInput,
                                onValueChange = { noGoGroupNameInput = it },
                                placeholder = {
                                    Text(
                                        "Enter group name (e.g. Unsafe)",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    addNoGoGroup()
                                    focusManager.clearFocus()
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NoGoRedPrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    cursorColor = NoGoRedPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    addNoGoGroup()
                                    focusManager.clearFocus()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NoGoRedPrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Group", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        if (noGoGroups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                noGoGroups.forEach { group ->
                                    AreaGroupCard(
                                        group = group,
                                        onToggle = {
                                            noGoGroups = noGoGroups.map { g ->
                                                if (g.id == group.id) g.copy(isEnabled = !g.isEnabled) else g
                                            }
                                        },
                                        onFiltersToggle = {
                                            noGoGroups = noGoGroups.map { g ->
                                                if (g.id == group.id) g.copy(filtersEnabled = !g.filtersEnabled) else g
                                            }
                                        },
                                        onMinFareChange = { newValue ->
                                            noGoGroups = noGoGroups.map { g ->
                                                if (g.id == group.id) g.copy(minFare = newValue) else g
                                            }
                                        },
                                        onMaxPickupChange = { newValue ->
                                            noGoGroups = noGoGroups.map { g ->
                                                if (g.id == group.id) g.copy(maxPickupKm = newValue) else g
                                            }
                                        },
                                        onMaxDropChange = { newValue ->
                                            noGoGroups = noGoGroups.map { g ->
                                                if (g.id == group.id) g.copy(maxDropKm = newValue) else g
                                            }
                                        },
                                        onDelete = {
                                            noGoGroups = noGoGroups.filterNot { it.id == group.id }
                                        }
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No No-Go groups yet. Create one to block specific areas.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private val GoToGreenPrimary = Color(0xFF2E7D32)
private val GoToGreenBg = Color(0xFFE8F5E9)
private val GoToGreenBorder = Color(0xFFA5D6A7)
private val GoToGreenDark = Color(0xFF1B5E20)

private val NoGoRedPrimary = Color(0xFFC62828)
private val NoGoRedBg = Color(0xFFFFEBEE)
private val NoGoRedBorder = Color(0xFFEF9A9A)
private val NoGoRedDark = Color(0xFFB71C1C)

@Composable
fun AreaGroupCard(
    group: AreaGroup,
    onToggle: () -> Unit,
    onFiltersToggle: () -> Unit,
    onMinFareChange: (Float) -> Unit,
    onMaxPickupChange: (Float) -> Unit,
    onMaxDropChange: (Float) -> Unit,
    onDelete: () -> Unit
) {
    val bgColor = if (group.type == AreaType.GO_TO) GoToGreenBg else NoGoRedBg
    val borderColor = if (group.type == AreaType.GO_TO) GoToGreenBorder else NoGoRedBorder
    val primaryColor = if (group.type == AreaType.GO_TO) GoToGreenPrimary else NoGoRedPrimary
    val darkColor = if (group.type == AreaType.GO_TO) GoToGreenDark else NoGoRedDark

    var minFareText by remember { mutableStateOf(group.minFare.toString()) }
    var maxPickupText by remember { mutableStateOf(group.maxPickupKm.toString()) }
    var maxDropText by remember { mutableStateOf(group.maxDropKm.toString()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Group Header with Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = darkColor
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = group.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete group",
                            tint = darkColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Custom Filters (Restored for Go-To groups with Min Fare & Max Pickup; removed from No-Go groups)
            if (group.type == AreaType.GO_TO) {
                HorizontalDivider(color = borderColor.copy(alpha = 0.5f))

                // Filters Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Custom Filters",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkColor
                    )
                    Switch(
                        checked = group.filtersEnabled,
                        onCheckedChange = { onFiltersToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                // Filters Section (shown only if enabled)
                if (group.filtersEnabled) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        // Min Fare
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Min Fare (₹)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = darkColor
                            )
                            OutlinedTextField(
                                value = minFareText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                        minFareText = input
                                        input.toFloatOrNull()?.let { onMinFareChange(it) }
                                    }
                                },
                                placeholder = { Text("50", fontSize = 14.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = borderColor
                                )
                            )
                        }

                        // Max Pickup KM
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Max Pickup (km)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = darkColor
                            )
                            OutlinedTextField(
                                value = maxPickupText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                        maxPickupText = input
                                        input.toFloatOrNull()?.let { onMaxPickupChange(it) }
                                    }
                                },
                                placeholder = { Text("3.0", fontSize = 14.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = borderColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
