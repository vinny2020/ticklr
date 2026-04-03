import Contacts
import SwiftData
import Foundation

struct ContactImportService {

    /// Requests Contacts permission and imports all contacts into SwiftData.
    /// Call from onboarding only — requires CNContactStore access.
    @MainActor
    static func importFromiOS(context: ModelContext) async throws {
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

        // Fetch existing fingerprints to detect duplicates
        let existingFingerprints: Set<String> = {
            let descriptor = FetchDescriptor<Contact>()
            let all = (try? context.fetch(descriptor)) ?? []
            return Set(all.map(\.fingerprint).filter { !$0.isEmpty })
        }()

        let request = CNContactFetchRequest(keysToFetch: keys)
        var imported = 0
        var skipped = 0

        try store.enumerateContacts(with: request) { cnContact, _ in
            let phoneNumbers = cnContact.phoneNumbers.map { $0.value.stringValue }
            let emails = cnContact.emailAddresses.map { $0.value as String }

            let fingerprint = ContactFingerprint.compute(
                firstName: cnContact.givenName,
                lastName: cnContact.familyName,
                phoneNumbers: phoneNumbers,
                emails: emails
            )

            if !fingerprint.isEmpty && existingFingerprints.contains(fingerprint) {
                skipped += 1
                return
            }

            let contact = Contact(
                firstName: cnContact.givenName,
                lastName: cnContact.familyName,
                phoneNumbers: phoneNumbers,
                emails: emails,
                company: cnContact.organizationName,
                jobTitle: cnContact.jobTitle,
                importSource: .ios,
                fingerprint: fingerprint
            )
            context.insert(contact)
            imported += 1
        }

        try context.save()
        print("Ticklr: Imported \(imported) contacts from iOS, skipped \(skipped) duplicates")
    }
}
