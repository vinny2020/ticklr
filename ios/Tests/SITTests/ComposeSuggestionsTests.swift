import XCTest
import SwiftData
import MessageUI
@testable import Ticklr

/// TIC-86: recipient-suggestion assembly (due-first ordering, recents
/// exclusion+cap, empty states) and the post-send surface exclusivity decision
/// (create-a-tickle offer vs. TIC-82 completion/undo — never both).
@MainActor
final class ComposeSuggestionsTests: XCTestCase {

    private var container: ModelContainer!
    private var context: ModelContext!
    private var priorNotificationsEnabled: Any?

    override func setUp() async throws {
        // Inserting a due reminder can touch scheduleNotification indirectly via
        // other helpers; disable the in-app pref so no async Task is spawned that
        // could outlive the in-memory container (mirrors TickleSendCompletionTests).
        priorNotificationsEnabled = UserDefaults.standard.object(forKey: "tickleNotificationsEnabled")
        UserDefaults.standard.set(false, forKey: "tickleNotificationsEnabled")

        container = try ModelContainer(
            for: TickleReminder.self, Contact.self, ContactGroup.self, MessageTemplate.self,
            configurations: .init(isStoredInMemoryOnly: true)
        )
        context = container.mainContext
    }

    override func tearDown() {
        if let prior = priorNotificationsEnabled {
            UserDefaults.standard.set(prior, forKey: "tickleNotificationsEnabled")
        } else {
            UserDefaults.standard.removeObject(forKey: "tickleNotificationsEnabled")
        }
        container = nil
        context = nil
    }

    // MARK: - Helpers

    @discardableResult
    private func makeContact(_ first: String, lastContactedAt: Date? = nil) -> Contact {
        let c = Contact(firstName: first, lastName: "Test", phoneNumbers: ["+15551230000"])
        c.lastContactedAt = lastContactedAt
        context.insert(c)
        return c
    }

    /// Attaches an active reminder due `secondsAgo` in the past (so it's overdue).
    private func attachDueReminder(to contact: Contact, secondsAgo: TimeInterval) {
        let r = TickleReminder(contact: contact, note: "ping", frequency: .monthly)
        r.nextDueDate = Date().addingTimeInterval(-secondsAgo)
        r.status = .active
        context.insert(r)
    }

    // MARK: - Due-today ordering (most overdue first)

    func testDueTodayOrderedMostOverdueFirst() throws {
        let a = makeContact("Amy")
        let b = makeContact("Bob")
        let c = makeContact("Cal")
        attachDueReminder(to: a, secondsAgo: 3600)        // 1h overdue
        attachDueReminder(to: b, secondsAgo: 7 * 86_400)  // 7d overdue — most overdue
        attachDueReminder(to: c, secondsAgo: 2 * 86_400)  // 2d overdue
        try context.save()

        let result = ComposeSuggestions.assemble(from: [a, b, c])
        XCTAssertEqual(result.dueToday.map(\.firstName), ["Bob", "Cal", "Amy"],
                       "due-today must be ordered most-overdue (earliest due date) first")
        XCTAssertTrue(result.recents.isEmpty, "everyone here is due, so recents is empty")
    }

    func testDueTodayUsesEarliestReminderPerContact() throws {
        // A contact with two due reminders sorts by its most-overdue one.
        let a = makeContact("Amy")
        let b = makeContact("Bob")
        attachDueReminder(to: a, secondsAgo: 1 * 86_400)
        attachDueReminder(to: a, secondsAgo: 10 * 86_400) // earliest for Amy
        attachDueReminder(to: b, secondsAgo: 5 * 86_400)
        try context.save()

        let result = ComposeSuggestions.assemble(from: [a, b])
        XCTAssertEqual(result.dueToday.map(\.firstName), ["Amy", "Bob"],
                       "Amy's 10-day-overdue reminder should rank her ahead of Bob")
    }

    func testFutureAndCompletedRemindersAreNotDueToday() throws {
        let a = makeContact("Amy")
        let future = TickleReminder(contact: a, note: "later", frequency: .monthly)
        future.nextDueDate = Date().addingTimeInterval(7 * 86_400)
        future.status = .active
        context.insert(future)
        let done = TickleReminder(contact: a, note: "done", frequency: .oneTime)
        done.nextDueDate = Date().addingTimeInterval(-86_400)
        done.status = .completed
        context.insert(done)
        try context.save()

        let result = ComposeSuggestions.assemble(from: [a])
        XCTAssertTrue(result.dueToday.isEmpty,
                      "a contact with only future/completed reminders is not due today")
    }

    // MARK: - Recents (exclusion + cap + ordering)

    func testRecentsExcludeDueTodayAndSortDescending() throws {
        let now = Date()
        let due = makeContact("Due", lastContactedAt: now.addingTimeInterval(-10))
        attachDueReminder(to: due, secondsAgo: 3600) // also recently contacted
        let older = makeContact("Older", lastContactedAt: now.addingTimeInterval(-1000))
        let newer = makeContact("Newer", lastContactedAt: now.addingTimeInterval(-100))
        try context.save()

        let result = ComposeSuggestions.assemble(from: [due, older, newer])
        XCTAssertEqual(result.dueToday.map(\.firstName), ["Due"])
        XCTAssertEqual(result.recents.map(\.firstName), ["Newer", "Older"],
                       "recents excludes the due contact and sorts most-recent first")
    }

    func testRecentsExcludeContactsNeverContacted() throws {
        let contacted = makeContact("Contacted", lastContactedAt: Date())
        _ = makeContact("Never", lastContactedAt: nil)
        try context.save()

        let result = ComposeSuggestions.assemble(from: try allContacts())
        XCTAssertEqual(result.recents.map(\.firstName), ["Contacted"],
                       "a contact with nil lastContactedAt never appears in recents")
        _ = contacted
    }

    func testRecentsCappedAtProvidedLimit() throws {
        var contacts: [Contact] = []
        for i in 0..<10 {
            // Older index → older date, so index 9 is most recent.
            contacts.append(makeContact("C\(i)", lastContactedAt: Date().addingTimeInterval(Double(i))))
        }
        try context.save()

        let result = ComposeSuggestions.assemble(from: contacts, cap: 3)
        XCTAssertEqual(result.recents.count, 3, "recents must honor the cap")
        XCTAssertEqual(result.recents.map(\.firstName), ["C9", "C8", "C7"],
                       "the cap keeps the most-recent contacts")
    }

    func testDefaultRecentsCapIsFive() throws {
        var contacts: [Contact] = []
        for i in 0..<8 {
            contacts.append(makeContact("C\(i)", lastContactedAt: Date().addingTimeInterval(Double(i))))
        }
        try context.save()

        let result = ComposeSuggestions.assemble(from: contacts)
        XCTAssertEqual(result.recents.count, ComposeSuggestions.recentsCap)
        XCTAssertEqual(result.recents.count, 5)
    }

    // MARK: - Empty states

    func testEmptyContactListYieldsNoSuggestions() {
        let result = ComposeSuggestions.assemble(from: [])
        XCTAssertTrue(result.dueToday.isEmpty)
        XCTAssertTrue(result.recents.isEmpty)
    }

    func testContactsWithNoRemindersAndNoHistoryYieldEmptySections() throws {
        _ = makeContact("Ghost", lastContactedAt: nil)
        try context.save()
        let result = ComposeSuggestions.assemble(from: try allContacts())
        XCTAssertTrue(result.dueToday.isEmpty)
        XCTAssertTrue(result.recents.isEmpty,
                      "no due reminder and no lastContactedAt → both sections empty")
    }

    // MARK: - Post-send surface exclusivity (offer vs. undo vs. nothing)

    func testPostSendSurfaceRoutesToCompletionUndoWhenReminderAttached() {
        XCTAssertEqual(
            ComposeSuggestions.postSendSurface(
                result: .sent, hadDueReminderAttached: true, contactHasOpenReminder: true
            ),
            .completionUndo,
            "a sent message with a reminder attached uses the TIC-82 completion/undo path"
        )
    }

    func testPostSendSurfaceRoutesToSuggestTickleWhenContactUncovered() {
        XCTAssertEqual(
            ComposeSuggestions.postSendSurface(
                result: .sent, hadDueReminderAttached: false, contactHasOpenReminder: false
            ),
            .suggestTickle,
            "a plain send to a contact with no open reminder offers to create a tickle"
        )
    }

    func testPostSendSurfaceIsNoneWhenContactAlreadyCovered() {
        // Plain send, but the contact already has an active/snoozed reminder
        // (just not currently due) — offering another would invite duplicates.
        XCTAssertEqual(
            ComposeSuggestions.postSendSurface(
                result: .sent, hadDueReminderAttached: false, contactHasOpenReminder: true
            ),
            .none,
            "a contact already covered by an open reminder must not be offered a duplicate"
        )
    }

    func testPostSendSurfaceIsNoneForNonSends() {
        for result: MessageComposeResult? in [.cancelled, .failed, nil] {
            for attached in [true, false] {
                for open in [true, false] {
                    XCTAssertEqual(
                        ComposeSuggestions.postSendSurface(
                            result: result, hadDueReminderAttached: attached, contactHasOpenReminder: open
                        ),
                        .none,
                        "\(String(describing: result)) must present no surface (attached: \(attached), open: \(open))"
                    )
                }
            }
        }
    }

    func testPostSendSurfaceNeverShowsBothForOneSend() {
        // At most one actionable surface per send, by construction.
        let withReminder = ComposeSuggestions.postSendSurface(
            result: .sent, hadDueReminderAttached: true, contactHasOpenReminder: true
        )
        let withoutReminder = ComposeSuggestions.postSendSurface(
            result: .sent, hadDueReminderAttached: false, contactHasOpenReminder: false
        )
        XCTAssertNotEqual(withReminder, withoutReminder)
        XCTAssertTrue(withReminder == .completionUndo && withoutReminder == .suggestTickle)
    }

    // MARK: - Offer blocked by existing coverage (hasOpenReminder, end-to-end)

    func testOfferBlockedWhenContactHasActiveNotDueReminder() throws {
        let contact = makeContact("Covered")
        let upcoming = TickleReminder(contact: contact, note: "later", frequency: .monthly)
        upcoming.nextDueDate = Date().addingTimeInterval(7 * 86_400) // active, NOT due
        upcoming.status = .active
        context.insert(upcoming)
        try context.save()

        XCTAssertTrue(ComposeSuggestions.hasOpenReminder(contact))
        XCTAssertEqual(
            ComposeSuggestions.postSendSurface(
                result: .sent,
                hadDueReminderAttached: false,
                contactHasOpenReminder: ComposeSuggestions.hasOpenReminder(contact)
            ),
            .none,
            "an ACTIVE (not currently due) reminder blocks the create-a-tickle offer"
        )
    }

    func testOfferBlockedWhenContactHasSnoozedReminder() throws {
        let contact = makeContact("Snoozy")
        let snoozed = TickleReminder(contact: contact, note: "zzz", frequency: .weekly)
        snoozed.nextDueDate = Date().addingTimeInterval(3 * 86_400)
        snoozed.status = .snoozed
        context.insert(snoozed)
        try context.save()

        XCTAssertTrue(ComposeSuggestions.hasOpenReminder(contact))
        XCTAssertEqual(
            ComposeSuggestions.postSendSurface(
                result: .sent,
                hadDueReminderAttached: false,
                contactHasOpenReminder: ComposeSuggestions.hasOpenReminder(contact)
            ),
            .none,
            "a SNOOZED reminder blocks the create-a-tickle offer"
        )
    }

    func testOfferAllowedWhenContactHasOnlyCompletedReminders() throws {
        let contact = makeContact("Done")
        let done = TickleReminder(contact: contact, note: "once", frequency: .oneTime)
        done.nextDueDate = Date().addingTimeInterval(-86_400)
        done.status = .completed
        context.insert(done)
        try context.save()

        XCTAssertFalse(ComposeSuggestions.hasOpenReminder(contact),
                       "completed reminders don't count as coverage")
        XCTAssertEqual(
            ComposeSuggestions.postSendSurface(
                result: .sent,
                hadDueReminderAttached: false,
                contactHasOpenReminder: ComposeSuggestions.hasOpenReminder(contact)
            ),
            .suggestTickle,
            "only-completed reminders leave the contact uncovered — offer stands"
        )
    }

    func testHasOpenReminderFalseForContactWithNoReminders() throws {
        let contact = makeContact("Bare")
        try context.save()
        XCTAssertFalse(ComposeSuggestions.hasOpenReminder(contact))
    }

    // MARK: - Helper

    private func allContacts() throws -> [Contact] {
        try context.fetch(FetchDescriptor<Contact>())
    }
}
