package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String = "#3B82F6"
)

@Entity(
    tableName = "projects",
    foreignKeys = [ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)]
)
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val name: String,
    val status: String = "active"
)

@Entity(
    tableName = "actions",
    foreignKeys = [ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)]
)
data class Action(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val milestone: String = "",
    val name: String,
    val estimatedDurationMins: Int,
    val energyDemand: Int, // 1 (low) to 3 (high)
    val urgency: Int, // 1 (low) to 3 (high)
    val context: String,
    val strategicRelevance: Int = 2,
    val status: String = "ready", // ready, completed, rejected, parked, daily_win
    val score: Float = 0f,
    val deferredDateMs: Long? = null
)

@Entity(tableName = "daily_contexts")
data class DailyContext(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateMs: Long,
    val usableTimeMins: Int,
    val energyLevel: Int,
    val hasCommitments: Boolean,
    val dailyWinActionId: Int?,
    val availableContext: String = "Anywhere"
)

@Entity(tableName = "decision_logs")
data class DecisionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionId: Int,
    val decision: String, // "completed", "too_big", "wrong_context", "not_now"
    val timestamp: Long = System.currentTimeMillis()
)
