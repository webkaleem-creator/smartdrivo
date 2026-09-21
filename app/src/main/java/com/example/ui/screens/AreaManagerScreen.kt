package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FirebaseRepository
import com.example.data.PreferencesManager
import com.example.model.AreaGroup
import com.example.model.AreaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

private val GoToGreen = Color(0xFF1E88E5)
private val GoToGreenBg = Color(0xFFE3F2FD)
private val GoToGreenBorder = Color(0xFF90CAF9)
private val NoGoRed = Color(0xFFC62828)
private val NoGoRedBg = Color(0xFFFFEBEE)
private val NoGoRedBorder = Color(0xFFEF9A9A)

private fun cleanAreaGroups(groups: List<AreaGroup>): List<AreaGroup> =
    groups.map { group ->
        group.copy(
            keywords = group.keywords
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(group.name, ignoreCase = true) }
                .distinctBy { it.lowercase() },
            filtersEnabled = false
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaManagerScreen(
    prefs: PreferencesManager,
    repository: FirebaseRepository,
    onBack: () -> Unit
) {
    val storedGoTo by prefs.goToAreas.collectAsState()
    val storedNoGo by prefs.noGoAreas.collectAsState()

    var goToGroups by remember { mutableStateOf(cleanAreaGroups(prefs.goToAreas.value)) }
    var noGoGroups by remember { mutableStateOf(cleanAreaGroups(prefs.noGoAreas.value)) }
    var selectedType by remember { mutableStateOf(AreaType.GO_TO) }
    var editorGroup by remember { mutableStateOf<AreaGroup?>(null) }
    var showNewEditor by remember { mutableStateOf(false) }
    var deleteGroup by remember { mutableStateOf<AreaGroup?>(null) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(storedGoTo) {
        val clean = cleanAreaGroups(storedGoTo)
        if (clean != goToGroups) goToGroups = clean
    }

    LaunchedEffect(storedNoGo) {
        val clean = cleanAreaGroups(storedNoGo)
        if (clean != noGoGroups) noGoGroups = clean
    }

    fun persist(go: List<AreaGroup>, no: List<AreaGroup>) {
        val cleanGo = cleanAreaGroups(go)
        val cleanNo = cleanAreaGroups(no)

        goToGroups = cleanGo
        noGoGroups = cleanNo

        prefs.isGoToEnabled = cleanGo.any { it.isEnabled && it.keywords.isNotEmpty() }
        prefs.isNoGoEnabled = cleanNo.any { it.isEnabled && it.keywords.isNotEmpty() }

        scope.launch(Dispatchers.IO) {
            prefs.saveAreaGroups(cleanGo, cleanNo)
            repository.syncAreas(cleanGo + cleanNo)
        }
    }

    fun saveGroup(saved: AreaGroup) {
        if (saved.name.isBlank()) return

        if (saved.type == AreaType.GO_TO) {
            val duplicate = goToGroups.any {
                it.id != saved.id && it.name.equals(saved.name, ignoreCase = true)
            }
            if (duplicate) {
                scope.launch {
                    snackbar.showSnackbar("Go-To group '${saved.name}' already exists")
                }
                return
            }

            val exists = goToGroups.any { it.id == saved.id }
            val updated = if (exists) {
                goToGroups.map { if (it.id == saved.id) saved else it }
            } else {
                goToGroups + saved
            }

            persist(updated, noGoGroups)

            val saveAction = if (exists) "updated" else "created"
            val status = if (saved.isEnabled && saved.keywords.isNotEmpty()) {
                "ACTIVE"
            } else {
                "OFF"
            }

            scope.launch {
                val message =
                    "Go-To '${saved.name}' $saveAction ✓ • $status"
                snackbar.showSnackbar(message)
            }
        } else {
            val duplicate = noGoGroups.any {
                it.id != saved.id && it.name.equals(saved.name, ignoreCase = true)
            }
            if (duplicate) {
                scope.launch {
                    snackbar.showSnackbar("No-Go group '${saved.name}' already exists")
                }
                return
            }

            val exists = noGoGroups.any { it.id == saved.id }
            val updated = if (exists) {
                noGoGroups.map { if (it.id == saved.id) saved else it }
            } else {
                noGoGroups + saved
            }

            persist(goToGroups, updated)

            val saveAction = if (exists) "updated" else "created"
            val status = if (saved.isEnabled && saved.keywords.isNotEmpty()) {
                "ACTIVE"
            } else {
                "OFF"
            }

            scope.launch {
                val message =
                    "No-Go '${saved.name}' $saveAction ✓ • $status"
                snackbar.showSnackbar(message)
            }
        }
    }
    val visibleGroups = if (selectedType == AreaType.GO_TO) goToGroups else noGoGroups
    val accent = if (selectedType == AreaType.GO_TO) GoToGreen else NoGoRed

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Area Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Preferred destinations and blocked areas",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editorGroup = null
                    showNewEditor = true
                },
                containerColor = accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add group")
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .padding(3.dp)
                ) {
                    CompactAreaTab(
                        text = "GO TO (${goToGroups.size})",
                        selected = selectedType == AreaType.GO_TO,
                        color = GoToGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedType = AreaType.GO_TO
                    }

                    CompactAreaTab(
                        text = "NO GO (${noGoGroups.size})",
                        selected = selectedType == AreaType.NO_GO,
                        color = NoGoRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedType = AreaType.NO_GO
                    }
                }
            }

            if (visibleGroups.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == AreaType.GO_TO) {
                                GoToGreenBg
                            } else {
                                NoGoRedBg
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedType == AreaType.GO_TO) {
                                GoToGreenBorder
                            } else {
                                NoGoRedBorder
                            }
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (selectedType == AreaType.GO_TO) {
                                "No Go-To groups. Tap + to create one."
                            } else {
                                "No No-Go groups. Tap + to create one."
                            },
                            modifier = Modifier.padding(14.dp),
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = visibleGroups,
                    key = { _, group -> group.id }
                ) { _, group ->
                    CompactAreaGroupCard(
                        group = group,
                        onEdit = {
                            editorGroup = group
                            showNewEditor = false
                        },
                        onToggle = {
                            if (group.type == AreaType.GO_TO) {
                                persist(
                                    goToGroups.map {
                                        if (it.id == group.id) {
                                            it.copy(isEnabled = !it.isEnabled)
                                        } else {
                                            it
                                        }
                                    },
                                    noGoGroups
                                )
                            } else {
                                persist(
                                    goToGroups,
                                    noGoGroups.map {
                                        if (it.id == group.id) {
                                            it.copy(isEnabled = !it.isEnabled)
                                        } else {
                                            it
                                        }
                                    }
                                )
                            }
                        },
                        onDelete = {
                            deleteGroup = group
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(86.dp))
            }
        }
    }

    if (showNewEditor || editorGroup != null) {
        GroupEditorDialog(
            group = editorGroup,
            type = editorGroup?.type ?: selectedType,
            onDismiss = {
                editorGroup = null
                showNewEditor = false
            },
            onSave = { saved ->
                saveGroup(saved)
                editorGroup = null
                showNewEditor = false
            }
        )
    }

    deleteGroup?.let { target ->
        Dialog(
            onDismissRequest = {
                deleteGroup = null
            }
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Delete Group?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete '${target.name}'? All area names inside this group will also be deleted.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                deleteGroup = null
                            }
                        ) {
                            Text(
                                text = "No",
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (target.type == AreaType.GO_TO) {
                                    persist(
                                        goToGroups.filterNot { it.id == target.id },
                                        noGoGroups
                                    )
                                } else {
                                    persist(
                                        goToGroups,
                                        noGoGroups.filterNot { it.id == target.id }
                                    )
                                }
                                deleteGroup = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoGoRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Yes, Delete",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAreaTab(
    text: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) color.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                fontSize = 13.sp,
                color = if (selected) color else Color(0xFF64748B),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CompactAreaGroupCard(
    group: AreaGroup,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isGoTo = group.type == AreaType.GO_TO
    val accent = if (isGoTo) GoToGreen else NoGoRed
    val bg = if (isGoTo) GoToGreenBg else NoGoRedBg
    val border = if (isGoTo) GoToGreenBorder else NoGoRedBorder

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(bg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isGoTo) Icons.Default.Place else Icons.Default.Block,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "${group.keywords.size} areas",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Text(
                            "•",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Surface(
                            color = if (group.isEnabled) {
                                accent.copy(alpha = 0.12f)
                            } else {
                                Color(0xFFF1F5F9)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (group.isEnabled) "ACTIVE" else "OFF",
                                modifier = Modifier.padding(
                                    horizontal = 6.dp,
                                    vertical = 2.dp
                                ),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (group.isEnabled) {
                                    accent
                                } else {
                                    Color(0xFF64748B)
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit group",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete group",
                        tint = NoGoRed,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Switch(
                    checked = group.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = accent,
                        checkedThumbColor = Color.White
                    )
                )
            }

            if (group.keywords.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    group.keywords.take(5).joinToString("   •   "),
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isGoTo) {
                val goToMinFare =
                    if (group.maxFare > 0f) group.maxFare else group.minFare

                Spacer(Modifier.height(7.dp))
                Surface(
                    color = GoToGreenBg.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Minimum Fare: ${
                            if (goToMinFare > 0f) "₹${goToMinFare.toInt()}" else "No minimum"
                        }   •   Pickup: ${
                            if (group.maxPickupKm > 0f) "${group.maxPickupKm} km" else "No limit"
                        }   •   Drop: ${
                            if (group.maxDropKm > 0f) "${group.maxDropKm} km" else "No limit"
                        }",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        maxLines = 2,
                        fontWeight = FontWeight.Medium,
                        color = GoToGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupEditorDialog(
    group: AreaGroup?,
    type: AreaType,
    onDismiss: () -> Unit,
    onSave: (AreaGroup) -> Unit
) {
    val isGoTo = type == AreaType.GO_TO
    val accent = if (isGoTo) GoToGreen else NoGoRed
    val bg = if (isGoTo) GoToGreenBg else NoGoRedBg

    var name by remember(group?.id, type) {
        mutableStateOf(group?.name.orEmpty())
    }
    var minFareText by remember(group?.id, type) {
        mutableStateOf(
            group
                ?.let { saved ->
                    if (saved.maxFare > 0f) saved.maxFare else saved.minFare
                }
                ?.takeIf { it > 0f }
                ?.let {
                    if (it % 1f == 0f) it.toInt().toString()
                    else it.toString()
                }
                .orEmpty()
        )
    }
    var maxPickupText by remember(group?.id, type) {
        mutableStateOf(
            group?.maxPickupKm
                ?.takeIf { it > 0f }
                ?.let { it.toString() }
                .orEmpty()
        )
    }
    var maxDropText by remember(group?.id, type) {
        mutableStateOf(
            group?.maxDropKm
                ?.takeIf { it > 0f }
                ?.let { it.toString() }
                .orEmpty()
        )
    }
    var keywords by remember(group?.id, type) {
        mutableStateOf(group?.keywords ?: emptyList())
    }
    var areaInput by remember(group?.id, type) {
        mutableStateOf("")
    }
    var editingIndex by remember(group?.id, type) {
        mutableStateOf<Int?>(null)
    }

    fun submitArea() {
        val value = areaInput.trim()
        if (value.isBlank()) return
        if (value.equals(name.trim(), ignoreCase = true)) return

        val updated = keywords.toMutableList()
        val editAt = editingIndex

        if (editAt != null && editAt in updated.indices) {
            if (updated.withIndex().any {
                    it.index != editAt &&
                        it.value.equals(value, ignoreCase = true)
                }) return
            updated[editAt] = value
        } else {
            if (updated.any { it.equals(value, ignoreCase = true) }) return
            updated.add(value)
        }

        keywords = updated
        areaInput = ""
        editingIndex = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    if (group == null) {
                        if (isGoTo) "New GO TO Group" else "New NO GO Group"
                    } else {
                        if (isGoTo) "Edit GO TO Group" else "Edit NO GO Group"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Home, Old City, Airport") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        cursorColor = accent
                    )
                )

                if (isGoTo) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "GO TO Limits",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = accent
                    )
                    Text(
                        "These group limits apply after destination area matches.",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = minFareText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                    minFareText = input
                                }
                            },
                            label = { Text("Minimum Fare", fontSize = 10.sp) },
                            placeholder = { Text("No minimum", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                cursorColor = accent
                            )
                        )

                        OutlinedTextField(
                            value = maxPickupText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                    maxPickupText = input
                                }
                            },
                            label = { Text("Maximum Pickup", fontSize = 10.sp) },
                            placeholder = { Text("km", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                cursorColor = accent
                            )
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    OutlinedTextField(
                        value = maxDropText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                maxDropText = input
                            }
                        },
                        label = { Text("Maximum Drop Distance (km)", fontSize = 10.sp) },
                        placeholder = { Text("No limit", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            cursorColor = accent
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    if (isGoTo) "Destination Areas (${keywords.size})"
                    else "Blocked Areas (${keywords.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = areaInput,
                        onValueChange = { areaInput = it },
                        placeholder = {
                            Text(
                                if (editingIndex == null) "Add area name"
                                else "Edit area name"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitArea() }
                        ),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            cursorColor = accent
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { submitArea() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (editingIndex == null) "Add" else "Save")
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (keywords.isEmpty()) {
                    Surface(
                        color = bg.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(9.dp)
                    ) {
                        Text(
                            "No areas added yet",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        itemsIndexed(
                            items = keywords,
                            key = { index, word -> "$index-$word" }
                        ) { index, word ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        bg.copy(alpha = 0.65f),
                                        RoundedCornerShape(9.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    word,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp
                                )

                                IconButton(
                                    onClick = {
                                        keywords = keywords.toMutableList().also {
                                            if (index in it.indices) it.removeAt(index)
                                        }
                                        if (editingIndex == index) {
                                            editingIndex = null
                                            areaInput = ""
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete area",
                                        tint = NoGoRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        enabled = name.trim().isNotBlank(),
                        onClick = {
                            onSave(
                                AreaGroup(
                                    id = group?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    isEnabled = group?.isEnabled ?: true,
                                    type = type,
                                    keywords = keywords
                                        .map { it.trim() }
                                        .filter {
                                            it.isNotBlank() &&
                                                !it.equals(name.trim(), ignoreCase = true)
                                        }
                                        .distinctBy { it.lowercase() },
                                    filtersEnabled = false,
                                    minFare = if (isGoTo) {
                                        minFareText.toFloatOrNull() ?: 0f
                                    } else {
                                        group?.minFare ?: 0f
                                    },
                                    // maxFare is legacy for GO TO. Clear it after
                                    // saving so Minimum Fare becomes authoritative.
                                    maxFare = if (isGoTo) {
                                        0f
                                    } else {
                                        group?.maxFare ?: 0f
                                    },
                                    minPickupKm = group?.minPickupKm ?: 0f,
                                    maxPickupKm = if (isGoTo) {
                                        maxPickupText.toFloatOrNull() ?: 0f
                                    } else {
                                        group?.maxPickupKm ?: 0f
                                    },
                                    maxDropKm = if (isGoTo) {
                                        maxDropText.toFloatOrNull() ?: 0f
                                    } else {
                                        group?.maxDropKm ?: 0f
                                    }
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Group")
                    }
                }
            }
        }
    }
}
