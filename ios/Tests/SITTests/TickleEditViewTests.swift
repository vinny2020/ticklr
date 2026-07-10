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
