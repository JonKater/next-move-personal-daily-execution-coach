# Task 2 report — recommendation and decision transitions

Status: `DONE_WITH_CONCERNS`

Commit: `1d0495a fix: make next-action decisions advance correctly`

## Files committed

- `app/src/main/java/com/example/NextMoveApplication.kt`
- `app/src/main/java/com/example/data/Entities.kt`
- `app/src/main/java/com/example/data/NextMoveDao.kt`
- `app/src/main/java/com/example/data/NextMoveDatabase.kt`
- `app/src/main/java/com/example/data/NextMoveRepository.kt`
- `app/src/main/java/com/example/ui/NextMoveViewModel.kt`
- `app/src/main/java/com/example/ui/screens/DailyCompassScreen.kt`
- `app/src/main/java/com/example/ui/screens/NextMoveScreen.kt`
- `app/src/test/java/com/example/data/NextMoveRepositoryTest.kt`
- `app/src/test/java/com/example/data/RecommendationPolicyTest.kt`

## RED evidence

The tests were written before their production implementation.

1. `.\gradlew.bat testDebugUnitTest --tests "com.example.data.RecommendationPolicyTest"`
   - Did not reach test compilation: the Gradle wrapper download was denied by the sandbox (`java.net.SocketException: Permission denied: getsockopt`).
2. Elevated retry of the same command
   - Reached Android Gradle Plugin configuration and failed at the known non-ASCII Windows worktree path check.
3. `cmd.exe /c "gradlew.bat -Pandroid.overridePathCheck=true testDebugUnitTest --tests com.example.data.RecommendationPolicyTest --tests com.example.data.NextMoveRepositoryTest"`
   - Reached project configuration with the temporary path override, then failed before compilation because no Android SDK is configured: `SDK location not found`.

The third attempt is the closest available RED result. It proves neither source compilation nor the intended missing-symbol failure because the machine lacks an SDK; this is an environment limitation, not a passing test result.

## GREEN evidence

- Implemented the policy and data schema after the tests:
  - `scoreAction` rejects zero/invalid duration or energy, a context mismatch, and current-local-day deferral; it applies the specified deterministic scoring. No action is a recommendation candidate until a Daily Context exists, so every recommendation is policy-filtered.
  - Room v2 adds `deferredDateMs` and `availableContext`, with registered `MIGRATION_1_2`.
  - Candidate SQL joins active projects only.
  - Deferral, ordinary decisions, and split replacement writes are DAO transactions. Split retains the original action fields while resetting the two replacement names, durations, status, and daily deferral as specified.
  - Morning Compass stores one of the exact contexts `Anywhere`, `Computer`, `Phone`, or `Errands`; Not now uses the safe nullable daily-context lookup and defers for the current local day.
- `cmd.exe /c "gradlew.bat -Pandroid.overridePathCheck=true help"`
  - `BUILD SUCCESSFUL`.
- `cmd.exe /c "gradlew.bat -Pandroid.overridePathCheck=true tasks --all"`
  - `BUILD SUCCESSFUL`; Android tasks resolved successfully at configuration time.
- `git diff --check`
  - Passed with no whitespace errors before commit.
- `git diff --cached --check`
  - Passed with no whitespace errors immediately before commit.

The focused post-implementation command was run again:

`cmd.exe /c "gradlew.bat -Pandroid.overridePathCheck=true testDebugUnitTest --tests com.example.data.RecommendationPolicyTest --tests com.example.data.NextMoveRepositoryTest"`

It again stopped before compilation with `SDK location not found`. Therefore no Android unit test is reported as executed or passing.

## Self-review against Task 2 and the MVP spec

- Pure tests cover duration fit/penalty, energy fit/penalty, exact current-context exclusion, invalid duration/energy, and same-day deferral.
- Room-backed repository tests cover no-context exclusion, same-day disappearance/next-day return, inactive-project candidate exclusion, and split-replacement field retention.
- The migration is version `1 -> 2`, contains the exact required SQL defaults, and is registered on database construction.
- No `dailyContext.value!!` was introduced; an early Not now tap safely returns while context loads.
- `not_now` no longer changes the action status to `parked`; it appends the required decision log and persists the local-day deferral.
- The app remains local-only: no networking, Firebase, Gemini/API-key, dependency, SDK-level, or signing configuration changes were made.
- The committed scope is limited to the ten Task 2 production/test files listed above.

## Concerns / follow-up required

An Android SDK (and, on this Windows path, either an ASCII worktree location or the temporary `android.overridePathCheck=true` override) is required to compile and execute the new tests. Run the focused unit-test command, then the full unit/lint/assembly suite, in a configured Android environment before release or acceptance.

## Review round 1 fixes

Commit: `58988d0 fix: stabilize recommendation ordering and migration tests`

### Changed files

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/schemas/com.example.data.NextMoveDatabase/1.json`
- `app/src/androidTest/java/com/example/data/NextMoveDatabaseMigrationTest.kt`
- `app/src/main/java/com/example/data/NextMoveDatabase.kt`
- `app/src/main/java/com/example/data/NextMoveRepository.kt`
- `app/src/test/java/com/example/data/NextMoveRepositoryTest.kt`

### RED attempts

The equal-score repository test and the v1-to-v2 `MigrationTestHelper` instrumentation test were written before the comparator/configuration changes.

- `cmd.exe /c "gradlew.bat -Pandroid.overridePathCheck=true testDebugUnitTest --tests com.example.data.NextMoveRepositoryTest"`
  - Did not reach compilation because `SDK location not found`.
- `cmd.exe /c "gradlew.bat -Pandroid.overridePathCheck=true connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.data.NextMoveDatabaseMigrationTest"`
  - Did not reach Android-test compilation because `SDK location not found`.

The closest executable RED evidence is therefore the SDK blocker. Static inspection of the pre-fix repository confirmed that score-only sorting left equal-score ordering dependent on the unordered DAO query.

### GREEN attempts and results

- `Get-Content -Raw app\\schemas\\com.example.data.NextMoveDatabase\\1.json | ConvertFrom-Json | Out-Null`
  - Passed; the v1 schema fixture is valid JSON.
- `cmd.exe /c "gradlew.bat -Pandroid.overridePathCheck=true help"`
  - `BUILD SUCCESSFUL` after the final configuration fix.
- `cmd.exe /c "gradlew.bat -Pandroid.overridePathCheck=true :app:dependencies --configuration debugAndroidTestRuntimeClasspath"`
  - Resolved the Room test metadata into the local Gradle cache; no configuration failure was reported.
- `git diff --check` and `git diff --cached --check`
  - Passed with no whitespace errors.
- Post-fix focused unit and instrumentation commands were rerun and both again stopped at `SDK location not found` before source compilation or execution. No SDK-dependent test is claimed as passing.

### Self-review

- Equal-scored actions now use `id` ascending as an explicit deterministic secondary key after score descending. The Room-backed repository test inserts two equal-score actions and asserts their exact ID order.
- The migration test uses the official Room `MigrationTestHelper` as a JUnit rule, creates an actual v1 database from the checked-in v1 schema fixture, inserts legacy action and daily-context rows, applies `MIGRATION_1_2`, and checks nullable `deferredDateMs` plus default `availableContext = "Anywhere"`.
- Room schema export is enabled, KSP exports schemas to `app/schemas`, that directory is included in Android-test assets, and `androidx.room:room-testing` is limited to `androidTestImplementation`.
- The production delta is limited to deterministic ordering and schema-export enablement; SDK versions, local-only product behavior, signing, and the one-off path override are unchanged.

### Remaining concerns

The Android SDK is still absent, so neither the new Room-backed unit test nor the instrumentation migration test compiled or ran locally. A configured Android environment must execute both focused commands before this review round can be regarded as fully verified.
