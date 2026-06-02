package com.allensandiego.notepad.sync

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import java.io.File
import java.net.URI
import java.sql.DriverManager
import java.util.Properties
import com.bugsnag.android.Bugsnag

class SupabaseClient(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences(
        if (com.allensandiego.notepad.util.TestDetector.isUnderTest(context)) "supabase_prefs_test" else "supabase_prefs",
        Context.MODE_PRIVATE
    )

    fun getSupabaseUrl(): String = sharedPrefs.getString("url", "") ?: ""
    fun getSupabaseApiKey(): String = sharedPrefs.getString("api_key", "") ?: ""

    fun saveCredentials(url: String, apiKey: String): Boolean {
        val trimmedUrl = url.trim()
        val trimmedKey = apiKey.trim()

        if (trimmedUrl.isBlank() || trimmedKey.isBlank()) {
            sharedPrefs.edit()
                .putString("url", "")
                .putString("api_key", "")
                .apply()
            return true
        }

        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return false
        }

        sharedPrefs.edit()
            .putString("url", trimmedUrl.removeSuffix("/"))
            .putString("api_key", trimmedKey)
            .apply()
        return true
    }



    fun isConfigured(): Boolean {
        return getSupabaseUrl().isNotEmpty() && getSupabaseApiKey().isNotEmpty()
    }

    suspend fun testConnection(): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_tables"
            val response: HttpResponse = client.get(url) {
                getBaseHeaders(this)
            }
            // 401 means invalid key. 404 (table not found yet) or 200 (table exists) means auth succeeded.
            response.status.value != 401
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    private fun getDbHost(): String {
        val url = getSupabaseUrl()
        return try {
            val uri = URI(url)
            uri.host ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun initializeDatabaseSchema(password: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dbHost = getDbHost()
        if (dbHost.isEmpty()) return@withContext false
        val connectionUrl = "jdbc:postgresql://$dbHost:5432/postgres?ssl=true&sslmode=require"
        
        return@withContext try {
            Class.forName("org.postgresql.Driver")
            val props = Properties()
            props.setProperty("user", "postgres")
            props.setProperty("password", password)
            
            DriverManager.getConnection(connectionUrl, props).use { conn ->
                conn.createStatement().use { stmt ->
                    val sql = """
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

                        ALTER TABLE custom_tables DISABLE ROW LEVEL SECURITY;
                        ALTER TABLE custom_fields DISABLE ROW LEVEL SECURITY;
                        ALTER TABLE custom_records DISABLE ROW LEVEL SECURITY;
                        ALTER TABLE custom_values DISABLE ROW LEVEL SECURITY;
                    """.trimIndent()
                    stmt.execute(sql)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    private fun getBaseHeaders(builder: io.ktor.client.request.HttpRequestBuilder) {
        builder.header("apikey", getSupabaseApiKey())
    }

    // --- PostgREST DB Operations ---

    suspend fun upsertTable(id: String, name: String, parentTableId: String?, createdAt: Long): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_tables"
            val payload = listOf(TablePayload(id, name, parentTableId, createdAt))

            val response: HttpResponse = client.post(url) {
                getBaseHeaders(this)
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    suspend fun deleteTable(id: String): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_tables"
            val response: HttpResponse = client.delete(url) {
                getBaseHeaders(this)
                parameter("id", "eq.$id")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    suspend fun upsertField(
        id: String,
        tableId: String,
        name: String,
        type: String,
        required: Boolean,
        defaultValue: String?,
        defaultType: String?,
        isSystem: Boolean = false
    ): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_fields"
            val payload = listOf(FieldPayload(id, tableId, name, type, required, defaultValue, defaultType, isSystem))

            val response: HttpResponse = client.post(url) {
                getBaseHeaders(this)
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    suspend fun deleteField(id: String): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_fields"
            val response: HttpResponse = client.delete(url) {
                getBaseHeaders(this)
                parameter("id", "eq.$id")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    suspend fun upsertRecord(id: String, tableId: String, createdAt: Long): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_records"
            val payload = listOf(RecordPayload(id, tableId, createdAt))
            
            val response: HttpResponse = client.post(url) {
                getBaseHeaders(this)
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    suspend fun deleteRecord(id: String): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_records"
            val response: HttpResponse = client.delete(url) {
                getBaseHeaders(this)
                parameter("id", "eq.$id")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    suspend fun upsertValue(id: String, recordId: String, fieldId: String, valueText: String?): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_values"
            val payload = listOf(ValuePayload(id, recordId, fieldId, valueText))
            
            val response: HttpResponse = client.post(url) {
                getBaseHeaders(this)
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }

    // --- Supabase Storage API Operations ---

    suspend fun uploadFile(remotePath: String, localFile: File): String? {
        if (!isConfigured()) return null
        return try {
            val bucket = "attachments"
            val url = "${getSupabaseUrl()}/storage/v1/object/$bucket/$remotePath"
            val fileBytes = localFile.readBytes()
            
            val putResponse: HttpResponse = client.put(url) {
                getBaseHeaders(this)
                contentType(ContentType.Application.OctetStream)
                setBody(fileBytes)
            }

            if (putResponse.status.isSuccess()) {
                "${getSupabaseUrl()}/storage/v1/object/public/$bucket/$remotePath"
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            null
        }
    }
}

// --- Serializable DTOs for Supabase Payload ---

@Serializable
data class TablePayload(
    val id: String,
    val name: String,
    val parent_table_id: String?,
    val created_at: Long
)

@Serializable
data class FieldPayload(
    val id: String,
    val table_id: String,
    val name: String,
    val type: String,
    val required: Boolean,
    val default_value: String?,
    val default_type: String?,
    val is_system: Boolean = false
)

@Serializable
data class RecordPayload(
    val id: String,
    val table_id: String,
    val created_at: Long
)

@Serializable
data class ValuePayload(
    val id: String,
    val record_id: String,
    val field_id: String,
    val value_text: String?
)
