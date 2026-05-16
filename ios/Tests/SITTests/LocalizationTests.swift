import XCTest
@testable import Ticklr

final class LocalizationTests: XCTestCase {

    // MARK: - Catalog presence

    func testLocalizableCatalogExistsInBundle() throws {
        // Xcode compiles .xcstrings → Localizable.strings at build time.
        // Use a class from the Ticklr app target to resolve the app bundle (not the test bundle).
        let appBundle = Bundle(for: Contact.self)
        let url = appBundle.url(forResource: "Localizable", withExtension: "strings")
        XCTAssertNotNil(url, "Localizable.strings not found in Ticklr app bundle — xcstrings may not have been compiled")
    }

    // MARK: - Key resolution

    func testCommonKeysResolveToNonEmptyStrings() {
        let keys: [String] = [
            "common.save",
            "common.cancel",
            "common.delete",
            "common.done",
            "common.edit",
            "common.ok",
            "common.add",
            "common.create",
            "common.cannotUndo",
        ]
        assertKeysAreLocalized(keys)
    }

    func testTabKeysResolve() {
        let keys: [String] = [
            "tab.network",
            "tab.tickle",
            "tab.groups",
            "tab.compose",
            "tab.settings",
        ]
        assertKeysAreLocalized(keys)
    }

    func testSettingsKeysResolve() {
        let keys: [String] = [
            "settings.navTitle",
            "settings.section.data",
            "settings.row.contacts",
            "settings.row.importContacts",
            "settings.row.messageTemplates",
            "settings.row.tickleReminders",
            "settings.section.notifications",
            "settings.footer.notificationsDenied",
            "settings.footer.notificationsOff",
            "settings.section.tickleDefaults",
            "settings.row.defaultFrequency",
            "settings.section.about",
            "settings.row.app",
            "settings.row.version",
            "settings.row.builtBy",
            "settings.button.resetOnboarding",
            "settings.button.enableInSettings",
        ]
        assertKeysAreLocalized(keys)
    }

    func testTickleListKeysResolve() {
        let keys: [String] = [
            "tickleList.navTitle",
            "tickleList.section.due",
            "tickleList.section.upcoming",
            "tickleList.section.snoozed",
            "tickleList.empty.title",
            "tickleList.empty.description",
            "tickleList.action.snooze",
        ]
        assertKeysAreLocalized(keys)
    }

    func testTickleRowKeysResolve() {
        let keys: [String] = [
            "tickleRow.due.today",
            "tickleRow.due.yesterday",
            "tickleRow.unknown",
        ]
        assertKeysAreLocalized(keys)
    }

    func testTickleEditKeysResolve() {
        let keys: [String] = [
            "tickleEdit.section.who",
            "tickleEdit.target.contact",
            "tickleEdit.target.group",
            "tickleEdit.placeholder.contact",
            "tickleEdit.noGroups.title",
            "tickleEdit.noGroups.description",
            "tickleEdit.button.createGroup",
            "tickleEdit.placeholder.group",
            "tickleEdit.section.schedule",
            "tickleEdit.row.frequency",
            "tickleEdit.row.starting",
            "tickleEdit.section.note",
            "tickleEdit.placeholder.note",
            "tickleEdit.default.note",
            "tickleEdit.navTitle.edit",
            "tickleEdit.navTitle.new",
            "tickleEdit.toast.saved",
            "tickleEdit.toast.updated",
        ]
        assertKeysAreLocalized(keys)
    }

    func testNetworkKeysResolve() {
        let keys: [String] = [
            "networkList.navTitle",
            "networkList.search",
            "networkList.menu.newContact",
            "networkList.menu.getStarted",
            "networkList.empty.title",
            "networkList.empty.description",
            "contactPicker.navTitle",
            "contactPicker.search",
            "contactPicker.empty.title",
            "contactPicker.empty.description",
        ]
        assertKeysAreLocalized(keys)
    }

    func testContactDetailKeysResolve() {
        let keys: [String] = [
            "contact.section.phone",
            "contact.section.email",
            "contact.section.notes",
            "contact.section.tags",
            "contact.section.groups",
            "contact.section.name",
            "contact.section.work",
            "contact.placeholder.firstName",
            "contact.placeholder.lastName",
            "contact.placeholder.company",
            "contact.placeholder.jobTitle",
            "contact.placeholder.phoneNumber",
            "contact.placeholder.addPhone",
            "contact.placeholder.emailAddress",
            "contact.placeholder.addEmail",
            "contact.placeholder.addTag",
            "contactDetail.button.addTickle",
            "contactDetail.button.addToGroup",
            "contactDetail.button.message",
            "contactDetail.button.deleteContact",
            "contactEdit.navTitle",
            "addContact.navTitle",
        ]
        assertKeysAreLocalized(keys)
    }

    func testGroupKeysResolve() {
        let keys: [String] = [
            "groupList.navTitle",
            "groupList.empty.title",
            "groupList.empty.description",
            "groupEdit.navTitle.new",
            "groupEdit.navTitle.edit",
            "groupEdit.section.emoji",
            "groupEdit.label.custom",
            "groupEdit.placeholder.customEmoji",
            "groupEdit.section.name",
            "groupEdit.placeholder.name",
            "groupEdit.error.duplicate",
            "groupDetail.action.remove",
            "groupDetail.empty.title",
            "groupDetail.empty.description",
            "groupDetail.menu.addMembers",
            "groupDetail.menu.editGroup",
            "addToGroup.navTitle",
            "addToGroup.placeholder.groupName",
            "addToGroup.button.createNew",
            "addToGroup.error.duplicateName",
            "addMembers.search",
            "addMembers.empty.title",
            "addMembers.empty.description",
        ]
        assertKeysAreLocalized(keys)
    }

    func testComposeKeysResolve() {
        let keys: [String] = [
            "compose.navTitle",
            "compose.label.to",
            "compose.warning.noPhone",
            "compose.search",
            "compose.label.template",
            "compose.template.none",
            "compose.placeholder.template",
            "compose.label.message",
            "compose.button.send",
            "compose.alert.cannotSend.title",
            "compose.alert.cannotSend.message",
        ]
        assertKeysAreLocalized(keys)
    }

    func testOnboardingAndImportKeysResolve() {
        let keys: [String] = [
            "onboarding.subtitle",
            "onboarding.button.getStarted",
            "onboarding.button.skipImport",
            "import.navTitle",
            "import.button.iphoneContacts",
            "import.button.linkedinConnections",
            "import.footer.description",
            "import.section.linkedinGuide",
            "import.step.1",
            "import.step.2",
            "import.step.3",
            "import.step.4",
            "import.step.5",
            "import.step.6",
            "import.step.7",
            "import.alert.error.title",
        ]
        assertKeysAreLocalized(keys)
    }

    func testFrequencyKeysResolve() {
        let keys: [String] = [
            "frequency.daily",
            "frequency.weekly",
            "frequency.biweekly",
            "frequency.monthly",
            "frequency.bimonthly",
            "frequency.quarterly",
            "frequency.custom",
        ]
        assertKeysAreLocalized(keys)
    }

    func testTemplateKeysResolve() {
        let keys: [String] = [
            "templateList.navTitle",
            "templateEdit.section.title",
            "templateEdit.placeholder.title",
            "templateEdit.section.message",
            "templateEdit.navTitle.new",
            "templateEdit.navTitle.edit",
            "templateEdit.toast.saved",
            "templateEdit.toast.updated",
        ]
        assertKeysAreLocalized(keys)
    }

    // MARK: - Warm redesign

    /// All 52 warm-redesign keys, grouped by surface.
    private static let warmKeys: [String] = [
        // category × 7 fields
        "warm.category.family.label", "warm.category.friends.label", "warm.category.work.label",
        "warm.category.milestones.label", "warm.category.community.label",
        "warm.category.family.groupName", "warm.category.friends.groupName", "warm.category.work.groupName",
        "warm.category.milestones.groupName", "warm.category.community.groupName",
        "warm.category.family.headline.line1", "warm.category.friends.headline.line1",
        "warm.category.work.headline.line1", "warm.category.milestones.headline.line1",
        "warm.category.community.headline.line1",
        "warm.category.family.headline.line2", "warm.category.friends.headline.line2",
        "warm.category.work.headline.line2", "warm.category.milestones.headline.line2",
        "warm.category.community.headline.line2",
        "warm.category.family.body", "warm.category.friends.body", "warm.category.work.body",
        "warm.category.milestones.body", "warm.category.community.body",
        "warm.category.family.prompt", "warm.category.friends.prompt", "warm.category.work.prompt",
        "warm.category.milestones.prompt", "warm.category.community.prompt",
        "warm.category.family.promptShort", "warm.category.friends.promptShort",
        "warm.category.work.promptShort", "warm.category.milestones.promptShort",
        "warm.category.community.promptShort",
        // card chrome
        "warm.card.tickleIdea",
        // screen subtitles + chrome
        "warm.network.subtitle", "warm.network.filter.all", "warm.network.empty.filtered",
        "warm.tickle.subtitle.empty",
        "warm.groups.subtitle", "warm.groups.yourGroups",
        // onboarding
        "warm.onboarding.title", "warm.onboarding.subtitle",
        "warm.onboarding.cta.import", "warm.onboarding.cta.add", "warm.onboarding.footer",
        // contact detail
        "warm.contact.addPhoto", "warm.contact.tickleEvery", "warm.contact.sendTickle",
        "warm.contact.createTickle", "warm.contact.call", "warm.contact.email",
        "warm.contact.lastConnected", "warm.contact.notes.placeholder",
    ]

    /// Every warm key resolves to a non-empty value in the default (English) bundle.
    func testWarmKeysResolveInDefaultLocale() {
        assertKeysAreLocalized(Self.warmKeys)
    }

    /// Every warm key has a non-empty translation in every shipped locale.
    /// Catches keys we forgot to add to a locale during translation passes.
    func testWarmKeysCoveredAcrossAllLocales() throws {
        let appBundle = Bundle(for: Contact.self)
        let shippedLocales = ["en", "ar", "cs", "de", "el", "es", "fr", "he", "hi", "hu",
                              "it", "ja", "ko", "nl", "pl", "pt", "ro", "ru", "sv", "ur", "zh-Hans"]
        var missing: [String] = []
        for code in shippedLocales {
            guard let lpath = appBundle.path(forResource: code, ofType: "lproj"),
                  let lbundle = Bundle(path: lpath) else {
                XCTFail("\(code).lproj missing from app bundle")
                continue
            }
            for key in Self.warmKeys {
                let value = lbundle.localizedString(forKey: key, value: "__MISSING__", table: nil)
                if value == "__MISSING__" || value.isEmpty {
                    missing.append("\(code) ← \(key)")
                }
            }
        }
        XCTAssert(missing.isEmpty,
                  "warm keys missing from compiled bundles:\n  " + missing.joined(separator: "\n  "))
    }

    // MARK: - Interpolation

    func testInterpolatedOverdueLabel() {
        let days = 3
        let result = String(localized: "tickleRow.due.overdue \(days)")
        XCTAssertFalse(result.isEmpty, "overdue label should not be empty")
        XCTAssertTrue(result.contains("3"), "overdue label should contain the day count")
    }

    func testInterpolatedUpcomingLabel() {
        let days = 5
        let result = String(localized: "tickleRow.due.upcoming \(days)")
        XCTAssertFalse(result.isEmpty, "upcoming label should not be empty")
        XCTAssertTrue(result.contains("5"), "upcoming label should contain the day count")
    }

    func testPluralContactCount() {
        let oneResult = String(localized: "groupList.contactCount \(1)")
        let manyResult = String(localized: "groupList.contactCount \(5)")
        XCTAssertTrue(oneResult.contains("contact"), "singular should contain 'contact'")
        XCTAssertTrue(manyResult.contains("contacts"), "plural should contain 'contacts'")
    }

    func testPluralCustomInterval() {
        let oneResult = String(localized: "tickleEdit.stepper.customInterval \(1)")
        let manyResult = String(localized: "tickleEdit.stepper.customInterval \(7)")
        XCTAssertTrue(oneResult.contains("day") && !oneResult.contains("days"),
                      "singular should use 'day' not 'days': \(oneResult)")
        XCTAssertTrue(manyResult.contains("days"),
                      "plural should use 'days': \(manyResult)")
    }

    func testDeleteConfirmTitle() {
        let name = "Jane Doe"
        let result = String(localized: "contactDetail.deleteConfirm.title \(name)")
        XCTAssertTrue(result.contains("Jane Doe"), "confirm title should contain the contact name")
        XCTAssertFalse(result.isEmpty)
    }

    func testToastAddedToNamedGroup() {
        let contactName = "Alice Smith"
        let groupName = "Hiking Crew"
        let result = String(localized: "addMembers.toast.addedToNamedGroup \(contactName) \(groupName)")
        XCTAssertTrue(result.contains("Alice Smith"), "toast should contain contact name")
        XCTAssertTrue(result.contains("Hiking Crew"), "toast should contain group name")
    }

    func testToastAddedToGroup() {
        let contactName = "Bob Jones"
        let result = String(localized: "addMembers.toast.addedToGroup \(contactName)")
        XCTAssertTrue(result.contains("Bob Jones"), "toast should contain contact name")
        XCTAssertTrue(result.contains("group"), "toast should contain generic 'group' fallback")
    }

    // MARK: - TickleFrequency.localizedName

    func testFrequencyLocalizedNames() {
        XCTAssertEqual(TickleFrequency.daily.localizedName, "Daily")
        XCTAssertEqual(TickleFrequency.weekly.localizedName, "Weekly")
        XCTAssertEqual(TickleFrequency.biweekly.localizedName, "Every 2 weeks")
        XCTAssertEqual(TickleFrequency.monthly.localizedName, "Monthly")
        XCTAssertEqual(TickleFrequency.bimonthly.localizedName, "Every 2 months")
        XCTAssertEqual(TickleFrequency.quarterly.localizedName, "Quarterly")
        XCTAssertEqual(TickleFrequency.custom.localizedName, "Custom")
    }

    // MARK: - RTL Localizations

    /// xcstrings compiles each locale into `<lang>.lproj/Localizable.strings` at build time.
    /// Missing RTL locale bundles usually mean the catalog entries were dropped during build.
    func testRTLLocalizationsCompiledIntoBundle() throws {
        let appBundle = Bundle(for: Contact.self)
        for code in ["ar", "he", "ur"] {
            let path = appBundle.path(forResource: code, ofType: "lproj")
            XCTAssertNotNil(path, "\(code).lproj not found in app bundle — entries may be missing from the String Catalog")
        }
    }

    /// Loads each RTL lproj directly and confirms sample keys resolve to the injected text.
    /// Catches both "locale missing" and "locale falling back to English" regressions.
    func testRTLSampleKeysResolveToLocalizedText() throws {
        let appBundle = Bundle(for: Contact.self)

        let localeCases: [(code: String, samples: [(key: String, expected: String)])] = [
            ("ar", [
                ("settings.navTitle", "الإعدادات"),
                ("tab.network", "الشبكة"),
                ("common.save", "حفظ"),
                ("tickleEdit.navTitle.new", "تذكير جديد"),
            ]),
            ("he", [
                ("settings.navTitle", "הגדרות"),
                ("tab.network", "רשת"),
                ("common.save", "שמירה"),
                ("tickleEdit.navTitle.new", "תזכורת חדשה"),
            ]),
            ("ur", [
                ("settings.navTitle", "ترتیبات"),
                ("tab.network", "نیٹ ورک"),
                ("common.save", "محفوظ کریں"),
                ("tickleEdit.navTitle.new", "نئی یاددہانی"),
            ]),
        ]

        for (code, samples) in localeCases {
            let path = try XCTUnwrap(appBundle.path(forResource: code, ofType: "lproj"))
            let bundle = try XCTUnwrap(Bundle(path: path))
            for (key, expected) in samples {
                let value = bundle.localizedString(forKey: key, value: "", table: nil)
                XCTAssertEqual(value, expected, "\(code) value for \(key) should be \(expected), got \(value)")
            }
        }
    }

    // MARK: - Helper

    private func assertKeysAreLocalized(_ keys: [String]) {
        for key in keys {
            let localized = String(localized: String.LocalizationValue(key))
            XCTAssertFalse(localized.isEmpty, "Localized string for '\(key)' should not be empty")
            XCTAssertNotEqual(localized, key, "Key '\(key)' was not found in String Catalog — raw key returned")
        }
    }
}
