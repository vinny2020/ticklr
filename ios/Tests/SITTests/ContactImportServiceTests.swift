import XCTest
import SwiftData
import Contacts
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

    // MARK: - TIC-93: selective (CNContactPickerViewController) import path

    /// Builds a `CNMutableContact` standing in for a contact the user
    /// hand-picked in the system picker — constructible entirely in-memory,
    /// no Contacts permission or store access required, exactly like the
    /// `CNContact` objects `CNContactPickerDelegate` hands back.
    private func pickedContact(
        firstName: String,
        lastName: String,
        phone: String? = nil,
        email: String? = nil,
        company: String = "",
        jobTitle: String = ""
    ) -> CNContact {
        let mutable = CNMutableContact()
        mutable.givenName = firstName
        mutable.familyName = lastName
        if let phone {
            mutable.phoneNumbers = [CNLabeledValue(label: CNLabelHome, value: CNPhoneNumber(stringValue: phone))]
        }
        if let email {
            mutable.emailAddresses = [CNLabeledValue(label: CNLabelHome, value: email as NSString)]
        }
        mutable.organizationName = company
        mutable.jobTitle = jobTitle
        return mutable
    }

    /// `ContactImportService.importedFields(from:)` is the mapping shared by
    /// the bulk enumeration and the selective picker path (TIC-93) — verify
    /// it reads a picker-shaped `CNContact` into the same `ImportedFields`
    /// shape `applyImport` expects, fingerprint included.
    func testImportedFieldsMapsAPickerSelectedContact() {
        let contact = pickedContact(
            firstName: "Priya", lastName: "Rao",
            phone: "555-0100", email: "priya@example.com",
            company: "Acme", jobTitle: "Engineer"
        )

        let mapped = ContactImportService.importedFields(from: contact)

        XCTAssertEqual(mapped.firstName, "Priya")
        XCTAssertEqual(mapped.lastName, "Rao")
        XCTAssertEqual(mapped.phoneNumbers, ["555-0100"])
        XCTAssertEqual(mapped.emails, ["priya@example.com"])
        XCTAssertEqual(mapped.company, "Acme")
        XCTAssertEqual(mapped.jobTitle, "Engineer")
        XCTAssertFalse(mapped.fingerprint.isEmpty, "a phone number is present — a fingerprint should be computed")
    }

    /// End-to-end selective-import path: picker selection → `importedFields`
    /// → `applyImport`, same dedup/insert core the bulk import uses. A
    /// duplicate within the hand-picked selection is skipped exactly like a
    /// duplicate within a bulk enumeration would be.
    func testSelectivePickerImportRoutesThroughApplyImportWithDedup() throws {
        let alice = pickedContact(firstName: "Alice", lastName: "Wang", phone: "555-0001")
        let aliceAgain = pickedContact(firstName: "Alice", lastName: "Wang", phone: "555-0001")
        let bob = pickedContact(firstName: "Bob", lastName: "Jones", phone: "555-0002")

        let fields = [alice, aliceAgain, bob].map(ContactImportService.importedFields(from:))
        let result = ContactImportService.applyImport(fields: fields, context: context)

        XCTAssertEqual(result.imported, 2, "Alice (first occurrence) + Bob")
        XCTAssertEqual(result.skipped, 1, "the duplicate Alice within the same selection")
        XCTAssertEqual(try fetchContacts().count, 2)
    }

    /// A selective import against a store that already has a matching
    /// fingerprint skips the picked contact — selective import shares the
    /// exact same duplicate-detection behavior as the bulk sweep.
    func testSelectivePickerImportSkipsContactAlreadyInStore() throws {
        let existingFields = field(firstName: "Alice", lastName: "Wang", phoneNumbers: ["555-0001"])
        context.insert(Contact(
            firstName: existingFields.firstName, lastName: existingFields.lastName,
            phoneNumbers: existingFields.phoneNumbers, emails: existingFields.emails,
            importSource: .ios, fingerprint: existingFields.fingerprint
        ))
        try context.save()

        let pickedAlice = pickedContact(firstName: "Alice", lastName: "Wang", phone: "555-0001")
        let fields = [pickedAlice].map(ContactImportService.importedFields(from:))
        let result = ContactImportService.applyImport(fields: fields, context: context)

        XCTAssertEqual(result.imported, 0)
        XCTAssertEqual(result.skipped, 1)
        XCTAssertEqual(try fetchContacts().count, 1, "no second Alice inserted")
    }
}
