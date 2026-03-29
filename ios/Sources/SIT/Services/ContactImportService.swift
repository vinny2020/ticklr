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

        let request = CNContactFetchRequest(keysToFetch: keys)
        var imported = 0

        try store.enumerateContacts(with: request) { cnContact, _ in
            let contact = Contact(
                firstName: cnContact.givenName,
                lastName: cnContact.familyName,
                phoneNumbers: cnContact.phoneNumbers.map { $0.value.stringValue },
                emails: cnContact.emailAddresses.map { $0.value as String },
                company: cnContact.organizationName,
                jobTitle: cnContact.jobTitle,
                importSource: .ios
            )
            context.insert(contact)
            imported += 1
        }

        try context.save()
        print("Ticklr: Imported \(imported) contacts from iOS")
    }
}
