import SwiftData
import Foundation

enum TickleFrequency: String, Codable, CaseIterable {
    case daily        = "Daily"
    case weekly       = "Weekly"
    case biweekly     = "Every 2 weeks"
    case monthly      = "Monthly"
    case bimonthly    = "Every 2 months"
    case quarterly    = "Quarterly"
    case custom       = "Custom"
}

enum TickleStatus: String, Codable {
    case active
    case snoozed
    case completed
}

@Model
final class TickleReminder {
    var id: UUID
    var contact: Contact?
    var group: ContactGroup?
    var note: String
    var frequency: TickleFrequency
    var customIntervalDays: Int?
    var startDate: Date
    var nextDueDate: Date
    var lastCompletedDate: Date?
    var status: TickleStatus
    var createdAt: Date

    init(
        contact: Contact? = nil,
        group: ContactGroup? = nil,
        note: String = "",
        frequency: TickleFrequency = .monthly,
        customIntervalDays: Int? = nil,
        startDate: Date = Date()
    ) {
        self.id = UUID()
        self.contact = contact
        self.group = group
        self.note = note
        self.frequency = frequency
        self.customIntervalDays = customIntervalDays
        self.startDate = startDate
        self.nextDueDate = startDate
        self.status = .active
        self.createdAt = Date()
    }
}
