import SwiftUI

struct OnboardingView: View {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var showingImport = false

    var body: some View {
        VStack(spacing: 32) {
            Spacer()

            VStack(spacing: 16) {
                Image(systemName: "person.2.circle.fill")
                    .font(.system(size: 72))
                    .foregroundStyle(.indigo)

                Text("Ticklr")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text(String(localized: "onboarding.subtitle"))
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Spacer()

            VStack(spacing: 12) {
                Button {
                    showingImport = true
                } label: {
                    Label(String(localized: "onboarding.button.getStarted"), systemImage: "arrow.right.circle.fill")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.indigo)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                Button {
                    hasCompletedOnboarding = true
                } label: {
                    Text(String(localized: "onboarding.button.skipImport"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
        .sheet(isPresented: $showingImport) {
            ImportView()
        }
    }
}
