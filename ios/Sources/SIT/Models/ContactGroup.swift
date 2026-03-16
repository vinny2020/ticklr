import SwiftData
import Foundation

@Model
final class ContactGroup {
    var id: UUID
    var name: String
    var emoji: String
    var createdAt: Date
    var contacts: [Contact]

    init(name: String, emoji: String = "👥") {
        self.id = UUID()
        self.name = name
        self.emoji = emoji
        self.createdAt = Date()
        self.contacts = []
    }
}
