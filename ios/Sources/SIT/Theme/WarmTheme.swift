import SwiftUI

// Warm-redesign palette ported from
// assets/design-system/project/warm-redesign/system.jsx (WARMTH constant).
// Spec defines light only; dark variants here are derived (HANDOFF doesn't
// spec dark mode — see project_warm_redesign memory).

enum Warmth: String, CaseIterable {
    case subtle, mid, strong
}

struct WarmPalette {
    let paper: Color
    let paperSurface: Color
    let paperSurfaceAlt: Color
    let cardBg: Color
    let cardBorder: Color
    let cardShadow: Color
    let ink: Color
    let ink2: Color
    let ink3: Color
}

enum WarmTheme {
    static func palette(for warmth: Warmth) -> WarmPalette {
        switch warmth {
        case .subtle: return subtle
        case .mid: return mid
        case .strong: return strong
        }
    }

    static let subtle = WarmPalette(
        paper:           dyn(light: "#FFFFFF", dark: "#161310"),
        paperSurface:    dyn(light: "#F2EFE8", dark: "#1F1B16"),
        paperSurfaceAlt: dyn(light: "#F8F4EB", dark: "#251F18"),
        cardBg:          dyn(light: "#FBF7EE", dark: "#241E16"),
        cardBorder:      dyn(light: "#3C2A14", lightAlpha: 0.06, dark: "#F5E6C8", darkAlpha: 0.08),
        cardShadow:      Color.black.opacity(0.04),
        ink:             dyn(light: "#1A1F2A", dark: "#F4EFE3"),
        ink2:            dyn(light: "#5C6470", dark: "#B8A98E"),
        ink3:            dyn(light: "#9099A4", dark: "#847865")
    )

    static let mid = WarmPalette(
        paper:           dyn(light: "#F4EFE3", dark: "#13110D"),
        paperSurface:    dyn(light: "#EFE9DB", dark: "#1A1712"),
        paperSurfaceAlt: dyn(light: "#E8E1CF", dark: "#211C16"),
        cardBg:          dyn(light: "#FAF5E8", dark: "#26201A"),
        cardBorder:      dyn(light: "#3C2A14", lightAlpha: 0.08, dark: "#F5E6C8", darkAlpha: 0.10),
        cardShadow:      Color.black.opacity(0.05),
        ink:             dyn(light: "#28241D", dark: "#F4EFE3"),
        ink2:            dyn(light: "#6B5F4F", dark: "#B5A78E"),
        ink3:            dyn(light: "#9A8E7C", dark: "#7E725F")
    )

    static let strong = WarmPalette(
        paper:           dyn(light: "#EFE9DA", dark: "#100E09"),
        paperSurface:    dyn(light: "#E7DFCC", dark: "#171410"),
        paperSurfaceAlt: dyn(light: "#DDD3BC", dark: "#1E1A14"),
        cardBg:          dyn(light: "#FAF4E2", dark: "#241E16"),
        cardBorder:      dyn(light: "#3C2A14", lightAlpha: 0.10, dark: "#F5E6C8", darkAlpha: 0.12),
        cardShadow:      Color.black.opacity(0.06),
        ink:             dyn(light: "#211C13", dark: "#F4EFE3"),
        ink2:            dyn(light: "#6C5E48", dark: "#B0A285"),
        ink3:            dyn(light: "#998B72", dark: "#796C56")
    )
}

enum WarmRadius {
    static let cardHero: CGFloat = 22
    static let card: CGFloat = 20
    static let cardCompact: CGFloat = 18
    static let surface: CGFloat = 16
    static let badge: CGFloat = 14
    static let chip: CGFloat = 999  // pill
    static let photo: CGFloat = 36
}

enum WarmSpacing {
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 22
    static let xxl: CGFloat = 32
}

// MARK: - Color helpers

private func dyn(light: String, dark: String) -> Color {
    Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light)
    })
}

private func dyn(light: String, lightAlpha: Double, dark: String, darkAlpha: Double) -> Color {
    Color(uiColor: UIColor { trait in
        let hex = trait.userInterfaceStyle == .dark ? dark : light
        let alpha = trait.userInterfaceStyle == .dark ? darkAlpha : lightAlpha
        return UIColor(hex: hex).withAlphaComponent(CGFloat(alpha))
    })
}

extension UIColor {
    fileprivate convenience init(hex: String) {
        var hexValue: UInt64 = 0
        let scanner = Scanner(string: hex.replacingOccurrences(of: "#", with: ""))
        scanner.scanHexInt64(&hexValue)
        let r = CGFloat((hexValue & 0xFF0000) >> 16) / 255
        let g = CGFloat((hexValue & 0x00FF00) >> 8) / 255
        let b = CGFloat(hexValue & 0x0000FF) / 255
        self.init(red: r, green: g, blue: b, alpha: 1)
    }
}

// Convenience access from SwiftUI environment.
extension EnvironmentValues {
    var warmth: Warmth {
        get { self[WarmthKey.self] }
        set { self[WarmthKey.self] = newValue }
    }
}

private struct WarmthKey: EnvironmentKey {
    static let defaultValue: Warmth = .subtle
}
