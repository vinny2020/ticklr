import SwiftUI

/// Pill-shaped filter chip used on the Network screen.
/// iOS uses `border-radius: 999` per HANDOFF lines 60-62.
struct WarmFilterChip: View {
    enum Kind {
        case all
        case category(WarmCategory)
        /// A user-created (non-canonical) group filter (TIC-88). Carries the
        /// group's emoji so the chip can show it where a category shows its SF
        /// Symbol. Styled neutrally (like `.all`) so custom-group chips read as
        /// distinct from the canonical category chips that follow "All".
        case userGroup(emoji: String)
    }

    let kind: Kind
    let label: String
    var count: Int? = nil
    let isActive: Bool
    let action: () -> Void

    var warmth: Warmth = .subtle

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if case .category(let cat) = kind {
                    Image(systemName: cat.systemImageName)
                        .font(.system(size: 11, weight: .semibold))
                }
                if case .userGroup(let emoji) = kind {
                    Text(emoji)
                        .font(.system(size: 12))
                }
                Text(label)
                    .font(.system(size: 13, weight: .semibold))
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
                if let count {
                    Text("\(count)")
                        .font(.system(size: 11, weight: .semibold))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(countBadgeBackground)
                        .foregroundStyle(countBadgeForeground)
                        .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .foregroundStyle(foreground)
            .background(background)
            .clipShape(Capsule())
            .overlay(
                Capsule().stroke(borderColor, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    private var background: Color {
        switch (kind, isActive) {
        case (.all, true), (.userGroup, true):    palette.ink
        case (.all, false), (.userGroup, false):  palette.cardBg
        case (.category(let c), true):            c.palette.accent
        case (.category(let c), false):           c.palette.accentTint
        }
    }

    private var foreground: Color {
        switch (kind, isActive) {
        case (.all, true), (.userGroup, true):    palette.paper
        case (.all, false), (.userGroup, false):  palette.ink
        case (.category, true):                   Color(red: 0.98, green: 0.96, blue: 0.89)
        case (.category(let c), false):           c.palette.accent
        }
    }

    private var borderColor: Color {
        switch (kind, isActive) {
        case (.category, false), (.userGroup, false):  palette.cardBorder
        default:                                       Color.clear
        }
    }

    private var countBadgeBackground: Color {
        switch (kind, isActive) {
        case (.all, true), (.userGroup, true):    palette.paper.opacity(0.18)
        case (.all, false), (.userGroup, false):  palette.ink.opacity(0.06)
        case (.category, true):                   Color.white.opacity(0.22)
        case (.category(let c), false):           c.palette.accentBadge
        }
    }

    private var countBadgeForeground: Color {
        foreground
    }
}

#Preview("Inactive") {
    HStack(spacing: 8) {
        WarmFilterChip(kind: .all, label: "All", count: 142, isActive: false) {}
        WarmFilterChip(kind: .category(.family), label: "Family", count: 12, isActive: false) {}
        WarmFilterChip(kind: .category(.friends), label: "Friends", count: 24, isActive: false) {}
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}

#Preview("Active") {
    HStack(spacing: 8) {
        WarmFilterChip(kind: .all, label: "All", count: 142, isActive: true) {}
        WarmFilterChip(kind: .category(.family), label: "Family", count: 12, isActive: true) {}
        WarmFilterChip(kind: .category(.work), label: "Work", count: 8, isActive: true) {}
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}
