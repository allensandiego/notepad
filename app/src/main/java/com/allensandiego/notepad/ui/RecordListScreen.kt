package com.allensandiego.notepad.ui

import com.allensandiego.notepad.ui.components.NativeAdCard
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allensandiego.notepad.db.DatabaseDao
import com.allensandiego.notepad.db.FieldEntity
import com.allensandiego.notepad.db.RecordEntity
import com.allensandiego.notepad.db.TableEntity
import com.allensandiego.notepad.db.ValueEntity
import com.allensandiego.notepad.sync.SyncEngine
import kotlinx.coroutines.launch

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListContent(
    table: TableEntity,
    databaseDao: DatabaseDao,
    syncEngine: SyncEngine,
    onEditRecord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val PrimaryEmerald = MaterialTheme.colorScheme.primary
    val SecondaryNavy = MaterialTheme.colorScheme.background
    val SurfaceDark = MaterialTheme.colorScheme.surface
    val TextLight = MaterialTheme.colorScheme.onSurface
    val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current

    val records by databaseDao.getRecordsForTableFlow(table.id).collectAsState(initial = emptyList())
    val fields by databaseDao.getFieldsForTableFlow(table.id).collectAsState(initial = emptyList())
    
    val recordValuesMap = remember { mutableStateMapOf<String, List<ValueEntity>>() }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(records) {
        records.forEach { record ->
            val vals = databaseDao.getValuesForRecord(record.id)
            recordValuesMap[record.id] = vals
        }
    }

    val filteredRecords = remember(records, recordValuesMap, searchQuery) {
        if (searchQuery.isBlank()) {
            records
        } else {
            records.filter { record ->
                val values = recordValuesMap[record.id] ?: emptyList()
                values.any { it.valueText?.contains(searchQuery, ignoreCase = true) == true }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SecondaryNavy)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search records...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    val startTime = System.currentTimeMillis()
                    val syncSuccess = syncEngine.triggerSync()
                    val duration = System.currentTimeMillis() - startTime
                    
                    // Keep the spinner visible for at least 800ms for a smooth animation feel
                    if (duration < 800) {
                        kotlinx.coroutines.delay(800 - duration)
                    }
                    isRefreshing = false

                    if (syncSuccess) {
                        Toast.makeText(context, "Sync completed successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Sync completed with offline status.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching records found" else "No records yet.\nTap + to add a record.",
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredRecords) { record ->
                        val values = recordValuesMap[record.id] ?: emptyList()
                        RecordCard(
                            record = record,
                            fields = fields,
                            values = values,
                            onCardClick = { onEditRecord(record.id) },
                            onDelete = {
                                scope.launch {
                                    databaseDao.deleteRecord(record.id)
                                    val payload = "{\"id\":\"${record.id}\",\"table_id\":\"${record.tableId}\",\"created_at\":${record.createdAt}}"
                                    syncEngine.queueItem("DELETE", "RECORD", payload)
                                }
                            },
                            databaseDao = databaseDao
                        )
                    }
                    item {
                        NativeAdCard(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RecordCard(
    record: RecordEntity,
    fields: List<FieldEntity>,
    values: List<ValueEntity>,
    onCardClick: () -> Unit,
    onDelete: () -> Unit,
    databaseDao: DatabaseDao
) {
    val SurfaceDark = MaterialTheme.colorScheme.surface
    val TextLight = MaterialTheme.colorScheme.onSurface
    val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateFormatted = java.text.DateFormat.getDateTimeInstance()
                    .format(java.util.Date(record.createdAt))
                Text(
                    text = "Created: $dateFormatted",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Record",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            fields.forEach { field ->
                // Hide the system PK field (id) from record cards
                if (field.isSystem && field.name == "id") return@forEach

                val valueObj = values.find { it.fieldId == field.id }
                val rawValue = valueObj?.valueText ?: ""

                if (rawValue.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val labelPrefix = if (field.isSystem && field.name.endsWith("_id")) "🔗 " else ""
                        Text(
                            text = "$labelPrefix${field.name}: ",
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.width(110.dp)
                        )

                        when {
                            // FK field: resolve parent record display label
                            field.isSystem && field.name.endsWith("_id") -> {
                                var displayLabel by remember(rawValue) { mutableStateOf(rawValue) }
                                LaunchedEffect(rawValue) {
                                    val resolved = databaseDao.getRecordDisplayValue(rawValue)
                                    if (!resolved.isNullOrBlank()) displayLabel = resolved
                                }
                                Text(
                                    text = displayLabel,
                                    color = TextLight,
                                    fontSize = 13.sp
                                )
                            }
                            field.type == "FILE" && (rawValue.startsWith("http") || rawValue.isNotEmpty()) -> {
                                val context = LocalContext.current
                                val isImage = rawValue.endsWith(".jpg") || rawValue.endsWith(".jpeg") ||
                                              rawValue.endsWith(".png") || rawValue.contains("image")
                                val displayName = com.allensandiego.notepad.util.FileHelper.getCleanFileName(rawValue)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF2DD4BF).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .clickable {
                                                com.allensandiego.notepad.util.FileHelper.openFile(context, rawValue)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isImage) "Image Attachment 🖼️" else "Document Attachment 📄",
                                            color = Color(0xFF2DD4BF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            com.allensandiego.notepad.util.FileHelper.openFile(context, rawValue)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "View File",
                                            tint = Color(0xFF2DD4BF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (rawValue.startsWith("http")) {
                                        IconButton(
                                            onClick = {
                                                com.allensandiego.notepad.util.FileHelper.downloadFile(context, rawValue, displayName)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download File",
                                                tint = Color(0xFF2DD4BF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = rawValue,
                                    color = TextLight,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
