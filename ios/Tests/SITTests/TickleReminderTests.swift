import XCTest
@testable import Ticklr

final class TickleReminderTests: XCTestCase {

    func testInitWithWeeklyAndTodayStartDateSetsNextDueDateOneWeekAhead() {
        // Regression guard: a Weekly tickle created today must not fire today.
        let now = Date()
        let r = TickleReminder(note: "ping", frequency: .weekly, startDate: now)
        let expected = Calendar.current.date(byAdding: .day, value: 7, to: now)!
        XCTAssertEqual(r.nextDueDate, expected)
    }

    func testInitWithMonthlyAndTodayStartDateSetsNextDueDateOneMonthAhead() {
        let now = Date()
        let r = TickleReminder(note: "ping", frequency: .monthly, startDate: now)
        let expected = Calendar.current.date(byAdding: .month, value: 1, to: now)!
        XCTAssertEqual(r.nextDueDate, expected)
    }

    func testInitWithFutureStartDateUsesStartDateAsNextDueDate() {
        // Picking an explicit future start date treats it as the literal first occurrence.
        let future = Date().addingTimeInterval(60 * 60 * 24 * 30) // 30 days out
        let r = TickleReminder(note: "ping", frequency: .weekly, startDate: future)
        XCTAssertEqual(r.nextDueDate, future)
    }

    func testInitWithCustomFrequencyUsesCustomDays() {
        let now = Date()
        let r = TickleReminder(
            note: "ping",
            frequency: .custom,
            customIntervalDays: 21,
            startDate: now
        )
        let expected = Calendar.current.date(byAdding: .day, value: 21, to: now)!
        XCTAssertEqual(r.nextDueDate, expected)
    }
}
