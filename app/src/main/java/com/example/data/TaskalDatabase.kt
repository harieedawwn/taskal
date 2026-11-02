package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        TaskListEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        ReminderEntity::class,
        NoteEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        FocusSessionEntity::class,
        DashboardWidgetEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TaskalDatabase : RoomDatabase() {
    abstract fun dao(): TaskalDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Defined forward-compatible migration hook
            }
        }

        @Volatile
        private var INSTANCE: TaskalDatabase? = null

        fun getInstance(context: Context): TaskalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskalDatabase::class.java,
                    "taskal_local_vault.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
