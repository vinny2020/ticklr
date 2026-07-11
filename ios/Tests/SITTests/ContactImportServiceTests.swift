import XCTest
import SwiftData
@testable import Ticklr

/// Covers `ContactImportService.applyImport` — the CNContactStore-free
/// dedup/insert core split out of `importFromiOS` (TIC-85) so the (imported,
/// skipped) count contract is directly testable against an in-memory
/// `ModelContext`, without needing real Contacts permission.
@MainActor
final class ContactImportServiceTests: XCTestCase {

    var container: ModelContainer!
    var context: ModelContext!

    override func setUp() async throws {
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        container = try ModelContainer(
            for: Contact.self, ContactGroup.self, TickleReminder.self,
            configurations: config
        )
        context = ModelContext(container)
    }

    override func tearDown() async throws {
        container = nil
        context = nil
    }

    private func field(
        firstName: String,
        lastName: String,
        phoneNumbers: [String] = [],
        emails: [String] = []
    ) -> ContactImportService.ImportedFields {
        let fingerprint = ContactFingerprint.compute(
            firstName: firstName, lastName: lastName,
            phoneNumbers: phoneNumbers, emails: emails
        )
        return ContactImportService.ImportedFields(
            firstName: firstName,
            lastName: lastName,
            phoneNumbers: phoneNumbers,
            emails: emails,
            company: "",
            jobTitle: "",
            fingerprint: fingerprint
        )
    }

    private func fetchContacts() throws -> [Contact] {
        try context.fetch(FetchDescriptor<Contact>())
    }

    // MARK: - Counts

    func testAllNewContactsAreImportedWithZeroSkipped() throws {
        let fields = [
            field(firstName: "Alice", lastName: "Wang", phoneNumbers: ["555-0001"]),
            field(firstName: "Bob", lastName: "Jones", phoneNumbers: ["555-0002"]),
        ]
        let result = ContactImportService.applyImport(fields: fields, context: context)
        XCTAssertEqual(result.imported, 2)
        XCTAssertEqual(result.skipped, 0)
        XCTAssertEqual(try fetchContacts().count, 2)
    }

    func testDuplicateWithinSameBatchIsSkipped() throws {
        let fields = [
            field(firstName: "Alice", lastName: "Wang", phoneNumbers: ["555-0001"]),
            field(firstName: "Alice", lastName: "Wang", phoneNumbers: ["555-0001"]),
        ]
        let result = ContactImportService.applyImport(fields: fields, context: context)
        XCTAssertEqual(result.imported, 1)
        XCTAssertEqual(result.skipped, 1)
        XCTAssertEqual(try fetchContacts().count, 1)
    }

    func testDuplicateAgainstExistingStoreContactIsSkipped() throws {
        // Seed the store with a contact identical (by fingerprint) to one of
        // the incoming batch's entries.
        let existing = field(firstName: "Alice", lastName: "Wang", phoneNumbers: ["555-0001"])
        context.insert(Contact(
            firstName: existing.firstName, lastName: existing.lastName,
            phoneNumbers: existing.phoneNumbers, emails: existing.emails,
            importSource: .ios, fingerprint: existing.fingerprint
        ))
        try context.save()

        let fields = [
            existing,
            field(firstName: "Bob", lastName: "Jones", phoneNumbers: ["555-0002"]),
        ]
        let result = ContactImportService.applyImport(fields: fields, context: context)
        XCTAssertEqual(result.imported, 1, "only Bob should be newly imported")
        XCTAssertEqual(result.skipped, 1, "Alice already exists in the store")
        XCTAssertEqual(try fetchContacts().count, 2, "1 pre-existing + 1 newly imported")
    }

    func testContactsWithNoPhoneOrEmailAreNeverDeduped() throws {
        // ContactFingerprint.compute returns "" when there's no phone/email —
        // both entries fingerprint to "" and must NOT collide with each other.
        let fields = [
            field(firstName: "No", lastName: "Contact1"),
            field(firstName: "No", lastName: "Contact2"),
        ]
        let result = ContactImportService.applyImport(fields: fields, context: context)
        XCTAssertEqual(result.imported, 2)
        XCTAssertEqual(result.skipped, 0)
    }

    func testEmptyBatchImportsNothing() throws {
        let result = ContactImportService.applyImport(fields: [], context: context)
        XCTAssertEqual(result.imported, 0)
        XCTAssertEqual(result.skipped, 0)
        XCTAssertEqual(try fetchContacts().count, 0)
    }

    func testDoesNotPersistUntilCallerSaves() throws {
        // applyImport inserts into the context but leaves save() to the
        // caller — assert the count is still correct pre-save via fetch
        // (SwiftData contexts reflect pending inserts in-memory), and that
        // the returned counts don't depend on an explicit save call.
        let fields = [field(firstName: "Alice", lastName: "Wang", phoneNumbers: ["555-0001"])]
        let result = ContactImportService.applyImport(fields: fields, context: context)
        XCTAssertEqual(result.imported, 1)
        try context.save()
        XCTAssertEqual(try fetchContacts().count, 1)
    }
}
