import XCTest
import SwiftData
import MessageUI
@testable import Ticklr

/// TIC-82: sending a text to a due contact auto-completes their tickle, with Undo.
/// Covers the completion decision (`ComposeView.isSuccessfulSend`), the guarded
/// completion path (`TickleScheduler.completeAfterSend`), and undo restoration.
@MainActor
final class TickleSendCompletionTests: XCTestCase {

    private var container: ModelContainer!
    private var context: ModelContext!
    private var priorNotificationsEnabled: Any?

    override func setUp() async throws {
        // `markComplete` (exercised below) calls `scheduleNotification`, which
        // spawns an async @MainActor Task that touches the model. That Task would
        // outlive the test and fault on the destroyed in-memory container at
        // teardown. Disabling the in-app notifications pref makes
        // `scheduleNotification` short-circuit *before* spawning any Task, so the
        // synchronous completion logic under test runs in isolation.
        priorNotificationsEnabled = UserDefaults.standard.object(forKey: "tickleNotificationsEnabled")
        UserDefaults.standard.set(false, forKey: "tickleNotificationsEnabled")

        container = try ModelContainer(
            for: TickleReminder.self, Contact.self, ContactGroup.self, MessageTemplate.self,
            configurations: .init(isStoredInMemoryOnly: true)
        )
        context = container.mainContext
    }

    override func tearDown() {
        if let prior = priorNotificationsEnabled {
            UserDefaults.standard.set(prior, forKey: "tickleNotificationsEnabled")
        } else {
            UserDefaults.standard.removeObject(forKey: "tickleNotificationsEnabled")
        }
        container = nil
        context = nil
    }

    // MARK: - Helpers

    /// Inserts a monthly reminder for a fresh contact and forces it due by
    /// back-dating `nextDueDate` a day into the past.
    @discardableResult
    private func makeDueReminder(status: TickleStatus = .active) -> TickleReminder {
        let contact = Contact(firstName: "Alice", lastName: "Doe", phoneNumbers: ["+15551234567"])
        context.insert(contact)
        let reminder = TickleReminder(contact: contact, note: "ping", frequency: .monthly)
        reminder.nextDueDate = Date().addingTimeInterval(-86_400) // yesterday → due
        reminder.status = status
        context.insert(reminder)
        try? context.save()
        return reminder
    }

    // MARK: - Completion decision (only .sent completes)

    func testOnlySentResultCompletesTickle() {
        XCTAssertTrue(ComposeView.isSuccessfulSend(.sent))
        XCTAssertFalse(ComposeView.isSuccessfulSend(.cancelled),
                       "A cancelled composer must not complete the tickle")
        XCTAssertFalse(ComposeView.isSuccessfulSend(.failed),
                       "A failed send must not complete the tickle")
        XCTAssertFalse(ComposeView.isSuccessfulSend(nil))
    }

    // MARK: - completeAfterSend: happy path (sent + due)

    func testCompleteAfterSendCompletesDueRecurringReminder() {
        let reminder = makeDueReminder()
        let priorDue = reminder.nextDueDate

        let snapshot = TickleScheduler.completeAfterSend(reminder: reminder, context: context)

        XCTAssertNotNil(snapshot, "A due reminder should complete on send")
        // Recurring reminder: stays active, advances to a future due date, stamps completion.
        XCTAssertEqual(reminder.status, .active)
        XCTAssertGreaterThan(reminder.nextDueDate, priorDue, "nextDueDate should advance")
        XCTAssertNotNil(reminder.lastCompletedDate)
        XCTAssertGreaterThan(reminder.nextDueDate, Date(), "advanced due date should be in the future")
    }

    func testCompleteAfterSendCompletesDueSnoozedReminderThatIsPastDue() {
        // A snoozed reminder whose snooze window has elapsed is still "due".
        let reminder = makeDueReminder(status: .snoozed)
        let snapshot = TickleScheduler.completeAfterSend(reminder: reminder, context: context)
        XCTAssertNotNil(snapshot, "A past-due snoozed reminder should complete on send")
        XCTAssertEqual(reminder.status, .active)
    }

    func testCompleteAfterSendCompletesOneTimeReminder() {
        let contact = Contact(firstName: "Bob", lastName: "Roe", phoneNumbers: ["+15550000000"])
        context.insert(contact)
        let reminder = TickleReminder(contact: contact, note: "once", frequency: .oneTime,
                                      startDate: Date().addingTimeInterval(-86_400))
        context.insert(reminder)
        try? context.save()

        let snapshot = TickleScheduler.completeAfterSend(reminder: reminder, context: context)
        XCTAssertNotNil(snapshot)
        XCTAssertEqual(reminder.status, .completed, "A one-time reminder is marked completed")
        XCTAssertNotNil(reminder.lastCompletedDate)
    }

    // MARK: - completeAfterSend: guards (not due / already completed / deleted)

    func testCompleteAfterSendDoesNothingWhenNotYetDue() {
        let reminder = makeDueReminder()
        reminder.nextDueDate = Date().addingTimeInterval(7 * 86_400) // future
        try? context.save()

        let snapshot = TickleScheduler.completeAfterSend(reminder: reminder, context: context)
        XCTAssertNil(snapshot, "An upcoming (not-yet-due) reminder should not complete on send")
        XCTAssertNil(reminder.lastCompletedDate)
    }

    func testCompleteAfterSendDoesNothingWhenAlreadyCompletedMeanwhile() {
        let contact = Contact(firstName: "Cara", lastName: "Poe", phoneNumbers: ["+15551112222"])
        context.insert(contact)
        let reminder = TickleReminder(contact: contact, note: "once", frequency: .oneTime,
                                      startDate: Date().addingTimeInterval(-86_400))
        context.insert(reminder)
        // Simulate the reminder having been marked done on the Tickle tab while
        // the composer was open.
        TickleScheduler.markComplete(reminder: reminder, context: context)
        XCTAssertEqual(reminder.status, .completed)

        let snapshot = TickleScheduler.completeAfterSend(reminder: reminder, context: context)
        XCTAssertNil(snapshot, "An already-completed reminder must not complete again")
    }

    func testCompleteAfterSendDoesNothingWhenDeletedMeanwhile() {
        let reminder = makeDueReminder()
        // Simulate deletion while the composer was open.
        context.delete(reminder)
        try? context.save()

        let snapshot = TickleScheduler.completeAfterSend(reminder: reminder, context: context)
        XCTAssertNil(snapshot, "A deleted reminder must not resurrect on send")
    }

    // MARK: - Undo restores exact prior state

    func testUndoRestoresExactPriorStateForRecurring() {
        let reminder = makeDueReminder()
        let priorDue = reminder.nextDueDate
        let priorStatus = reminder.status
        let priorCompleted = reminder.lastCompletedDate // nil

        let snapshot = try? XCTUnwrap(
            TickleScheduler.completeAfterSend(reminder: reminder, context: context)
        )
        XCTAssertNotNil(snapshot)
        // State moved off its prior values.
        XCTAssertNotEqual(reminder.nextDueDate, priorDue)

        TickleScheduler.undoCompletion(snapshot!, context: context)

        XCTAssertEqual(reminder.nextDueDate, priorDue, "undo restores exact nextDueDate")
        XCTAssertEqual(reminder.status, priorStatus, "undo restores status")
        XCTAssertEqual(reminder.lastCompletedDate, priorCompleted, "undo restores lastCompletedDate (nil)")
    }

    func testUndoRestoresCompletedStatusForOneTime() {
        let contact = Contact(firstName: "Dana", lastName: "Loe", phoneNumbers: ["+15553334444"])
        context.insert(contact)
        let reminder = TickleReminder(contact: contact, note: "once", frequency: .oneTime,
                                      startDate: Date().addingTimeInterval(-86_400))
        context.insert(reminder)
        try? context.save()
        let priorDue = reminder.nextDueDate

        let snapshot = TickleScheduler.completeAfterSend(reminder: reminder, context: context)!
        XCTAssertEqual(reminder.status, .completed)

        TickleScheduler.undoCompletion(snapshot, context: context)
        XCTAssertEqual(reminder.status, .active, "undo restores the reminder to active")
        XCTAssertEqual(reminder.nextDueDate, priorDue)
        XCTAssertNil(reminder.lastCompletedDate)
    }

    func testUndoIsSafeAfterReminderDeleted() {
        let reminder = makeDueReminder()
        let snapshot = TickleScheduler.completeAfterSend(reminder: reminder, context: context)!
        context.delete(reminder)
        try? context.save()
        // Should not crash / throw when the target is gone.
        TickleScheduler.undoCompletion(snapshot, context: context)
    }

    // MARK: - Undo must not re-arm the overdue notification (orchestrator finding)

    func testShouldRearmAfterUndoFalseForOverdueRestore() {
        // The only auto-complete case: the reminder was due/overdue when the send
        // completed it, so the restored nextDueDate is in the past. Re-arming
        // would hit scheduleNotification's overdue branch and banner "reach out
        // to X" 5 seconds after the user just texted X and tapped Undo.
        // Anchor "now" at noon so today's 9am trigger has deterministically passed
        // regardless of what wall-clock time the tests run at.
        let cal = Calendar.current
        let noon = cal.date(bySettingHour: 12, minute: 0, second: 0, of: Date())!
        XCTAssertFalse(TickleScheduler.shouldRearmAfterUndo(nextDueDate: noon.addingTimeInterval(-86_400), now: noon),
                       "an overdue restore must not re-arm a notification")
        XCTAssertFalse(TickleScheduler.shouldRearmAfterUndo(nextDueDate: noon, now: noon),
                       "a due-right-now restore must not re-arm a notification")
    }

    func testShouldRearmAfterUndoFalseWhenTodayNineAMTriggerAlreadyPassed() {
        // Exact parity with scheduleNotification's trigger computation: a due
        // date later *today* still maps to a today-at-9am trigger; once 9am has
        // passed, scheduling it would also fire the 5-second banner.
        let cal = Calendar.current
        let noon = cal.date(bySettingHour: 12, minute: 0, second: 0, of: Date())!
        let laterToday = cal.date(bySettingHour: 22, minute: 0, second: 0, of: noon)!
        XCTAssertFalse(TickleScheduler.shouldRearmAfterUndo(nextDueDate: laterToday, now: noon),
                       "a same-day due date whose 9am trigger has passed must not re-arm")
    }

    func testShouldRearmAfterUndoTrueForFutureTrigger() {
        let cal = Calendar.current
        let noon = cal.date(bySettingHour: 12, minute: 0, second: 0, of: Date())!
        let nextWeek = cal.date(byAdding: .day, value: 7, to: noon)!
        XCTAssertTrue(TickleScheduler.shouldRearmAfterUndo(nextDueDate: nextWeek, now: noon),
                      "a genuinely future trigger should re-arm on undo")
    }

    func testUndoOfOverdueRestoreRestoresStateWithoutRearm() throws {
        // End-to-end: complete a due reminder via send, undo it, and confirm the
        // restored (overdue) state is exact AND is one shouldRearmAfterUndo
        // rejects — i.e. the undo path takes the no-notification branch.
        let reminder = makeDueReminder()
        let priorDue = reminder.nextDueDate

        let snapshot = try XCTUnwrap(
            TickleScheduler.completeAfterSend(reminder: reminder, context: context)
        )
        TickleScheduler.undoCompletion(snapshot, context: context)

        XCTAssertEqual(reminder.nextDueDate, priorDue)
        XCTAssertFalse(TickleScheduler.shouldRearmAfterUndo(nextDueDate: reminder.nextDueDate),
                       "the restored overdue date must be rejected by the re-arm gate")
    }

    func testUndoRestoresSnoozedStatusWithoutArmingPastTrigger() throws {
        // A snoozed reminder whose snooze window elapsed was auto-completed by a
        // send; undo must restore .snoozed (not .active) and its past due date —
        // which the re-arm gate rejects, so no stale trigger is armed.
        let reminder = makeDueReminder(status: .snoozed)
        let priorDue = reminder.nextDueDate

        let snapshot = try XCTUnwrap(
            TickleScheduler.completeAfterSend(reminder: reminder, context: context)
        )
        XCTAssertEqual(reminder.status, .active, "completion moves a recurring reminder to active")

        TickleScheduler.undoCompletion(snapshot, context: context)

        XCTAssertEqual(reminder.status, .snoozed, "undo restores the snoozed status exactly")
        XCTAssertEqual(reminder.nextDueDate, priorDue)
        XCTAssertNil(reminder.lastCompletedDate)
        XCTAssertFalse(TickleScheduler.shouldRearmAfterUndo(nextDueDate: reminder.nextDueDate),
                       "the restored past trigger must not be re-armed")
    }

    // MARK: - isDueForSendCompletion predicate

    func testIsDueForSendCompletionPredicate() {
        let now = Date()
        let due = makeDueReminder()
        XCTAssertTrue(TickleScheduler.isDueForSendCompletion(due, now: now))

        due.nextDueDate = now.addingTimeInterval(3600)
        XCTAssertFalse(TickleScheduler.isDueForSendCompletion(due, now: now),
                       "future-dated reminder is not due for completion")

        due.nextDueDate = now.addingTimeInterval(-3600)
        due.status = .completed
        XCTAssertFalse(TickleScheduler.isDueForSendCompletion(due, now: now),
                       "completed reminder is never due for completion")
    }
}
