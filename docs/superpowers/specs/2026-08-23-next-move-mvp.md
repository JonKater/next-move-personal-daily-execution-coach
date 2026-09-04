# Next Move MVP Stabilization Specification

## Product goal

Turn the generated Android prototype into a trustworthy local-first execution coach that answers one narrow question: “Given my available time, energy, and location/context, what should I do next?”

## MVP requirements

1. A fresh clone builds and runs with the checked-in Gradle wrapper and default Android debug signing.
2. Unit tests compile and cover recommendation ordering, daily deferral, splitting, completion, and daily rollover.
3. A user can add a real action without first creating a goal or project; the app creates a local Inbox project when needed.
4. Morning Compass collects only inputs used by recommendation logic: usable minutes, energy 1–3, and available context.
5. “Not now” immediately advances to another eligible action and makes the deferred action eligible again on the next local day.
6. Inactive-project actions and malformed actions are never recommended.
7. Split action names must be nonblank and durations must be positive; the original and both replacements change atomically.
8. Completing an action, capturing a parked thought, and saving an evening review persist the data they claim to save.
9. The deterministic scoring explanation is visible to the user. No AI capability is advertised until a server-backed implementation exists.
10. Core screens respect system insets, small displays, landscape, and font scaling without hiding primary controls.

## Non-goals for this stabilization

- Cloud sync, accounts, calendar imports, notifications, widgets, and social features.
- A Gemini integration or client-packaged API key.
- Automatic behavioral learning from review text.
- A goal/project management suite beyond the automatic Inbox project.

## Acceptance flow

1. Install on an empty emulator.
2. Set 60 available minutes, medium energy, and Computer context.
3. Add three actions with different durations, energy demands, urgency, and contexts.
4. See one recommendation plus a concise explanation of its fit.
5. Tap “Not now” and see a different action without losing the first action permanently.
6. Split an oversized action and see two valid replacements.
7. Start an action, capture a thought, complete it, and verify both records after process recreation.
8. Save an evening review and verify it after process recreation.
9. Advance the device date by one day and confirm the deferred action is eligible again.
10. Run all unit, lint, and connected UI tests successfully from the checked-in wrapper.
