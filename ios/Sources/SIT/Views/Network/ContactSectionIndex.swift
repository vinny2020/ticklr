import Foundation

/// Pure A–Z section-index logic behind `NetworkListView`'s fast-scroll rail
/// (TIC-92). SwiftUI has no native `sectionIndexTitles`, so the view sections a
/// plain `List` by these buckets and overlays a letter rail wired to a
/// `ScrollViewReader`. Kept free of SwiftUI/SwiftData so the bucketing rules are
/// unit-testable without hosting the view or a `ModelContainer`.
enum ContactSectionIndex {

    /// The alphabetization key for a contact: last name, falling back to first
    /// name when the last name is blank (mirrors the list's lastName sort while
    /// still giving first-name-only contacts a sensible bucket). Trimmed.
    static func sortKey(lastName: String, firstName: String) -> String {
        let last = lastName.trimmingCharacters(in: .whitespacesAndNewlines)
        if !last.isEmpty { return last }
        return firstName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    static func sortKey(for contact: Contact) -> String {
        sortKey(lastName: contact.lastName, firstName: contact.firstName)
    }

    /// The section bucket for a sort key: the first character, diacritic-folded
    /// to its base and uppercased, when it's a letter — otherwise "#" (digits,
    /// symbols, emoji, or empty). Folding groups "Ó" under "O" and "ñ" under "N".
    static func sectionLetter(for key: String) -> String {
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let first = trimmed.first else { return "#" }
        let folded = String(first).folding(options: .diacriticInsensitive, locale: .current)
        guard let base = folded.first, base.isLetter else { return "#" }
        return String(base).uppercased()
    }

    /// One section: its index letter and the members that fall under it, in order.
    struct Section<T>: Identifiable {
        let letter: String
        let items: [T]
        var id: String { letter }
    }

    /// Buckets `items` into ordered A–Z sections by `key`. Items are first sorted
    /// by the key (localized, case-insensitive); lettered sections come out in
    /// alphabetical order and the non-letter "#" bucket, if present, always sorts
    /// last — matching the system Contacts index.
    static func sections<T>(from items: [T], key: (T) -> String) -> [Section<T>] {
        let sorted = items.sorted {
            key($0).localizedCaseInsensitiveCompare(key($1)) == .orderedAscending
        }
        var order: [String] = []
        var buckets: [String: [T]] = [:]
        for item in sorted {
            let letter = sectionLetter(for: key(item))
            if buckets[letter] == nil {
                buckets[letter] = []
                order.append(letter)
            }
            buckets[letter]?.append(item)
        }
        let letters = order.sorted { a, b in
            if a == "#" { return false }   // "#" always last
            if b == "#" { return true }
            return a.localizedCaseInsensitiveCompare(b) == .orderedAscending
        }
        return letters.map { Section(letter: $0, items: buckets[$0] ?? []) }
    }

    /// Convenience overload for live `Contact`s, keyed by `sortKey(for:)`.
    static func sections(from contacts: [Contact]) -> [Section<Contact>] {
        sections(from: contacts) { sortKey(for: $0) }
    }
}
