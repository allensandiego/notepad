package com.allensandiego.notepad.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.allensandiego.notepad.db.DatabaseDao
import com.allensandiego.notepad.db.FieldEntity
import com.allensandiego.notepad.db.RecordEntity
import com.allensandiego.notepad.db.SyncItemEntity
import com.allensandiego.notepad.db.TableEntity
import com.allensandiego.notepad.db.ValueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import com.bugsnag.android.Bugsnag

class SyncEngine(
    private val context: Context,
    private val databaseDao: DatabaseDao,
    private val supabaseClient: SupabaseClient
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun queueItem(action: String, targetType: String, payload: String) {
        val item = SyncItemEntity(
            action = action,
            targetType = targetType,
            payloadJson = payload
        )
        databaseDao.insertSyncItem(item)
        if (isOnline()) {
            triggerSync()
        }
    }

    suspend fun triggerSync(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline() || !supabaseClient.isConfigured()) return@withContext false

        mutex.withLock {
            // 1. PUSH: Process any outstanding local actions in the queue
            val queue = databaseDao.getSyncQueue()
            for (item in queue) {
                val success = processSyncItem(item)
                if (success) {
                    databaseDao.deleteSyncItem(item.id)
                } else {
                    // Stop sync if any queued item fails to push to prevent out-of-order execution
                    return@withContext false
                }
            }

            // 2. PULL & MERGE: Retrieve the entire state from Supabase
            val remoteTables = supabaseClient.fetchAllTables() ?: return@withContext false
            val remoteFields = supabaseClient.fetchAllFields() ?: return@withContext false
            val remoteRecords = supabaseClient.fetchAllRecords() ?: return@withContext false
            val remoteValues = supabaseClient.fetchAllValues() ?: return@withContext false

            databaseDao.syncDatabaseState(
                remoteTables = remoteTables.map { it.toEntity() },
                remoteFields = remoteFields.map { it.toEntity() },
                remoteRecords = remoteRecords.map { it.toEntity() },
                remoteValues = remoteValues.map { it.toEntity() }
            )

            return@withContext true
        }
    }

    private suspend fun processSyncItem(item: SyncItemEntity): Boolean {
        return try {
            when (item.targetType) {
                "SCHEMA_TABLE" -> {
                    val table = json.decodeFromString<TableEntity>(item.payloadJson)
                    if (item.action == "DELETE") {
                        supabaseClient.deleteTable(table.id)
                    } else {
                        supabaseClient.upsertTable(table.id, table.name, table.parentTableId, table.createdAt)
                    }
                }
                "SCHEMA_FIELD" -> {
                    val field = json.decodeFromString<FieldEntity>(item.payloadJson)
                    if (item.action == "DELETE") {
                        supabaseClient.deleteField(field.id)
                    } else {
                        supabaseClient.upsertField(
                            field.id,
                            field.tableId,
                            field.name,
                            field.type,
                            field.required,
                            field.defaultValue,
                            field.defaultType,
                            field.isSystem
                        )
                    }
                }
                "RECORD" -> {
                    val record = json.decodeFromString<RecordEntity>(item.payloadJson)
                    if (item.action == "DELETE") {
                        supabaseClient.deleteRecord(record.id)
                    } else {
                        supabaseClient.upsertRecord(record.id, record.tableId, record.createdAt)
                    }
                }
                "VALUE" -> {
                    val value = json.decodeFromString<ValueEntity>(item.payloadJson)
                    supabaseClient.upsertValue(value.id, value.recordId, value.fieldId, value.valueText)
                }
                "FILE" -> {
                    val filePayload = json.decodeFromString<FileSyncPayload>(item.payloadJson)
                    val localFile = File(filePayload.localPath)
                    if (!localFile.exists()) {
                        return true
                    }
                    val publicUrl = supabaseClient.uploadFile(filePayload.remoteName, localFile)
                    if (publicUrl != null) {
                        val updatedValue = ValueEntity(
                            id = filePayload.valueId,
                            recordId = filePayload.recordId,
                            fieldId = filePayload.fieldId,
                            valueText = publicUrl
                        )
                        databaseDao.insertValue(updatedValue)
                        supabaseClient.upsertValue(updatedValue.id, updatedValue.recordId, updatedValue.fieldId, publicUrl)
                    } else {
                        false
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Bugsnag.notify(e)
            false
        }
    }
}

@Serializable
data class FileSyncPayload(
    val valueId: String,
    val recordId: String,
    val fieldId: String,
    val localPath: String,
    val remoteName: String
)
