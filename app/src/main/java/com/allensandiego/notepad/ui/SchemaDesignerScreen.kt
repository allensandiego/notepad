package com.allensandiego.notepad.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.allensandiego.notepad.db.DatabaseDao
import com.allensandiego.notepad.db.FieldEntity
import com.allensandiego.notepad.db.SchemaHelper
import com.allensandiego.notepad.db.TableEntity
import com.allensandiego.notepad.sync.SyncEngine
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.runtime.LaunchedEffect
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.BreadcrumbType

import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.MaterialTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCollectionDialog(
    databaseDao: DatabaseDao,
    syncEngine: SyncEngine,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        Bugsnag.leaveBreadcrumb("Opened Create Collection Screen", emptyMap(), BreadcrumbType.NAVIGATION)
    }
    val scope = rememberCoroutineScope()
    val allTables by databaseDao.getAllTablesFlow().collectAsState(initial = emptyList())

    var collectionName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // Parent collection picker state
    var parentExpanded by remember { mutableStateOf(false) }
    var selectedParent by remember { mutableStateOf<TableEntity?>(null) }

    val PrimaryEmerald = MaterialTheme.colorScheme.primary
    val SurfaceDark = MaterialTheme.colorScheme.surface
    val TextLight = MaterialTheme.colorScheme.onSurface
    val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("New Collection", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = collectionName,
                    onValueChange = {
                        collectionName = it
                        isError = false
                    },
                    label = { Text("Collection Name", color = TextMuted) },
                    placeholder = { Text("e.g. Tasks, Notes, Clients", color = TextMuted.copy(alpha = 0.5f)) },
                    singleLine = true,
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text("Name cannot be empty", color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Parent collection dropdown (optional)
                ExposedDropdownMenuBox(
                    expanded = parentExpanded,
                    onExpandedChange = { parentExpanded = !parentExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedParent?.name ?: "— None (top-level) —",
                        onValueChange = {},
                        label = { Text("Parent Collection (optional)", color = TextMuted) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = parentExpanded,
                        onDismissRequest = { parentExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        // None option
                        DropdownMenuItem(
                            text = { Text("— None (top-level) —", color = TextMuted) },
                            onClick = {
                                selectedParent = null
                                parentExpanded = false
                            }
                        )
                        allTables.forEach { table ->
                            DropdownMenuItem(
                                text = { Text(table.name, color = TextLight) },
                                onClick = {
                                    selectedParent = table
                                    parentExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (collectionName.trim().isEmpty()) {
                                isError = true
                            } else {
                                scope.launch {
                                    val newTable = TableEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = collectionName.trim(),
                                        parentTableId = selectedParent?.id
                                    )
                                    databaseDao.insertTable(newTable)
                                    val json = Json { encodeDefaults = true }
                                    val payload = json.encodeToString(newTable)
                                    syncEngine.queueItem("INSERT", "SCHEMA_TABLE", payload)

                                    // Auto-insert system fields (PK + FK if child)
                                    SchemaHelper.insertSystemFields(
                                        dao = databaseDao,
                                        syncEngine = syncEngine,
                                        table = newTable,
                                        allTables = databaseDao.getAllTables()
                                    )
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                    ) {
                        Text("Create", color = TextLight)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldDesignerDialog(
    table: TableEntity,
    databaseDao: DatabaseDao,
    syncEngine: SyncEngine,
    onDismiss: () -> Unit
) {
    LaunchedEffect(table) {
        Bugsnag.leaveBreadcrumb("Opened Schema Designer: ${table.name}", emptyMap(), BreadcrumbType.NAVIGATION)
    }
    val allFields by databaseDao.getFieldsForTableFlow(table.id).collectAsState(initial = emptyList())
    // System fields at top, user fields below
    val systemFields = allFields.filter { it.isSystem }
    val userFields = allFields.filter { !it.isSystem }

    // Children of this table
    val allTables by databaseDao.getAllTablesFlow().collectAsState(initial = emptyList())
    val childTables = allTables.filter { it.parentTableId == table.id }.sortedBy { it.name }

    var showAddFieldDialog by remember { mutableStateOf(false) }
    // Which child table's schema is being viewed (recursive open)
    var selectedChildForSchema by remember { mutableStateOf<TableEntity?>(null) }
    var showAddChildDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val PrimaryEmerald = MaterialTheme.colorScheme.primary
    val SecondaryNavy = MaterialTheme.colorScheme.background
    val SurfaceDark = MaterialTheme.colorScheme.surface
    val TextLight = MaterialTheme.colorScheme.onSurface
    val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${table.name} Fields",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showAddFieldDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Field", tint = PrimaryEmerald)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (allFields.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No Fields Defined", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Add fields (e.g. Doctor, Date, Prescription File) to start storing data in this collection.",
                                fontSize = 14.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // System fields (PK / FK) — locked, shown first
                            items(systemFields) { field ->
                                FieldRow(
                                    field = field,
                                    isSystem = true,
                                    containerColor = SecondaryNavy,
                                    textLight = TextLight,
                                    textMuted = TextMuted,
                                    onDelete = null // system fields cannot be deleted
                                )
                            }
                            // User-defined fields
                            items(userFields) { field ->
                                FieldRow(
                                    field = field,
                                    isSystem = false,
                                    containerColor = SecondaryNavy,
                                    textLight = TextLight,
                                    textMuted = TextMuted,
                                    onDelete = {
                                        scope.launch {
                                            databaseDao.deleteField(field.id)
                                            val json = Json { encodeDefaults = true }
                                            syncEngine.queueItem("DELETE", "SCHEMA_FIELD", json.encodeToString(field))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // ─── Child Collections Section ───────────────────────────────
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Child Collections",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                    IconButton(onClick = { showAddChildDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Child Collection",
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (childTables.isEmpty()) {
                    Text(
                        text = "No child collections yet. Tap + to add one.",
                        fontSize = 12.sp,
                        color = TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        childTables.forEach { child ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = child.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextLight
                                        )
                                        Text(
                                            text = "child of ${table.name}",
                                            fontSize = 10.sp,
                                            color = TextMuted.copy(alpha = 0.6f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { selectedChildForSchema = child },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Configure ${child.name} Schema",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Recursive: open child's schema designer
    if (selectedChildForSchema != null) {
        FieldDesignerDialog(
            table = selectedChildForSchema!!,
            databaseDao = databaseDao,
            syncEngine = syncEngine,
            onDismiss = { selectedChildForSchema = null }
        )
    }

    // Add Child Collection dialog
    if (showAddChildDialog) {
        var childName by remember { mutableStateOf("") }
        var childNameError by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { showAddChildDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "New Child Collection",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    Text(
                        "Parent: ${table.name}",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it; childNameError = false },
                        label = { Text("Collection Name", color = TextMuted) },
                        singleLine = true,
                        isError = childNameError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (childNameError) {
                        Text("Name cannot be empty", color = Color.Red, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddChildDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) { Text("Cancel", color = TextMuted) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (childName.trim().isEmpty()) {
                                    childNameError = true
                                } else {
                                    scope.launch {
                                        val newChild = TableEntity(
                                            id = UUID.randomUUID().toString(),
                                            name = childName.trim(),
                                            parentTableId = table.id
                                        )
                                        databaseDao.insertTable(newChild)
                                        val json = Json { encodeDefaults = true }
                                        syncEngine.queueItem("INSERT", "SCHEMA_TABLE", json.encodeToString(newChild))
                                        SchemaHelper.insertSystemFields(
                                            dao = databaseDao,
                                            syncEngine = syncEngine,
                                            table = newChild,
                                            allTables = databaseDao.getAllTables()
                                        )
                                    }
                                    showAddChildDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) { Text("Create", color = TextLight) }
                    }
                }
            }
        }
    }

    if (showAddFieldDialog) {
        var fieldName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("TEXT") }
        var isRequired by remember { mutableStateOf(false) }
        var defaultType by remember { mutableStateOf("None") }
        var staticDefaultValue by remember { mutableStateOf("") }
        
        var isError by remember { mutableStateOf(false) }
        
        val typeOptions = listOf("TEXT", "NUMBER", "BOOLEAN", "DATE", "FILE")
        var typeExpanded by remember { mutableStateOf(false) }

        val defaultOptions = when (selectedType) {
            "TEXT" -> listOf("None", "STATIC", "UUID", "AUTO_INCREMENT")
            "NUMBER" -> listOf("None", "STATIC", "AUTO_INCREMENT")
            "DATE" -> listOf("None", "STATIC", "TODAY")
            "BOOLEAN" -> listOf("None", "STATIC")
            "FILE" -> listOf("None")
            else -> listOf("None")
        }
        var defaultExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddFieldDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text("Add Field", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = fieldName,
                            onValueChange = {
                                fieldName = it
                                isError = false
                            },
                            label = { Text("Field Name", color = TextMuted) },
                            placeholder = { Text("e.g. Doctor Name", color = TextMuted.copy(alpha = 0.5f)) },
                            singleLine = true,
                            isError = isError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryEmerald,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isError) {
                            Text("Name cannot be empty", color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded }
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = selectedType,
                                onValueChange = {},
                                label = { Text("Data Type", color = TextMuted) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryEmerald,
                                    unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                typeOptions.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption, color = TextLight) },
                                        onClick = {
                                            selectedType = selectionOption
                                            defaultType = "None"
                                            staticDefaultValue = ""
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Required field", color = TextLight, fontSize = 14.sp)
                            Switch(
                                checked = isRequired,
                                onCheckedChange = { isRequired = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextLight,
                                    checkedTrackColor = PrimaryEmerald,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceDark
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (defaultOptions.size > 1) {
                            ExposedDropdownMenuBox(
                                expanded = defaultExpanded,
                                onExpandedChange = { defaultExpanded = !defaultExpanded }
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = defaultType,
                                    onValueChange = {},
                                    label = { Text("Default Value Behavior", color = TextMuted) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = defaultExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryEmerald,
                                        unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                        focusedTextColor = TextLight,
                                        unfocusedTextColor = TextLight
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = defaultExpanded,
                                    onDismissRequest = { defaultExpanded = false },
                                    modifier = Modifier.background(SurfaceDark)
                                ) {
                                    defaultOptions.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption, color = TextLight) },
                                            onClick = {
                                                defaultType = selectionOption
                                                if (selectionOption != "STATIC") {
                                                    staticDefaultValue = ""
                                                }
                                                defaultExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        AnimatedVisibility(visible = defaultType == "STATIC") {
                            OutlinedTextField(
                                value = staticDefaultValue,
                                onValueChange = { staticDefaultValue = it },
                                label = { Text("Default Value", color = TextMuted) },
                                placeholder = {
                                    val hint = when (selectedType) {
                                        "NUMBER" -> "e.g. 98.6"
                                        "BOOLEAN" -> "e.g. true"
                                        "DATE" -> "yyyy-MM-dd"
                                        else -> "e.g. Dr. Smith"
                                    }
                                    Text(hint, color = TextMuted.copy(alpha = 0.5f))
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryEmerald,
                                    unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showAddFieldDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Text("Cancel", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (fieldName.trim().isEmpty()) {
                                        isError = true
                                    } else {
                                        scope.launch {
                                            val defVal = if (defaultType == "STATIC") staticDefaultValue.trim() else null
                                            val defType = if (defaultType != "None") defaultType else null
                                            
                                            val newField = FieldEntity(
                                                id = UUID.randomUUID().toString(),
                                                tableId = table.id,
                                                name = fieldName.trim(),
                                                type = selectedType,
                                                required = isRequired,
                                                defaultValue = defVal,
                                                defaultType = defType,
                                                isSystem = false
                                            )
                                            databaseDao.insertField(newField)
                                            
                                            val json = Json { encodeDefaults = true }
                                            syncEngine.queueItem("INSERT", "SCHEMA_FIELD", json.encodeToString(newField))
                                        }
                                        showAddFieldDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                            ) {
                                Text("Add", color = TextLight)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Renders a single field row, with lock icon and PK/FK badges for system fields. */
@Composable
private fun FieldRow(
    field: FieldEntity,
    isSystem: Boolean,
    containerColor: Color,
    textLight: Color,
    textMuted: Color,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSystem) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "System field",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(field.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textLight)
                    if (field.required && !isSystem) {
                        Text(" *", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (badgeLabel, badgeColor) = when {
                        isSystem && field.name == "id" -> "PK" to Color(0xFF34D399)
                        isSystem -> "FK" to Color(0xFFFB923C)
                        else -> field.type to when (field.type) {
                            "TEXT" -> Color(0xFF60A5FA)
                            "NUMBER" -> Color(0xFF34D399)
                            "BOOLEAN" -> Color(0xFFFB923C)
                            "DATE" -> Color(0xFFC084FC)
                            "FILE" -> Color(0xFF2DD4BF)
                            else -> textMuted
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeLabel, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!isSystem && field.defaultType != null && field.defaultType != "None") {
                        Spacer(modifier = Modifier.width(8.dp))
                        val defText = if (field.defaultType == "STATIC") {
                            "Default: ${field.defaultValue}"
                        } else {
                            "Default: ${field.defaultType}"
                        }
                        Text(defText, color = textMuted, fontSize = 11.sp)
                    }
                }
            }

            // Only show delete button for non-system fields
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Field", tint = Color(0xFFF87171))
                }
            } else {
                // Spacer to keep row height consistent
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}
