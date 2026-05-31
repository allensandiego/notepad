package com.allensandiego.notepad.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TableEntity::class,
        FieldEntity::class,
        RecordEntity::class,
        ValueEntity::class,
        SyncItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NotepadDatabase : RoomDatabase() {

    abstract fun databaseDao(): DatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: NotepadDatabase? = null

        fun getDatabase(context: Context): NotepadDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbName = if (com.allensandiego.notepad.util.TestDetector.isUnderTest(context)) {
                    "notepad_database_test"
                } else {
                    "notepad_database"
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotepadDatabase::class.java,
                    dbName
                )
                .fallbackToDestructiveMigration() // Simple strategy for initial versions
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
