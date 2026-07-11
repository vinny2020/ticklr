import XCTest
@testable import Ticklr

/// Covers the one-shot post-onboarding landing signal (TIC-85): request →
/// consume-once → cleared. `ContentView` reads `consumeLandingRequest()` to
/// pick the initial tab; `NetworkListView` reads `consumeToast()` the same
/// way for the optional import-result message.
final class PostOnboardingLandingTests: XCTestCase {

    override func setUp() {
        super.setUp()
        // Start from a clean slate — these are UserDefaults-backed one-shot
        // flags, not test-isolated by default.
        _ = PostOnboardingLanding.consumeLandingRequest()
        _ = PostOnboardingLanding.consumeToast()
    }

    override func tearDown() {
        _ = PostOnboardingLanding.consumeLandingRequest()
        _ = PostOnboardingLanding.consumeToast()
        super.tearDown()
    }

    func testNoRequestPendingByDefault() {
        XCTAssertFalse(PostOnboardingLanding.consumeLandingRequest())
        XCTAssertNil(PostOnboardingLanding.consumeToast())
    }

    func testRequestWithoutToastIsConsumedOnce() {
        PostOnboardingLanding.requestNetworkLanding()

        XCTAssertTrue(PostOnboardingLanding.consumeLandingRequest(), "first read should see the pending request")
        XCTAssertFalse(PostOnboardingLanding.consumeLandingRequest(), "second read must not replay it")
        XCTAssertNil(PostOnboardingLanding.consumeToast(), "no toast was attached")
    }

    func testRequestWithToastCarriesTheMessageIndependently() {
        PostOnboardingLanding.requestNetworkLanding(toast: "Imported 12 contacts · 3 duplicates skipped")

        XCTAssertTrue(PostOnboardingLanding.consumeLandingRequest())
        // The toast is consumed separately (by NetworkListView, not
        // ContentView) and survives the landing-flag consumption above.
        XCTAssertEqual(PostOnboardingLanding.consumeToast(), "Imported 12 contacts · 3 duplicates skipped")
        // ...but is itself one-shot.
        XCTAssertNil(PostOnboardingLanding.consumeToast())
    }

    func testEmptyToastStringIsNotStored() {
        PostOnboardingLanding.requestNetworkLanding(toast: "")
        XCTAssertTrue(PostOnboardingLanding.consumeLandingRequest())
        XCTAssertNil(PostOnboardingLanding.consumeToast(), "an empty toast string should behave like no toast")
    }

    func testSecondRequestOverwritesAPreviousUnconsumedOne() {
        PostOnboardingLanding.requestNetworkLanding(toast: "first")
        PostOnboardingLanding.requestNetworkLanding(toast: "second")

        XCTAssertTrue(PostOnboardingLanding.consumeLandingRequest())
        XCTAssertEqual(PostOnboardingLanding.consumeToast(), "second")
    }
}
