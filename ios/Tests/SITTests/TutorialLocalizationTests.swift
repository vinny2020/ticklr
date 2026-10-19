import XCTest
@testable import Ticklr

final class TutorialLocalizationTests: XCTestCase {
    private let locales = ["en", "ar", "cs", "de", "el", "es", "fr", "he", "hi", "hu", "it", "ja", "ko", "nl", "pl", "pt", "ro", "ru", "sv", "ur", "zh-Hans"]

    func testEveryTutorialPageAndExampleResolvesInEveryAppLanguage() throws {
        let appBundle = Bundle(for: Contact.self)
        let catalogURL = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("Sources/SIT/Resources/Tutorials.xcstrings")
        let catalog = try XCTUnwrap(JSONSerialization.jsonObject(with: Data(contentsOf: catalogURL)) as? [String: Any])
        let keys = try XCTUnwrap(catalog["strings"] as? [String: Any]).keys
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
