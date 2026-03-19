import Foundation
import SwiftData

/// DEBUG ONLY — loads test_contacts.csv from the app bundle and imports it
/// via LinkedInCSVParser. Gated by #if DEBUG so it is compiled out of
/// release builds.
#if DEBUG
struct SeedDataService {

    /// Inserts all contacts from the bundled test_contacts.csv into the given context.
    /// Returns the number of contacts inserted.
    @discardableResult
    static func seedTestContacts(context: ModelContext) throws -> Int {
        guard let url = Bundle.main.url(forResource: "test_contacts", withExtension: "csv") else {
            throw SeedError.fileNotFound
        }
        try LinkedInCSVParser.parse(url: url, context: context)
        // Count how many were just inserted by fetching total
        let descriptor = FetchDescriptor<Contact>()
        let all = try context.fetch(descriptor)
        return all.count
    }

    enum SeedError: LocalizedError {
        case fileNotFound
        var errorDescription: String? {
            "test_contacts.csv not found in app bundle."
        }
    }
}
#endif
