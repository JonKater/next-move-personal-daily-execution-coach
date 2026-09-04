package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NextMoveDao {
    // Goals
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    // Projects
    @Query("SELECT * FROM projects")
    fun getAllProjects(): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    // Actions
    @Query("""
        SELECT actions.* FROM actions
        INNER JOIN projects ON projects.id = actions.projectId
        WHERE projects.status = 'active'
          AND (actions.status = 'ready' OR actions.status = 'daily_win')
    """)
    fun getActiveActions(): Flow<List<Action>>

    @Query("SELECT * FROM actions WHERE id = :id")
    suspend fun getActionById(id: Int): Action?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: Action): Long
    
    @Update
    suspend fun updateAction(action: Action)

    @Query("UPDATE actions SET status = :status WHERE id = :id")
    suspend fun updateActionStatus(id: Int, status: String)

    @Query("UPDATE actions SET deferredDateMs = :dateMs WHERE id = :actionId")
    suspend fun setDeferredDate(actionId: Int, dateMs: Long)

    @Transaction
    suspend fun recordDecisionAndUpdateStatus(actionId: Int, decision: String, status: String) {
        insertDecisionLog(DecisionLog(actionId = actionId, decision = decision))
        updateActionStatus(actionId, status)
    }

    @Transaction
    suspend fun deferActionForDate(actionId: Int, dateMs: Long) {
        insertDecisionLog(DecisionLog(actionId = actionId, decision = "not_now"))
        setDeferredDate(actionId, dateMs)
    }

    @Transaction
    suspend fun splitActionAtomically(
        actionId: Int,
        newName1: String,
        newName2: String,
        dur1: Int,
        dur2: Int,
    ) {
        require(newName1.isNotBlank() && newName2.isNotBlank())
        require(dur1 > 0 && dur2 > 0)
        val original = requireNotNull(getActionById(actionId))
        updateActionStatus(actionId, "split")
        insertDecisionLog(DecisionLog(actionId = actionId, decision = "too_big"))
        insertAction(
            original.copy(
                id = 0,
                name = newName1.trim(),
                estimatedDurationMins = dur1,
                status = "ready",
                deferredDateMs = null
            )
        )
        insertAction(
            original.copy(
                id = 0,
                name = newName2.trim(),
                estimatedDurationMins = dur2,
                status = "ready",
                deferredDateMs = null
            )
        )
    }

    // Daily Context
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyContext(context: DailyContext): Long

    @Query("SELECT * FROM daily_contexts ORDER BY dateMs DESC LIMIT 1")
    fun getLatestDailyContext(): Flow<DailyContext?>
    
    // Decision Logs
    @Insert
    suspend fun insertDecisionLog(log: DecisionLog)
}
