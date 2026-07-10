import Foundation
import SwiftData
import UserNotifications

/// Where tapping a tickle reminder notification (or its actions) should take the
/// user. Resolved from the notification's `userInfo` re-validated against the
/// live store, so a reminder deleted or no-longer-due while the banner sat on
/// screen never opens a stale composer (TIC-83). Parity with Android's
/// `ticklr://compose?contactId=&reminderId=` deep link.
enum TickleNotificationRoute: Equatable {
    /// Open Compose pre-addressed to `contactID` with `reminderID` attached, so a
    /// send auto-completes the due tickle (TIC-82's completion path).
    case compose(reminderID: UUID, contactID: UUID)
    /// Fall back to the Tickle tab — used when the reminder is gone, no longer
    /// due, or is a group reminder with no single contact to address.
    case tickleTab
}

/// Pure, testable routing + action logic for tickle reminder notifications
/// (TIC-83). Kept out of the `UNUserNotificationCenterDelegate` so the decision
/// ("valid due contact reminder → compose; stale/group → tickle tab") and the
/// Done/Snooze store mutations can be unit-tested against an in-memory
/// `ModelContext`, exactly like `TickleScheduler.completeAfterSend`.
enum TickleNotificationRouter {

    // MARK: - Category / action identifiers

    static let categoryID = "TICKLE_REMINDER"
    static let doneActionID = "TICKLE_DONE"
    static let snoozeActionID = "TICKLE_SNOOZE"

    /// The notification category carrying the Done / Snooze banner actions. Both
    /// actions are non-foreground (`options: []`) so they run in the background
    /// without bringing the app forward — completion/snooze happen silently via
    /// the delegate. Titles reuse already-localized strings (`common.done`,
    /// `tickleList.action.snooze`) so they stay consistent with the swipe/detail
    /// actions and need no new catalog entries.
    static var notificationCategory: UNNotificationCategory {
        let done = UNNotificationAction(
            identifier: doneActionID,
            title: String(localized: "common.done"),
            options: []
        )
        let snooze = UNNotificationAction(
            identifier: snoozeActionID,
            title: String(localized: "tickleList.action.snooze"),
            options: []
        )
        return UNNotificationCategory(
            identifier: categoryID,
            actions: [done, snooze],
            intentIdentifiers: [],
            options: []
        )
    }

    /// Registers `notificationCategory` so scheduled notifications carrying
    /// `categoryIdentifier == categoryID` surface the Done / Snooze buttons.
    /// Called once at launch from `SITApp.init()`.
    static func registerCategories() {
        UNUserNotificationCenter.current().setNotificationCategories([notificationCategory])
    }

    // MARK: - userInfo payload

    /// The `userInfo` attached to a scheduled tickle notification. Plain string
    /// values keyed by the reminder's (and, when present, the contact's) stable
    /// UUID — never a live `@Model` reference — so it survives serialization and
    /// a cold launch. `contactID` is omitted for group reminders.
    static func userInfo(reminderID: UUID, contactID: UUID?) -> [String: String] {
        var info: [String: String] = ["reminderID": reminderID.uuidString]
        if let contactID { info["contactID"] = contactID.uuidString }
        return info
    }

    /// Reads the reminder / contact UUIDs back out of a delivered notification's
    /// `userInfo`. Tolerant of a missing or malformed payload (returns `nil`s).
    static func ids(from userInfo: [AnyHashable: Any]) -> (reminderID: UUID?, contactID: UUID?) {
        let reminderID = (userInfo["reminderID"] as? String).flatMap(UUID.init)
        let contactID = (userInfo["contactID"] as? String).flatMap(UUID.init)
        return (reminderID, contactID)
    }

    // MARK: - Route resolution (the tap decision)

    /// Decides where a notification *tap* should land, re-validating against the
    /// live store first. Compose only when the reminder still exists, is still due
    /// (`isDueForSendCompletion`), and has a contact to address; every other case
    /// — missing id, deleted, no-longer-due, or a group reminder — falls back to
    /// the Tickle tab.
    @MainActor
    static func resolveRoute(reminderID: UUID?, context: ModelContext, now: Date = Date()) -> TickleNotificationRoute {
        guard let reminderID,
              let reminder = fetchReminder(reminderID, context: context) else {
            return .tickleTab
        }
        guard TickleScheduler.isDueForSendCompletion(reminder, now: now) else {
            return .tickleTab
        }
        guard let contact = reminder.contact else {
            return .tickleTab // group reminder — no single recipient to pre-address
        }
        return .compose(reminderID: reminderID, contactID: contact.id)
    }

    // MARK: - Background actions (Done / Snooze)

    /// Completes the reminder behind a "Done" banner action, routing through the
    /// same `TickleScheduler.markComplete` path the in-app UI uses so recurring
    /// reminders re-arm and notification state stays in sync. No-op (returns
    /// `false`) if the reminder was deleted meanwhile.
    @MainActor
    @discardableResult
    static func completeReminder(_ reminderID: UUID, context: ModelContext) -> Bool {
        guard let reminder = fetchReminder(reminderID, context: context) else { return false }
        TickleScheduler.markComplete(reminder: reminder, context: context)
        return true
    }

    /// Snoozes the reminder behind a "Snooze" banner action (7-day default) via
    /// the existing `TickleScheduler.snooze`. No-op if the reminder is gone.
    @MainActor
    @discardableResult
    static func snoozeReminder(_ reminderID: UUID, days: Int = 7, context: ModelContext) -> Bool {
        guard let reminder = fetchReminder(reminderID, context: context) else { return false }
        TickleScheduler.snooze(reminder: reminder, days: days, context: context)
        return true
    }

    // MARK: - Lookup

    @MainActor
    static func fetchReminder(_ id: UUID, context: ModelContext) -> TickleReminder? {
        let descriptor = FetchDescriptor<TickleReminder>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }
}

/// Bridges the `UNUserNotificationCenterDelegate` (which lives outside SwiftUI)
/// to `ContentView`. A tap resolves to a `TickleNotificationRoute` and is stashed
/// here; `ContentView` observes and consumes it — covering both a warm tap (the
/// change fires while the UI is live) and a cold start (the route is set before
/// `ContentView` appears, so it is read on `onAppear`).
@MainActor
final class TickleNotificationCoordinator: ObservableObject {
    static let shared = TickleNotificationCoordinator()

    /// The pending tap route, or `nil` when there is nothing to act on. Set by the
    /// delegate, cleared by `ContentView` once consumed.
    @Published var pendingRoute: TickleNotificationRoute?

    private init() {}

    func post(_ route: TickleNotificationRoute) {
        pendingRoute = route
    }
}
