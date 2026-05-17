import SwiftUI

/// Circular avatar showing a contact's initials, tinted by their category.
/// Two callers: list rows (`size: 36`) and the empty contact-detail
/// affordance (`size: 132, dashed: true`). For the dashed variant + camera
/// badge + "Add a photo" link, see `MonogramPhotoAffordance`.
struct MonogramAvatar: View {
    let initials: String
    let category: WarmCategory
    var size: CGFloat = 36

    var body: some View {
        let palette = category.palette
        Circle()
            .fill(palette.accent)
            .frame(width: size, height: size)
            .overlay(
                Text(initials.uppercased())
                    .font(.system(size: size * 0.40, weight: .semibold))
                    .foregroundStyle(Color(red: 0.98, green: 0.96, blue: 0.89))
            )
    }
}

/// Empty-state photo affordance for the Contact Detail header.
/// Dashed circle in the category accent + faded monogram + camera badge.
/// Spec: HANDOFF.md lines 22-30.
struct MonogramPhotoAffordance: View {
    let initials: String
    let category: WarmCategory
    var size: CGFloat = 132

    var body: some View {
        let palette = category.palette
        ZStack(alignment: .bottomTrailing) {
            ZStack {
                Circle()
                    .strokeBorder(
                        palette.accent.opacity(0.55),
                        style: StrokeStyle(lineWidth: 2, dash: [8, 6])
                    )
                    .frame(width: size, height: size)
                Text(initials.uppercased())
                    .font(.system(size: size * 0.32, weight: .semibold))
                    .foregroundStyle(palette.accent.opacity(0.32))
            }

            Circle()
                .fill(palette.accent)
                .frame(width: size * 0.27, height: size * 0.27)
                .overlay(
                    Image(systemName: "camera.fill")
                        .font(.system(size: size * 0.12, weight: .semibold))
                        .foregroundStyle(.white)
                )
                .offset(x: size * 0.03, y: size * 0.03)
        }
        .frame(width: size, height: size)
    }
}

#Preview("List avatars") {
    HStack(spacing: 16) {
        ForEach(WarmCategory.allCases) { cat in
            MonogramAvatar(initials: "AB", category: cat)
        }
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}

#Preview("Photo affordance") {
    HStack(spacing: 24) {
        ForEach([WarmCategory.family, .friends, .work]) { cat in
            MonogramPhotoAffordance(initials: "JS", category: cat)
        }
    }
    .padding()
    .background(WarmTheme.subtle.paper)
}
