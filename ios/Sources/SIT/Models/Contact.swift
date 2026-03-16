import SwiftData
import Foundation

enum ImportSource: String, Codable {
    case manual
    case ios
    case linkedin
}

@Model
final class Contact {
    var id: UUID
    var firstName: String
    var lastName: String
    var phoneNumbers: [String]
    var emails: [String]
    var company: String
    var jobTitle: String
    var notes: String
    var tags: [String]
    var importSource: ImportSource
    var createdAt: Date
    var lastContactedAt: Date?

    @Relationship(deleteRule: .nullify, inverse: \ContactGroup.contacts)
    var groups: [ContactGroup]

    init(
        firstName: String,
        lastName: String,
        phoneNumbers: [String] = [],
        emails: [String] = [],
        company: String = "",
        jobTitle: String = "",
        notes: String = "",
        tags: [String] = [],
        importSource: ImportSource = .manual
    ) {
        self.id = UUID()
        self.firstName = firstName
        self.lastName = lastName
        self.phoneNumbers = phoneNumbers
        self.emails = emails
        self.company = company
        self.jobTitle = jobTitle
        self.notes = notes
        self.tags = tags
        self.importSource = importSource
        self.createdAt = Date()
        self.groups = []
    }

    var fullName: String {
        "\(firstName) \(lastName)".trimmingCharacters(in: .whitespaces)
    }

    var initials: String {
        let f = firstName.prefix(1).uppercased()
        let l = lastName.prefix(1).uppercased()
        return "\(f)\(l)"
    }
}
