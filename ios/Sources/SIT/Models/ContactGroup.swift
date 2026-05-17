import SwiftData
import Foundation

@Model
final class ContactGroup {
    var id: UUID
    var name: String
    var emoji: String
    var createdAt: Date
    var contacts: [Contact]
    var tickles: [TickleReminder] = []

    init(name: String, emoji: String = "👥", id: UUID = UUID()) {
        self.id = id
        self.name = name
        self.emoji = emoji
        self.createdAt = Date()
        self.contacts = []
    }
}
