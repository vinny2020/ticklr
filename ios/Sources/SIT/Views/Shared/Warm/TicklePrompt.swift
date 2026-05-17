import SwiftUI

/// Bottom decorative strip on a warm card. Reads "Tickle idea: {prompt}".
/// Mirrors `TicklePrompt` in system.jsx (lines 273-298). Non-interactive —
/// the prompt is a gentle suggestion, not an action target.
struct TicklePrompt: View {
    let category: WarmCategory
    var warmth: Warmth = .subtle

    var body: some View {
        let palette = category.palette
        HStack(spacing: 10) {
            Circle()
                .fill(palette.accent)
                .frame(width: 6, height: 6)
            Text(String(localized: "warm.card.tickleIdea", defaultValue: "Tickle idea:"))
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(palette.accent)
            Text(category.localizedPromptShort)
                .font(.system(size: 12))
                .foregroundStyle(WarmTheme.palette(for: warmth).ink2)
                .lineLimit(1)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(palette.accentTint)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

#Preview {
    VStack(spacing: 12) {
        ForEach(WarmCategory.allCases) { cat in
            TicklePrompt(category: cat)
        }
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}
