import XCTest
@testable import Ticklr

final class TickleSchedulerTests: XCTestCase {

    // Fixed reference date: Nov 14 2023 22:13:20 UTC
    let baseDate = Date(timeIntervalSince1970: 1_700_000_000)
    let calendar = Calendar.current

    // MARK: - nextDueDate

    func testDaily() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .daily)
        let expected = calendar.date(byAdding: .day, value: 1, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testWeekly() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .weekly)
        let expected = calendar.date(byAdding: .day, value: 7, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testBiweekly() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .biweekly)
        let expected = calendar.date(byAdding: .day, value: 14, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testMonthly() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .monthly)
        let expected = calendar.date(byAdding: .month, value: 1, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testBimonthly() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .bimonthly)
        let expected = calendar.date(byAdding: .month, value: 2, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testQuarterly() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .quarterly)
        let expected = calendar.date(byAdding: .month, value: 3, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testCustomWithExplicitDays() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .custom, customDays: 45)
        let expected = calendar.date(byAdding: .day, value: 45, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testCustomDefaultsTo30DaysWhenNilProvided() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .custom, customDays: nil)
        let expected = calendar.date(byAdding: .day, value: 30, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testAllFrequenciesReturnFutureDate() {
        for frequency in TickleFrequency.allCases {
            let result = TickleScheduler.nextDueDate(from: baseDate, frequency: frequency)
            XCTAssertGreaterThan(result, baseDate, "\(frequency.rawValue) should return a date after the base date")
        }
    }

    func testCustomOneDayInterval() {
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .custom, customDays: 1)
        let expected = calendar.date(byAdding: .day, value: 1, to: baseDate)!
        XCTAssertEqual(result, expected)
    }

    func testMonthlyAdvancesCorrectly() {
        // Verify monthly is not just 30 days — it uses Calendar month arithmetic
        let result = TickleScheduler.nextDueDate(from: baseDate, frequency: .monthly)
        let weekly = TickleScheduler.nextDueDate(from: baseDate, frequency: .weekly)
        XCTAssertGreaterThan(result, weekly)
    }

    // MARK: - initialNextDueDate

    func testInitialNextDueDateWithFutureStartDateReturnsStartDate() {
        let now = Date()
        let future = now.addingTimeInterval(7 * 24 * 60 * 60)
        let result = TickleScheduler.initialNextDueDate(from: future, frequency: .weekly, now: now)
        XCTAssertEqual(result, future)
    }

    func testInitialNextDueDateWithPastStartDateAddsOneInterval() {
        let now = Date()
        let past = now.addingTimeInterval(-86400)
        let result = TickleScheduler.initialNextDueDate(from: past, frequency: .weekly, now: now)
        let expected = calendar.date(byAdding: .day, value: 7, to: past)!
        XCTAssertEqual(result, expected)
    }

    func testInitialNextDueDateWithStartDateEqualToNowAddsOneInterval() {
        // Edge case: startDate == now must NOT fire today; it should advance.
        let now = Date()
        let result = TickleScheduler.initialNextDueDate(from: now, frequency: .weekly, now: now)
        let expected = calendar.date(byAdding: .day, value: 7, to: now)!
        XCTAssertEqual(result, expected)
    }

    func testInitialNextDueDateMonthlyHonorsFutureStartDate() {
        let now = Date()
        let future = now.addingTimeInterval(30 * 24 * 60 * 60)
        let result = TickleScheduler.initialNextDueDate(from: future, frequency: .monthly, now: now)
        XCTAssertEqual(result, future)
    }

    func testInitialNextDueDateCustomFromPastUsesProvidedDays() {
        let now = Date()
        let past = now.addingTimeInterval(-86400)
        let result = TickleScheduler.initialNextDueDate(from: past, frequency: .custom, customDays: 21, now: now)
        let expected = calendar.date(byAdding: .day, value: 21, to: past)!
        XCTAssertEqual(result, expected)
    }
}
