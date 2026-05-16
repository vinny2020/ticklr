import SwiftUI

/// 56×56 rounded square with the category icon.
/// Mirrors the `CategoryBadge` in system.jsx (lines 258-271).
struct CategoryBadge: View {
    let category: WarmCategory
    var size: CGFloat = 56

    var body: some View {
        let palette = category.palette
        RoundedRectangle(cornerRadius: WarmRadius.badge, style: .continuous)
            .fill(palette.accentBadge)
            .frame(width: size, height: size)
            .overlay(
                Image(systemName: category.systemImageName)
                    .font(.system(size: size * 0.36, weight: .semibold))
                    .foregroundStyle(palette.accent)
            )
    }
}

#Preview {
    HStack(spacing: 12) {
        ForEach(WarmCategory.allCases) { cat in
            CategoryBadge(category: cat)
        }
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}
