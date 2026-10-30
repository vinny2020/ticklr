import XCTest

/// TIC-110 baseline: native navigation remains available after launch and
/// orientation requests on Duo, standard iPhone, and iPad. This checks launch
/// continuity; actual fold/pose geometry is validated separately in TIC-115.
final class BaselineLaunchUITests: XCTestCase {
    func testLaunchAndRotate() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments += ["-tickleNotificationsEnabled", "NO", "-hasCompletedOnboarding", "YES", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        XCUIDevice.shared.orientation = .portrait
        defer { XCUIDevice.shared.orientation = .portrait }
        app.launch()
        for orientation in [UIDeviceOrientation.portrait, .landscapeLeft, .landscapeRight] {
            XCUIDevice.shared.orientation = orientation
            let network = app.buttons["person.2.fill"].firstMatch
            let fallback = app.buttons["Network"].firstMatch
            XCTAssertTrue(network.waitForExistence(timeout: 20) || fallback.waitForExistence(timeout: 5),
                          "Native navigation must appear after launch/rotation")
            let screenshot = XCTAttachment(screenshot: app.screenshot())
            screenshot.name = "Baseline-\(orientation.rawValue)"
            screenshot.lifetime = .keepAlways
            add(screenshot)
        }
    }
}
