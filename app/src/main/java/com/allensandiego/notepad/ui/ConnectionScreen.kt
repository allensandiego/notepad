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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import com.allensandiego.notepad.sync.SupabaseClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.BreadcrumbType

import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    supabaseClient: SupabaseClient,
    onConnected: () -> Unit
) {
    LaunchedEffect(Unit) {
        Bugsnag.leaveBreadcrumb("Opened Connection Screen", emptyMap(), BreadcrumbType.NAVIGATION)
    }
    val PrimaryEmerald = MaterialTheme.colorScheme.primary
    val PrimaryDeepTeal = MaterialTheme.colorScheme.primaryContainer
    val SecondaryNavy = MaterialTheme.colorScheme.background
    val SurfaceDark = MaterialTheme.colorScheme.surface
    val TextLight = MaterialTheme.colorScheme.onSurface
    val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val AccentMint = MaterialTheme.colorScheme.secondary

    var supabaseUrl by remember { mutableStateOf(supabaseClient.getSupabaseUrl()) }
    var apiKey by remember { mutableStateOf(supabaseClient.getSupabaseApiKey()) }
    var dbPassword by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var showDbPassword by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf("") }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showSqlDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SecondaryNavy, SurfaceDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SUPABASE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AccentMint,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Text(
                text = "Sync Credentials",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextLight,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Connect to your personal Supabase instance by entering your Project URL and Publishable API Key. Provide your Database Password to automatically configure all remote tables.",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase Project URL", color = TextMuted) },
                        placeholder = { Text("https://your-project.supabase.co", color = TextMuted.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor = AccentMint,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Supabase API Key", color = TextMuted) },
                        placeholder = { Text("sb_publishable_...", color = TextMuted.copy(alpha = 0.5f)) },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showApiKey) "Hide API Key" else "Show API Key",
                                    tint = TextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor = AccentMint,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = dbPassword,
                        onValueChange = { dbPassword = it },
                        label = { Text("Database Password (for auto-setup)", color = TextMuted) },
                        placeholder = { Text("Required if tables are not set up yet", color = TextMuted.copy(alpha = 0.5f)) },
                        singleLine = true,
                        visualTransformation = if (showDbPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showDbPassword = !showDbPassword }) {
                                Icon(
                                    imageVector = if (showDbPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showDbPassword) "Hide password" else "Show password",
                                    tint = TextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor = AccentMint,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val uriHandler = LocalUriHandler.current
                    Text(
                        text = "🚀 One-Click Supabase Database Setup",
                        color = AccentMint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { uriHandler.openUri("https://supabase.com/dashboard/new?repository=https://github.com/allensandiego/notepad") }
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Need an account? Sign up on Supabase",
                        color = AccentMint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { uriHandler.openUri("https://supabase.com/dashboard/sign-in") }
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Show Supabase Database Setup SQL",
                        color = AccentMint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showSqlDialog = true }
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (supabaseUrl.isNotEmpty() && apiKey.isNotEmpty()) {
                                isTesting = true
                                testMessage = "Testing API connection..."
                                testSuccess = null
                                
                                scope.launch {
                                    val saved = supabaseClient.saveCredentials(supabaseUrl, apiKey)
                                    if (!saved) {
                                        isTesting = false
                                        testSuccess = false
                                        testMessage = "Invalid URL or API Key format."
                                        return@launch
                                    }
                                    
                                    val connected = supabaseClient.testConnection()
                                    if (!connected) {
                                        isTesting = false
                                        testSuccess = false
                                        testMessage = "Connection failed. Check your URL and API Key."
                                        return@launch
                                    }
                                    
                                    if (dbPassword.isNotEmpty()) {
                                        testMessage = "Configuring database tables..."
                                        val error = supabaseClient.initializeDatabaseSchema(dbPassword)
                                        if (error != null) {
                                            isTesting = false
                                            testSuccess = false
                                            testMessage = "Database setup failed: $error"
                                            return@launch
                                        }
                                    } else {
                                        testMessage = "Verifying database tables..."
                                        val tablesExist = supabaseClient.upsertTable("ping", "Connection Probe", null, System.currentTimeMillis())
                                        if (tablesExist) {
                                            supabaseClient.deleteTable("ping")
                                        } else {
                                            isTesting = false
                                            testSuccess = false
                                            testMessage = "Tables not found. Enter Database Password to automatically create them."
                                            return@launch
                                        }
                                    }
                                    
                                    isTesting = false
                                    testSuccess = true
                                    testMessage = "Connected and initialized successfully!"
                                    onConnected()
                                }
                            } else {
                                testSuccess = false
                                testMessage = "URL and API Key cannot be empty."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (isTesting) "Testing..." else "Connect Database",
                            color = TextLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            supabaseClient.saveCredentials("", "")
                            onConnected()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Skip & Work Offline Only",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = testMessage.isNotEmpty()) {
                val color = when (testSuccess) {
                    true -> AccentMint
                    false -> Color(0xFFF87171)
                    else -> TextMuted
                }
                Text(
                    text = testMessage,
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (showSqlDialog) {
            Dialog(onDismissRequest = { showSqlDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(550.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Supabase Database Setup",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Run the following SQL in your Supabase SQL Editor to create the necessary tables and disable RLS policies (so the app can sync).",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val sqlScript = """
                            -- 1. Create metadata tables
                            
                            CREATE TABLE IF NOT EXISTS custom_tables (
                                id TEXT PRIMARY KEY,
                                name TEXT NOT NULL UNIQUE,
                                parent_table_id TEXT REFERENCES custom_tables(id) ON DELETE CASCADE,
                                created_at BIGINT NOT NULL
                            );

                            CREATE TABLE IF NOT EXISTS custom_fields (
                                id TEXT PRIMARY KEY,
                                table_id TEXT NOT NULL REFERENCES custom_tables(id) ON DELETE CASCADE,
                                name TEXT NOT NULL,
                                type TEXT NOT NULL,
                                required BOOLEAN NOT NULL,
                                default_value TEXT,
                                default_type TEXT,
                                is_system BOOLEAN NOT NULL DEFAULT FALSE
                            );

                            CREATE TABLE IF NOT EXISTS custom_records (
                                id TEXT PRIMARY KEY,
                                table_id TEXT NOT NULL REFERENCES custom_tables(id) ON DELETE CASCADE,
                                created_at BIGINT NOT NULL
                            );

                            CREATE TABLE IF NOT EXISTS custom_values (
                                id TEXT PRIMARY KEY,
                                record_id TEXT NOT NULL REFERENCES custom_records(id) ON DELETE CASCADE,
                                field_id TEXT NOT NULL REFERENCES custom_fields(id) ON DELETE CASCADE,
                                value_text TEXT,
                                UNIQUE(record_id, field_id)
                            );

                            -- Disable RLS on data tables to allow client-side PostgREST synchronization
                            ALTER TABLE custom_tables DISABLE ROW LEVEL SECURITY;
                            ALTER TABLE custom_fields DISABLE ROW LEVEL SECURITY;
                            ALTER TABLE custom_records DISABLE ROW LEVEL SECURITY;
                            ALTER TABLE custom_values DISABLE ROW LEVEL SECURITY;

                            -- 2. Setup Storage Bucket for file attachments
                            INSERT INTO storage.buckets (id, name, public)
                            VALUES ('attachments', 'attachments', true)
                            ON CONFLICT (id) DO NOTHING;

                            -- Enable RLS on storage.objects to define explicit policies
                            ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

                            -- Allow public upload access to the attachments bucket
                            DROP POLICY IF EXISTS "Allow public upload to attachments" ON storage.objects;
                            CREATE POLICY "Allow public upload to attachments"
                            ON storage.objects FOR INSERT
                            TO public
                            WITH CHECK (bucket_id = 'attachments');

                            -- Allow public read access to the attachments bucket
                            DROP POLICY IF EXISTS "Allow public select from attachments" ON storage.objects;
                            CREATE POLICY "Allow public select from attachments"
                            ON storage.objects FOR SELECT
                            TO public
                            USING (bucket_id = 'attachments');
                        """.trimIndent()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(SecondaryNavy, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = sqlScript,
                                color = TextLight,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Supabase SQL Script", sqlScript)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "SQL copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Copy SQL", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { showSqlDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
