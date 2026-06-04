package com.allensandiego.notepad.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allensandiego.notepad.db.DatabaseDao
import com.allensandiego.notepad.db.FieldEntity
import com.allensandiego.notepad.db.RecordEntity
import com.allensandiego.notepad.db.TableEntity
import com.allensandiego.notepad.db.ValueEntity
import com.allensandiego.notepad.sync.FileSyncPayload
import com.allensandiego.notepad.sync.SyncEngine
import kotlinx.coroutines.launch
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.BreadcrumbType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.MaterialTheme

data class EditScreenState(
    val table: TableEntity,
    val recordId: String?,
    val prePopulatedValues: Map<String, String> = emptyMap()
)

@Composable
fun ChildCollectionSection(
    childTable: TableEntity,
    parentTable: TableEntity,
    parentRecordId: String,
    databaseDao: DatabaseDao,
    onNavigateToChild: (TableEntity, String?, Map<String, String>) -> Unit,
    primaryColor: Color,
    surfaceColor: Color,
    textLight: Color,
    textMuted: Color
) {
    val childFields by databaseDao.getFieldsForTableFlow(childTable.id).collectAsState(initial = emptyList())
    val fkField = remember(childFields) {
        childFields.find { it.isSystem && it.name.equals("${parentTable.name}_id", ignoreCase = true) }
    }

    val childRecords by remember(fkField, parentRecordId) {
        if (fkField != null) {
            databaseDao.getChildRecordsFlow(childTable.id, fkField.id, parentRecordId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val labelMap = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(childRecords) {
        childRecords.forEach { record ->
            if (!labelMap.containsKey(record.id)) {
                scope.launch {
                    val label = databaseDao.getRecordDisplayValue(record.id) ?: record.id
                    labelMap[record.id] = label
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = childTable.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = textLight
            )
            Button(
                onClick = {
                    if (fkField != null) {
                        onNavigateToChild(
                            childTable,
                            null,
                            mapOf(fkField.id to parentRecordId)
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add ${childTable.name}",
                    color = primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (childRecords.isEmpty()) {
            Text(
                text = "No ${childTable.name} records linked.",
                color = textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                childRecords.forEach { record ->
                    val label = labelMap[record.id] ?: record.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToChild(childTable, record.id, emptyMap()) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, textMuted.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = textLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "View details",
                                tint = textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.material3.HorizontalDivider(color = textMuted.copy(alpha = 0.1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditDialog(
    table: TableEntity,
    recordId: String?,
    databaseDao: DatabaseDao,
    syncEngine: SyncEngine,
    onDismiss: () -> Unit
) {
    var navStack by remember { mutableStateOf(listOf(EditScreenState(table, recordId))) }
    val currentScreen = navStack.last()

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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            key(currentScreen) {
                RecordEditContent(
                    currentScreen = currentScreen,
                    databaseDao = databaseDao,
                    syncEngine = syncEngine,
                    onSaveCompleted = {
                        if (navStack.size > 1) {
                            navStack = navStack.dropLast(1)
                        } else {
                            onDismiss()
                        }
                    },
                    onCancelClicked = {
                        if (navStack.size > 1) {
                            navStack = navStack.dropLast(1)
                        } else {
                            onDismiss()
                        }
                    },
                    onNavigateToChild = { childTable, childRecordId, prePopulated ->
                        navStack = navStack + EditScreenState(childTable, childRecordId, prePopulated)
                    },
                    isStackNotEmpty = navStack.size > 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditContent(
    currentScreen: EditScreenState,
    databaseDao: DatabaseDao,
    syncEngine: SyncEngine,
    onSaveCompleted: () -> Unit,
    onCancelClicked: () -> Unit,
    onNavigateToChild: (TableEntity, String?, Map<String, String>) -> Unit,
    isStackNotEmpty: Boolean
) {
    LaunchedEffect(currentScreen) {
        Bugsnag.leaveBreadcrumb(
            "Opened Record Edit Screen",
            mapOf("collection" to currentScreen.table.name, "recordId" to (currentScreen.recordId ?: "new")),
            BreadcrumbType.NAVIGATION
        )
    }
    val context = LocalContext.current
    val table = currentScreen.table
    val recordId = currentScreen.recordId
    val prePopulatedValues = currentScreen.prePopulatedValues

    val fields by databaseDao.getFieldsForTableFlow(table.id).collectAsState(initial = emptyList())
    val childTables by databaseDao.getChildTablesForParent(table.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val formValues = remember { mutableStateMapOf<String, String>() }
    val filePaths = remember { mutableStateMapOf<String, String>() }

    var isLoaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val PrimaryEmerald = MaterialTheme.colorScheme.primary
    val SecondaryNavy = MaterialTheme.colorScheme.background
    val SurfaceDark = MaterialTheme.colorScheme.surface
    val TextLight = MaterialTheme.colorScheme.onSurface
    val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val AccentMint = MaterialTheme.colorScheme.secondary
    val PrimaryDeepTeal = MaterialTheme.colorScheme.primaryContainer

    LaunchedEffect(fields, recordId) {
        if (fields.isNotEmpty() && !isLoaded) {
            if (recordId != null) {
                val existingValues = databaseDao.getValuesForRecord(recordId)
                existingValues.forEach { valueEntity ->
                    formValues[valueEntity.fieldId] = valueEntity.valueText ?: ""
                }
            } else {
                fields.forEach { field ->
                    when {
                        prePopulatedValues.containsKey(field.id) -> {
                            formValues[field.id] = prePopulatedValues[field.id]!!
                        }
                        field.isSystem && field.name == "id" -> {
                            formValues[field.id] = UUID.randomUUID().toString()
                        }
                        field.isSystem -> {
                            formValues[field.id] = ""
                        }
                        else -> when (field.defaultType) {
                            "STATIC" -> {
                                formValues[field.id] = field.defaultValue ?: ""
                            }
                            "TODAY" -> {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                formValues[field.id] = dateFormat.format(Date())
                            }
                            "UUID" -> {
                                formValues[field.id] = UUID.randomUUID().toString()
                            }
                            "AUTO_INCREMENT" -> {
                                scope.launch {
                                    val records = databaseDao.getRecordsForTable(table.id)
                                    formValues[field.id] = (records.size + 1).toString()
                                }
                            }
                            else -> {
                                if (field.type == "BOOLEAN") {
                                    formValues[field.id] = "false"
                                } else {
                                    formValues[field.id] = ""
                                }
                            }
                        }
                    }
                }
            }
            isLoaded = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isStackNotEmpty) {
                    IconButton(onClick = onCancelClicked) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = TextLight
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (recordId == null) "New ${table.name}" else "Edit ${table.name}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )
            }
            Row {
                IconButton(onClick = onCancelClicked) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = TextMuted
                    )
                }
                IconButton(onClick = {
                    var valid = true
                    fields.forEach { field ->
                        if (field.isSystem && field.name == "id") return@forEach
                        val value = formValues[field.id] ?: ""
                        if (field.required && value.isBlank()) {
                            valid = false
                            errorMessage = "Field '${field.name}' is required."
                        }
                    }
                    
                    if (valid) {
                        scope.launch {
                            val currentRecordId = recordId ?: UUID.randomUUID().toString()
                            val isNew = recordId == null

                            fields.find { it.isSystem && it.name == "id" }?.let { pkField ->
                                formValues[pkField.id] = currentRecordId
                            }
                            
                            val record = RecordEntity(
                                id = currentRecordId,
                                tableId = table.id,
                                createdAt = if (isNew) System.currentTimeMillis() else (databaseDao.getRecordById(currentRecordId)?.createdAt ?: System.currentTimeMillis())
                            )
                            
                            val values = fields.map { field ->
                                val valText = formValues[field.id] ?: ""
                                ValueEntity(
                                    id = UUID.randomUUID().toString(),
                                    recordId = currentRecordId,
                                    fieldId = field.id,
                                    valueText = valText
                                )
                            }
                            
                            databaseDao.insertRecordWithValues(record, values)
                            
                            if (isNew) {
                                val rPayload = Json.encodeToString(record)
                                syncEngine.queueItem("INSERT", "RECORD", rPayload)
                            }
                            
                            for (value in values) {
                                val isFileField = fields.find { it.id == value.fieldId }?.type == "FILE"
                                if (isFileField) {
                                    val localPath = filePaths[value.fieldId]
                                    if (localPath != null) {
                                        val file = File(localPath)
                                        val filePayload = FileSyncPayload(
                                            valueId = value.id,
                                            recordId = value.recordId,
                                            fieldId = value.fieldId,
                                            localPath = localPath,
                                            remoteName = "attachments/${currentRecordId}_${file.name}"
                                        )
                                        syncEngine.queueItem("INSERT", "FILE", Json.encodeToString(filePayload))
                                    }
                                } else {
                                    val vPayload = Json.encodeToString(value)
                                    syncEngine.queueItem("INSERT", "VALUE", vPayload)
                                }
                            }
                            
                            onSaveCompleted()
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = AccentMint
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = TextMuted)
            }
        } else {
            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(errorMessage, color = Color(0xFFF87171), fontSize = 14.sp)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(fields) { field ->
                    if (field.isSystem) return@items

                    val currentValue = formValues[field.id] ?: ""

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = field.name,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                fontSize = 14.sp
                            )
                            if (field.required && !field.isSystem) {
                                Text(" *", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        when {
                            field.isSystem && field.name.endsWith("_id") -> {
                                val parentTableName = field.name.removeSuffix("_id")
                                LinkedRecordPicker(
                                    parentTableName = parentTableName,
                                    currentValue = currentValue,
                                    onValueChange = { formValues[field.id] = it },
                                    databaseDao = databaseDao,
                                    primaryColor = PrimaryEmerald,
                                    surfaceColor = SurfaceDark,
                                    textLight = TextLight,
                                    textMuted = TextMuted
                                )
                            }
                            field.type == "TEXT" -> {
                                OutlinedTextField(
                                    value = currentValue,
                                    onValueChange = { formValues[field.id] = it },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryEmerald,
                                        unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                        focusedTextColor = TextLight,
                                        unfocusedTextColor = TextLight
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            field.type == "NUMBER" -> {
                                OutlinedTextField(
                                    value = currentValue,
                                    onValueChange = { formValues[field.id] = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryEmerald,
                                        unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                        focusedTextColor = TextLight,
                                        unfocusedTextColor = TextLight
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            field.type == "BOOLEAN" -> {
                                val isChecked = currentValue.toBoolean()
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { formValues[field.id] = it.toString() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextLight,
                                        checkedTrackColor = PrimaryEmerald,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = SurfaceDark
                                    )
                                )
                            }
                            field.type == "DATE" -> {
                                OutlinedTextField(
                                    value = currentValue,
                                    onValueChange = { formValues[field.id] = it },
                                    placeholder = { Text("yyyy-MM-dd", color = TextMuted.copy(alpha = 0.4f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryEmerald,
                                        unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                        focusedTextColor = TextLight,
                                        unfocusedTextColor = TextLight
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            field.type == "FILE" -> {
                                FileAttachmentInput(
                                    currentValue = currentValue,
                                    localPath = filePaths[field.id],
                                    onFileSelected = { selectedUri ->
                                        val cacheFile = copyUriToCache(context, selectedUri)
                                        if (cacheFile != null) {
                                            filePaths[field.id] = cacheFile.absolutePath
                                            formValues[field.id] = cacheFile.name
                                        }
                                    },
                                    onClearFile = {
                                        filePaths.remove(field.id)
                                        formValues[field.id] = ""
                                    }
                                )
                            }
                        }
                    }
                }

                if (recordId != null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Linked Child Collections",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (childTables.isEmpty()) {
                        item {
                            Text(
                                text = "No child collections defined.",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        items(childTables) { childTable ->
                            ChildCollectionSection(
                                childTable = childTable,
                                parentTable = table,
                                parentRecordId = recordId,
                                databaseDao = databaseDao,
                                onNavigateToChild = onNavigateToChild,
                                primaryColor = PrimaryEmerald,
                                surfaceColor = SurfaceDark,
                                textLight = TextLight,
                                textMuted = TextMuted
                            )
                        }
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Save this record first to link child records.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * A dropdown that shows records from the parent table identified by [parentTableName].
 * Each item's label is the parent record's first non-system field value (readable label).
 * The stored value is always the parent record's UUID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedRecordPicker(
    parentTableName: String,
    currentValue: String,     // currently selected parent record UUID
    onValueChange: (String) -> Unit,
    databaseDao: DatabaseDao,
    primaryColor: Color,
    surfaceColor: Color,
    textLight: Color,
    textMuted: Color
) {
    val allTables by databaseDao.getAllTablesFlow().collectAsState(initial = emptyList())
    val parentTable = allTables.find { it.name.equals(parentTableName, ignoreCase = true) }

    val parentRecords by (parentTable?.let { databaseDao.getRecordsForTableFlow(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    // Map recordId -> displayLabel (resolved async)
    val labelMap = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(parentRecords) {
        parentRecords.forEach { record ->
            if (!labelMap.containsKey(record.id)) {
                scope.launch {
                    val label = databaseDao.getRecordDisplayValue(record.id) ?: record.id
                    labelMap[record.id] = label
                }
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }
    val displayLabel = if (currentValue.isBlank()) "— Select ${parentTableName} —"
                       else labelMap[currentValue] ?: currentValue

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = displayLabel,
            onValueChange = {},
            label = { Text("Linked ${parentTableName}", color = textMuted) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = textMuted.copy(alpha = 0.3f),
                focusedTextColor = textLight,
                unfocusedTextColor = textLight
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(surfaceColor)
        ) {
            if (parentRecords.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No ${parentTableName} records yet", color = textMuted) },
                    onClick = { expanded = false }
                )
            } else {
                parentRecords.forEach { record ->
                    val label = labelMap[record.id] ?: record.id
                    DropdownMenuItem(
                        text = { Text(label, color = textLight) },
                        onClick = {
                            onValueChange(record.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FileAttachmentInput(
    currentValue: String,
    localPath: String?,
    onFileSelected: (Uri) -> Unit,
    onClearFile: () -> Unit
) {
    val context = LocalContext.current
    val SurfaceDark = MaterialTheme.colorScheme.surface
    val TextLight = MaterialTheme.colorScheme.onSurface
    val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val PrimaryDeepTeal = MaterialTheme.colorScheme.primaryContainer
    val AccentMint = MaterialTheme.colorScheme.secondary

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onFileSelected(uri)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TextMuted.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (currentValue.isNotEmpty() || localPath != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayName = com.allensandiego.notepad.util.FileHelper.getCleanFileName(localPath ?: currentValue)
                    Text(
                        text = displayName,
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val pathOrUrl = localPath ?: currentValue
                            if (pathOrUrl.isNotEmpty()) {
                                com.allensandiego.notepad.util.FileHelper.openFile(context, pathOrUrl)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "View File",
                                tint = AccentMint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (currentValue.startsWith("http")) {
                            IconButton(onClick = {
                                com.allensandiego.notepad.util.FileHelper.downloadFile(context, currentValue, displayName)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download File",
                                    tint = AccentMint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(onClick = onClearFile) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear File",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Attachment (Image/PDF/Doc)", color = TextLight, fontSize = 13.sp)
                }
            }
        }
    }
}

fun copyUriToCache(context: Context, uri: Uri): File? {
    return try {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor?.moveToFirst()
        val displayName = cursor?.getString(nameIndex ?: -1) ?: "temp_file"
        cursor?.close()

        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val cacheFile = File(context.cacheDir, "${UUID.randomUUID()}_$displayName")
        cacheFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        cacheFile
    } catch (e: Exception) {
        e.printStackTrace()
        Bugsnag.notify(e)
        null
    }
}
