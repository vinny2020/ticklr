import SwiftUI

/// Small uppercase label used above sections and inside cards.
/// Mirrors `Eyebrow` in system.jsx (lines 406-415).
struct WarmEyebrow: View {
    let text: String
    var warmth: Warmth = .subtle

    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 11, weight: .semibold))
            .tracking(11 * 0.16)
            .foregroundStyle(WarmTheme.palette(for: warmth).ink3)
    }
}

#Preview {
    VStack(alignment: .leading, spacing: 16) {
        WarmEyebrow(text: "Today")
        WarmEyebrow(text: "Upcoming")
        WarmEyebrow(text: "Snoozed")
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}
