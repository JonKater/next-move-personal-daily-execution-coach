package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Goal::class, Project::class, Action::class, DailyContext::class, DecisionLog::class],
    version = 2,
    exportSchema = true
)
abstract class NextMoveDatabase : RoomDatabase() {
    abstract fun dao(): NextMoveDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE actions ADD COLUMN deferredDateMs INTEGER")
                db.execSQL(
                    "ALTER TABLE daily_contexts ADD COLUMN availableContext TEXT NOT NULL DEFAULT 'Anywhere'"
                )
            }
        }
    }
}
