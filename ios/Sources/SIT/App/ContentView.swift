import SwiftUI

enum AppTab {
    case network, tickle, groups, compose, settings
}

struct ContentView: View {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var selectedTab: AppTab = .tickle

    var body: some View {
        if hasCompletedOnboarding {
            TabView(selection: $selectedTab) {
                NetworkListView()
                    .tabItem { Label(String(localized: "tab.network"), systemImage: "person.2.fill") }
                    .tag(AppTab.network)
                GroupListView()
                    .tabItem { Label(String(localized: "tab.groups"), systemImage: "person.3.fill") }
                    .tag(AppTab.groups)
                TickleListView()
                    .tabItem { Label(String(localized: "tab.tickle"), systemImage: "bell.badge.fill") }
                    .tag(AppTab.tickle)
                ComposeView(onCancel: { selectedTab = .tickle })
                    .tabItem { Label(String(localized: "tab.compose"), systemImage: "square.and.pencil") }
                    .tag(AppTab.compose)
                SettingsView()
                    .tabItem { Label(String(localized: "tab.settings"), systemImage: "gearshape.fill") }
                    .tag(AppTab.settings)
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

#Preview {
    ContentView()
}
