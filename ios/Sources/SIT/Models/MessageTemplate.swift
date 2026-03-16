import SwiftData
import Foundation

@Model
final class MessageTemplate {
    var id: UUID
    var title: String
    var body: String
    var createdAt: Date

    init(title: String, body: String) {
        self.id = UUID()
        self.title = title
        self.body = body
        self.createdAt = Date()
    }
}
