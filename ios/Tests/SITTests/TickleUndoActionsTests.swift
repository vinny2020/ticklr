import XCTest
import SwiftData
@testable import Ticklr

/// TIC-87: undo for every in-app mark-done + snooze-with-duration + undo, and the
/// group-delete cascade confirmation. Covers the shared snapshot machinery
/// (`completeWithSnapshot` / `snoozeWithSnapshot` and their `undo…` mirrors),
/// snooze duration math, and the tickle count surfaced in the group-delete message.
@MainActor
final class TickleUndoActionsTests: XCTestCase {

    private var container: ModelContainer!
    private var context: ModelContext!
    private var priorNotificationsEnabled: Any?

    override func setUp() async throws {
        // Same rationale as TickleSendCompletionTests: `snooze`/`markComplete`
        // call `scheduleNotification`, which spawns an async @MainActor Task that
        // touches the model and would outlive the test. Disabling the pref makes
        // scheduling short-circuit before any Task is spawned.
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

    @discardableResult
    private func makeReminder(status: TickleStatus = .active,
                             frequency: TickleFrequency = .monthly,
                             dueOffset: TimeInterval = -86_400) -> TickleReminder {
        let contact = Contact(firstName: "Alice", lastName: "Doe", phoneNumbers: ["+15551234567"])
        context.insert(contact)
        let reminder = TickleReminder(contact: contact, note: "ping", frequency: frequency)
        reminder.nextDueDate = Date().addingTimeInterval(dueOffset)
        reminder.status = status
        context.insert(reminder)
        try? context.save()
        return reminder
    }

    // MARK: - In-app mark-done + undo

    func testCompleteWithSnapshotAdvancesRecurringAndSnapshotsPriorState() {
        let reminder = makeReminder()
        let priorDue = reminder.nextDueDate

        let snapshot = TickleScheduler.completeWithSnapshot(reminder: reminder, context: context)

        XCTAssertEqual(snapshot.reminderID, reminder.id)
        XCTAssertEqual(snapshot.nextDueDate, priorDue, "snapshot captures the pre-completion due date")
        XCTAssertEqual(snapshot.status, .active)
        XCTAssertNil(snapshot.lastCompletedDate)
        XCTAssertEqual(reminder.status, .active, "a recurring tickle stays active after completing")
        XCTAssertGreaterThan(reminder.nextDueDate, priorDue, "next due date advanced a full interval")
        XCTAssertNotNil(reminder.lastCompletedDate)
    }

    func testUndoCompletionRestoresExactPriorState() {
        let reminder = makeReminder()
        let priorDue = reminder.nextDueDate

        let snapshot = TickleScheduler.completeWithSnapshot(reminder: reminder, context: context)
        TickleScheduler.undoCompletion(snapshot, context: context)

        XCTAssertEqual(reminder.status, .active)
        XCTAssertEqual(reminder.nextDueDate, priorDue, "undo restores the exact prior due date")
        XCTAssertNil(reminder.lastCompletedDate, "undo restores the nil lastCompletedDate")
    }

    func testCompleteAndUndoOneTimeReminder() {
        let reminder = makeReminder(frequency: .oneTime)
        let priorDue = reminder.nextDueDate

        let snapshot = TickleScheduler.completeWithSnapshot(reminder: reminder, context: context)
        XCTAssertEqual(reminder.status, .completed, "a one-time tickle is completed outright")

        TickleScheduler.undoCompletion(snapshot, context: context)
        XCTAssertEqual(reminder.status, .active, "undo restores it to active")
        XCTAssertEqual(reminder.nextDueDate, priorDue)
        XCTAssertNil(reminder.lastCompletedDate)
    }

    // MARK: - Snooze snapshot + undo

    func testSnoozeWithSnapshotSnapshotsPriorActiveState() {
        let reminder = makeReminder(status: .active)
        let priorDue = reminder.nextDueDate

        let snapshot = TickleScheduler.snoozeWithSnapshot(reminder: reminder, days: 7, context: context)

        XCTAssertEqual(snapshot.status, .active, "snapshot captures the pre-snooze status")
        XCTAssertEqual(snapshot.nextDueDate, priorDue, "snapshot captures the pre-snooze due date")
        XCTAssertEqual(reminder.status, .snoozed, "the reminder is now snoozed")
        XCTAssertGreaterThan(reminder.nextDueDate, Date(), "snoozed due date is in the future")
    }

    func testUndoSnoozeRestoresExactPriorState() {
        let reminder = makeReminder(status: .active)
        let priorDue = reminder.nextDueDate

        let snapshot = TickleScheduler.snoozeWithSnapshot(reminder: reminder, days: 3, context: context)
        TickleScheduler.undoSnooze(snapshot, context: context)

        XCTAssertEqual(reminder.status, .active, "undo restores the exact prior status")
        XCTAssertEqual(reminder.nextDueDate, priorDue, "undo restores the exact prior due date")
    }

    func testResnoozeAnAlreadySnoozedReminderUndoesToEarlierSnooze() {
        // A reminder already snoozed 3 days out, re-snoozed 7 days out. Undo must
        // restore the *snoozed* status and the earlier (+3) window exactly.
        let reminder = makeReminder(status: .active)
        _ = TickleScheduler.snoozeWithSnapshot(reminder: reminder, days: 3, context: context)
        let firstSnoozeDue = reminder.nextDueDate
        XCTAssertEqual(reminder.status, .snoozed)

        let snapshot = TickleScheduler.snoozeWithSnapshot(reminder: reminder, days: 7, context: context)
        XCTAssertEqual(snapshot.status, .snoozed, "re-snooze snapshots the prior snoozed status")
        XCTAssertEqual(snapshot.nextDueDate, firstSnoozeDue, "re-snooze snapshots the earlier snooze window")
        XCTAssertGreaterThan(reminder.nextDueDate, firstSnoozeDue, "re-snooze pushed the due date further out")

        TickleScheduler.undoSnooze(snapshot, context: context)
        XCTAssertEqual(reminder.status, .snoozed, "undo restores snoozed, not active")
        XCTAssertEqual(reminder.nextDueDate, firstSnoozeDue, "undo restores the earlier snooze window")
    }

    // MARK: - Snooze duration math (1d / 3d / 7d)

    func testSnoozeDurationMath() {
        for days in [1, 3, 7] {
            let reminder = makeReminder(status: .active)
            _ = TickleScheduler.snoozeWithSnapshot(reminder: reminder, days: days, context: context)
            let expected = Calendar.current.date(byAdding: .day, value: days, to: Date())!
            XCTAssertEqual(reminder.nextDueDate.timeIntervalSince(expected), 0, accuracy: 5,
                           "snooze(\(days)) should land ~\(days) days out")
        }
    }

    // MARK: - Group-delete cascade

    func testDeleteGroupCascadesAndCountMatchesMessage() {
        let group = ContactGroup(name: "Hiking Crew")
        context.insert(group)
        let n = 3
        for i in 0..<n {
            let r = TickleReminder(group: group, note: "reminder \(i)", frequency: .weekly)
            context.insert(r)
            group.tickles.append(r)
        }
        try? context.save()

        // The count the confirmation message announces.
        XCTAssertEqual(group.tickles.count, n)
        let message = String(localized: "groupList.deleteConfirm.message \(group.tickles.count)")
        XCTAssertTrue(message.contains("\(n)"), "cascade message should announce the count: \(message)")
        XCTAssertFalse(message.isEmpty)
        XCTAssertNotEqual(message, "groupList.deleteConfirm.message \(n)", "plural key must resolve")

        // Deleting the group cascades to every attached tickle.
        TickleScheduler.deleteGroup(group, context: context)
        try? context.save()

        let remainingReminders = (try? context.fetch(FetchDescriptor<TickleReminder>()))?.count ?? -1
        let remainingGroups = (try? context.fetch(FetchDescriptor<ContactGroup>()))?.count ?? -1
        XCTAssertEqual(remainingReminders, 0, "all \(n) attached tickles are deleted with the group")
        XCTAssertEqual(remainingGroups, 0, "the group itself is deleted")
    }

    // MARK: - Group-delete message copy

    func testGroupDeleteTitleInterpolatesName() {
        let name = "Hiking Crew"
        let title = String(localized: "groupList.deleteConfirm.title \(name)")
        XCTAssertTrue(title.contains(name), "delete title should contain the group name: \(title)")
        XCTAssertFalse(title.isEmpty)
    }

    func testGroupDeleteMessagePlural() {
        let one = String(localized: "groupList.deleteConfirm.message \(1)")
        let many = String(localized: "groupList.deleteConfirm.message \(5)")
        XCTAssertTrue(one.contains("tickle") && !one.contains("tickles"),
                      "singular should use 'tickle' not 'tickles': \(one)")
        XCTAssertTrue(many.contains("tickles"), "plural should use 'tickles': \(many)")
    }
}
