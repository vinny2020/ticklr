import Foundation
import SwiftData
import UserNotifications

struct TickleScheduler {

    static func requestPermissionIfNeeded() {
        Task {
            let center = UNUserNotificationCenter.current()
            let settings = await center.notificationSettings()
            guard settings.authorizationStatus == .notDetermined else { return }
            _ = try? await center.requestAuthorization(options: [.alert, .sound])
        }
    }

    static func scheduleNotification(for reminder: TickleReminder) {
        let content = UNMutableNotificationContent()

        let name: String
        if let contact = reminder.contact {
            name = contact.fullName.isEmpty ? "someone" : contact.fullName
        } else if let group = reminder.group {
            name = group.name
        } else {
            name = "someone"
        }

        content.title = "Time to reach out to \(name)"
        content.body = reminder.note.isEmpty ? reminder.frequency.rawValue : reminder.note
        content.sound = .default

        let trigger: UNNotificationTrigger
        let cal = Calendar.current
        let todayAt9 = cal.date(bySettingHour: 9, minute: 0, second: 0, of: reminder.nextDueDate) ?? reminder.nextDueDate
        if todayAt9 <= Date() {
            // Due date is today past 9am, or overdue — fire in 5 seconds
            trigger = UNTimeIntervalNotificationTrigger(timeInterval: 5, repeats: false)
        } else {
            var components = cal.dateComponents([.year, .month, .day], from: reminder.nextDueDate)
            components.hour = 9
            components.minute = 0
            trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        }

        let request = UNNotificationRequest(
            identifier: "tickle-\(reminder.id.uuidString)",
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request)
    }

    static func cancelNotification(for reminder: TickleReminder) {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: ["tickle-\(reminder.id.uuidString)"])
    }

    @MainActor
    static func markComplete(reminder: TickleReminder, context: ModelContext) {
        reminder.lastCompletedDate = Date()
        reminder.nextDueDate = nextDueDate(from: Date(), frequency: reminder.frequency, customDays: reminder.customIntervalDays)
        reminder.status = .active
        cancelNotification(for: reminder)
        scheduleNotification(for: reminder)
        try? context.save()
    }

    @MainActor
    static func snooze(reminder: TickleReminder, days: Int = 7, context: ModelContext) {
        reminder.nextDueDate = Calendar.current.date(byAdding: .day, value: days, to: Date()) ?? Date()
        reminder.status = .snoozed
        cancelNotification(for: reminder)
        scheduleNotification(for: reminder)
        try? context.save()
    }

    static func nextDueDate(from date: Date, frequency: TickleFrequency, customDays: Int? = nil) -> Date {
        let cal = Calendar.current
        switch frequency {
        case .daily:      return cal.date(byAdding: .day,   value: 1,              to: date) ?? date
        case .weekly:     return cal.date(byAdding: .day,   value: 7,              to: date) ?? date
        case .biweekly:   return cal.date(byAdding: .day,   value: 14,             to: date) ?? date
        case .monthly:    return cal.date(byAdding: .month, value: 1,              to: date) ?? date
        case .bimonthly:  return cal.date(byAdding: .month, value: 2,              to: date) ?? date
        case .quarterly:  return cal.date(byAdding: .month, value: 3,              to: date) ?? date
        case .custom:     return cal.date(byAdding: .day,   value: customDays ?? 30, to: date) ?? date
        }
    }
}
