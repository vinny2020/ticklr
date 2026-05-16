import SwiftUI

/// Warm contact row used by NetworkListView. Replaces the previous
/// indigo-monogram + trailing-icon-stack design with the warm pattern:
/// `ContactPhotoView` (size 36, category-tinted), name + company stack,
/// and a single trailing chevron.
struct ContactRowView: View {
    let contact: Contact
    var warmth: Warmth = .subtle

    var body: some View {
        let category = WarmCategory.resolve(for: contact)
        let palette = WarmTheme.palette(for: warmth)

        HStack(spacing: 12) {
            ContactPhotoView(contact: contact, category: category, style: .list)

            VStack(alignment: .leading, spacing: 1) {
                Text(contact.fullName)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(palette.ink)
                    .lineLimit(1)
                if !contact.company.isEmpty {
                    Text(contact.company)
                        .font(.system(size: 13))
                        .foregroundStyle(palette.ink2)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 8)

            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(palette.ink3)
        }
        .padding(.horizontal, WarmSpacing.lg)
        .padding(.vertical, WarmSpacing.md)
        .contentShape(Rectangle())
    }
}
