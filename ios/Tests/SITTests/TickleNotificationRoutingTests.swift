import XCTest
import SwiftData
import UserNotifications
@testable import Ticklr

/// TIC-83: tapping a tickle reminder notification opens Compose pre-addressed to
/// the reminder's contact (with the due reminder attached); Done/Snooze banner
/// actions mutate the reminder in the background. Covers the pure route decision
/// (`TickleNotificationRouter.resolveRoute`), the background action handlers, the
/// `userInfo` payload round-trip, and the registered category shape.
@MainActor
final class TickleNotificationRoutingTests: XCTestCase {

    private var container: ModelContainer!
    private var context: ModelContext!
    private var priorNotificationsEnabled: Any?

    override func setUp() async throws {
        // `markComplete` / `snooze` (exercised via the action handlers) call
        // `scheduleNotification`, which spawns an async @MainActor Task touching
        // the model — it would outlive the test and fault on the destroyed
        // in-memory container at teardown. Disabling the in-app notifications pref
        // makes `scheduleNotification` short-circuit before spawning any Task, so
        // the synchronous logic under test runs in isolation. (Same guard as
        // TickleSendCompletionTests.)
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
    private func makeDueContactReminder(status: TickleStatus = .active) -> TickleReminder {
        let contact = Contact(firstName: "Alice", lastName: "Doe", phoneNumbers: ["+15551234567"])
        context.insert(contact)
        let reminder = TickleReminder(contact: contact, note: "ping", frequency: .monthly)
        reminder.nextDueDate = Date().addingTimeInterval(-86_400) // yesterday → due
        reminder.status = status
        context.insert(reminder)
        try? context.save()
        return reminder
    }

    @discardableResult
    private func makeDueGroupReminder() -> TickleReminder {
        let group = ContactGroup(name: "Book Club")
        context.insert(group)
        let reminder = TickleReminder(group: group, note: "check in", frequency: .monthly)
        reminder.nextDueDate = Date().addingTimeInterval(-86_400)
        reminder.status = .active
        context.insert(reminder)
        try? context.save()
        return reminder
    }

    // MARK: - resolveRoute: compose (valid due contact reminder)

    func testResolveRouteComposesForDueContactReminder() {
        let reminder = makeDueContactReminder()
        let contactID = try? XCTUnwrap(reminder.contact?.id)

        let route = TickleNotificationRouter.resolveRoute(reminderID: reminder.id, context: context)

        XCTAssertEqual(route, .compose(reminderID: reminder.id, contactID: contactID!),
                       "a due reminder with a contact should open Compose pre-addressed to that contact")
    }

    func testResolveRouteComposesForPastDueSnoozedContactReminder() {
        // A snoozed reminder whose window elapsed is still "due".
        let reminder = makeDueContactReminder(status: .snoozed)
        let route = TickleNotificationRouter.resolveRoute(reminderID: reminder.id, context: context)
        if case .compose = route { /* ok */ } else {
            XCTFail("a past-due snoozed contact reminder should still route to Compose, got \(route)")
        }
    }

    // MARK: - resolveRoute: fall back to Tickle tab

    func testResolveRouteFallsBackWhenReminderMissing() {
        // Never inserted → not in the store.
        let route = TickleNotificationRouter.resolveRoute(reminderID: UUID(), context: context)
        XCTAssertEqual(route, .tickleTab, "a stale/deleted reminder id must fall back to the Tickle tab")
    }

    func testResolveRouteFallsBackWhenReminderIDNil() {
        let route = TickleNotificationRouter.resolveRoute(reminderID: nil, context: context)
        XCTAssertEqual(route, .tickleTab, "a notification with no reminder id must fall back to the Tickle tab")
    }

    func testResolveRouteFallsBackWhenNoLongerDue() {
        let reminder = makeDueContactReminder()
        reminder.nextDueDate = Date().addingTimeInterval(7 * 86_400) // pushed into the future
        try? context.save()

        let route = TickleNotificationRouter.resolveRoute(reminderID: reminder.id, context: context)
        XCTAssertEqual(route, .tickleTab, "an upcoming (no-longer-due) reminder must fall back to the Tickle tab")
    }

    func testResolveRouteFallsBackWhenCompletedMeanwhile() {
        let reminder = makeDueContactReminder()
        TickleScheduler.markComplete(reminder: reminder, context: context)

        let route = TickleNotificationRouter.resolveRoute(reminderID: reminder.id, context: context)
        XCTAssertEqual(route, .tickleTab, "a reminder completed while the banner sat must fall back to the Tickle tab")
    }

    func testResolveRouteFallsBackForGroupReminder() {
        let reminder = makeDueGroupReminder()
        let route = TickleNotificationRouter.resolveRoute(reminderID: reminder.id, context: context)
        XCTAssertEqual(route, .tickleTab, "a group reminder has no single contact — route to the Tickle tab")
    }

    // MARK: - Background actions: Done

    func testCompleteReminderMarksRecurringDone() {
        let reminder = makeDueContactReminder()
        let priorDue = reminder.nextDueDate

        let handled = TickleNotificationRouter.completeReminder(reminder.id, context: context)

        XCTAssertTrue(handled)
        XCTAssertEqual(reminder.status, .active, "a recurring reminder stays active after Done")
        XCTAssertGreaterThan(reminder.nextDueDate, priorDue, "Done advances the next due date")
        XCTAssertNotNil(reminder.lastCompletedDate, "Done stamps the completion date")
    }

    func testCompleteReminderIsNoOpWhenMissing() {
        let handled = TickleNotificationRouter.completeReminder(UUID(), context: context)
        XCTAssertFalse(handled, "completing a deleted reminder is a no-op")
    }

    // MARK: - Background actions: Snooze

    func testSnoozeReminderSnoozesSevenDays() {
        let reminder = makeDueContactReminder()

        let handled = TickleNotificationRouter.snoozeReminder(reminder.id, context: context)

        XCTAssertTrue(handled)
        XCTAssertEqual(reminder.status, .snoozed, "Snooze moves the reminder to snoozed")
        XCTAssertGreaterThan(reminder.nextDueDate, Date(), "Snooze pushes the due date into the future")
    }

    func testSnoozeReminderIsNoOpWhenMissing() {
        let handled = TickleNotificationRouter.snoozeReminder(UUID(), context: context)
        XCTAssertFalse(handled, "snoozing a deleted reminder is a no-op")
    }

    // MARK: - userInfo payload round-trip

    func testUserInfoRoundTripWithContact() {
        let reminderID = UUID()
        let contactID = UUID()
        let info = TickleNotificationRouter.userInfo(reminderID: reminderID, contactID: contactID)
        XCTAssertEqual(info["reminderID"], reminderID.uuidString)
        XCTAssertEqual(info["contactID"], contactID.uuidString)

        let ids = TickleNotificationRouter.ids(from: info)
        XCTAssertEqual(ids.reminderID, reminderID)
        XCTAssertEqual(ids.contactID, contactID)
    }

    func testUserInfoOmitsContactForGroupReminder() {
        let reminderID = UUID()
        let info = TickleNotificationRouter.userInfo(reminderID: reminderID, contactID: nil)
        XCTAssertNil(info["contactID"], "a group reminder carries no contactID")

        let ids = TickleNotificationRouter.ids(from: info)
        XCTAssertEqual(ids.reminderID, reminderID)
        XCTAssertNil(ids.contactID)
    }

    func testIdsFromMalformedUserInfoAreNil() {
        let ids = TickleNotificationRouter.ids(from: ["reminderID": "not-a-uuid", "contactID": 42])
        XCTAssertNil(ids.reminderID)
        XCTAssertNil(ids.contactID)
    }

    // MARK: - Registered category shape

    func testNotificationCategoryHasDoneAndSnoozeActions() {
        let category = TickleNotificationRouter.notificationCategory
        XCTAssertEqual(category.identifier, "TICKLE_REMINDER")
        XCTAssertEqual(category.actions.map(\.identifier),
                       [TickleNotificationRouter.doneActionID, TickleNotificationRouter.snoozeActionID])
        // Both actions must be non-foreground so they run in the background
        // without bringing the app forward.
        for action in category.actions {
            XCTAssertFalse(action.options.contains(.foreground),
                           "\(action.identifier) must be a background (non-foreground) action")
            XCTAssertFalse(action.title.isEmpty, "\(action.identifier) needs a localized title")
        }
    }
}
