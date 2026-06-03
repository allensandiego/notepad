package com.allensandiego.notepad

import android.content.Context
import android.content.SharedPreferences
import com.allensandiego.notepad.sync.SupabaseClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.io.File

class SupabaseIntegrationTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val prefsMap = HashMap<String, String>()

    private var supabaseUrl = ""
    private var supabaseApiKey = ""

    @Before
    fun setUp() {
        // Read env.example to get test credentials
        val envFile = findEnvFile()
        assertNotNull("env.example file not found in path!", envFile)

        envFile!!.readLines().forEach { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                when (key) {
                    "URL" -> supabaseUrl = value
                    "API_KEY" -> supabaseApiKey = value
                }
            }
        }

        assertTrue("URL is empty in env.example", supabaseUrl.isNotEmpty())
        assertTrue("API_KEY is empty in env.example", supabaseApiKey.isNotEmpty())

        // Setup mocked Context and SharedPreferences
        context = mock(Context::class.java)
        sharedPreferences = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)

        `when`(context.getSharedPreferences(eq("supabase_prefs"), eq(Context.MODE_PRIVATE)))
            .thenReturn(sharedPreferences)

        // Mock Editor putString/apply to store in our in-memory map
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(any(), any())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as String
            prefsMap[key] = value
            editor
        }
        `when`(editor.apply()).thenAnswer {
            // No-op
        }

        // Mock SharedPreferences getString to read from our in-memory map
        `when`(sharedPreferences.getString(any(), any())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val default = invocation.arguments[1] as? String
            prefsMap[key] ?: default
        }
    }

    private fun findEnvFile(): File? {
        // Search in current directory, parent directory, and grandparent directory
        val searchPaths = listOf(".", "..", "../..")
        for (path in searchPaths) {
            val file = File(path, "env.example")
            if (file.exists()) {
                return file
            }
        }
        return null
    }

    @Test
    fun testSupabaseConnectionAndSchemaInit() = runBlocking {
        val client = SupabaseClient(context)

        // 1. Save and verify credentials save successfully
        val saved = client.saveCredentials(supabaseUrl, supabaseApiKey)
        assertTrue("Failed to save credentials", saved)
        assertTrue("Supabase client is not configured", client.isConfigured())

        // 2. Test the API HTTP Connection
        println("=== Supabase URL: ${client.getSupabaseUrl()} ===")
        println("=== Supabase Key: ${client.getSupabaseApiKey()} ===")
        val apiConnected = client.testConnection()
        println("=== Test Connection Result: ${apiConnected} ===")
        assertTrue("Failed to connect to Supabase REST API", apiConnected)

        // 3. Verify tables exist via REST API
        val tables = client.tablesExist()
        assertTrue("Database tables not found. Run the setup SQL in the Supabase Dashboard first.", tables)

        // 4. Test Table Ops (Ping check to make sure table is queryable over REST)
        val tableUpserted = client.upsertTable("test_ping", "Test Ping Table", null, System.currentTimeMillis())
        assertTrue("Failed to upsert test row to custom_tables", tableUpserted)

        // 5. Clean up the ping row
        val tableDeleted = client.deleteTable("test_ping")
        assertTrue("Failed to delete test row from custom_tables", tableDeleted)
    }
}
