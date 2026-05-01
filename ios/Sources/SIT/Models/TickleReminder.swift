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

    var localizedName: String {
        switch self {
        case .daily:     return String(localized: "frequency.daily")
        case .weekly:    return String(localized: "frequency.weekly")
        case .biweekly:  return String(localized: "frequency.biweekly")
        case .monthly:   return String(localized: "frequency.monthly")
        case .bimonthly: return String(localized: "frequency.bimonthly")
        case .quarterly: return String(localized: "frequency.quarterly")
        case .custom:    return String(localized: "frequency.custom")
        }
    }
}

enum TickleStatus: String, Codable {
    case active
    case snoozed
    case completed
}

@Model
final class TickleReminder {
    var id: UUID
    @Relationship(deleteRule: .nullify, inverse: \Contact.tickles)
    var contact: Contact?

    @Relationship(deleteRule: .nullify, inverse: \ContactGroup.tickles)
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
        self.nextDueDate = TickleScheduler.initialNextDueDate(
            from: startDate,
            frequency: frequency,
            customDays: customIntervalDays
        )
        self.status = .active
        self.createdAt = Date()
    }
}
