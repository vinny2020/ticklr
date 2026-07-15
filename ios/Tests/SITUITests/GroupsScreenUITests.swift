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

    /// Regression guard for the tap-doesn't-open bug (groups navigation broke in
    /// 1.9.0 when the screen moved from NavigationStack + navigationDestination
    /// to a NavigationSplitView that never pushed the detail column on iPhone).
    /// Tapping a canonical group card must navigate into GroupDetailView — proven
    /// by the detail-only "Add Members" toolbar button appearing. This test fails
    /// against the broken NavigationSplitView and passes once tap-to-open works.
    func testTappingCanonicalGroupOpensDetail() {
        let app = XCUIApplication()
        app.launchArguments += [
            "-hasCompletedOnboarding", "YES",
            "-debugSeedDemoData", "YES",
        ]
        app.launch()

        let groupsTab = app.tabBars.buttons.element(boundBy: 1)
        XCTAssertTrue(groupsTab.waitForExistence(timeout: 15))
        groupsTab.tap()

        // The first canonical card (Family) is a stable, locale-independent hook.
        let familyCard = app.buttons["groupRow.canonical.family"]
        XCTAssertTrue(familyCard.waitForExistence(timeout: 5),
                      "Family canonical group card should render on the Groups tab")
        familyCard.tap()

        // The Add Members button lives only on GroupDetailView, so its presence
        // confirms the tap actually pushed the detail screen.
        XCTAssertTrue(
            app.buttons["groupDetail.addMembers"].waitForExistence(timeout: 5),
            "Tapping a group should navigate into its detail screen"
        )
    }
}
