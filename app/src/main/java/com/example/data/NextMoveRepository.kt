package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

fun scoreAction(action: Action, context: DailyContext): Action? {
    if (action.estimatedDurationMins <= 0) return null
    if (action.energyDemand !in 1..3 || context.energyLevel !in 1..3) return null
    if (action.deferredDateMs == context.dateMs) return null
    if (action.context != "Anywhere" && action.context != context.availableContext) return null

    var score = 0f
    score += if (action.estimatedDurationMins <= context.usableTimeMins) 5f else -10f
    score += if (action.energyDemand <= context.energyLevel) 5f else -5f
    score += action.urgency.coerceIn(1, 3) * 2f
    score += action.strategicRelevance.coerceIn(1, 3) * 1.5f
    return action.copy(score = score)
}

class NextMoveRepository(private val dao: NextMoveDao) {
    val allGoals: Flow<List<Goal>> = dao.getAllGoals()
    val allProjects: Flow<List<Project>> = dao.getAllProjects()
    val latestContext: Flow<DailyContext?> = dao.getLatestDailyContext()
    
    // Calculate score based on current context
    fun getScoredNextActions(): Flow<List<Action>> {
        return combine(dao.getActiveActions(), latestContext) { actions, context ->
            if (context == null) return@combine emptyList()
            
            actions.mapNotNull { action -> scoreAction(action, context) }
                .sortedByDescending { it.score }
        }
    }
    
    fun getTopRecommendation(): Flow<Action?> {
        return getScoredNextActions().map { actions ->
            actions.firstOrNull { it.status == "daily_win" } ?: actions.firstOrNull()
        }
    }

    suspend fun insertGoal(goal: Goal) = dao.insertGoal(goal)
    suspend fun insertProject(project: Project) = dao.insertProject(project)
    suspend fun insertAction(action: Action) = dao.insertAction(action)
    suspend fun updateActionStatus(id: Int, status: String) = dao.updateActionStatus(id, status)
    suspend fun saveDailyContext(context: DailyContext) = dao.insertDailyContext(context)

    suspend fun recordDecision(actionId: Int, decision: String, status: String) {
        dao.recordDecisionAndUpdateStatus(actionId, decision, status)
    }

    suspend fun deferActionForDate(actionId: Int, dateMs: Long) {
        dao.deferActionForDate(actionId, dateMs)
    }

    suspend fun splitAction(actionId: Int, newName1: String, newName2: String, dur1: Int, dur2: Int) {
        dao.splitActionAtomically(actionId, newName1, newName2, dur1, dur2)
    }
}
