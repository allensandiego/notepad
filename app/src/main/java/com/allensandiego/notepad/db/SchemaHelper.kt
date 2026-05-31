package com.allensandiego.notepad.db

import com.allensandiego.notepad.sync.SyncEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Centralised helper for inserting system-managed fields immediately after a
 * [TableEntity] is persisted. Keeps this logic out of UI composables so it can
 * also be called from tests or future batch-import paths.
 */
object SchemaHelper {

    private val json = Json { encodeDefaults = true }

    /**
     * Must be called (inside a coroutine) right after [DatabaseDao.insertTable].
     *
     * Inserts:
     *  1. A `id` TEXT field with defaultType=UUID (primary key)
     *  2. A `<parentName>_id` TEXT field (foreign key) if [table] has a [TableEntity.parentTableId]
     *
     * Both fields are marked [FieldEntity.isSystem] = true so the UI renders them
     * as locked and skips them in the record-edit form.
     */
    suspend fun insertSystemFields(
        dao: DatabaseDao,
        syncEngine: SyncEngine,
        table: TableEntity,
        allTables: List<TableEntity>
    ) {
        // 1. Primary key field — auto-generated UUID, hidden in edit form
        val pkField = FieldEntity(
            id = UUID.randomUUID().toString(),
            tableId = table.id,
            name = "id",
            type = "TEXT",
            required = true,
            defaultValue = null,
            defaultType = "UUID",
            isSystem = true
        )
        dao.insertField(pkField)
        syncEngine.queueItem("INSERT", "SCHEMA_FIELD", json.encodeToString(pkField))

        // 2. Foreign key field — only for child collections
        if (table.parentTableId != null) {
            val parent = allTables.find { it.id == table.parentTableId }
            if (parent != null) {
                val fkField = FieldEntity(
                    id = UUID.randomUUID().toString(),
                    tableId = table.id,
                    name = "${parent.name}_id",
                    type = "TEXT",
                    required = true,
                    defaultValue = null,
                    defaultType = null,
                    isSystem = true
                )
                dao.insertField(fkField)
                syncEngine.queueItem("INSERT", "SCHEMA_FIELD", json.encodeToString(fkField))
            }
        }
    }
}
