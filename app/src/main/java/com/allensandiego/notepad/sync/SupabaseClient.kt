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

    /**
     * Checks if the required database tables exist by querying the REST API.
     * Returns true if the tables are accessible, false if they return 404 or error.
     */
    suspend fun tablesExist(): Boolean {
        if (!isConfigured()) return false
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/custom_tables?select=id&limit=0"
            val response: HttpResponse = client.get(url) {
                getBaseHeaders(this)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
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
