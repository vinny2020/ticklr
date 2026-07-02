import SwiftUI

struct OnboardingView: View {
    @State private var presented: Sheet? = nil

    private enum Sheet: Identifiable {
        case importContacts
        case addContact
        var id: Int { hashValue }
    }

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    var body: some View {
        let palette = palette
        GeometryReader { geo in
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    brandRow
                        .padding(.top, max(geo.safeAreaInsets.top, 12))

                    titleBlock
                        .padding(.horizontal, WarmSpacing.lg)

                    WarmCard(category: .family,
                             variant: .hero,
                             warmth: warmth,
                             showPrompt: false)
                        .padding(.horizontal, WarmSpacing.lg)

                    ctaStack
                        .padding(.horizontal, WarmSpacing.lg)

                    privacyFooter
                        .padding(.horizontal, WarmSpacing.lg)
                        .padding(.bottom, max(geo.safeAreaInsets.bottom, 16))
                }
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity)
                .frame(minHeight: geo.size.height - geo.safeAreaInsets.top - geo.safeAreaInsets.bottom,
                       alignment: .top)
            }
            .scrollIndicators(.hidden)
        }
        .background(palette.paper.ignoresSafeArea())
        // Onboarding completes only on a success path — `ImportView` and
        // `AddContactView` each set `hasCompletedOnboarding` when an import
        // finishes or a first contact is saved. Cancelling either sheet leaves
        // the flag untouched so the user returns to onboarding rather than
        // being dropped into an empty app (TIC-78).
        .sheet(item: $presented) { sheet in
            switch sheet {
            case .importContacts: ImportView()
            case .addContact:     AddContactView()
            }
        }
    }

    // MARK: - Pieces

    private var brandRow: some View {
        HStack(spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(red: 0.039, green: 0.086, blue: 0.157))
                    .frame(width: 28, height: 28)
                Image(systemName: "bolt.fill")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(Color(red: 0.96, green: 0.78, blue: 0.25))
            }
            Text("Ticklr")
                .font(.custom("BebasNeue-Regular", size: 22))
                .tracking(-0.4)
                .foregroundStyle(palette.ink)
        }
        .padding(.horizontal, WarmSpacing.lg)
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "warm.onboarding.title", defaultValue: "Stay in Touch"))
                .font(WarmHeadingFont.font(size: 44, warmth: warmth))
                .tracking(WarmHeadingFont.tracking(warmth: warmth))
                .foregroundStyle(WarmCategory.family.palette.accent)
                .fixedSize(horizontal: false, vertical: true)

            Text(String(localized: "warm.onboarding.subtitle",
                        defaultValue: "A gentler way to keep the people who matter close."))
                .font(.system(size: 16))
                .foregroundStyle(palette.ink2)
                .lineSpacing(2)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var ctaStack: some View {
        VStack(spacing: 10) {
            Button {
                presented = .importContacts
            } label: {
                Text(String(localized: "warm.onboarding.cta.import", defaultValue: "Build my network"))
                    .font(.system(size: 16, weight: .semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color(red: 0.039, green: 0.086, blue: 0.157))
                    .foregroundStyle(Color(red: 0.98, green: 0.96, blue: 0.89))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(.plain)

            Button {
                presented = .addContact
            } label: {
                Text(String(localized: "warm.onboarding.cta.add", defaultValue: "Add my first contact"))
                    .font(.system(size: 14, weight: .semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .foregroundStyle(palette.ink2)
            }
            .buttonStyle(.plain)
        }
    }

    private var privacyFooter: some View {
        Text(String(localized: "warm.onboarding.footer",
                    defaultValue: "Ticklr is private by design. Your contacts and notes never leave this device."))
            .font(.system(size: 11.5))
            .foregroundStyle(palette.ink3)
            .multilineTextAlignment(.leading)
            .lineSpacing(1)
            .padding(.top, 4)
    }
}

#Preview {
    OnboardingView()
}
