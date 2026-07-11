import Contacts
import SwiftData
import Foundation

struct ContactImportService {

    /// Sendable snapshot of the fields pulled off a `CNContact`, plus its
    /// precomputed dedup fingerprint. `CNContact` itself is not Sendable,
    /// so instances never leave the detached task that enumerates them —
    /// only these plain values cross back to the main actor. Not `private`
    /// (TIC-85) so `applyImport` — the CNContactStore-free dedup/insert core —
    /// is directly unit-testable against an in-memory `ModelContext`.
    struct ImportedFields: Sendable {
        let firstName: String
        let lastName: String
        let phoneNumbers: [String]
        let emails: [String]
        let company: String
        let jobTitle: String
        let fingerprint: String
    }

    /// Requests Contacts permission and imports all contacts into SwiftData.
    /// Call from onboarding or any recurring "import from iPhone Contacts"
    /// entry point — requires CNContactStore access. Returns the (imported,
    /// skipped) counts (TIC-85) so callers can surface real feedback instead
    /// of the old silent `print()`.
    @MainActor
    @discardableResult
    static func importFromiOS(context: ModelContext) async throws -> (imported: Int, skipped: Int) {
        // Requesting access and enumerating are both blocking, synchronous
        // CNContactStore calls — run them off the main actor so onboarding
        // doesn't freeze for the whole import (mirrors ContactPhotoFetcher's
        // detached enumeration).
        let fetched: [ImportedFields] = try await Task.detached(priority: .userInitiated) {
            let store = CNContactStore()
            let keys: [CNKeyDescriptor] = [
                CNContactGivenNameKey as CNKeyDescriptor,
                CNContactFamilyNameKey as CNKeyDescriptor,
                CNContactPhoneNumbersKey as CNKeyDescriptor,
                CNContactEmailAddressesKey as CNKeyDescriptor,
                CNContactOrganizationNameKey as CNKeyDescriptor,
                CNContactJobTitleKey as CNKeyDescriptor
            ]

            try await store.requestAccess(for: .contacts)

            let request = CNContactFetchRequest(keysToFetch: keys)
            var results: [ImportedFields] = []
            try store.enumerateContacts(with: request) { cnContact, _ in
                let phoneNumbers = cnContact.phoneNumbers.map { $0.value.stringValue }
                let emails = cnContact.emailAddresses.map { $0.value as String }
                let fingerprint = ContactFingerprint.compute(
                    firstName: cnContact.givenName,
                    lastName: cnContact.familyName,
                    phoneNumbers: phoneNumbers,
                    emails: emails
                )
                results.append(
                    ImportedFields(
                        firstName: cnContact.givenName,
                        lastName: cnContact.familyName,
                        phoneNumbers: phoneNumbers,
                        emails: emails,
                        company: cnContact.organizationName,
                        jobTitle: cnContact.jobTitle,
                        fingerprint: fingerprint
                    )
                )
            }
            return results
        }.value

        let result = applyImport(fields: fetched, context: context)
        try context.save()
        return result
    }

    /// The CNContactStore-free dedup/insert core (TIC-85): given already-fetched
    /// `fields`, skips anything matching an existing (or earlier-in-this-batch)
    /// fingerprint and inserts the rest as `Contact`s. Split out from
    /// `importFromiOS` so it's unit-testable against an in-memory `ModelContext`
    /// without touching the real Contacts store. Does not call `context.save()`
    /// itself — callers control when persistence happens.
    @MainActor
    static func applyImport(fields: [ImportedFields], context: ModelContext) -> (imported: Int, skipped: Int) {
        // Fetch existing fingerprints to detect duplicates. Mutated as we go so
        // duplicates *within* this same import are also caught, not just ones
        // that already existed in the store before the loop started.
        var seenFingerprints: Set<String> = {
            let descriptor = FetchDescriptor<Contact>()
            let all = (try? context.fetch(descriptor)) ?? []
            return Set(all.map(\.fingerprint).filter { !$0.isEmpty })
        }()

        var imported = 0
        var skipped = 0

        for field in fields {
            if !field.fingerprint.isEmpty && seenFingerprints.contains(field.fingerprint) {
                skipped += 1
                continue
            }

            let contact = Contact(
                firstName: field.firstName,
                lastName: field.lastName,
                phoneNumbers: field.phoneNumbers,
                emails: field.emails,
                company: field.company,
                jobTitle: field.jobTitle,
                importSource: .ios,
                fingerprint: field.fingerprint
            )
            context.insert(contact)
            if !field.fingerprint.isEmpty { seenFingerprints.insert(field.fingerprint) }
            imported += 1
        }

        return (imported, skipped)
    }
}
