package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NextMoveRepositoryTest {
    private lateinit var database: NextMoveDatabase
    private lateinit var dao: NextMoveDao
    private lateinit var repository: NextMoveRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextMoveDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.dao()
        repository = NextMoveRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `daily deferral hides action today and restores it tomorrow`() = runBlocking {
        val projectId = insertProject(status = "active")
        val actionId = repository.insertAction(action(projectId = projectId)).toInt()
        repository.saveDailyContext(context(dateMs = 1L))

        assertEquals(listOf(actionId), repository.getScoredNextActions().first().map { it.id })

        repository.deferActionForDate(actionId, 1L)
        assertTrue(repository.getScoredNextActions().first().isEmpty())

        repository.saveDailyContext(context(dateMs = 2L))
        assertEquals(listOf(actionId), repository.getScoredNextActions().first().map { it.id })
    }

    @Test
    fun `actions are not candidates until daily context exists`() = runBlocking {
        val projectId = insertProject(status = "active")
        repository.insertAction(action(projectId = projectId))

        assertTrue(repository.getScoredNextActions().first().isEmpty())
    }

    @Test
    fun `inactive project actions never enter recommendation candidates`() = runBlocking {
        val activeProjectId = insertProject(status = "active")
        val inactiveProjectId = insertProject(status = "inactive")
        val activeActionId = repository.insertAction(action(projectId = activeProjectId)).toInt()
        repository.insertAction(action(projectId = inactiveProjectId, name = "Inactive action"))
        repository.saveDailyContext(context())

        assertEquals(listOf(activeActionId), repository.getScoredNextActions().first().map { it.id })
    }

    @Test
    fun `equal-score recommendations use ascending action ID as a tie breaker`() = runBlocking {
        val projectId = insertProject(status = "active")
        val firstId = repository.insertAction(action(projectId = projectId, name = "First")).toInt()
        val secondId = repository.insertAction(action(projectId = projectId, name = "Second")).toInt()
        repository.saveDailyContext(context())

        assertEquals(
            listOf(firstId, secondId),
            repository.getScoredNextActions().first().map { it.id }
        )
    }

    @Test
    fun `split atomically retains original action fields on both replacements`() = runBlocking {
        val projectId = insertProject(status = "active")
        val original = action(
            projectId = projectId,
            milestone = "Launch",
            energyDemand = 3,
            urgency = 1,
            strategicRelevance = 3,
            context = "Computer",
            deferredDateMs = 5L,
            score = 12.5f
        )
        val originalId = repository.insertAction(original).toInt()

        repository.splitAction(originalId, " First half ", " Second half ", 10, 15)

        assertEquals("split", dao.getActionById(originalId)?.status)
        val replacements = dao.getActiveActions().first()
        assertEquals(listOf("First half", "Second half"), replacements.map { it.name })
        assertEquals(listOf(10, 15), replacements.map { it.estimatedDurationMins })
        replacements.forEach { replacement ->
            assertEquals(original.projectId, replacement.projectId)
            assertEquals(original.milestone, replacement.milestone)
            assertEquals(original.energyDemand, replacement.energyDemand)
            assertEquals(original.urgency, replacement.urgency)
            assertEquals(original.context, replacement.context)
            assertEquals(original.strategicRelevance, replacement.strategicRelevance)
            assertEquals(original.score, replacement.score)
            assertEquals("ready", replacement.status)
            assertEquals(null, replacement.deferredDateMs)
        }
    }

    private suspend fun insertProject(status: String): Int {
        val goalId = repository.insertGoal(Goal(name = "Goal")).toInt()
        return repository.insertProject(Project(goalId = goalId, name = "Project", status = status)).toInt()
    }

    private fun action(
        projectId: Int,
        name: String = "Action",
        milestone: String = "",
        energyDemand: Int = 2,
        urgency: Int = 2,
        strategicRelevance: Int = 2,
        context: String = "Anywhere",
        deferredDateMs: Long? = null,
        score: Float = 0f
    ) = Action(
        projectId = projectId,
        milestone = milestone,
        name = name,
        estimatedDurationMins = 20,
        energyDemand = energyDemand,
        urgency = urgency,
        context = context,
        strategicRelevance = strategicRelevance,
        deferredDateMs = deferredDateMs,
        score = score
    )

    private fun context(dateMs: Long = 1L) = DailyContext(
        dateMs = dateMs,
        usableTimeMins = 60,
        energyLevel = 2,
        hasCommitments = false,
        dailyWinActionId = null,
        availableContext = "Anywhere"
    )
}
