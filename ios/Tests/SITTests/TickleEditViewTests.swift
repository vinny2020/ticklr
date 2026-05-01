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

    func testEditingOverdueTickleRecomputesNextDueDateFromStartDate() throws {
        // The other half of the rule: when startDate is in the past, the
        // helper must roll forward by one interval rather than firing today.
        // Models the flow where a user opens an overdue tickle and saves.
        let contact = makeContact()
        let pastStart = Date().addingTimeInterval(-30 * 24 * 60 * 60)
        let reminder = TickleReminder(
            contact: contact,
            note: "overdue",
            frequency: .weekly,
            startDate: pastStart
        )
        // Force nextDueDate to past so we can detect the recomputation.
        reminder.nextDueDate = pastStart

        let view = TickleEditView(existing: reminder)
        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.save")).tap()

        // With nextDueDate == pastStart on the edit form, startDate state
        // also = pastStart. initialNextDueDate(past, weekly) = pastStart + 7d.
        let expected = Calendar.current.date(byAdding: .day, value: 7, to: pastStart)!
        XCTAssertEqual(
            reminder.nextDueDate, expected,
            "Editing an overdue tickle must roll its nextDueDate forward by one interval"
        )
    }
}
