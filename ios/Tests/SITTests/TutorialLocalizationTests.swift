import XCTest
@testable import Ticklr

final class TutorialLocalizationTests: XCTestCase {
    private let locales = ["en", "ar", "cs", "de", "el", "es", "fr", "he", "hi", "hu", "it", "ja", "ko", "nl", "pl", "pt", "ro", "ru", "sv", "ur", "zh-Hans"]

    func testEveryTutorialPageAndExampleResolvesInEveryAppLanguage() throws {
        let appBundle = Bundle(for: Contact.self)
        // Cloud tests run on a separate machine from compilation. Only the app
        // bundle travels with them; the compiler's source path does not.
        let englishPath = try XCTUnwrap(appBundle.path(forResource: "en", ofType: "lproj"))
        let englishBundle = try XCTUnwrap(Bundle(path: englishPath))
        let tableURL = try XCTUnwrap(englishBundle.url(forResource: "Tutorials", withExtension: "strings"))
        let table = try XCTUnwrap(PropertyListSerialization.propertyList(
            from: Data(contentsOf: tableURL), format: nil) as? [String: String])
        XCTAssertEqual(table.count, 46, "The compiled tutorial table must include every page and example")
        let keys = table.keys
        for locale in locales {
            let path = try XCTUnwrap(appBundle.path(forResource: locale, ofType: "lproj"), locale)
            let bundle = try XCTUnwrap(Bundle(path: path))
            for key in keys {
                let value = bundle.localizedString(forKey: key, value: "__missing__", table: "Tutorials")
                XCTAssertFalse(value.isEmpty, "\(locale)/\(key)")
                XCTAssertNotEqual(value, "__missing__", "\(locale)/\(key)")
            }
            let create = bundle.localizedString(forKey: "warm.contact.createTickle", value: nil, table: "Localizable")
            XCTAssertTrue(bundle.localizedString(forKey: "tutorial.tickle.1.body", value: nil, table: "Tutorials").contains(create), "\(locale): instructions must name the real action")
            if locale != "en" {
                XCTAssertNotEqual(bundle.localizedString(forKey: "tutorial.phone.1.body", value: nil, table: "Tutorials"),
                                  "Start with the contacts on your phone. Your Ticklr network stays on this device.",
                                  "\(locale) must not silently use English tutorial copy")
            }
        }
    }

    func testLocalizedProgressKeepsBothNumbersIncludingReorderedLanguages() throws {
        let appBundle = Bundle(for: Contact.self)
        for locale in locales {
            let bundle = try XCTUnwrap(Bundle(path: XCTUnwrap(appBundle.path(forResource: locale, ofType: "lproj"))))
            let format = bundle.localizedString(forKey: "tutorial.progress", value: nil, table: "Tutorials")
            let rendered = String(format: format, 7, 13)
            XCTAssertTrue(rendered.contains("7"), locale)
            XCTAssertTrue(rendered.contains("13"), locale)
            XCTAssertFalse(rendered.contains("%"), locale)
        }
    }
}
