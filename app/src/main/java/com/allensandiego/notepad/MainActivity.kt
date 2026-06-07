package com.allensandiego.notepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.allensandiego.notepad.db.NotepadDatabase
import com.allensandiego.notepad.sync.SupabaseClient
import com.allensandiego.notepad.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.BreadcrumbType

import com.allensandiego.notepad.ui.theme.ThemePreference
import com.allensandiego.notepad.ui.theme.getThemePreference
import com.allensandiego.notepad.ui.theme.NotepadTheme
import com.allensandiego.notepad.ui.MainScreen
import com.allensandiego.notepad.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Bugsnag.leaveBreadcrumb("App launch activity created", emptyMap(), BreadcrumbType.STATE)

        // Initialize Local DB and Sync Engines
        val database = NotepadDatabase.getDatabase(this)
        val dao = database.databaseDao()
        val supabaseClient = SupabaseClient(this)
        val syncEngine = SyncEngine(this, dao, supabaseClient)

        // Automatically trigger sync on application launch in background
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (supabaseClient.isConfigured()) {
                    syncEngine.triggerSync()
                }
            }
        }

        setContent {
            var currentTheme by remember {
                mutableStateOf(getThemePreference(this@MainActivity))
            }
            var isSettingsOpen by remember { mutableStateOf(false) }

            NotepadTheme(themePreference = currentTheme) {
                if (isSettingsOpen) {
                    SettingsScreen(
                        supabaseClient = supabaseClient,
                        syncEngine = syncEngine,
                        currentTheme = currentTheme,
                        onThemeChanged = { currentTheme = it },
                        onBack = { isSettingsOpen = false }
                    )
                } else {
                    MainScreen(
                        databaseDao = dao,
                        syncEngine = syncEngine,
                        onNavigateToSettings = { isSettingsOpen = true }
                    )
                }
            }
        }
    }
}