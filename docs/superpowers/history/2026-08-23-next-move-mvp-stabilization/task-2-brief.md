### Task 2: Make recommendation and decision transitions correct

**Files:**
- Modify: `app/src/main/java/com/example/data/Entities.kt`
- Modify: `app/src/main/java/com/example/data/NextMoveDao.kt`
- Modify: `app/src/main/java/com/example/data/NextMoveDatabase.kt`
- Modify: `app/src/main/java/com/example/NextMoveApplication.kt`
- Modify: `app/src/main/java/com/example/data/NextMoveRepository.kt`
- Modify: `app/src/main/java/com/example/ui/NextMoveViewModel.kt`
- Modify: `app/src/main/java/com/example/ui/screens/DailyCompassScreen.kt`
- Modify: `app/src/main/java/com/example/ui/screens/NextMoveScreen.kt`
- Create: `app/src/test/java/com/example/data/RecommendationPolicyTest.kt`
- Create: `app/src/test/java/com/example/data/NextMoveRepositoryTest.kt`

**Interfaces:**
- Consumes: `Action`, `DailyContext`, `Project`, and the existing local-day timestamp stored in `DailyContext.dateMs`.
- Produces: `scoreAction(action: Action, context: DailyContext): Action?`; `deferActionForDate(actionId: Int, dateMs: Long)`; `splitActionAtomically(actionId: Int, newName1: String, newName2: String, dur1: Int, dur2: Int)`; context-aware candidate selection.

- [ ] **Step 1: Write failing pure scoring tests**

Create tests covering duration, energy, current context, inactive-project exclusion at the DAO layer, and malformed duration. The core assertion must include:

```kotlin
@Test
fun `wrong context is ineligible`() {
  val context = DailyContext(dateMs = 1L, usableTimeMins = 60, energyLevel = 2, hasCommitments = false, dailyWinActionId = null, availableContext = "Phone")
  val action = Action(projectId = 1, name = "Desktop task", estimatedDurationMins = 20, energyDemand = 2, urgency = 3, context = "Computer")
  assertNull(scoreAction(action, context))
}
```

- [ ] **Step 2: Run the focused test and confirm failure**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.data.RecommendationPolicyTest"
```

Expected: compilation fails because `availableContext` and `scoreAction` do not exist.

- [ ] **Step 3: Add context and daily-deferral schema fields**

Add to `Action`:

```kotlin
val deferredDateMs: Long? = null,
```

Add to `DailyContext`:

```kotlin
val availableContext: String = "Anywhere",
```

Set the database version to 2 and add:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE actions ADD COLUMN deferredDateMs INTEGER")
    db.execSQL("ALTER TABLE daily_contexts ADD COLUMN availableContext TEXT NOT NULL DEFAULT 'Anywhere'")
  }
}
```

Register `MIGRATION_1_2` in `NextMoveApplication` with `.addMigrations(NextMoveDatabase.MIGRATION_1_2)`.

- [ ] **Step 4: Implement the pure recommendation policy**

Add this top-level function in `NextMoveRepository.kt` and use it from `getScoredNextActions()`:

```kotlin
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
```

Remove the unused `dao.getLatestDailyContext()` local variable from `getTopRecommendation()`.

- [ ] **Step 5: Filter inactive projects and make writes atomic**

Replace the active-action query with:

```kotlin
@Query("""
  SELECT actions.* FROM actions
  INNER JOIN projects ON projects.id = actions.projectId
  WHERE projects.status = 'active'
    AND (actions.status = 'ready' OR actions.status = 'daily_win')
""")
fun getActiveActions(): Flow<List<Action>>
```

Add DAO transaction methods that defer an action for a local date and that update the original, write the decision log, and insert both split actions in one `@Transaction`. Make `NextMoveRepository.recordDecision()` and `splitAction()` call only those transactional DAO methods.

```kotlin
@Query("UPDATE actions SET deferredDateMs = :dateMs WHERE id = :actionId")
suspend fun setDeferredDate(actionId: Int, dateMs: Long)

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
  insertAction(original.copy(id = 0, name = newName1.trim(), estimatedDurationMins = dur1, status = "ready", deferredDateMs = null))
  insertAction(original.copy(id = 0, name = newName2.trim(), estimatedDurationMins = dur2, status = "ready", deferredDateMs = null))
}
```

- [ ] **Step 6: Wire the UI to real daily deferral and current context**

Change the button callback to:

```kotlin
onClick = { viewModel.handleActionDecision(action, "not_now") }
```

Map `not_now` to `deferActionForDate(action.id, dailyContext.value!!.dateMs)` rather than changing status to `parked`. Replace the commitments switch in Morning Compass with a Material 3 single-choice control for `Anywhere`, `Computer`, `Phone`, and `Errands`, and persist that value as `availableContext`.

```kotlin
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
```

Change `submitDailyCompass` to accept `availableContext: String`, set the legacy `hasCommitments` column to `false`, and assign `availableContext = availableContext` in the entity constructor.

- [ ] **Step 7: Verify recommendation behavior**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.data.RecommendationPolicyTest" --tests "com.example.data.NextMoveRepositoryTest"
```

Expected: tests prove a deferred action disappears for the current `dateMs`, returns for a different `dateMs`, and inactive-project actions never enter the candidate list.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main app/src/test
git commit -m "fix: make next-action decisions advance correctly"
```

---

