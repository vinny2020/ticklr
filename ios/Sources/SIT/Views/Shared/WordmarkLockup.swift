import SwiftUI

/// Ticklr wordmark lockup — warm "Open Ink" theme.
///
/// Evolves the retired navy/amber card into an open, editorial lockup: the
/// Bebas Neue wordmark in warm ink sits straight on the surface, a hairline
/// rule above the "YOUR PEOPLE MATTER" tagline. No background — it adapts to
/// whatever paper/dark surface it's placed on, and flips ink colors for dark
/// mode automatically.
///
/// - `wordmarkSize`  point size of the Bebas wordmark (drives all other metrics)
/// - `showRule`      hairline divider; auto-suppress below ~36pt for legibility
/// - `dot`           optional terracotta full-stop ("Ticklr.") — one warm accent
struct WordmarkLockup: View {
    var wordmarkSize: CGFloat = 64
    var showRule: Bool = true
    var dot: Bool = false

    @Environment(\.colorScheme) private var scheme

    // ── Warm theme tokens ───────────────────────────────────────────────
    private var ink: Color {        // wordmark
        scheme == .dark ? Color(red: 0.957, green: 0.937, blue: 0.890)   // #F4EFE3
                        : Color(red: 0.102, green: 0.122, blue: 0.165)   // #1A1F2A
    }
    private var ink2: Color {       // tagline
        scheme == .dark ? Color(red: 0.722, green: 0.663, blue: 0.557)   // #B8A98E
                        : Color(red: 0.361, green: 0.392, blue: 0.439)   // #5C6470
    }
    private var ink3: Color {       // rule
        scheme == .dark ? Color(red: 0.486, green: 0.424, blue: 0.314)   // #7C6C50
                        : Color(red: 0.565, green: 0.600, blue: 0.643)   // #9099A4
    }
    private var terracotta: Color { // optional dot
        scheme == .dark ? Color(red: 0.816, green: 0.541, blue: 0.416)   // #D08A6A
                        : Color(red: 0.698, green: 0.388, blue: 0.259)   // #B26342
    }

    private var taglineSize: CGFloat { max(9, wordmarkSize * 0.17) }
    private var ruleVisible: Bool { showRule && wordmarkSize >= 36 }

    var body: some View {
        VStack(spacing: wordmarkSize * 0.16) {
            HStack(alignment: .bottom, spacing: wordmarkSize * 0.06) {
                Text("Ticklr")
                    .font(.custom("BebasNeue-Regular", size: wordmarkSize))
                    .tracking(-wordmarkSize * 0.012)
                    .foregroundStyle(ink)
                if dot {
                    Circle()
                        .fill(terracotta)
                        .frame(width: wordmarkSize * 0.15, height: wordmarkSize * 0.15)
                        .padding(.bottom, wordmarkSize * 0.10)
                }
            }
            if ruleVisible {
                Rectangle()
                    .fill(ink3.opacity(scheme == .dark ? 0.6 : 0.5))
                    .frame(width: wordmarkSize * 0.65, height: 1)
            }
            Text("YOUR PEOPLE MATTER")
                .font(.system(size: taglineSize, weight: .medium))
                .tracking(taglineSize * 0.34)
                .foregroundStyle(ink2)
        }
    }
}

#Preview("Light") {
    WordmarkLockup()
        .padding(40)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 1, green: 1, blue: 1))
        .preferredColorScheme(.light)
}

#Preview("Dark") {
    WordmarkLockup(dot: true)
        .padding(40)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 0.086, green: 0.075, blue: 0.063)) // #161310
        .preferredColorScheme(.dark)
}
