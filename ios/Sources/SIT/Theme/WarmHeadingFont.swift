import SwiftUI

// Heading-font selection for warm-redesign cards & screens.
// Mirrors `headingFontFor(locale, warmth)` in
// assets/design-system/project/warm-redesign/system.jsx (line 188).
//
// Latin: Bebas Neue (Subtle) / Newsreader (Mid, Strong).
// ar / hi / ja: bundled Noto families (Noto Naskh Arabic, Noto Serif
// Devanagari, Noto Serif JP). The Noto bundles are added in a follow-up
// commit; until they're registered, SwiftUI silently falls back to the
// system serif.

enum WarmHeadingFont {
    /// Returns a SwiftUI `Font` sized to `size` for the given locale and
    /// warmth tier. Tracking and weight follow the spec (semibold + normal
    /// tracking for non-Latin scripts).
    static func font(size: CGFloat, locale: Locale = .current, warmth: Warmth = .subtle) -> Font {
        let family = familyName(locale: locale, warmth: warmth)
        if let family {
            return Font.custom(family, size: size)
        }
        // Latin Mid / Strong — Newsreader not yet bundled, fall back to serif.
        if warmth != .subtle {
            return Font.system(size: size, weight: .medium, design: .serif)
        }
        // Latin Subtle: SF Pro until Bebas Neue (already bundled) is loaded.
        return Font.custom("BebasNeue-Regular", size: size)
    }

    /// Returns the registered font name, or nil when SwiftUI should pick a
    /// system fallback. Keep names in sync with the postscript names of the
    /// .ttf files added to `Resources/Fonts/`.
    private static func familyName(locale: Locale, warmth: Warmth) -> String? {
        switch locale.language.languageCode?.identifier {
        case "ar":
            return "NotoNaskhArabic-SemiBold"
        case "hi":
            return "NotoSerifDevanagari-SemiBold"
        case "ja":
            return "NotoSerifJP-SemiBold"
        default:
            switch warmth {
            case .subtle:
                return nil  // handled by caller (Bebas Neue already bundled)
            case .mid, .strong:
                return "Newsreader-Medium"
            }
        }
    }

    /// Letter-spacing (tracking) per spec line 200.
    static func tracking(locale: Locale = .current, warmth: Warmth = .subtle) -> CGFloat {
        switch locale.language.languageCode?.identifier {
        case "ar", "hi", "ja":
            return 0
        default:
            switch warmth {
            case .subtle: return 0.01
            case .mid:    return -0.01
            case .strong: return -0.015
            }
        }
    }
}
