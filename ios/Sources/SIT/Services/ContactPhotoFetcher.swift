import Foundation
import Contacts
import UIKit

/// Read-only resolver for system Contacts photos. Matches a Ticklr
/// `Contact` against `CNContactStore` entries by normalised phone or
/// email — never by name (too unreliable across locales). Returns nil if
/// no match or no photo. Caches results for the session.
///
/// Authorization is shared with the existing import flow
/// (`NSContactsUsageDescription` + `CNContactStore.requestAccess`); this
/// fetcher silently returns nil if the user has not granted access.
enum ContactPhotoFetcher {

    /// Cache keyed by `Contact.id.uuidString`. NSCache is documented as
    /// thread-safe; `nonisolated(unsafe)` opts it out of Swift 6's
    /// Sendable check.
    nonisolated(unsafe) private static let cache = NSCache<NSString, UIImage>()

    /// Caller passes Sendable primitives so the SwiftData `Contact` model
    /// (non-Sendable) never crosses the actor boundary.
    static func fetch(contactId: UUID,
                      phoneNumbers: [String],
                      emails: [String]) async -> UIImage? {
        let key = contactId.uuidString as NSString
        if let cached = cache.object(forKey: key) {
            return cached
        }
        guard CNContactStore.authorizationStatus(for: .contacts) == .authorized else {
            return nil
        }

        let phones = Set(phoneNumbers.map(normalisePhone).filter { !$0.isEmpty })
        let normEmails = Set(emails.map {
            $0.lowercased().trimmingCharacters(in: .whitespaces)
        }.filter { !$0.isEmpty })

        guard !phones.isEmpty || !normEmails.isEmpty else { return nil }

        let resolved = await Task.detached(priority: .userInitiated) {
            return enumerateMatch(phones: phones, emails: normEmails)
        }.value

        if let resolved {
            cache.setObject(resolved, forKey: key)
        }
        return resolved
    }

    static func clearCache() {
        cache.removeAllObjects()
    }

    // MARK: - Internals

    private static func enumerateMatch(phones: Set<String>,
                                       emails: Set<String>) -> UIImage? {
        let store = CNContactStore()
        let keys: [CNKeyDescriptor] = [
            CNContactPhoneNumbersKey as CNKeyDescriptor,
            CNContactEmailAddressesKey as CNKeyDescriptor,
            CNContactThumbnailImageDataKey as CNKeyDescriptor,
        ]
        let request = CNContactFetchRequest(keysToFetch: keys)

        var found: UIImage?
        do {
            try store.enumerateContacts(with: request) { cn, stop in
                let cnPhones = Set(cn.phoneNumbers.map { normalisePhone($0.value.stringValue) })
                let cnEmails = Set(cn.emailAddresses.map {
                    ($0.value as String).lowercased().trimmingCharacters(in: .whitespaces)
                })
                if !phones.isDisjoint(with: cnPhones) || !emails.isDisjoint(with: cnEmails) {
                    if let data = cn.thumbnailImageData,
                       let image = UIImage(data: data) {
                        found = image
                        stop.pointee = true
                    }
                }
            }
        } catch {
            return nil
        }
        return found
    }

    /// Normalise to digits-only, last 10 (US-bias but matches what the
    /// importer does when fingerprinting).
    private static func normalisePhone(_ raw: String) -> String {
        let digits = raw.components(separatedBy: CharacterSet.decimalDigits.inverted).joined()
        return String(digits.suffix(10))
    }
}
