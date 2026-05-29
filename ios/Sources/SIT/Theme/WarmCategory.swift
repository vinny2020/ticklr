import SwiftUI
import Foundation

// The 5 canonical relationship categories. Per the warm-redesign spec,
// "categories ARE groups" — each category is identified by a stable UUID
// that gets seeded as a `ContactGroup.id` on first launch (see
// CanonicalGroupSeed). User-created groups have other UUIDs and fall back to
// the `.community` palette in the UI.
//
// The UUIDs below are checked-in constants and MUST NEVER change once shipped
// — they're how every install identifies a canonical group. Generated once
// via uuidgen for the v1 warm-redesign branch.

enum WarmCategory: String, CaseIterable, Identifiable {
    case family
    case friends
    case work
    case milestones
    case community

    var id: String { rawValue }

    var groupUUID: UUID {
        switch self {
        case .family:     UUID(uuidString: "CA2C12E0-0FAD-4019-A681-0EC31436D02B")!
        case .friends:    UUID(uuidString: "B03D94E2-FAAD-4F9F-970E-DDAEDB1415A9")!
        case .work:       UUID(uuidString: "20C1A7D7-293C-437D-BD20-DFAE01AF78EB")!
        case .milestones: UUID(uuidString: "7FFDC83B-6713-4314-9B70-7971833E2D79")!
        case .community:  UUID(uuidString: "1850BD29-904F-430F-8DBC-1B7AF15836C9")!
        }
    }

    /// Stable display order on the Groups list: Family → Friends → Work →
    /// Milestones → Community. User-created groups follow after, sorted by
    /// createdAt.
    var sortOrder: Int {
        switch self {
        case .family: 0
        case .friends: 1
        case .work: 2
        case .milestones: 3
        case .community: 4
        }
    }

    static func from(groupId: UUID) -> WarmCategory? {
        allCases.first { $0.groupUUID == groupId }
    }

    /// Resolve a category for a contact based on their first canonical
    /// group membership. Falls back to `.community` so every contact
    /// has a tint to use (used for monograms, photo affordance, etc.
    /// where SOME tint is always needed).
    static func resolve(for contact: Contact) -> WarmCategory {
        resolveOptional(for: contact) ?? .community
    }

    /// Resolve a category for the contact's MOST RECENT canonical group
    /// membership, or nil if they aren't in any canonical group. SwiftData
    /// preserves append order on the relationship array; walking in
    /// reverse gives the last-added canonical group, which approximates
    /// "most recently added to". Used for the Contact Detail bottom hero
    /// card, which should hide entirely for uncategorized contacts.
    static func resolveOptional(for contact: Contact) -> WarmCategory? {
        for group in contact.groups.reversed() {
            if let cat = from(groupId: group.id) {
                return cat
            }
        }
        return nil
    }
}

// MARK: - Palette

struct WarmCategoryPalette {
    let accent: Color
    let accentSoft: Color
    let accentTint: Color
    let accentBadge: Color
}

extension WarmCategory {
    var palette: WarmCategoryPalette {
        switch self {
        case .family:
            return WarmCategoryPalette(
                accent:       hex("#9C3F3C"),
                accentSoft:   hex("#E8C9C4"),
                accentTint:   hex("#F4E2DC"),
                accentBadge:  hex("#F0CCC2")
            )
        case .friends:
            return WarmCategoryPalette(
                accent:       hex("#3F5C7A"),
                accentSoft:   hex("#C7D4E0"),
                accentTint:   hex("#DCE5ED"),
                accentBadge:  hex("#D2DDE7")
            )
        case .work:
            return WarmCategoryPalette(
                accent:       hex("#4F6B47"),
                accentSoft:   hex("#C7D3BD"),
                accentTint:   hex("#DCE4D2"),
                accentBadge:  hex("#CFDBC4")
            )
        case .milestones:
            return WarmCategoryPalette(
                accent:       hex("#A7791C"),
                accentSoft:   hex("#E6D1A0"),
                accentTint:   hex("#EDDEB6"),
                accentBadge:  hex("#E8CF94")
            )
        case .community:
            return WarmCategoryPalette(
                accent:       hex("#B26342"),
                accentSoft:   hex("#E8C2AC"),
                accentTint:   hex("#F0D4C2"),
                accentBadge:  hex("#EBC4AE")
            )
        }
    }

    /// SF Symbol that stands in for the spec's monoline icon.
    var systemImageName: String {
        switch self {
        case .family:     "heart.fill"
        case .friends:    "sparkles"
        case .work:       "briefcase.fill"
        case .milestones: "calendar"
        case .community:  "person.3.fill"
        }
    }

    /// Default emoji used as a `ContactGroup.emoji` fallback for the
    /// seeded canonical groups. Users can change this in Group edit.
    var defaultEmoji: String {
        switch self {
        case .family:     "👨\u{200D}👩\u{200D}👧"
        case .friends:    "💛"
        case .work:       "💼"
        case .milestones: "🎂"
        case .community:  "🏘️"
        }
    }
}

// MARK: - Localization

extension WarmCategory {
    /// Chip / filter label ("Family", "Friends", …). Distinct from the group
    /// name a user sees on the Groups list (which may be "Close Friends" /
    /// "Neighbors & Community" — see `localizedGroupName`).
    var localizedLabel: String {
        switch self {
        case .family:     String(localized: "warm.category.family.label", defaultValue: "Family")
        case .friends:    String(localized: "warm.category.friends.label", defaultValue: "Friends")
        case .work:       String(localized: "warm.category.work.label", defaultValue: "Work")
        case .milestones: String(localized: "warm.category.milestones.label", defaultValue: "Milestones")
        case .community:  String(localized: "warm.category.community.label", defaultValue: "Community")
        }
    }

    var localizedGroupName: String {
        switch self {
        case .family:     String(localized: "warm.category.family.groupName", defaultValue: "Family")
        case .friends:    String(localized: "warm.category.friends.groupName", defaultValue: "Close Friends")
        case .work:       String(localized: "warm.category.work.groupName", defaultValue: "Work")
        case .milestones: String(localized: "warm.category.milestones.groupName", defaultValue: "Milestones")
        case .community:  String(localized: "warm.category.community.groupName", defaultValue: "Neighbors & Community")
        }
    }

    var localizedHeadlineLine1: String {
        switch self {
        case .family:     String(localized: "warm.category.family.headline.line1", defaultValue: "Family")
        case .friends:    String(localized: "warm.category.friends.headline.line1", defaultValue: "Friends Stay")
        case .work:       String(localized: "warm.category.work.headline.line1", defaultValue: "Stronger")
        case .milestones: String(localized: "warm.category.milestones.headline.line1", defaultValue: "Remember")
        case .community:  String(localized: "warm.category.community.headline.line1", defaultValue: "Stronger")
        }
    }

    var localizedHeadlineLine2: String {
        switch self {
        case .family:     String(localized: "warm.category.family.headline.line2", defaultValue: "first, always")
        case .friends:    String(localized: "warm.category.friends.headline.line2", defaultValue: "Friends")
        case .work:       String(localized: "warm.category.work.headline.line2", defaultValue: "Networks")
        case .milestones: String(localized: "warm.category.milestones.headline.line2", defaultValue: "What Matters")
        case .community:  String(localized: "warm.category.community.headline.line2", defaultValue: "Together")
        }
    }

    var localizedBody: String {
        switch self {
        case .family:     String(localized: "warm.category.family.body", defaultValue: "The people who know you best are always worth checking in on.")
        case .friends:    String(localized: "warm.category.friends.body", defaultValue: "Nurture the bonds that time and distance can\u{2019}t fade.")
        case .work:       String(localized: "warm.category.work.body", defaultValue: "Grow alongside the people you build with — quarter by quarter.")
        case .milestones: String(localized: "warm.category.milestones.body", defaultValue: "Never miss the birthdays, anniversaries, and quiet milestones that count.")
        case .community:  String(localized: "warm.category.community.body", defaultValue: "Your wider circle keeps you grounded. Tend to it.")
        }
    }

    var localizedPrompt: String {
        switch self {
        case .family:     String(localized: "warm.category.family.prompt", defaultValue: "Send a \u{201C}thinking of you\u{201D} note to someone special.")
        case .friends:    String(localized: "warm.category.friends.prompt", defaultValue: "Reconnect with a friend you haven\u{2019}t talked to in a while.")
        case .work:       String(localized: "warm.category.work.prompt", defaultValue: "Check in and strengthen a professional bond.")
        case .milestones: String(localized: "warm.category.milestones.prompt", defaultValue: "Set a reminder for what matters most.")
        case .community:  String(localized: "warm.category.community.prompt", defaultValue: "Plan time together with the people you love.")
        }
    }

    var localizedPromptShort: String {
        switch self {
        case .family:     String(localized: "warm.category.family.promptShort", defaultValue: "Send a thinking-of-you note")
        case .friends:    String(localized: "warm.category.friends.promptShort", defaultValue: "Reconnect with an old friend")
        case .work:       String(localized: "warm.category.work.promptShort", defaultValue: "Check in with a colleague")
        case .milestones: String(localized: "warm.category.milestones.promptShort", defaultValue: "Set a milestone reminder")
        case .community:  String(localized: "warm.category.community.promptShort", defaultValue: "Plan time with your circle")
        }
    }
}

// MARK: - Group display name

extension ContactGroup {
    /// Name to show in the UI. The 5 canonical (seeded) groups resolve to the
    /// current-locale category name so they follow the device language —
    /// their stored `name` is just a first-launch seed artifact and is NOT
    /// authoritative. User-created groups use their stored `name` as typed.
    ///
    /// Use this everywhere a group name is *displayed*; keep `name` for
    /// editing, sorting, and duplicate checks.
    var displayName: String {
        WarmCategory.from(groupId: id)?.localizedGroupName ?? name
    }

    /// True for the 5 seeded canonical category groups (whose name is derived,
    /// not user-editable).
    var isCanonicalCategory: Bool {
        WarmCategory.from(groupId: id) != nil
    }
}

// MARK: - hex helper

private func hex(_ value: String) -> Color {
    var hexValue: UInt64 = 0
    Scanner(string: value.replacingOccurrences(of: "#", with: "")).scanHexInt64(&hexValue)
    return Color(
        red:   Double((hexValue & 0xFF0000) >> 16) / 255,
        green: Double((hexValue & 0x00FF00) >> 8) / 255,
        blue:  Double(hexValue & 0x0000FF) / 255
    )
}
