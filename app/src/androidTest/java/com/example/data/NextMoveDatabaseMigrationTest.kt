package com.example.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NextMoveDatabaseMigrationTest {
    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NextMoveDatabase::class.java
    )

    @Test
    fun migrationFromV1AddsNullableDeferralAndAnywhereContextDefault() {
        val databaseName = "next-move-v1-to-v2"
        migrationTestHelper.createDatabase(databaseName, 1).apply {
            execSQL("INSERT INTO goals (id, name, color) VALUES (1, 'Goal', '#000000')")
            execSQL("INSERT INTO projects (id, goalId, name, status) VALUES (1, 1, 'Project', 'active')")
            execSQL(
                """
                INSERT INTO actions (
                    id, projectId, milestone, name, estimatedDurationMins, energyDemand,
                    urgency, context, strategicRelevance, status, score
                ) VALUES (1, 1, '', 'Action', 20, 2, 2, 'Anywhere', 2, 'ready', 0)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO daily_contexts (
                    id, dateMs, usableTimeMins, energyLevel, hasCommitments, dailyWinActionId
                ) VALUES (1, 1, 60, 2, 0, NULL)
                """.trimIndent()
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            NextMoveDatabase.MIGRATION_1_2
        )
        migrated.query("SELECT deferredDateMs FROM actions WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
        migrated.query("SELECT availableContext FROM daily_contexts WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Anywhere", cursor.getString(0))
        }
        migrated.close()
    }
}
