import SwiftUI

/// Cream-paper rounded container with hairline-separated rows. Replaces
/// SwiftUI's default `List` chrome on warm-redesigned screens.
struct WarmListContainer<Content: View>: View {
    var warmth: Warmth = .subtle
    @ViewBuilder var content: Content

    var body: some View {
        let palette = WarmTheme.palette(for: warmth)
        VStack(spacing: 0) {
            content
        }
        .background(palette.cardBg)
        .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                .stroke(palette.cardBorder, lineWidth: 1)
        )
    }
}

/// Hairline divider tinted to the warm palette's `cardBorder`. Use between
/// rows inside `WarmListContainer`.
struct WarmRowDivider: View {
    var warmth: Warmth = .subtle

    var body: some View {
        Rectangle()
            .fill(WarmTheme.palette(for: warmth).cardBorder)
            .frame(height: 1)
            .padding(.leading, 64)  // align past leading avatar
    }
}

#Preview {
    WarmListContainer {
        ForEach(0..<3) { i in
            HStack {
                MonogramAvatar(initials: "AB", category: .family)
                VStack(alignment: .leading) {
                    Text("Anna Brown")
                        .font(.system(size: 15, weight: .semibold))
                    Text("Family · last contacted 3d ago")
                        .font(.caption)
                        .foregroundStyle(WarmTheme.subtle.ink2)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(WarmTheme.subtle.ink3)
                    .flipsForRightToLeftLayoutDirection(true)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            if i < 2 { WarmRowDivider() }
        }
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}
