package com.allensandiego.notepad.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao {

    // --- Schema Operations ---
    @Query("SELECT * FROM custom_tables ORDER BY name ASC")
    fun getAllTablesFlow(): Flow<List<TableEntity>>

    @Query("SELECT * FROM custom_tables ORDER BY name ASC")
    suspend fun getAllTables(): List<TableEntity>

    @Query("SELECT * FROM custom_tables WHERE id = :tableId LIMIT 1")
    suspend fun getTableById(tableId: String): TableEntity?

    @Query("SELECT * FROM custom_fields WHERE table_id = :tableId")
    fun getFieldsForTableFlow(tableId: String): Flow<List<FieldEntity>>

    @Query("SELECT * FROM custom_fields WHERE table_id = :tableId")
    suspend fun getFieldsForTable(tableId: String): List<FieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: FieldEntity)

    @Query("DELETE FROM custom_tables WHERE id = :tableId")
    suspend fun deleteTable(tableId: String)

    @Query("DELETE FROM custom_fields WHERE id = :fieldId")
    suspend fun deleteField(fieldId: String)

    // --- Relationship Queries ---

    /** Returns all collections whose parentTableId matches [parentId]. */
    @Query("SELECT * FROM custom_tables WHERE parent_table_id = :parentId ORDER BY name ASC")
    fun getChildTablesForParent(parentId: String): Flow<List<TableEntity>>

    /**
     * Resolves the display label for a parent record: the value_text of the first
     * non-system field belonging to that record's table, ordered by field rowid.
     * Falls back to the recordId itself if no value is found.
     */
    @Query("""
        SELECT cv.value_text
        FROM custom_values cv
        INNER JOIN custom_fields cf ON cf.id = cv.field_id
        WHERE cv.record_id = :recordId
          AND cf.is_system = 0
        ORDER BY cf.rowid ASC
        LIMIT 1
    """)
    suspend fun getRecordDisplayValue(recordId: String): String?

    // --- Record Operations ---
    @Query("SELECT * FROM custom_records WHERE table_id = :tableId ORDER BY created_at DESC")
    fun getRecordsForTableFlow(tableId: String): Flow<List<RecordEntity>>

    @Query("""
        SELECT r.* FROM custom_records r
        INNER JOIN custom_values v ON v.record_id = r.id
        WHERE r.table_id = :childTableId
          AND v.field_id = :fkFieldId
          AND v.value_text = :parentRecordId
        ORDER BY r.created_at DESC
    """)
    fun getChildRecordsFlow(childTableId: String, fkFieldId: String, parentRecordId: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM custom_records WHERE table_id = :tableId ORDER BY created_at DESC")
    suspend fun getRecordsForTable(tableId: String): List<RecordEntity>

    @Query("SELECT * FROM custom_records WHERE id = :recordId LIMIT 1")
    suspend fun getRecordById(recordId: String): RecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity)

    @Query("DELETE FROM custom_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: String)

    // --- Value Operations ---
    @Query("SELECT * FROM custom_values WHERE record_id = :recordId")
    fun getValuesForRecordFlow(recordId: String): Flow<List<ValueEntity>>

    @Query("SELECT * FROM custom_values WHERE record_id = :recordId")
    suspend fun getValuesForRecord(recordId: String): List<ValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValue(value: ValueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValues(values: List<ValueEntity>)

    // --- Dynamic Helper for inserting a Record with its values transactionally ---
    @Transaction
    suspend fun insertRecordWithValues(record: RecordEntity, values: List<ValueEntity>) {
        insertRecord(record)
        insertValues(values)
    }

    // --- Sync Queue Operations ---
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getSyncQueue(): List<SyncItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(syncItem: SyncItemEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSyncItem(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun clearSyncQueue()
}
