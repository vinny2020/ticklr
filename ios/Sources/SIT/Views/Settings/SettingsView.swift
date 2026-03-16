import SwiftUI

struct SettingsView: View {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = true

    var body: some View {
        NavigationStack {
            List {
                Section("Data") {
                    NavigationLink("Import Contacts") {
                        ImportView()
                    }
                }

                Section("About") {
                    LabeledContent("App", value: "SIT: Stay in Touch")
                    LabeledContent("Version", value: "1.0.0")
                    LabeledContent("Built by", value: "Xaymaca")
                }

                Section {
                    Button("Reset Onboarding", role: .destructive) {
                        hasCompletedOnboarding = false
                    }
                }
            }
            .navigationTitle("Settings")
        }
    }
}
