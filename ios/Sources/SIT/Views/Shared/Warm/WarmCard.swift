import SwiftUI

/// The warm category card. Three variants per system.jsx (lines 302-403):
/// `.hero` (16:9 illustration on top, headline + body + prompt below),
/// `.compact` (square illustration left, condensed copy right),
/// `.row` (used on the Groups list — illustration left, name + count right).
struct WarmCard: View {
    enum Variant {
        case hero
        case compact
        case row
    }

    let category: WarmCategory
    var variant: Variant = .hero
    var warmth: Warmth = .subtle
    var showPrompt: Bool = true
    /// Optional override to display a count under the headline (e.g. on
    /// the Groups list — "12 contacts").
    var contactsCount: Int? = nil
    var onTap: (() -> Void)? = nil

    var body: some View {
        Button(action: { onTap?() }) {
            switch variant {
            case .hero:    heroBody
            case .compact: compactBody
            case .row:     rowBody
            }
        }
        .buttonStyle(.plain)
    }

    // MARK: - Variants

    private var heroBody: some View {
        let palette = WarmTheme.palette(for: warmth)
        return VStack(alignment: .leading, spacing: 0) {
            illustrationFrame(aspect: 16.0 / 9.0)
                .clipShape(
                    UnevenRoundedRectangle(
                        topLeadingRadius: WarmRadius.cardHero,
                        topTrailingRadius: WarmRadius.cardHero,
                        style: .continuous
                    )
                )

            VStack(alignment: .leading, spacing: 12) {
                CategoryBadge(category: category, size: 44)
                    .padding(.bottom, 4)

                headline

                Text(category.localizedBody)
                    .font(.system(size: 14))
                    .foregroundStyle(palette.ink2)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)

                if let contactsCount {
                    countLine(contactsCount)
                }

                if showPrompt {
                    TicklePrompt(category: category, warmth: warmth)
                        .padding(.top, 4)
                }
            }
            .padding(WarmSpacing.lg)
        }
        .background(palette.cardBg)
        .clipShape(RoundedRectangle(cornerRadius: WarmRadius.cardHero, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: WarmRadius.cardHero, style: .continuous)
                .stroke(palette.cardBorder, lineWidth: 1)
        )
        .shadow(color: palette.cardShadow, radius: 2, x: 0, y: 1)
    }

    private var compactBody: some View {
        let palette = WarmTheme.palette(for: warmth)
        return HStack(alignment: .top, spacing: 14) {
            illustrationFrame(aspect: 1)
                .frame(width: 88, height: 88)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            VStack(alignment: .leading, spacing: 6) {
                headline
                if let contactsCount {
                    countLine(contactsCount)
                } else {
                    Text(category.localizedBody)
                        .font(.system(size: 13))
                        .foregroundStyle(palette.ink2)
                        .lineLimit(2)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(WarmSpacing.md)
        .background(palette.cardBg)
        .clipShape(RoundedRectangle(cornerRadius: WarmRadius.cardCompact, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: WarmRadius.cardCompact, style: .continuous)
                .stroke(palette.cardBorder, lineWidth: 1)
        )
    }

    private var rowBody: some View {
        let palette = WarmTheme.palette(for: warmth)
        return HStack(spacing: 14) {
            illustrationFrame(aspect: 1)
                .frame(width: 132, height: 132)
                .clipShape(
                    UnevenRoundedRectangle(
                        topLeadingRadius: WarmRadius.card,
                        bottomLeadingRadius: WarmRadius.card,
                        style: .continuous
                    )
                )

            VStack(alignment: .leading, spacing: 6) {
                headline
                if let contactsCount {
                    countLine(contactsCount)
                }
                if showPrompt {
                    Text("\(category.localizedPromptShort)  →")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(category.palette.accent)
                        .padding(.top, 4)
                }
            }
            .padding(.vertical, WarmSpacing.md)
            Spacer(minLength: 0)
        }
        .padding(.trailing, WarmSpacing.lg)
        .background(palette.cardBg)
        .clipShape(RoundedRectangle(cornerRadius: WarmRadius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: WarmRadius.card, style: .continuous)
                .stroke(palette.cardBorder, lineWidth: 1)
        )
    }

    // MARK: - Pieces

    private var headline: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(category.localizedHeadlineLine1)
                .font(WarmHeadingFont.font(size: variant == .hero ? 28 : 20, warmth: warmth))
                .tracking(WarmHeadingFont.tracking(warmth: warmth))
                .foregroundStyle(category.palette.accent)
            Text(category.localizedHeadlineLine2)
                .font(WarmHeadingFont.font(size: variant == .hero ? 28 : 20, warmth: warmth))
                .tracking(WarmHeadingFont.tracking(warmth: warmth))
                .foregroundStyle(category.palette.accent)
        }
    }

    private func countLine(_ n: Int) -> some View {
        // Plural-aware key wired up in the localization commit.
        Text("\(n) contacts")
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(WarmTheme.palette(for: warmth).ink2)
    }

    private func illustrationFrame(aspect: CGFloat) -> some View {
        WarmIllustration(category: category)
            .aspectRatio(aspect, contentMode: .fit)
    }
}

#Preview("Hero") {
    ScrollView {
        VStack(spacing: 16) {
            ForEach(WarmCategory.allCases) { cat in
                WarmCard(category: cat, variant: .hero)
            }
        }
        .padding()
    }
    .background(WarmTheme.subtle.paper)
}

#Preview("Compact") {
    VStack(spacing: 12) {
        ForEach(WarmCategory.allCases) { cat in
            WarmCard(category: cat, variant: .compact, showPrompt: false, contactsCount: 12)
        }
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}

#Preview("Row") {
    VStack(spacing: 12) {
        ForEach(WarmCategory.allCases) { cat in
            WarmCard(category: cat, variant: .row, showPrompt: true, contactsCount: 8)
        }
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}
