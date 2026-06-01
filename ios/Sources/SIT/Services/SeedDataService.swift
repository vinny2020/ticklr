import Foundation
import SwiftData

/// DEBUG ONLY — loads test_contacts.csv from the app bundle and imports it
/// via LinkedInCSVParser. Gated by #if DEBUG so it is compiled out of
/// release builds.
#if DEBUG
struct SeedDataService {

    /// Inserts all contacts from the bundled test_contacts.csv into the given context.
    /// Returns the number of contacts inserted.
    @discardableResult
    static func seedTestContacts(context: ModelContext) throws -> (imported: Int, skipped: Int) {
        guard let url = Bundle.main.url(forResource: "test_contacts", withExtension: "csv") else {
            throw SeedError.fileNotFound
        }
        return try LinkedInCSVParser.parse(url: url, context: context)
    }

    /// Populates the app with screenshot-ready demo data in one tap: reuses
    /// existing contacts (seeding the test set first only if there are too few),
    /// distributes them across the canonical groups, and creates a realistic
    /// spread of tickles — a few due today, some upcoming, one snoozed, and a
    /// Milestones group reminder. Group assignment is idempotent; tickles are
    /// only created when none exist yet, so re-tapping won't pile up duplicates.
    @discardableResult
    static func seedDemoData(context: ModelContext) throws -> String {
        // 1. Contacts — use what's there; seed the test set only if sparse.
        var contacts = try context.fetch(
            FetchDescriptor<Contact>(sortBy: [SortDescriptor(\.firstName)])
        )
        if contacts.count < 6 {
            try seedTestContacts(context: context)
            contacts = try context.fetch(
                FetchDescriptor<Contact>(sortBy: [SortDescriptor(\.firstName)])
            )
        }
        guard !contacts.isEmpty else { return "No contacts available to build demo data from." }

        // 2. Canonical groups by stable UUID (auto-seeded at launch; recreate if missing).
        func group(_ cat: WarmCategory) -> ContactGroup {
            let uuid = cat.groupUUID
            let desc = FetchDescriptor<ContactGroup>(predicate: #Predicate { $0.id == uuid })
            if let existing = try? context.fetch(desc).first { return existing }
            let g = ContactGroup(name: cat.localizedGroupName, emoji: cat.defaultEmoji, id: cat.groupUUID)
            context.insert(g)
            return g
        }
        let family = group(.family)
        let friends = group(.friends)
        let work = group(.work)
        let milestones = group(.milestones)
        let community = group(.community)

        // 3. Distribute contacts across groups (idempotent: skip if already a member).
        func add(_ contact: Contact, to g: ContactGroup) {
            if !g.contacts.contains(where: { $0.id == contact.id }) {
                g.contacts.append(contact)
            }
        }
        let buckets = [family, friends, work, community]
        for (i, contact) in contacts.enumerated() {
            add(contact, to: buckets[i % buckets.count])
            if i % 4 == 0 { add(contact, to: family) }   // a few belong to more than one circle
        }

        // 4. Tickles — create the demo spread, idempotent per note so a stray
        //    pre-existing reminder doesn't block it and re-tapping won't duplicate.
        let existingNotes = Set(try context.fetch(FetchDescriptor<TickleReminder>()).map(\.note))
        let cal = Calendar.current
        let today = cal.startOfDay(for: Date())
        var createdTickles = 0
        func tickle(
            _ contact: Contact?, group: ContactGroup? = nil, note: String,
            frequency: TickleFrequency, dueInDays: Int, status: TickleStatus = .active
        ) {
            guard !existingNotes.contains(note) else { return }
            let t = TickleReminder(contact: contact, group: group, note: note,
                                   frequency: frequency, startDate: today)
            t.nextDueDate = cal.date(byAdding: .day, value: dueInDays, to: today) ?? today
            t.status = status
            context.insert(t)
            createdTickles += 1
        }
        let c = contacts
        // Due today / overdue
        tickle(c[0], note: "Catch up over coffee", frequency: .monthly, dueInDays: 0)
        if c.count > 1 { tickle(c[1], note: "Check in — it's been a while", frequency: .weekly, dueInDays: 0) }
        if c.count > 2 { tickle(c[2], note: "Quick hello", frequency: .biweekly, dueInDays: -2) }
        // Upcoming
        if c.count > 3 { tickle(c[3], note: "Plan a call", frequency: .monthly, dueInDays: 3) }
        if c.count > 4 { tickle(c[4], note: "Quarterly sync", frequency: .quarterly, dueInDays: 9) }
        // Snoozed
        if c.count > 5 { tickle(c[5], note: "Reschedule lunch", frequency: .monthly, dueInDays: 6, status: .snoozed) }
        // Milestones group reminder
        tickle(nil, group: milestones, note: "🎂 Birthday this week", frequency: .quarterly, dueInDays: 1)

        try context.save()
        // Use displayName (live-localized) not the stored name, which for canonical
        // groups is frozen at the first-launch locale and would mislead here.
        return "Demo data ready: \(contacts.count) contacts across "
            + "\(family.displayName)/\(friends.displayName)/\(work.displayName)/"
            + "\(community.displayName)/\(milestones.displayName), \(createdTickles) new tickles ✓"
    }

    enum SeedError: LocalizedError {
        case fileNotFound
        var errorDescription: String? {
            "test_contacts.csv not found in app bundle."
        }
    }
}
#endif
