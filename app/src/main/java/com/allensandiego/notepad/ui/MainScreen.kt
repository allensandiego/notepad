package com.allensandiego.notepad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allensandiego.notepad.db.DatabaseDao
import com.allensandiego.notepad.db.TableEntity
import com.allensandiego.notepad.sync.SyncEngine
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.BreadcrumbType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    databaseDao: DatabaseDao,
    syncEngine: SyncEngine,
    onNavigateToSettings: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val tables by databaseDao.getAllTablesFlow().collectAsState(initial = emptyList())
    var selectedTable by remember { mutableStateOf<TableEntity?>(null) }

    LaunchedEffect(selectedTable) {
        Bugsnag.leaveBreadcrumb(
            "Opened Main Screen",
            mapOf("selectedTable" to (selectedTable?.name ?: "none")),
            BreadcrumbType.NAVIGATION
        )
    }

    // Dialog states
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var selectedTableForFields by remember { mutableStateOf<TableEntity?>(null) }
    var collectionToDelete by remember { mutableStateOf<TableEntity?>(null) }
    
    var activeRecordEditTable by remember { mutableStateOf<TableEntity?>(null) }
    var activeRecordEditId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Colors
    val PrimaryEmerald = MaterialTheme.colorScheme.primary
    val SecondaryNavy = MaterialTheme.colorScheme.background
    val SurfaceDark = MaterialTheme.colorScheme.surface
    val TextLight = MaterialTheme.colorScheme.onSurface
    val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val OutlineColor = MaterialTheme.colorScheme.outline

    // Sidebar Composable content
    val sidebarContent = @Composable {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(260.dp)
                .background(SurfaceDark)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Collections",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextLight
                )
                IconButton(
                    onClick = { showCreateCollectionDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Collection",
                        tint = PrimaryEmerald
                    )
                }
            }

            HorizontalDivider(color = OutlineColor.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

            if (tables.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No collections yet.\nTap + to create.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Only show top-level (root) collections in the sidebar.
                // Child collections are managed from within the Schema Designer.
                val rootTables = tables.filter { it.parentTableId == null }.sortedBy { it.name }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(rootTables) { table ->
                        val isSelected = selectedTable?.id == table.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) PrimaryEmerald.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .clickable {
                                    selectedTable = table
                                    if (!isWideScreen) {
                                        scope.launch { drawerState.close() }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryEmerald else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = table.name,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryEmerald else TextLight,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { selectedTableForFields = table },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Configure Schema",
                                        tint = TextMuted.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { collectionToDelete = table },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Collection",
                                        tint = Color(0xFFF87171).copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Main area content (Scaffold displaying selected collection or welcome)
    val mainContentArea = @Composable {
        val currentTable = selectedTable
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentTable?.name ?: "Notepad",
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    },
                    navigationIcon = {
                        if (!isWideScreen) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextLight)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
                )
            },
            floatingActionButton = {
                if (currentTable != null) {
                    FloatingActionButton(
                        onClick = {
                            activeRecordEditTable = currentTable
                            activeRecordEditId = null
                        },
                        containerColor = PrimaryEmerald,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Record")
                    }
                }
            },
            containerColor = SecondaryNavy
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentTable == null) {
                    // Empty Welcome State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Welcome to Notepad",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select a collection from the sidebar to view records, or create your first collection structure below to get started.",
                            fontSize = 14.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCreateCollectionDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create Collection", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Selected Collection Records
                    RecordListContent(
                        table = currentTable,
                        databaseDao = databaseDao,
                        syncEngine = syncEngine,
                        onEditRecord = { recordId ->
                            activeRecordEditTable = currentTable
                            activeRecordEditId = recordId
                        }
                    )
                }
            }
        }
    }

    if (isWideScreen) {
        // Persistent sidebar layout for wide screens
        Row(modifier = Modifier.fillMaxSize()) {
            sidebarContent()
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(OutlineColor.copy(alpha = 0.2f))
            )
            Box(modifier = Modifier.weight(1f)) {
                mainContentArea()
            }
        }
    } else {
        // Toggled navigation drawer layout for narrow screens
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = SurfaceDark,
                    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                ) {
                    sidebarContent()
                }
            }
        ) {
            mainContentArea()
        }
    }

    // Dialog overrides

    // 1. Create Collection Dialog
    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            databaseDao = databaseDao,
            syncEngine = syncEngine,
            onDismiss = { showCreateCollectionDialog = false }
        )
    }

    // 2. Manage/Design Fields Dialog
    if (selectedTableForFields != null) {
        FieldDesignerDialog(
            table = selectedTableForFields!!,
            databaseDao = databaseDao,
            syncEngine = syncEngine,
            onDismiss = { selectedTableForFields = null }
        )
    }

    // 3. Edit Record Dialog
    if (activeRecordEditTable != null) {
        RecordEditDialog(
            table = activeRecordEditTable!!,
            recordId = activeRecordEditId,
            databaseDao = databaseDao,
            syncEngine = syncEngine,
            onDismiss = {
                activeRecordEditTable = null
                activeRecordEditId = null
            }
        )
    }

    // 4. Delete Collection Confirmation Dialog
    if (collectionToDelete != null) {
        val target = collectionToDelete!!
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("Delete Collection", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the collection \"${target.name}\"? All columns and associated records will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            databaseDao.deleteTable(target.id)
                            val json = kotlinx.serialization.json.Json { encodeDefaults = true }
                            val payload = json.encodeToString(kotlinx.serialization.serializer(), target)
                            syncEngine.queueItem("DELETE", "SCHEMA_TABLE", payload)
                            if (selectedTable?.id == target.id) {
                                selectedTable = null
                            }
                            collectionToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = TextLight,
            textContentColor = TextMuted
        )
    }
}

/**
 * Produces a depth-first ordered list of (TableEntity, depth) pairs for the sidebar.
 * Top-level collections (parentTableId == null) appear at depth 0;
 * their children appear at depth 1 immediately after them.
 */
fun buildTreeOrder(tables: List<com.allensandiego.notepad.db.TableEntity>): List<Pair<com.allensandiego.notepad.db.TableEntity, Int>> {
    val result = mutableListOf<Pair<com.allensandiego.notepad.db.TableEntity, Int>>()
    val childrenMap = tables.groupBy { it.parentTableId }

    fun visit(table: com.allensandiego.notepad.db.TableEntity, depth: Int) {
        result.add(table to depth)
        childrenMap[table.id]?.sortedBy { it.name }?.forEach { visit(it, depth + 1) }
    }

    // Start with root-level tables (no parent), alphabetically sorted
    childrenMap[null]?.sortedBy { it.name }?.forEach { visit(it, 0) }
    return result
}
