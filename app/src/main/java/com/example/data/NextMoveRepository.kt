package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class NextMoveRepository(private val dao: NextMoveDao) {
    val allGoals: Flow<List<Goal>> = dao.getAllGoals()
    val allProjects: Flow<List<Project>> = dao.getAllProjects()
    val latestContext: Flow<DailyContext?> = dao.getLatestDailyContext()
    
    // Calculate score based on current context
    fun getScoredNextActions(): Flow<List<Action>> {
        return combine(dao.getActiveActions(), latestContext) { actions, context ->
            if (context == null) return@combine actions
            
            actions.map { action ->
                var score = 0f
                // 1. Fit with current time (penalize if longer than available)
                if (action.estimatedDurationMins <= context.usableTimeMins) {
                    score += 5f
                } else {
                    score -= 10f // Too long
                }
                
                // 2. Fit with energy (match demand with current level)
                if (action.energyDemand <= context.energyLevel) {
                    score += 5f
                } else {
                    score -= 5f // Too draining right now
                }
                
                // 3. Urgency
                score += action.urgency * 2f
                
                // 4. Strategic Alignment
                score += action.strategicRelevance * 1.5f
                
                action.copy(score = score)
            }.sortedByDescending { it.score }
        }
    }
    
    fun getTopRecommendation(): Flow<Action?> {
        return getScoredNextActions().map { actions ->
            // Try to find the daily win if selected
            val context = dao.getLatestDailyContext() // this needs refactoring, map is fine
            actions.firstOrNull { it.status == "daily_win" } ?: actions.firstOrNull()
        }
    }

    suspend fun insertGoal(goal: Goal) = dao.insertGoal(goal)
    suspend fun insertProject(project: Project) = dao.insertProject(project)
    suspend fun insertAction(action: Action) = dao.insertAction(action)
    suspend fun updateActionStatus(id: Int, status: String) = dao.updateActionStatus(id, status)
    suspend fun saveDailyContext(context: DailyContext) = dao.insertDailyContext(context)
    
    suspend fun recordDecision(actionId: Int, decision: String, status: String) {
        dao.insertDecisionLog(DecisionLog(actionId = actionId, decision = decision))
        dao.updateActionStatus(actionId, status)
    }

    suspend fun splitAction(actionId: Int, newName1: String, newName2: String, dur1: Int, dur2: Int) {
        val original = dao.getActionById(actionId) ?: return
        dao.updateActionStatus(actionId, "split")
        dao.insertDecisionLog(DecisionLog(actionId = actionId, decision = "too_big"))
        
        dao.insertAction(original.copy(id = 0, name = newName1, estimatedDurationMins = dur1, status = "ready"))
        dao.insertAction(original.copy(id = 0, name = newName2, estimatedDurationMins = dur2, status = "ready"))
    }
}
