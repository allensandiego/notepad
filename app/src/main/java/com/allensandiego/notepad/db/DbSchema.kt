package com.allensandiego.notepad.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "custom_tables",
    indices = [Index(value = ["name"], unique = true)]
)
data class TableEntity(
    @PrimaryKey val id: String, // UUID
    val name: String,
    @ColumnInfo(name = "parent_table_id") val parentTableId: String? = null, // null = top-level collection
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "custom_fields",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["table_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["table_id"])]
)
data class FieldEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "table_id") val tableId: String,
    val name: String,
    val type: String, // TEXT, NUMBER, BOOLEAN, DATE, FILE
    val required: Boolean,
    @ColumnInfo(name = "default_value") val defaultValue: String?,
    @ColumnInfo(name = "default_type") val defaultType: String?, // STATIC, TODAY, UUID, AUTO_INCREMENT
    @ColumnInfo(name = "is_system") val isSystem: Boolean = false // true = auto-generated PK/FK, cannot be deleted
)

@Serializable
@Entity(
    tableName = "custom_records",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["table_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["table_id"])]
)
data class RecordEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "table_id") val tableId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "custom_values",
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["field_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["record_id"]),
        Index(value = ["field_id"]),
        Index(value = ["record_id", "field_id"], unique = true)
    ]
)
data class ValueEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "field_id") val fieldId: String,
    @ColumnInfo(name = "value_text") val valueText: String? // Serialized value
)

@Entity(tableName = "sync_queue")
data class SyncItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String, // INSERT, UPDATE, DELETE
    @ColumnInfo(name = "target_type") val targetType: String, // SCHEMA_TABLE, SCHEMA_FIELD, RECORD, VALUE, FILE
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
