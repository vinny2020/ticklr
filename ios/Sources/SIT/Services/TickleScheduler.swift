import Foundation
import SwiftData
import UserNotifications

struct TickleScheduler {

    /// Requests notification authorization if it hasn't been determined yet, and
    /// reports whether the app is currently authorized to display notifications
    /// — either already authorized, or just granted. Callers await this before
    /// scheduling (see `scheduleNotification`) so a fresh install's first-ever
    /// tickle isn't dropped while the system permission prompt is still on
    /// screen (TIC-65).
    static func requestAuthorizationIfNeeded() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        case .denied:
            return false
        @unknown default:
            return false
        }
    }

    /// Schedules a local notification for `reminder`. Fire-and-forget from the
    /// caller's perspective (matches the other static scheduling calls in this
    /// file), but internally awaits authorization before calling `add(_:)` —
    /// scheduling immediately after a fresh install's first tickle used to race
    /// the still-pending permission dialog and silently fail (TIC-65). Errors
    /// from `add(_:)` are logged instead of being swallowed.
    @MainActor
    static func scheduleNotification(for reminder: TickleReminder) {
        // In-app "Tickle reminders" toggle (TIC-68) — checked synchronously
        // before any async work so a disabled pref never even requests
        // authorization.
        let notificationsEnabled = (UserDefaults.standard.object(forKey: "tickleNotificationsEnabled") as? Bool) ?? true
        guard notificationsEnabled else { return }

        // Snapshot everything the async Task needs as plain value types BEFORE the
        // Task closure captures anything. Reading @Model properties *inside* the
        // Task — i.e. after the `await` suspension, or after the model's context is
        // torn down / the model deleted — faults with a SwiftData assertion
        // ("model instance was destroyed"). The escaping closure must never
        // capture the TickleReminder (or its Contact/Group) itself.
        let identifier = "tickle-\(reminder.id.uuidString)"
        let name: String
        if let contact = reminder.contact {
            name = contact.fullName.isEmpty ? "someone" : contact.fullName
        } else if let group = reminder.group {
            name = group.name
        } else {
            name = "someone"
        }
        let bodyText = reminder.note.isEmpty ? reminder.frequency.localizedName : reminder.note
        let dueDate = reminder.nextDueDate
        // Snapshot the routing IDs as plain values too (same reason as `name`
        // above): the escaping Task must never touch `reminder`/`reminder.contact`
        // after the `await`. Drives tap routing + Done/Snooze actions (TIC-83).
        let userInfo = TickleNotificationRouter.userInfo(
            reminderID: reminder.id,
            contactID: reminder.contact?.id
        )

        Task { @MainActor in
            guard await requestAuthorizationIfNeeded() else {
                print("Ticklr: skipped scheduling \(identifier) — notifications not authorized")
                return
            }

            let content = UNMutableNotificationContent()
            content.title = String(localized: "notification.title \(name)")
            content.body = bodyText
            content.sound = .default
            content.userInfo = userInfo
            content.categoryIdentifier = TickleNotificationRouter.categoryID

            let trigger: UNNotificationTrigger
            let cal = Calendar.current
            let todayAt9 = cal.date(bySettingHour: 9, minute: 0, second: 0, of: dueDate) ?? dueDate
            if todayAt9 <= Date() {
                // Due date is today past 9am, or overdue — fire in 5 seconds
                trigger = UNTimeIntervalNotificationTrigger(timeInterval: 5, repeats: false)
            } else {
                var components = cal.dateComponents([.year, .month, .day], from: dueDate)
                components.hour = 9
                components.minute = 0
                trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            }

            let request = UNNotificationRequest(
                identifier: identifier,
                content: content,
                trigger: trigger
            )

            do {
                try await UNUserNotificationCenter.current().add(request)
            } catch {
                print("Ticklr: failed to schedule \(identifier): \(error)")
            }
        }
    }

    static func cancelNotification(for reminder: TickleReminder) {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: ["tickle-\(reminder.id.uuidString)"])
    }

    /// Deletes a contact together with every tickle that belongs to it,
    /// cancelling each tickle's pending notification first. Without this the
    /// `.nullify` delete rule would leave the reminders behind with
    /// `contact == nil` ("Unknown") and their notifications still armed —
    /// firing days after the contact is gone. Callers save the context.
    @MainActor
    static func deleteContact(_ contact: Contact, context: ModelContext) {
        for reminder in contact.tickles {
            cancelNotification(for: reminder)
            context.delete(reminder)
        }
        context.delete(contact)
    }

    /// Deletes a group together with every tickle attached to it, cancelling
    /// each tickle's pending notification first. Same orphaned-reminder
    /// hazard as `deleteContact`. Callers save the context.
    @MainActor
    static func deleteGroup(_ group: ContactGroup, context: ModelContext) {
        for reminder in group.tickles {
            cancelNotification(for: reminder)
            context.delete(reminder)
        }
        context.delete(group)
    }

    @MainActor
    static func markComplete(reminder: TickleReminder, context: ModelContext) {
        reminder.lastCompletedDate = Date()
        cancelNotification(for: reminder)
        if reminder.frequency == .oneTime {
            reminder.status = .completed
        } else {
            if reminder.frequency == .annual {
                // Anchor on startDate, never nextDueDate: snooze() overwrites
                // nextDueDate, so deriving the next occurrence from it shifts
                // the anniversary by the snooze amount — permanently (TIC-62).
                reminder.nextDueDate = nextAnnualDate(after: Date(), matchingMonthDayOf: reminder.startDate)
            } else {
                reminder.nextDueDate = nextDueDate(from: Date(), frequency: reminder.frequency, customDays: reminder.customIntervalDays)
            }
            reminder.status = .active
            scheduleNotification(for: reminder)
        }
        try? context.save()
    }

    // MARK: - Send-driven completion (TIC-82)

    /// The exact reminder state captured immediately before a send auto-completes
    /// a due tickle, so an Undo can restore it byte-for-byte. Value type keyed by
    /// the reminder's stable `id` (not a live model reference) so it survives the
    /// compose sheet dismissing and can be handed to whatever screen shows the
    /// undo toast.
    struct CompletionSnapshot: Sendable, Equatable {
        let reminderID: UUID
        let nextDueDate: Date
        let status: TickleStatus
        let lastCompletedDate: Date?
    }

    /// Whether `reminder` is still eligible for send-driven auto-completion:
    /// active or snoozed, and due now (`nextDueDate <= now`). A future-dated
    /// (upcoming or freshly-snoozed) or already-completed tickle returns false.
    static func isDueForSendCompletion(_ reminder: TickleReminder, now: Date = Date()) -> Bool {
        (reminder.status == .active || reminder.status == .snoozed) && reminder.nextDueDate <= now
    }

    /// Snapshots `reminder`'s exact pre-completion state, then completes it via
    /// `markComplete`. The shared snapshot+complete path behind every in-app
    /// mark-done entry point (row checkmark, leading swipe, action sheet, iPad
    /// pane — TIC-87) as well as the send-driven `completeAfterSend`. The returned
    /// snapshot lets the caller offer Undo through the same `undoCompletion`
    /// machinery. No due-ness re-validation here: an explicit in-app Done acts on
    /// the reminder the user is looking at, whatever its state. Callers save via
    /// `markComplete`.
    @MainActor
    static func completeWithSnapshot(
        reminder: TickleReminder,
        context: ModelContext
    ) -> CompletionSnapshot {
        let snapshot = CompletionSnapshot(
            reminderID: reminder.id,
            nextDueDate: reminder.nextDueDate,
            status: reminder.status,
            lastCompletedDate: reminder.lastCompletedDate
        )
        markComplete(reminder: reminder, context: context)
        return snapshot
    }

    /// Auto-completes the tickle associated with a just-sent text — but only after
    /// re-validating against the live store, mirroring TIC-66's guard-before-acting
    /// philosophy: the reminder must still exist (not deleted while the composer was
    /// up) and still be due. Returns the pre-completion snapshot on success (so the
    /// caller can offer Undo), or `nil` when the send should not complete anything.
    /// Completion routes through `completeWithSnapshot` → `markComplete` so
    /// notification state stays in sync and the Undo path is shared with the in-app
    /// mark-done surfaces (TIC-87).
    @MainActor
    static func completeAfterSend(
        reminder: TickleReminder,
        context: ModelContext,
        now: Date = Date()
    ) -> CompletionSnapshot? {
        let targetID = reminder.id
        let descriptor = FetchDescriptor<TickleReminder>(
            predicate: #Predicate { $0.id == targetID }
        )
        // Re-fetch: a reminder deleted (or completed) meanwhile won't come back due.
        guard let live = try? context.fetch(descriptor).first else { return nil }
        guard isDueForSendCompletion(live, now: now) else { return nil }
        return completeWithSnapshot(reminder: live, context: context)
    }

    /// Whether an undo-restored due date warrants re-arming a notification:
    /// only when its 9am trigger (the same computation `scheduleNotification`
    /// uses) is genuinely in the future. An auto-completed tickle is overdue by
    /// definition, so its restored date would land in `scheduleNotification`'s
    /// overdue branch — a 5-second `UNTimeIntervalNotificationTrigger` that would
    /// banner "time to reach out to X" moments after the user just texted X and
    /// tapped Undo. For overdue restores the in-app Due section carries the state
    /// (the user is literally looking at the app when they tap Undo); mirrors
    /// Android's `shouldArmAlarm`, which only arms future dates.
    static func shouldRearmAfterUndo(nextDueDate: Date, now: Date = Date()) -> Bool {
        let cal = Calendar.current
        let triggerDate = cal.date(bySettingHour: 9, minute: 0, second: 0, of: nextDueDate) ?? nextDueDate
        return triggerDate > now
    }

    /// Restores the exact pre-action state captured in `snapshot` and re-syncs the
    /// tickle's notification state for the restored due date. Shared by
    /// `undoCompletion` and `undoSnooze` — both need the same restore-fields +
    /// disarm + conditional-rearm sequence. No-op if the reminder has since been
    /// deleted.
    @MainActor
    private static func restore(_ snapshot: CompletionSnapshot, context: ModelContext) {
        let targetID = snapshot.reminderID
        let descriptor = FetchDescriptor<TickleReminder>(
            predicate: #Predicate { $0.id == targetID }
        )
        guard let live = try? context.fetch(descriptor).first else { return }
        live.nextDueDate = snapshot.nextDueDate
        live.status = snapshot.status
        live.lastCompletedDate = snapshot.lastCompletedDate
        // The completing/snoozing call armed a notification for the *new* due date
        // (recurring/snoozed) or cancelled it (one-time); always disarm that.
        // Re-arm only when the restored trigger is genuinely in the future — never
        // the overdue 5-second banner (see `shouldRearmAfterUndo`).
        cancelNotification(for: live)
        if shouldRearmAfterUndo(nextDueDate: live.nextDueDate) {
            scheduleNotification(for: live)
        }
        try? context.save()
    }

    /// Restores the exact pre-completion state captured in `snapshot`. Handles the
    /// notification re-arm rules; no-op if the reminder was since deleted.
    @MainActor
    static func undoCompletion(_ snapshot: CompletionSnapshot, context: ModelContext) {
        restore(snapshot, context: context)
    }

    // MARK: - Snooze (with undo, TIC-87)

    @MainActor
    static func snooze(reminder: TickleReminder, days: Int = 7, context: ModelContext) {
        reminder.nextDueDate = Calendar.current.date(byAdding: .day, value: days, to: Date()) ?? Date()
        reminder.status = .snoozed
        cancelNotification(for: reminder)
        scheduleNotification(for: reminder)
        try? context.save()
    }

    /// Snapshots `reminder`'s exact pre-snooze state, then snoozes it `days` into
    /// the future. Mirrors `completeWithSnapshot`: the returned snapshot (reusing
    /// `CompletionSnapshot`, whose reminderID/nextDueDate/status/lastCompletedDate
    /// fields fully capture a snooze) lets the caller offer Undo through
    /// `undoSnooze`. A re-snooze of an already-snoozed tickle captures `.snoozed`
    /// as the prior status, so Undo restores the earlier snooze window exactly.
    @MainActor
    static func snoozeWithSnapshot(
        reminder: TickleReminder,
        days: Int = 7,
        context: ModelContext
    ) -> CompletionSnapshot {
        let snapshot = CompletionSnapshot(
            reminderID: reminder.id,
            nextDueDate: reminder.nextDueDate,
            status: reminder.status,
            lastCompletedDate: reminder.lastCompletedDate
        )
        snooze(reminder: reminder, days: days, context: context)
        return snapshot
    }

    /// Restores the exact pre-snooze state captured in `snapshot`. Mirrors
    /// `undoCompletion` (same restore + notification re-sync); no-op if the
    /// reminder was since deleted.
    @MainActor
    static func undoSnooze(_ snapshot: CompletionSnapshot, context: ModelContext) {
        restore(snapshot, context: context)
    }

    static func nextDueDate(from date: Date, frequency: TickleFrequency, customDays: Int? = nil) -> Date {
        let cal = Calendar.current
        switch frequency {
        case .oneTime:    return date
        case .daily:      return cal.date(byAdding: .day,   value: 1,              to: date) ?? date
        case .weekly:     return cal.date(byAdding: .day,   value: 7,              to: date) ?? date
        case .biweekly:   return cal.date(byAdding: .day,   value: 14,             to: date) ?? date
        case .monthly:    return cal.date(byAdding: .month, value: 1,              to: date) ?? date
        case .bimonthly:  return cal.date(byAdding: .month, value: 2,              to: date) ?? date
        case .quarterly:  return cal.date(byAdding: .month, value: 3,              to: date) ?? date
        case .annual:     return cal.date(byAdding: .year,  value: 1,              to: date) ?? date
        case .custom:     return cal.date(byAdding: .day,   value: customDays ?? 30, to: date) ?? date
        }
    }

    static func nextAnnualDate(after date: Date, matchingMonthDayOf annualDate: Date) -> Date {
        let cal = Calendar.current
        let annualComponents = cal.dateComponents([.month, .day, .hour, .minute, .second], from: annualDate)
        let currentYear = cal.component(.year, from: date)
        var candidateComponents = DateComponents()
        candidateComponents.year = currentYear
        candidateComponents.month = annualComponents.month
        candidateComponents.day = annualComponents.day
        candidateComponents.hour = annualComponents.hour
        candidateComponents.minute = annualComponents.minute
        candidateComponents.second = annualComponents.second

        if let candidate = cal.date(from: candidateComponents), candidate > date {
            return candidate
        }
        candidateComponents.year = currentYear + 1
        return cal.date(from: candidateComponents) ?? cal.date(byAdding: .year, value: 1, to: annualDate) ?? annualDate
    }

    /// Decides the initial `nextDueDate` for a tickle being created or edited.
    /// A future `startDate` is honored as the literal first occurrence; a
    /// `startDate` of today or in the past collapses to "one interval from
    /// `startDate`" so a tickle created today doesn't fire today.
    static func initialNextDueDate(
        from startDate: Date,
        frequency: TickleFrequency,
        customDays: Int? = nil,
        now: Date = Date()
    ) -> Date {
        if frequency == .oneTime { return startDate }
        if frequency == .annual {
            return nextAnnualDate(after: now, matchingMonthDayOf: startDate)
        }
        if startDate > now { return startDate }
        return nextDueDate(from: startDate, frequency: frequency, customDays: customDays)
    }
}
