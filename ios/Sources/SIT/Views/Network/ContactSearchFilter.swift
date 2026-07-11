import Foundation

/// Pure, case-insensitive search predicate behind `NetworkListView`'s search
/// field (TIC-92). Broadened from the old name/company-only match to also cover
/// phone numbers (digit-normalized), tags, and notes. Kept free of
/// SwiftUI/SwiftData so the matching rules are unit-testable without hosting the
/// view or spinning up a `ModelContainer`.
enum ContactSearchFilter {

    /// Strips a phone string down to its digits so a loose numeric query matches
    /// regardless of formatting — "5551234" matches a stored "+1 (555) 123-4…"
    /// and a query of "(555)" matches "5551234567".
    static func normalizedDigits(_ s: String) -> String {
        s.filter { $0.isNumber }
    }

    /// Whether a contact's fields match `query`. An empty/whitespace query
    /// matches everything (the "no search" case). Field-level overload so tests
    /// can exercise the rules without constructing SwiftData models.
    static func matches(name: String,
                        company: String,
                        phoneNumbers: [String],
                        tags: [String],
                        notes: String,
                        query: String) -> Bool {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return true }

        if name.localizedCaseInsensitiveContains(q) { return true }
        if company.localizedCaseInsensitiveContains(q) { return true }
        if notes.localizedCaseInsensitiveContains(q) { return true }
        if tags.contains(where: { $0.localizedCaseInsensitiveContains(q) }) { return true }

        // Phone matching only engages when the query carries digits, so a plain
        // text query like "Ann" never spuriously lands inside a phone number.
        let qDigits = normalizedDigits(q)
        if !qDigits.isEmpty,
           phoneNumbers.contains(where: { normalizedDigits($0).contains(qDigits) }) {
            return true
        }
        return false
    }

    /// Convenience overload for a live `Contact`.
    static func matches(_ contact: Contact, query: String) -> Bool {
        matches(name: contact.fullName,
                company: contact.company,
                phoneNumbers: contact.phoneNumbers,
                tags: contact.tags,
                notes: contact.notes,
                query: query)
    }
}
