package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecommendationPolicyTest {
    @Test
    fun `wrong context is ineligible`() {
        val context = DailyContext(
            dateMs = 1L,
            usableTimeMins = 60,
            energyLevel = 2,
            hasCommitments = false,
            dailyWinActionId = null,
            availableContext = "Phone"
        )
        val action = Action(
            projectId = 1,
            name = "Desktop task",
            estimatedDurationMins = 20,
            energyDemand = 2,
            urgency = 3,
            context = "Computer"
        )

        assertNull(scoreAction(action, context))
    }

    @Test
    fun `anywhere action receives score from duration energy urgency and relevance`() {
        val context = context(usableTimeMins = 60, energyLevel = 2)
        val action = action(
            estimatedDurationMins = 30,
            energyDemand = 2,
            urgency = 3,
            strategicRelevance = 2,
            context = "Anywhere"
        )

        assertEquals(19f, scoreAction(action, context)?.score)
    }

    @Test
    fun `overlong and high-energy action receives penalties`() {
        val context = context(usableTimeMins = 30, energyLevel = 1)
        val action = action(
            estimatedDurationMins = 45,
            energyDemand = 3,
            urgency = 1,
            strategicRelevance = 1
        )

        assertEquals(-11.5f, scoreAction(action, context)?.score)
    }

    @Test
    fun `malformed duration is ineligible`() {
        assertNull(scoreAction(action(estimatedDurationMins = 0), context()))
    }

    @Test
    fun `invalid energy demand is ineligible`() {
        assertNull(scoreAction(action(energyDemand = 4), context()))
    }

    @Test
    fun `action deferred for current local day is ineligible`() {
        assertNull(scoreAction(action(deferredDateMs = 1L), context(dateMs = 1L)))
    }

    private fun context(
        dateMs: Long = 1L,
        usableTimeMins: Int = 60,
        energyLevel: Int = 2,
        availableContext: String = "Anywhere"
    ) = DailyContext(
        dateMs = dateMs,
        usableTimeMins = usableTimeMins,
        energyLevel = energyLevel,
        hasCommitments = false,
        dailyWinActionId = null,
        availableContext = availableContext
    )

    private fun action(
        estimatedDurationMins: Int = 20,
        energyDemand: Int = 2,
        urgency: Int = 2,
        strategicRelevance: Int = 2,
        context: String = "Anywhere",
        deferredDateMs: Long? = null
    ) = Action(
        projectId = 1,
        name = "Action",
        estimatedDurationMins = estimatedDurationMins,
        energyDemand = energyDemand,
        urgency = urgency,
        strategicRelevance = strategicRelevance,
        context = context,
        deferredDateMs = deferredDateMs
    )
}
