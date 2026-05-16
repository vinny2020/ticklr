import SwiftUI

/// Bottom strip on a warm card. Reads "Tickle idea: {prompt}".
/// Mirrors `TicklePrompt` in system.jsx (lines 273-298).
struct TicklePrompt: View {
    let category: WarmCategory
    var warmth: Warmth = .subtle
    var onTap: (() -> Void)? = nil

    var body: some View {
        let palette = category.palette
        Button(action: { onTap?() }) {
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
                Image(systemName: "chevron.right")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(palette.accent)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(palette.accentTint)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
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
