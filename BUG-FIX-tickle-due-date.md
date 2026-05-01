# Bug: Tickle due date set to "today" instead of next occurrence

> **Status: RESOLVED — 2026-05-01.** Both platforms patched; iOS 69 / Android 82 unit tests pass. The fix was implemented as a small refactor (extract the decision into a `TickleScheduler` helper) rather than the inline conditional originally proposed, so the same rule applies to create and edit on both platforms and is independently testable.
>
> Shipping commits:
> - `2e46185` — `fix(ios): tickle nextDueDate now advances one interval instead of firing today`
> - `1c7f9d6` — `fix(android): preserve tickle nextDueDate on note-only edits, recompute on schedule changes`
> - `4212bdc` — `test(android): add Robolectric + Compose UI test + Hilt testing infra`
> - `b042d82` — `test(android): add TickleEditScreen wiring tests using mockk`
>
> Notable deviations from the plan below:
> - The "open design question" was answered in favor of the smarter rule (`startDate > now ? startDate : nextDueDate(...)`); this also means editing an unchanged future-dated tickle on iOS is now a no-op rather than a regression.
> - All new test cases live at the helper level (`TickleSchedulerTests.initialNextDueDate*`, `TickleSchedulerTest.nextDueDateForSave*`, `TickleReminderTests`) PLUS three end-to-end Android Compose tests (`TickleEditScreenTest`) using a newly-added Robolectric + Compose UI test + Hilt + mockk stack — covers both the decision logic AND the wiring.
> - iOS screen-level tests are not included; the iOS `ios/CLAUDE.md` "no third-party dependencies in shipping code" rule was clarified in the same series to allow test-only deps (e.g. ViewInspector) for any future iOS UI test work.

## Symptom

Creating a Weekly tickle on a Thursday fires the notification immediately
instead of next Thursday. Same for Monthly — it treats today as the due date
rather than looking ahead one calendar month.

## Root cause analysis

The `nextDueDate(from:frequency:customDays:)` helper itself is correct on both
platforms — it adds the right interval to a given date. The bug is in **how
it's called** (or not called) when a tickle is saved.

### iOS — broken on both create AND edit

**`ios/Sources/SIT/Models/TickleReminder.swift` (line 64)**

The initializer copies `startDate` directly into `nextDueDate` without applying
the frequency:

```swift
self.startDate = startDate
self.nextDueDate = startDate   // ← never adds the interval
```

**`ios/Sources/SIT/Views/Tickle/TickleEditView.swift` (`save()`)**

- Edit branch (line ~121): `r.nextDueDate = startDate` — same problem,
  written explicitly.
- Create branch (lines ~127–134): builds a `TickleReminder(...)` and inherits
  the buggy default from the model init.

**Why it fires immediately**

`TickleScheduler.scheduleNotification` (lines 34–37) checks "today at 9am":

```swift
let todayAt9 = cal.date(bySettingHour: 9, minute: 0, second: 0, of: reminder.nextDueDate) ?? reminder.nextDueDate
if todayAt9 <= Date() {
    trigger = UNTimeIntervalNotificationTrigger(timeInterval: 5, repeats: false)
}
```

Creating a Weekly tickle Thursday afternoon → `nextDueDate = Thu 3pm`,
`todayAt9 = Thu 9am`, condition is true → fires in 5 seconds. Matches the
symptom exactly.

### Android — create is correct, edit is broken (different bug, same family)

**`android/app/src/main/java/com/xaymaca/sit/ui/tickle/TickleEditScreen.kt`
(lines 117–121)**

```kotlin
val nextDue = TickleScheduler.nextDueDate(
    from = startDate,
    frequency = selectedFrequency.name,
    customDays = if (selectedFrequency == TickleFrequency.CUSTOM) customIntervalDays else null
)
```

For a **new** tickle: `startDate` is `System.currentTimeMillis()` (line 56,
no DatePicker exists in the form), so `nextDue = now + interval`. ✅

For an **edit**: `startDate` is reloaded from `reminder.startDate` — the
*original creation date* (lines 71–87). Then `nextDue = original_startDate +
interval`, which is almost always in the past. Two consequences:

1. The alarm fires immediately on save.
2. Any `nextDueDate` advancement from prior `markComplete` calls is wiped out.

So editing an existing Android tickle — even just to fix a typo in the note —
resets it to overdue.

### Why the unit tests didn't catch this

`TickleSchedulerTests.swift` and `TickleSchedulerTest.kt` only exercise the
`nextDueDate(from:frequency:)` math in isolation. The math is fine. Neither
test suite covers the save flow in the edit screen, which is where the bug
lives.

## Fix plan

Recommend two PRs (iOS first — it's the worse bug) so each can be tested and
shipped independently.

### iOS fix

**1. `ios/Sources/SIT/Models/TickleReminder.swift`**

In the initializer, replace the buggy line with a call to the scheduler:

```swift
self.nextDueDate = TickleScheduler.nextDueDate(
    from: startDate,
    frequency: frequency,
    customDays: customIntervalDays
)
```

**2. `ios/Sources/SIT/Views/Tickle/TickleEditView.swift`, `save()`**

In the edit branch (around line 121), replace `r.nextDueDate = startDate`
with:

```swift
r.nextDueDate = TickleScheduler.nextDueDate(
    from: startDate,
    frequency: frequency,
    customDays: intervalDays
)
```

The create branch needs no further change once the model init is fixed.

### Android fix

**`android/app/src/main/java/com/xaymaca/sit/ui/tickle/TickleEditScreen.kt`**

The save handler unconditionally recomputes `nextDue` from `startDate`, which
destroys edits to existing reminders. Change it so we only recompute
`nextDueDate` for a new tickle or when the frequency / custom interval
actually changed; otherwise preserve the loaded reminder's existing
`nextDueDate`. Sketch:

```kotlin
val originalReminder = if (tickleId != null) tickleViewModel.getReminderById(tickleId) else null
val newCustomDays = if (selectedFrequency == TickleFrequency.CUSTOM) customIntervalDays else null
val frequencyChanged = originalReminder != null && (
    originalReminder.frequency != selectedFrequency.name ||
    originalReminder.customIntervalDays != newCustomDays
)

val nextDue = if (originalReminder == null || frequencyChanged) {
    // New tickle, or schedule changed — recompute from now
    TickleScheduler.nextDueDate(
        from = System.currentTimeMillis(),
        frequency = selectedFrequency.name,
        customDays = newCustomDays
    )
} else {
    originalReminder.nextDueDate
}
```

Note `from = System.currentTimeMillis()` for the new/changed case — for a
new tickle or a frequency change, "next" should mean "one interval from
now," not "one interval from the original `startDate`."

## New tests to add

These would have caught both bugs:

- **iOS** — `TickleEditViewTests.testCreateWithWeeklySetsNextDueDateOneWeekAhead`
- **iOS** — `TickleEditViewTests.testEditChangingFrequencyRecomputesNextDueDate`
- **iOS** — `TickleReminderTests.testInitWithWeeklySetsNextDueDateOneWeekFromStartDate`
- **Android** — `TickleEditScreenTest.editingNoteOnly_preservesNextDueDate`
- **Android** — `TickleEditScreenTest.changingFrequency_recomputesFromNow`
- **Android** — `TickleEditScreenTest.creatingWeekly_setsNextDueDateOneWeekAhead`

## Manual verification

After patching, smoke test on each platform:

1. Create a Weekly tickle → confirm `nextDueDate` is ~7 days out and no
   notification appears in the next minute.
2. Create a Monthly tickle → confirm `nextDueDate` is one calendar month out.
3. Edit an existing tickle's note only → confirm `nextDueDate` is unchanged.
4. Edit an existing tickle to change Weekly → Monthly → confirm `nextDueDate`
   is now ~1 month from now.

## Open design question (decide before merging iOS PR) — DECIDED: smarter rule

On iOS the user can pick `startDate` via a DatePicker. With this fix, picking
"Jan 15 2027 + Weekly" would yield a first reminder of Jan 22 2027, not Jan 15
— because we now always add one interval to the chosen `startDate`.

**Resolution:** Adopted the smarter rule below and extracted it into
`TickleScheduler.initialNextDueDate(from:frequency:customDays:now:)`, which
both `TickleReminder.init` and `TickleEditView.save()` now call. This both
fixes the today-fires-today bug and honors a user-picked future date as the
literal first occurrence.

If you'd rather treat a user-picked future `startDate` as the *literal first
occurrence*, the rule becomes:

```swift
nextDueDate = startDate > Date()
    ? startDate
    : TickleScheduler.nextDueDate(from: startDate, frequency: frequency, customDays: customDays)
```

This preserves the "today → next interval" behavior the bug report asks for,
while still honoring an explicitly future-picked first-occurrence date.

## Files touched (summary)

- `ios/Sources/SIT/Models/TickleReminder.swift`
- `ios/Sources/SIT/Views/Tickle/TickleEditView.swift`
- `android/app/src/main/java/com/xaymaca/sit/ui/tickle/TickleEditScreen.kt`
- `ios/Tests/SITTests/TickleSchedulerTests.swift` (add edit-flow tests)
- `android/app/src/test/java/com/xaymaca/sit/TickleSchedulerTest.kt` (add edit-flow tests)
