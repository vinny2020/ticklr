import Foundation
import SwiftData

/// Seeds the 5 canonical relationship groups (Family / Close Friends /
/// Work / Milestones / Neighbors & Community) on first launch.
///
/// Per the warm-redesign decision "categories ARE groups", each canonical
/// category is a real `ContactGroup` identified by a stable UUID checked
/// in to `WarmCategory.groupUUID`. This service keeps those rows present:
///   - If a row with the canonical UUID already exists → leave it alone.
///   - Else if the user already has a group whose name (case-insensitive)
///     matches the canonical localized name → adopt it by rewriting its
///     id to the canonical UUID. Their existing contacts/tickles in that
///     group are preserved.
///   - Else → insert a new `ContactGroup` with the canonical UUID,
///     localized name, and category emoji.
///
/// Idempotent — safe to run on every launch. No UserDefaults flag.
enum CanonicalGroupSeed {

    static func seedIfNeeded(container: ModelContainer) {
        let context = ModelContext(container)
        let existing = (try? context.fetch(FetchDescriptor<ContactGroup>())) ?? []
        let existingById = Dictionary(uniqueKeysWithValues: existing.map { ($0.id, $0) })
        let existingByName = Dictionary(grouping: existing) {
            $0.name.lowercased().trimmingCharacters(in: .whitespaces)
        }

        var dirty = false
        for category in WarmCategory.allCases {
            let canonicalId = category.groupUUID
            if existingById[canonicalId] != nil { continue }

            let localizedName = category.localizedGroupName
            let nameKey = localizedName.lowercased().trimmingCharacters(in: .whitespaces)
            if let collisions = existingByName[nameKey], let adopt = collisions.first {
                adopt.id = canonicalId
                dirty = true
                continue
            }

            let group = ContactGroup(
                name: localizedName,
                emoji: category.defaultEmoji,
                id: canonicalId
            )
            context.insert(group)
            dirty = true
        }

        if dirty {
            try? context.save()
        }
    }
}
