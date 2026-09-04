package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    @Query("SELECT * FROM actions WHERE status = 'ready' OR status = 'daily_win'")
    fun getActiveActions(): Flow<List<Action>>

    @Query("SELECT * FROM actions WHERE id = :id")
    suspend fun getActionById(id: Int): Action?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: Action): Long
    
    @Update
    suspend fun updateAction(action: Action)

    @Query("UPDATE actions SET status = :status WHERE id = :id")
    suspend fun updateActionStatus(id: Int, status: String)

    // Daily Context
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyContext(context: DailyContext): Long

    @Query("SELECT * FROM daily_contexts ORDER BY dateMs DESC LIMIT 1")
    fun getLatestDailyContext(): Flow<DailyContext?>
    
    // Decision Logs
    @Insert
    suspend fun insertDecisionLog(log: DecisionLog)
}
