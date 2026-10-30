import XCTest

extension XCUIApplication {
    /// Query the item itself: adaptive tabs can move between a bottom bar,
    /// vertical bar, or sidebar while the app is launching/resizing.
    func openTab(_ name: String, file: StaticString = #filePath, line: UInt = #line) {
        let symbols = ["network": "person.2.fill", "groups": "person.3.fill",
                       "tickle": "bell.badge.fill", "compose": "square.and.pencil",
                       "settings": "gearshape.fill"]
        let item = buttons[symbols[name] ?? name].firstMatch
        if item.waitForExistence(timeout: 15), item.isHittable {
            item.tap()
            return
        }
        // Native TabView can expose the tab item's own label rather than the
        // symbol identifier. These UI tests explicitly run in English.
        let label = name.prefix(1).uppercased() + name.dropFirst()
        let labeledItem = buttons[label].firstMatch
        XCTAssertTrue(labeledItem.waitForExistence(timeout: 5),
                      "Missing adaptive tab: \(name)", file: file, line: line)
        XCTAssertTrue(labeledItem.isHittable, "Tab is not reachable: \(name)", file: file, line: line)
        if labeledItem.exists && labeledItem.isHittable { labeledItem.tap() }
    }
}
