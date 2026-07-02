import Foundation
import CryptoKit

/// Generates a stable deduplication fingerprint for a contact.
///
/// Strategy (mirrors Android implementation):
///   fingerprint = SHA-1( normalize(firstName) + "|" + normalize(lastName) + "|" + normalize(primaryPhone or primaryEmail) )
///
/// Normalization:
///   - Trim and lowercase
///   - Phone: strip all non-digit characters
///   - Email: trim and lowercase
///
/// Returns an empty string when the contact has no phone AND no email — a name
/// alone is not a reliable identity (LinkedIn omits email for most connections,
/// so two different "John Smith"s must not collide). Empty fingerprints are
/// treated as unset and are never deduplicated. Android mirrors this rule.
enum ContactFingerprint {

    static func compute(
        firstName: String,
        lastName: String,
        phoneNumbers: [String],
        emails: [String]
    ) -> String {
        let first = firstName.trimmingCharacters(in: .whitespaces).lowercased()
        let last  = lastName.trimmingCharacters(in: .whitespaces).lowercased()

        let primaryPhone = phoneNumbers.first
            .map { $0.components(separatedBy: CharacterSet.decimalDigits.inverted).joined() }
            ?? ""
        let primaryEmail = emails.first?
            .trimmingCharacters(in: .whitespaces).lowercased()
            ?? ""

        let contactKey = primaryPhone.isEmpty ? primaryEmail : primaryPhone

        // No phone AND no email → not enough to safely dedup on. Leave unset.
        guard !contactKey.isEmpty else { return "" }

        let raw = "\(first)|\(last)|\(contactKey)"
        return sha1(raw)
    }

    private static func sha1(_ input: String) -> String {
        let data = Data(input.utf8)
        let digest = Insecure.SHA1.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
