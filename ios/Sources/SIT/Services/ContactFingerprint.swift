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
/// Returns an empty string if there is no identifying info — these contacts
/// are not deduplicated and can always be inserted.
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

        // Don't fingerprint contacts with no identifying info
        guard !first.isEmpty || !last.isEmpty || !contactKey.isEmpty else { return "" }

        let raw = "\(first)|\(last)|\(contactKey)"
        return sha1(raw)
    }

    private static func sha1(_ input: String) -> String {
        let data = Data(input.utf8)
        let digest = Insecure.SHA1.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
