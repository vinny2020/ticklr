import Contacts
import SwiftData
import Foundation

struct ContactImportService {

    /// Sendable snapshot of the fields pulled off a `CNContact`, plus its
    /// precomputed dedup fingerprint. `CNContact` itself is not Sendable,
    /// so instances never leave the detached task that enumerates them —
    /// only these plain values cross back to the main actor.
    private struct ImportedFields: Sendable {
        let firstName: String
        let lastName: String
        let phoneNumbers: [String]
        let emails: [String]
        let company: String
        let jobTitle: String
        let fingerprint: String
    }

    /// Requests Contacts permission and imports all contacts into SwiftData.
    /// Call from onboarding only — requires CNContactStore access.
    @MainActor
    static func importFromiOS(context: ModelContext) async throws {
        // Fetch existing fingerprints to detect duplicates. Mutated as we go so
        // duplicates *within* this same import are also caught, not just ones
        // that already existed in the store before the loop started.
        var seenFingerprints: Set<String> = {
            let descriptor = FetchDescriptor<Contact>()
            let all = (try? context.fetch(descriptor)) ?? []
            return Set(all.map(\.fingerprint).filter { !$0.isEmpty })
        }()

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

        var imported = 0
        var skipped = 0

        for fields in fetched {
            if !fields.fingerprint.isEmpty && seenFingerprints.contains(fields.fingerprint) {
                skipped += 1
                continue
            }

            let contact = Contact(
                firstName: fields.firstName,
                lastName: fields.lastName,
                phoneNumbers: fields.phoneNumbers,
                emails: fields.emails,
                company: fields.company,
                jobTitle: fields.jobTitle,
                importSource: .ios,
                fingerprint: fields.fingerprint
            )
            context.insert(contact)
            if !fields.fingerprint.isEmpty { seenFingerprints.insert(fields.fingerprint) }
            imported += 1
        }

        try context.save()
        print("Ticklr: Imported \(imported) contacts from iOS, skipped \(skipped) duplicates")
    }
}
