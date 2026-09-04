# Next Move MVP Stabilization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the generated Android prototype into a reproducible, honest, locally useful Next Move MVP.

**Architecture:** Keep the existing Room → repository → ViewModel → Compose structure. Move recommendation math and validation into pure Kotlin functions, keep multi-row state transitions inside Room transactions, and add only the persistence required by the current UI promises.

**Tech Stack:** Kotlin 2.2.10, Android Gradle Plugin 9.1.1, Gradle 9.3.1, Jetpack Compose Material 3, Room 2.7.0, JUnit 4, Robolectric, Compose UI Test.

**Spec:** `docs/superpowers/specs/2026-08-23-next-move-mvp.md`

## Global Constraints

- Preserve `minSdk = 24`, `targetSdk = 36`, and `compileSdk = 36.1`.
- Keep all user data local to Room for this MVP.
- Do not package a Gemini API key or advertise an AI capability.
- Use the exact context values `Anywhere`, `Computer`, `Phone`, and `Errands`.
- Treat `energyDemand` and `energyLevel` as integers from 1 through 3.
- Treat action duration as a positive integer number of minutes.
- Do not add navigation, authentication, networking, or dependency injection frameworks.

---

### Task 1: Restore a reproducible and honest build baseline

**Files:**
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `metadata.json`
- Modify: `README.md`
- Modify: `app/src/test/java/com/example/ExampleRobolectricTest.kt`
- Delete: `app/src/test/java/com/example/GreetingScreenshotTest.kt`
- Delete: `app/src/test/screenshots/greeting.png`

**Interfaces:**
- Consumes: the existing Android application module and `gradle-wrapper.properties` pinned to Gradle 9.3.1.
- Produces: `gradlew` and `gradlew.bat` entry points that build the project without manual signing edits; a test suite with no unresolved `Greeting` symbol.

- [ ] **Step 1: Add a failing baseline check**

Run from PowerShell:

```powershell
Test-Path .\gradlew.bat
Test-Path .\gradle\wrapper\gradle-wrapper.jar
```

Expected: both commands print `False` before the fix.

- [ ] **Step 2: Generate the complete Gradle wrapper**

Use Android Studio’s bundled Gradle or a temporary Gradle 9.3.1 installation:

```powershell
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
```

Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` exist, and the existing distribution URL remains `gradle-9.3.1-bin.zip`.

- [ ] **Step 3: Restore default debug signing**

Remove the custom `debugConfig` block and replace the current debug build type in `app/build.gradle.kts` with:

```kotlin
buildTypes {
  release {
    isCrunchPngs = false
    isMinifyEnabled = false
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    signingConfig = signingConfigs.getByName("release")
  }
  debug {}
}
```

Keep the release signing configuration unchanged.

- [ ] **Step 4: Remove unused AI and networking surface**

Remove the Google services and secrets plugin aliases from both Gradle plugin blocks. Remove the Firebase platform, Firebase AI, Firebase App Check, Retrofit, Moshi, OkHttp, logging interceptor, and their KSP codegen dependency from `app/build.gradle.kts`. Remove only the now-unreferenced aliases and versions from `gradle/libs.versions.toml`.

Set `metadata.json` to:

```json
{
  "name": "Next Move",
  "description": "Local-first Personal Daily Execution Coach that helps choose the next action.",
  "requestFramePermissions": [],
  "majorCapabilities": []
}
```

- [ ] **Step 5: Repair generated tests and documentation**

Change the Robolectric assertion to:

```kotlin
assertEquals("Next Move", appName)
```

Delete the unresolved `GreetingScreenshotTest.kt` and corrupted `greeting.png`. Rewrite the README run steps to require Android Studio, SDK 36.1, JDK 17 or newer, and `./gradlew`/`gradlew.bat`; remove `.env`, API-key, signing-line deletion, and Play upload-key instructions.

- [ ] **Step 6: Verify the baseline**

Run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Expected: all tasks pass and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 7: Commit**

```powershell
git add gradlew gradlew.bat gradle app/build.gradle.kts build.gradle.kts metadata.json README.md
git commit -m "build: restore reproducible Android baseline"
```

---

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

### Task 3: Let users add real actions

**Files:**
- Modify: `app/src/main/java/com/example/data/NextMoveDao.kt`
- Modify: `app/src/main/java/com/example/data/NextMoveRepository.kt`
- Modify: `app/src/main/java/com/example/ui/NextMoveViewModel.kt`
- Modify: `app/src/main/java/com/example/ui/screens/NextMoveScreen.kt`
- Create: `app/src/main/java/com/example/ui/screens/AddActionDialog.kt`
- Create: `app/src/test/java/com/example/data/InboxActionTest.kt`
- Create: `app/src/androidTest/java/com/example/AddActionFlowTest.kt`

**Interfaces:**
- Consumes: action name, duration, energy demand, urgency, and one of the four exact context values.
- Produces: `suspend fun addInboxAction(name: String, durationMins: Int, energyDemand: Int, urgency: Int, context: String): Long` and a visible Add Action flow.

- [ ] **Step 1: Write the failing Inbox transaction test**

The test must call `addInboxAction()` twice in an empty database and assert one goal, one Inbox project, and two actions with nonzero project IDs.

- [ ] **Step 2: Run the focused test and confirm failure**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.data.InboxActionTest"
```

Expected: failure because `addInboxAction()` does not exist.

- [ ] **Step 3: Add an atomic Inbox insertion path**

In the DAO, add exact lookups for an Inbox project and a goal named `Personal execution`, then add one `@Transaction` method that creates missing parents before inserting the action. Reject blank names, nonpositive durations, energy/urgency outside 1–3, and contexts outside the four allowed values with the exact `require` calls below before any database write.

```kotlin
@Query("SELECT * FROM goals WHERE name = 'Personal execution' LIMIT 1")
suspend fun getPersonalExecutionGoal(): Goal?

@Query("SELECT * FROM projects WHERE name = 'Inbox' AND status = 'active' LIMIT 1")
suspend fun getInboxProject(): Project?

@Transaction
suspend fun addInboxAction(
  name: String,
  durationMins: Int,
  energyDemand: Int,
  urgency: Int,
  context: String,
): Long {
  require(name.isNotBlank())
  require(durationMins > 0)
  require(energyDemand in 1..3)
  require(urgency in 1..3)
  require(context in setOf("Anywhere", "Computer", "Phone", "Errands"))

  val existingProject = getInboxProject()
  val projectId = if (existingProject != null) {
    existingProject.id
  } else {
    val goalId = getPersonalExecutionGoal()?.id
      ?: insertGoal(Goal(name = "Personal execution")).toInt()
    insertProject(Project(goalId = goalId, name = "Inbox")).toInt()
  }

  return insertAction(
    Action(
      projectId = projectId,
      name = name.trim(),
      estimatedDurationMins = durationMins,
      energyDemand = energyDemand,
      urgency = urgency,
      context = context,
    )
  )
}
```

Expose the DAO operation through the repository and call it from this ViewModel method:

```kotlin
fun addInboxAction(name: String, durationMins: Int, energyDemand: Int, urgency: Int, context: String) {
  viewModelScope.launch {
    repository.addInboxAction(name, durationMins, energyDemand, urgency, context)
  }
}
```

- [ ] **Step 4: Create the Add Action dialog**

`AddActionDialog` must expose:

```kotlin
@Composable
fun AddActionDialog(
  onDismiss: () -> Unit,
  onSave: (name: String, durationMins: Int, energyDemand: Int, urgency: Int, context: String) -> Unit,
)
```

Use a text field, positive integer duration field, 1–3 energy and urgency controls, and the exact context choices. Disable Save until the name is nonblank and duration parses to a positive integer; do not silently substitute 15 minutes.

- [ ] **Step 5: Replace the demo-only empty state**

Make “Add your first action” the primary empty-state action. Keep “Load demo actions” as a secondary explicitly labeled demo action. Add an “Add action” text button whenever a recommendation is visible.

- [ ] **Step 6: Verify the user flow**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.data.InboxActionTest"
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.AddActionFlowTest
```

Expected: the UI test creates a user-named action and sees it or another eligible action recommended without loading sample data.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main app/src/test app/src/androidTest
git commit -m "feat: add low-friction action intake"
```

---

### Task 4: Persist focus capture and evening review truthfully

**Files:**
- Modify: `app/src/main/java/com/example/data/Entities.kt`
- Modify: `app/src/main/java/com/example/data/NextMoveDao.kt`
- Modify: `app/src/main/java/com/example/data/NextMoveDatabase.kt`
- Modify: `app/src/main/java/com/example/NextMoveApplication.kt`
- Modify: `app/src/main/java/com/example/data/NextMoveRepository.kt`
- Modify: `app/src/main/java/com/example/ui/NextMoveViewModel.kt`
- Modify: `app/src/main/java/com/example/ui/screens/NextMoveScreen.kt`
- Create: `app/src/test/java/com/example/data/ReflectionPersistenceTest.kt`

**Interfaces:**
- Consumes: optional parked thought, completion event, completed-work text, blocker text, and current local-day timestamp.
- Produces: `ParkedThought`, `DailyReview`, `completeAction(actionId, thought)`, and `saveDailyReview(dateMs, completedText, blockedText)`.

- [ ] **Step 1: Write failing persistence tests**

Test that completing with a nonblank thought atomically marks the action completed, writes one decision log, and writes one parked thought. Test that saving a daily review and reopening the database returns both text fields unchanged.

- [ ] **Step 2: Run the tests and confirm failure**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.data.ReflectionPersistenceTest"
```

Expected: failure because the two entities and persistence methods do not exist.

- [ ] **Step 3: Add reflection entities and migration**

Add:

```kotlin
@Entity(tableName = "parked_thoughts")
data class ParkedThought(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val actionId: Int,
  val text: String,
  val capturedAtMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "daily_reviews")
data class DailyReview(
  @PrimaryKey val dateMs: Long,
  val completedText: String,
  val blockedText: String,
)
```

Set database version 3, include both entities, create both tables in `MIGRATION_2_3`, and register the migration.

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("CREATE TABLE IF NOT EXISTS parked_thoughts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, actionId INTEGER NOT NULL, text TEXT NOT NULL, capturedAtMs INTEGER NOT NULL)")
    db.execSQL("CREATE TABLE IF NOT EXISTS daily_reviews (dateMs INTEGER NOT NULL, completedText TEXT NOT NULL, blockedText TEXT NOT NULL, PRIMARY KEY(dateMs))")
  }
}
```

- [ ] **Step 4: Add transactional repository operations**

Add DAO transactions so completion, decision logging, and optional nonblank thought insertion are atomic. Use `@Insert(onConflict = OnConflictStrategy.REPLACE)` for `DailyReview` so saving the same local day updates rather than duplicates it.

```kotlin
@Insert
suspend fun insertParkedThought(thought: ParkedThought)

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertDailyReview(review: DailyReview)

@Transaction
suspend fun completeAction(actionId: Int, thought: String) {
  updateActionStatus(actionId, "completed")
  insertDecisionLog(DecisionLog(actionId = actionId, decision = "completed"))
  if (thought.isNotBlank()) {
    insertParkedThought(ParkedThought(actionId = actionId, text = thought.trim()))
  }
}
```

The repository `saveDailyReview` method must call `insertDailyReview(DailyReview(dateMs, completedText.trim(), blockedText.trim()))`.

- [ ] **Step 5: Wire honest UI labels and callbacks**

Rename “Start Focus Session” to “Start action” because no timer exists. Pass the thought to `completeAction`; when Stop is pressed, save a nonblank thought without completing. Change `EveningReviewDialog.onComplete` to `(String, String) -> Unit`, pass both field values, and persist them before closing.

- [ ] **Step 6: Verify persistence**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.data.ReflectionPersistenceTest"
```

Expected: all reflection and atomic-completion tests pass.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main app/src/test
git commit -m "feat: persist focus capture and daily review"
```

---

### Task 5: Harden split validation, layout, explanation, and end-to-end coverage

**Files:**
- Modify: `app/src/main/java/com/example/MainActivity.kt`
- Modify: `app/src/main/java/com/example/ui/screens/DailyCompassScreen.kt`
- Modify: `app/src/main/java/com/example/ui/screens/NextMoveScreen.kt`
- Create: `app/src/main/java/com/example/ui/ActionRecommendationUi.kt`
- Create: `app/src/test/java/com/example/ui/SplitActionValidationTest.kt`
- Create: `app/src/androidTest/java/com/example/NextMoveHappyPathTest.kt`

**Interfaces:**
- Consumes: `Modifier`, scored action, daily context, and split form values.
- Produces: `ActionRecommendationUi(action: Action, explanation: String)` and `validateSplit(firstName: String, firstMinutes: String, secondName: String, secondMinutes: String): Result<SplitActionInput>` with explicit validation errors.

- [ ] **Step 1: Write failing split-validation tests**

Create cases for blank names, nonnumeric durations, zero, negative values, and a valid split. The valid result must retain the original action’s project, context, energy, urgency, and strategic relevance.

- [ ] **Step 2: Implement explicit split validation**

Add:

```kotlin
data class SplitActionInput(val firstName: String, val firstMinutes: Int, val secondName: String, val secondMinutes: Int)

fun validateSplit(firstName: String, firstMinutes: String, secondName: String, secondMinutes: String): Result<SplitActionInput> {
  val first = firstMinutes.toIntOrNull()
  val second = secondMinutes.toIntOrNull()
  if (firstName.isBlank() || secondName.isBlank()) return Result.failure(IllegalArgumentException("Both action names are required"))
  if (first == null || second == null || first <= 0 || second <= 0) return Result.failure(IllegalArgumentException("Durations must be positive whole minutes"))
  return Result.success(SplitActionInput(firstName.trim(), first, secondName.trim(), second))
}
```

Show the error with `supportingText` and keep Split disabled until validation succeeds.

- [ ] **Step 3: Apply insets and scrolling correctly**

Give both screen composables a `modifier: Modifier = Modifier` parameter. In `MainActivity`, call them with `Modifier.padding(innerPadding)`. Replace fixed centered root columns with a `verticalScroll(rememberScrollState())` column using `imePadding()` and `navigationBarsPadding()`. Keep the recommendation card width bounded and remove the forced `aspectRatio(1f)` on constrained heights.

```kotlin
@Composable
private fun ExecutionScreenColumn(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .imePadding()
      .navigationBarsPadding()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    content = content,
  )
}
```

Apply the same modifier contract to `DailyCompassScreen`. In `MainActivity`, pass `Modifier.padding(innerPadding)` directly to the selected screen instead of creating an unused `let` value.

- [ ] **Step 4: Explain the recommendation**

Map the selected action and current context to one concise sentence such as `Fits 30 of 60 minutes • matches medium energy • Computer context`. Render it below the action metadata; do not claim learning or AI.

```kotlin
data class ActionRecommendationUi(val action: Action, val explanation: String)

fun Action.toRecommendationUi(context: DailyContext): ActionRecommendationUi {
  val energy = when (context.energyLevel) {
    1 -> "low"
    2 -> "medium"
    else -> "high"
  }
  return ActionRecommendationUi(
    action = this,
    explanation = "Fits $estimatedDurationMins of ${context.usableTimeMins} minutes • matches $energy energy • ${context.availableContext} context",
  )
}
```

- [ ] **Step 5: Add the real happy-path UI test**

Automate the acceptance flow through compass entry, action creation, recommendation, Not now, split, thought capture, completion, and evening review. Use stable Compose test tags for controls and assert persisted results through the repository after activity recreation.

- [ ] **Step 6: Run the complete release gate**

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest
```

Expected: every task passes; no unresolved symbol, stale app-name assertion, missing signing file, or clipping failure remains.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main app/src/test app/src/androidTest
git commit -m "test: cover the Next Move execution loop"
```

## Self-review record

- Spec coverage: each MVP requirement maps to at least one task; cloud, AI, and integrations remain excluded.
- Placeholder scan: no deferred implementation markers are present.
- Type consistency: `availableContext`, `deferredDateMs`, `addInboxAction`, `ParkedThought`, `DailyReview`, and `SplitActionInput` use the same names across producer and consumer tasks.
