package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Goal::class, Project::class, Action::class, DailyContext::class, DecisionLog::class],
    version = 1,
    exportSchema = false
)
abstract class NextMoveDatabase : RoomDatabase() {
    abstract fun dao(): NextMoveDao
}
