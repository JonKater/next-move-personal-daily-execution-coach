# SDD ledger — plan: docs/superpowers/plans/2026-08-23-next-move-mvp-stabilization.md

Branch: `codex/next-move-mvp-stabilization`
Baseline: `b8ab78b7d16699dec95f6259b87782eeb044aef8`
Spec: `docs/superpowers/specs/2026-08-23-next-move-mvp.md`

## Pre-flight scan

| Check | Producer / requirement | Consumer / verification | Finding and ruling |
|---|---|---|---|
| Task 1 self | Complete wrapper, default debug signing, honest dependencies/docs, repaired generated tests | `testDebugUnitTest lintDebug assembleDebug` and Task 1 commit | Conflict: Step 7's narrow `git add` omits changed/deleted test files from the Task 1 file list. Ruling: stage every Task 1 path, including deletions — keeps the task atomic — cost if wrong: the commit is slightly broader but remains wholly task-scoped and revertible. |
| Task 2 self | Schema v2, scoring, context, deferral, active-project filtering, transactions | Pure scoring plus Room/repository tests | Conflict: prose mentions `dailyContext.value!!`, while its required code uses a nullable safe lookup. Ruling: use the safe lookup — avoids a transient-state crash — cost if wrong: a very early tap can be ignored until context arrives. |
| Task 3 self | Transactional Inbox creation and Add Action UI | Inbox persistence and UI flow tests | Ambiguity: the plan does not say whether the public `addInboxAction` belongs only to the DAO or repository. Ruling: keep the transaction in DAO and expose a thin repository method — preserves architecture — cost if wrong: one small forwarding API. |
| Task 4 self | Parked thoughts, completion, and reviews persist | Reopen-database persistence tests | Conflict: Stop Focus must save a thought, but the listed interface exposes no standalone save operation. Ruling: add `saveParkedThought(actionId, thought)` through DAO/repository/ViewModel — required to satisfy the spec — cost if wrong: one additional focused persistence method. |
| Task 5 self | Split validation, responsive layout, explanation, happy path | Unit and connected UI tests | Conflict: pure `validateSplit` cannot prove that database replacements retain original action fields. Ruling: cover retention in Task 2's Room transaction test and keep Task 5 focused on form validation — puts behavior beside its implementation — cost if wrong: the test lands earlier than the prose suggests. |
| Tasks 1 → 2 | Wrapper and clean build baseline | Task 2 focused tests | Compatible; Task 2 depends on Task 1 tooling. |
| Tasks 1 → 3 | Wrapper and clean build baseline | Task 3 unit and UI tests | Compatible; Task 3 depends on Task 1 tooling. |
| Tasks 1 → 4 | Wrapper and clean build baseline | Task 4 persistence tests | Compatible; Task 4 depends on Task 1 tooling. |
| Tasks 1 → 5 | Wrapper and clean build baseline | Task 5 release gate | Compatible; Task 5 consumes the restored wrapper/signing. |
| Tasks 2 → 3 | Schema v2, context values, transactional DAO pattern | Inbox transaction and context input | Compatible; Task 3 must preserve v2 entities and exact context strings. |
| Tasks 2 → 4 | Database v2 and registered migration | Database v3 and `MIGRATION_2_3` | Compatible; migration chain must register both 1→2 and 2→3. |
| Tasks 2 → 5 | `availableContext`, daily deferral, atomic split | Recommendation explanation and split verification | Compatible with the split-test ruling above. |
| Tasks 3 → 4 | Add Action dialog and Inbox APIs | Focus/review persistence | Compatible; Task 4 does not change action intake contracts. |
| Tasks 3 → 5 | Add Action UI | End-to-end acceptance flow | Compatible; Task 3 must add stable test tags consumed by Task 5. |
| Tasks 4 → 5 | Durable focus and review callbacks | Happy-path process-recreation checks | Compatible; Task 5 verifies the persistence Task 4 produces. |

Ruling: Proceed despite an unavailable baseline test command — the original export has no Gradle wrapper or local Android SDK, and restoring that exact baseline is Task 1 — cost if wrong: Task 1 may surface additional environment or generated-project failures before feature work begins.

Task 1: complete (commits `7f6a071`, `9dcdf10`; re-review CLEAN). Configuration-only Gradle validation passed. SDK-dependent unit tests, lint, assembly, and APK generation remain unverified because no Android SDK is configured and the current Windows path is non-ASCII.
