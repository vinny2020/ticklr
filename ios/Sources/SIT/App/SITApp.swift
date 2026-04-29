import SwiftUI
import SwiftData

@main
struct SITApp: App {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var showLaunch = true

    let modelContainer: ModelContainer = {
        let schema = Schema([
            Contact.self,
            ContactGroup.self,
            MessageTemplate.self,
            TickleReminder.self
        ])
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        do {
            return try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("Failed to create ModelContainer: \(error)")
        }
    }()

    init() {
        MessageTemplateSeed.seedIfNeeded(container: modelContainer)
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                if showLaunch {
                    LaunchScreenView()
                        .transition(.opacity)
                } else if !hasCompletedOnboarding {
                    OnboardingView()
                        .transition(.opacity)
                } else {
                    ContentView()
                        .transition(.opacity)
                }
            }
            .animation(.easeOut(duration: 0.4), value: showLaunch)
            .animation(.easeOut(duration: 0.4), value: hasCompletedOnboarding)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    showLaunch = false
                }
            }
        }
        .modelContainer(modelContainer)
    }
}
