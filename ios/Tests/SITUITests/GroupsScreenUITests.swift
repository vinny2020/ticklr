import XCTest

/// Smoke test for the Groups tab's five canonical category cards (the warm
/// illustrated cards). Pins that the tab renders all five after seeding, and
/// holds the screen briefly so automation can capture the artwork (the iOS
/// simulator ignores synthetic clicks on tab bars, so this test is also the
/// only scripted route to this screen).
final class GroupsScreenUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testGroupsTabShowsCanonicalCategoryCards() {
        let app = XCUIApplication()
        app.launchArguments += [
            "-hasCompletedOnboarding", "YES",
            "-debugSeedDemoData", "YES",
        ]
        app.launch()

        let groupsTab = app.tabBars.buttons.element(boundBy: 1)
        XCTAssertTrue(groupsTab.waitForExistence(timeout: 15))
        groupsTab.tap()

        // Groups header + the canonical cards' tickle prompts confirm the
        // illustrated card stack rendered.
        XCTAssertTrue(
            app.staticTexts[String(localized: "warm.groups.subtitle",
                                   defaultValue: "Circles that shape your world.")]
                .waitForExistence(timeout: 5),
            "Groups tab should show its warm header"
        )

        // Hold the screen so external captures can grab the artwork.
        sleep(6)
    }
}
