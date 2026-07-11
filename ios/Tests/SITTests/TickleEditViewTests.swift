import XCTest
import SwiftData
import ViewInspector
@testable import Ticklr

/// Screen-level wiring tests for `TickleEditView`.
///
/// The pure decision logic is already covered by `TickleSchedulerTests`
/// (`initialNextDueDate*`) and `TickleReminderTests`; these tests exist to
/// catch the wiring bug class — someone bypasses the helper, passes the
/// wrong arguments, or skips the original-reminder lookup on edit.
///
/// Uses ViewInspector (test-only SPM dep, scoped to TicklrTests). The
/// `save()` closure mutates the SwiftData `@Model` reminder in place, so
/// the test verifies state by reading the same instance after tapping Save
/// — no `ModelContainer` round-trip required.
@MainActor
final class TickleEditViewTests: XCTestCase {

    private func makeContact() -> Contact {
        Contact(firstName: "Alice", lastName: "Doe")
    }

    func testEditingWithoutChangesPreservesNextDueDate() throws {
        // Regression: editing a tickle (e.g., to fix a typo in the note) must
        // not advance or reset its nextDueDate. Path A's helper test covers
        // the rule; this test verifies the screen actually calls the helper.
        let contact = makeContact()
        let futureDue = Date().addingTimeInterval(20 * 24 * 60 * 60)
        let reminder = TickleReminder(
            contact: contact,
            note: "before",
            frequency: .monthly,
            startDate: futureDue
        )
        // Init computes nextDueDate via the helper; for this case startDate is
        // future so nextDueDate == futureDue. Capture it explicitly.
        let originalNextDue = reminder.nextDueDate
        XCTAssertEqual(originalNextDue, futureDue)

        let view = TickleEditView(existing: reminder)
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        XCTAssertEqual(
            reminder.nextDueDate, originalNextDue,
            "Editing without changes must preserve nextDueDate"
        )
        XCTAssertEqual(reminder.frequency, .monthly)
    }

    func testEditingOverdueTickleWithoutScheduleChangePreservesDueDate() throws {
        // TIC-67 flipped this rule: a save that leaves the schedule untouched
        // must NOT roll nextDueDate forward — that silently skipped the due
        // occurrence when a user opened an overdue tickle just to fix a typo.
        let contact = makeContact()
        let pastStart = Date().addingTimeInterval(-30 * 24 * 60 * 60)
        let reminder = TickleReminder(
            contact: contact,
            note: "overdue",
            frequency: .weekly,
            startDate: pastStart
        )
        reminder.nextDueDate = pastStart

        let view = TickleEditView(existing: reminder)
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        XCTAssertEqual(
            reminder.nextDueDate, pastStart,
            "A schedule-untouched save on an overdue tickle must not skip the due occurrence"
        )
    }

    func testEditingWithEmptyNoteSubstitutesLocalizedDefault() throws {
        // Regression guard for the "Stay in touch" default. Empty (not
        // whitespace) note must be replaced with the localized default;
        // anything else is preserved as-is (with whitespace trimmed).
        let contact = makeContact()
        let reminder = TickleReminder(
            contact: contact,
            note: "",
            frequency: .monthly,
            startDate: Date().addingTimeInterval(7 * 24 * 60 * 60)
        )

        let view = TickleEditView(existing: reminder)
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        let expected = String(localized: "tickleEdit.default.note")
        XCTAssertEqual(reminder.note, expected)
        // Sanity-check the en source so a translation drift in the catalog
        // can't silently make the assertion meaningless.
        XCTAssertEqual(expected, "Stay in touch")
    }

    func testEditingWithWhitespaceOnlyNoteIsTrimmedNotDefaulted() throws {
        // Behavior contract: only literally empty triggers the default.
        // A typed space is intentional input — trim it instead of replacing.
        // (Trimmed result IS empty, but that's the user's choice, not ours.)
        let contact = makeContact()
        let reminder = TickleReminder(
            contact: contact,
            note: "   ",
            frequency: .monthly,
            startDate: Date().addingTimeInterval(7 * 24 * 60 * 60)
        )

        let view = TickleEditView(existing: reminder)
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        XCTAssertEqual(reminder.note, "", "Whitespace-only must be trimmed, not defaulted")
    }

    func testSavingExistingTickleInvokesOnSavedWithUpdatedMessageAndNoDelay() throws {
        // TIC-84: save must apply and the editor must close immediately — no
        // artificial 2s wait. There's no Task to await here on purpose: if
        // `onSaved` only fired after a sleep, this assertion (which runs the
        // very next line after `.tap()`) would see `savedMessage == nil` and
        // fail. Its passing IS the regression guard for the removed delay.
        let contact = makeContact()
        let reminder = TickleReminder(
            contact: contact,
            note: "before",
            frequency: .monthly,
            startDate: Date().addingTimeInterval(7 * 24 * 60 * 60)
        )
        var savedMessage: String?
        var closeCount = 0

        let view = TickleEditView(existing: reminder, onClose: { closeCount += 1 }) { message in
            savedMessage = message
        }
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        XCTAssertEqual(savedMessage, String(localized: "tickleEdit.toast.updated"))
        XCTAssertEqual(closeCount, 1, "Save must close the editor immediately, with no delay")
    }

    func testSavingNewTickleInvokesOnSavedWithSavedMessageAndNoDelay() throws {
        // Same contract for the create path (existing == nil): the toast text
        // differs ("saved" vs "updated"), and `onSaved` must still fire
        // synchronously with the save, not after a sleep.
        let contact = makeContact()
        var savedMessage: String?
        var closeCount = 0

        let view = TickleEditView(contact: contact, onClose: { closeCount += 1 }) { message in
            savedMessage = message
        }
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        XCTAssertEqual(savedMessage, String(localized: "tickleEdit.toast.saved"))
        XCTAssertEqual(closeCount, 1, "Save must close the editor immediately, with no delay")
    }

    // MARK: - TIC-91: Settings default frequency seeds new tickles

    /// `seededFrequency`/`resolvedDefaultFrequency` are pure static helpers
    /// (no SwiftUI rendering, no ModelContext) so — unlike the new-tickle
    /// save path noted above — they're directly assertable here.
    private var priorDefaultFrequencyRaw: String?

    override func setUp() {
        super.setUp()
        priorDefaultFrequencyRaw = UserDefaults.standard.string(forKey: "defaultTickleFrequency")
    }

    override func tearDown() {
        if let prior = priorDefaultFrequencyRaw {
            UserDefaults.standard.set(prior, forKey: "defaultTickleFrequency")
        } else {
            UserDefaults.standard.removeObject(forKey: "defaultTickleFrequency")
        }
        super.tearDown()
    }

    func testStoredDefaultFlowsIntoNewNonMilestoneTickle() {
        UserDefaults.standard.set(TickleFrequency.quarterly.rawValue, forKey: "defaultTickleFrequency")

        let resolved = TickleEditView.seededFrequency(prefilledCategory: nil)

        XCTAssertEqual(resolved, .quarterly, "A plain new tickle must seed from the Settings default, not a hardcoded .monthly")
    }

    func testStoredDefaultFlowsIntoNewTickleForNonMilestoneCategory() {
        // The seeding rule keys off "is this the Milestones hero", not "was
        // some prefilledCategory passed" — any other category must still
        // honor the stored default.
        UserDefaults.standard.set(TickleFrequency.weekly.rawValue, forKey: "defaultTickleFrequency")

        let resolved = TickleEditView.seededFrequency(prefilledCategory: .family)

        XCTAssertEqual(resolved, .weekly)
    }

    func testMilestonesHeroStaysAnnualRegardlessOfStoredDefault() {
        // Set the stored default to something that would visibly betray a
        // wiring mistake if Milestones ever started reading it.
        UserDefaults.standard.set(TickleFrequency.weekly.rawValue, forKey: "defaultTickleFrequency")

        let resolved = TickleEditView.seededFrequency(prefilledCategory: .milestones)

        XCTAssertEqual(resolved, .annual, "The Milestones hero must always seed .annual, independent of Settings")
    }

    func testMissingStoredDefaultFallsBackToMonthly() {
        UserDefaults.standard.removeObject(forKey: "defaultTickleFrequency")

        let resolved = TickleEditView.seededFrequency(prefilledCategory: nil)

        XCTAssertEqual(resolved, .monthly)
    }

    func testUnrecognizedStoredDefaultFallsBackToMonthly() {
        // Simulates a stale/garbage value (e.g. a removed rawValue from an
        // older build) rather than a value that was ever a legal case.
        UserDefaults.standard.set("Fortnightly", forKey: "defaultTickleFrequency")

        let resolved = TickleEditView.seededFrequency(prefilledCategory: nil)

        XCTAssertEqual(resolved, .monthly)
    }

    func testCustomStoredDefaultFallsBackToMonthly() {
        // .custom has no stored interval to seed a new reminder with. The
        // Settings picker already excludes it as a selectable option, but the
        // resolver still defends against it (e.g. a value written before that
        // exclusion existed, or edited directly via UserDefaults).
        UserDefaults.standard.set(TickleFrequency.custom.rawValue, forKey: "defaultTickleFrequency")

        let resolved = TickleEditView.seededFrequency(prefilledCategory: nil)

        XCTAssertEqual(resolved, .monthly)
    }

    func testEditingExistingTickleIgnoresStoredDefault() throws {
        // The stored Settings default must only influence brand-new tickles.
        // Editing an existing one keeps its own frequency even when the
        // stored default now points somewhere else entirely.
        UserDefaults.standard.set(TickleFrequency.quarterly.rawValue, forKey: "defaultTickleFrequency")

        let contact = makeContact()
        let reminder = TickleReminder(
            contact: contact,
            note: "before",
            frequency: .weekly,
            startDate: Date().addingTimeInterval(20 * 24 * 60 * 60)
        )

        let view = TickleEditView(existing: reminder)
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        XCTAssertEqual(reminder.frequency, .weekly, "Editing an existing tickle must not be overridden by the Settings default")
    }

    func testNewTickleInitSeedsPickerFromStoredDefault() {
        // Belt-and-suspenders check on the actual init path (not just the
        // extracted helper) for the plain "+" entry point used across the
        // app (Tickle tab, Contact Detail "Create a tickle").
        UserDefaults.standard.set(TickleFrequency.bimonthly.rawValue, forKey: "defaultTickleFrequency")

        let contact = makeContact()
        let view = TickleEditView(contact: contact)

        XCTAssertEqual(view.frequency, .bimonthly)
    }

    func testNewTickleInitForMilestonesHeroStaysAnnual() {
        UserDefaults.standard.set(TickleFrequency.weekly.rawValue, forKey: "defaultTickleFrequency")

        let view = TickleEditView(prefilledCategory: .milestones)

        XCTAssertEqual(view.frequency, .annual)
    }

    // MARK: - TIC-88: group-anchored tickle creation via the edit view path

    func testNewGroupTickleSeedsFrequencyFromStoredDefault() {
        // A group tickle (opened from a group's detail screen) seeds its
        // frequency from the Settings default, exactly like a contact tickle —
        // it must not be hardcoded, and the group binding must not divert it.
        UserDefaults.standard.set(TickleFrequency.quarterly.rawValue, forKey: "defaultTickleFrequency")

        let group = ContactGroup(name: "Hiking Crew")
        let view = TickleEditView(group: group)

        XCTAssertEqual(view.frequency, .quarterly)
    }

    func testEditingGroupTickleWithoutPickingContactKeepsGroupAndContactNil() throws {
        // The group-creation feature leans on the TIC-70 edit rule: a
        // group-anchored reminder saved without picking a contact keeps its
        // group link and its contact stays nil (rather than being unlinked or
        // silently turned into an "Unknown"-contact reminder).
        let group = ContactGroup(name: "Book Club")
        let futureStart = Date().addingTimeInterval(14 * 24 * 60 * 60)
        let reminder = TickleReminder(
            contact: nil,
            group: group,
            note: "plan next read",
            frequency: .monthly,
            startDate: futureStart
        )
        XCTAssertNil(reminder.contact)
        XCTAssertNotNil(reminder.group)

        let view = TickleEditView(existing: reminder)
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        XCTAssertNil(reminder.contact, "Saving a group tickle without a picked contact must leave contact nil")
        XCTAssertEqual(reminder.group?.id, group.id, "The group link must be preserved on save")
    }

    // NOTE on rapid-tap regression: a true end-to-end test on iOS would need
    // the ViewInspector inspection-callback pattern (adding a
    // `didAppear: ((Self) -> Void)?` to TickleEditView), because
    // @Environment(\.modelContext) only resolves under ViewHosting + the
    // callback path — direct `view.inspect()` bypasses the runtime
    // environment, so modelContext.insert(...) silently no-ops in the test
    // and the assertion sees zero reminders even on the first tap.
    //
    // We accept this gap because:
    // 1. The Android equivalent (TickleEditScreenTest, in the same change
    //    set) exercises the same bug class via Compose UI test, which has
    //    full state-driven recomposition and respects the `enabled` flag.
    // 2. The save-debounce code itself (`isSaving` + canSave + defer-inside-
    //    Task) is small and local enough to reason about by inspection.
    // 3. The spec doc gates the change on a manual rapid-tap smoke test.
}
