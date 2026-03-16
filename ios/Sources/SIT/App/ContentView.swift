import SwiftUI

struct ContentView: View {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false

    var body: some View {
        if hasCompletedOnboarding {
            TabView {
                NetworkListView()
                    .tabItem { Label("Network", systemImage: "person.2.fill") }
                TickleListView()
                    .tabItem { Label("Tickle", systemImage: "bell.badge.fill") }
                GroupListView()
                    .tabItem { Label("Groups", systemImage: "person.3.fill") }
                ComposeView()
                    .tabItem { Label("Compose", systemImage: "square.and.pencil") }
                SettingsView()
                    .tabItem { Label("Settings", systemImage: "gearshape.fill") }
            }
            .tint(.indigo)
        } else {
            OnboardingView()
        }
    }
}

#Preview {
    ContentView()
}
