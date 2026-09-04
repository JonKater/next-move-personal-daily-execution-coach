package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Action
import com.example.data.DailyContext
import com.example.data.Goal
import com.example.data.NextMoveRepository
import com.example.data.Project
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class NextMoveViewModel(private val repository: NextMoveRepository) : ViewModel() {

    val goals: StateFlow<List<Goal>> = repository.allGoals.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val projects: StateFlow<List<Project>> = repository.allProjects.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val dailyContext: StateFlow<DailyContext?> = repository.latestContext.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val topAction: StateFlow<Action?> = repository.getTopRecommendation().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    fun submitDailyCompass(timeMins: Int, energy: Int, availableContext: String) {
        viewModelScope.launch {
            // Check if we already have one for today
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            repository.saveDailyContext(
                DailyContext(
                    dateMs = today,
                    usableTimeMins = timeMins,
                    energyLevel = energy,
                    hasCommitments = false,
                    dailyWinActionId = null,
                    availableContext = availableContext
                )
            )
        }
    }

    fun handleActionDecision(action: Action, decision: String) {
        viewModelScope.launch {
            when (decision) {
                "not_now" -> {
                    val dateMs = dailyContext.value?.dateMs ?: return@launch
                    repository.deferActionForDate(action.id, dateMs)
                }
                "completed" -> repository.recordDecision(action.id, "completed", "completed")
                else -> repository.recordDecision(action.id, decision, "ready")
            }
        }
    }

    fun handleTooBig(action: Action, part1Name: String, part2Name: String, part1Dur: Int, part2Dur: Int) {
        viewModelScope.launch {
            repository.splitAction(action.id, part1Name, part2Name, part1Dur, part2Dur)
        }
    }

    fun addAction(action: Action) {
        viewModelScope.launch {
            repository.insertAction(action)
        }
    }

    fun setupSampleData() {
        viewModelScope.launch {
            val goalId = repository.insertGoal(Goal(name = "Ship Next Move App", color = "#F59E0B")).toInt()
            val projId = repository.insertProject(Project(goalId = goalId, name = "V1 Android")).toInt()
            
            repository.insertAction(Action(
                projectId = projId,
                name = "Setup Room Database",
                estimatedDurationMins = 30,
                energyDemand = 2,
                urgency = 3,
                context = "Computer"
            ))
            repository.insertAction(Action(
                projectId = projId,
                name = "Design Home Screen",
                estimatedDurationMins = 45,
                energyDemand = 3,
                urgency = 2,
                context = "Computer"
            ))
            repository.insertAction(Action(
                projectId = projId,
                name = "Write App Icon Generation Prompt",
                estimatedDurationMins = 10,
                energyDemand = 1,
                urgency = 1,
                context = "Computer"
            ))
        }
    }
}

class NextMoveViewModelFactory(private val repository: NextMoveRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NextMoveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NextMoveViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
