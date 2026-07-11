import XCTest
@testable import Ticklr

/// Covers `ImportFeedback.message(imported:skipped:)` — the shared toast
/// copy for both import sources (TIC-85), including the skipped-count
/// variants and the zero-imported "no new contacts" fallback that replaces
/// the old silent `print()`.
final class ImportFeedbackTests: XCTestCase {

    func testImportedOnlySingular() {
        let message = ImportFeedback.message(imported: 1, skipped: 0)
        XCTAssertTrue(message.contains("1"))
        XCTAssertTrue(message.localizedCaseInsensitiveContains("contact"))
        XCTAssertFalse(message.contains("·"), "no skipped suffix when nothing was skipped")
    }

    func testImportedOnlyPlural() {
        let message = ImportFeedback.message(imported: 12, skipped: 0)
        XCTAssertTrue(message.contains("12"))
        XCTAssertFalse(message.contains("·"))
    }

    func testImportedWithSkippedCombinesBothCounts() {
        let message = ImportFeedback.message(imported: 12, skipped: 3)
        XCTAssertTrue(message.contains("12"), "should mention the imported count: \(message)")
        XCTAssertTrue(message.contains("3"), "should mention the skipped count: \(message)")
        XCTAssertTrue(message.contains("·"), "combined message should use the house middle-dot separator")
    }

    func testImportedWithSingleSkippedUsesSingularSuffix() {
        let message = ImportFeedback.message(imported: 5, skipped: 1)
        XCTAssertTrue(message.contains("5"))
        XCTAssertTrue(message.contains("1"))
        // English singular is "duplicate" (not "duplicates") — assert the
        // plural form did NOT leak in for a count of 1.
        XCTAssertFalse(message.contains("duplicates"), "should use singular 'duplicate' for a skipped count of 1: \(message)")
    }

    func testZeroImportedZeroSkippedIsNotSilent() {
        let message = ImportFeedback.message(imported: 0, skipped: 0)
        XCTAssertFalse(message.isEmpty)
        XCTAssertTrue(message.localizedCaseInsensitiveContains("no") ||
                      message.localizedCaseInsensitiveContains("none") ||
                      message.contains("0"),
                      "zero-imported success should read as 'no new contacts', not silence: \(message)")
    }

    func testZeroImportedButSomeSkippedStillNamesTheSkippedCount() {
        // Every candidate in the batch was already a duplicate.
        let message = ImportFeedback.message(imported: 0, skipped: 4)
        XCTAssertTrue(message.contains("4"), "should still name how many duplicates were skipped: \(message)")
    }

    func testNeverReturnsEmptyString() {
        for imported in [0, 1, 2] {
            for skipped in [0, 1, 2] {
                XCTAssertFalse(ImportFeedback.message(imported: imported, skipped: skipped).isEmpty)
            }
        }
    }
}
