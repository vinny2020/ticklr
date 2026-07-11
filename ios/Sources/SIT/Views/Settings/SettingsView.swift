import SwiftUI
import SwiftData
import UserNotifications

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = true
    @AppStorage("tickleNotificationsEnabled") private var notificationsEnabled = true
    @AppStorage("defaultTickleFrequency") private var defaultFrequencyRaw = TickleFrequency.monthly.rawValue

    @Query private var contacts: [Contact]

    @State private var systemAuthStatus: UNAuthorizationStatus = .notDetermined
    @State private var seedMessage: String? = nil
    /// Import result toast (TIC-85) — surfaced here after the pushed
    /// "Import Contacts" screen pops back on a successful import.
    @State private var importToastMessage: String?
    #if DEBUG
    @State private var showClearConfirm = false
    #endif

    @Environment(\.horizontalSizeClass) private var hSize
    @Environment(\.scenePhase) private var scenePhase

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    private var readableSidePadding: CGFloat {
        hSize == .regular ? 156 : 0
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    private var defaultFrequency: Binding<TickleFrequency> {
        Binding(
            get: { TickleFrequency(rawValue: defaultFrequencyRaw) ?? .monthly },
            set: { defaultFrequencyRaw = $0.rawValue }
        )
    }

    var body: some View {
        NavigationStack {
            List {
                // MARK: Inline warm title (matches Groups / Tickle / Network / Compose)
                Section {
                    Text(String(localized: "settings.navTitle"))
                        .font(WarmHeadingFont.font(size: 32, warmth: warmth))
                        .tracking(WarmHeadingFont.tracking(warmth: warmth))
                        .foregroundStyle(palette.ink)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 4, trailing: 16))
                }

                // MARK: — Data
                Section {
                    LabeledContent(String(localized: "settings.row.contacts"), value: contacts.count.formatted())
                    NavigationLink(String(localized: "settings.row.importContacts")) {
                        ImportView(onImportFinished: { importToastMessage = $0 })
                    }
                    NavigationLink(String(localized: "settings.row.messageTemplates")) {
                        TemplateListView()
                    }
                } header: {
                    warmHeader(String(localized: "settings.section.data"))
                }

                // MARK: — Notifications
                Section {
                    if systemAuthStatus == .denied {
                        LabeledContent(String(localized: "settings.row.tickleReminders")) {
                            Button(String(localized: "settings.button.enableInSettings")) {
                                if let url = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }
                            .font(.subheadline)
                        }
                    } else {
                        Toggle(String(localized: "settings.row.tickleReminders"), isOn: $notificationsEnabled)
                            .onChange(of: notificationsEnabled) { _, enabled in
                                handleNotificationToggle(enabled: enabled)
                            }
                    }
                } header: {
                    warmHeader(String(localized: "settings.section.notifications"))
                } footer: {
                    if systemAuthStatus == .denied {
                        Text(String(localized: "settings.footer.notificationsDenied"))
                            .foregroundStyle(palette.ink3)
                    } else if !notificationsEnabled {
                        Text(String(localized: "settings.footer.notificationsOff"))
                            .foregroundStyle(palette.ink3)
                    }
                }

                // MARK: — Tickle Defaults
                Section {
                    Picker(String(localized: "settings.row.defaultFrequency"), selection: defaultFrequency) {
                        ForEach(TickleFrequency.allCases.filter { $0 != .custom && $0 != .oneTime }, id: \.self) { freq in
                            Text(freq.localizedName).tag(freq)
                        }
                    }
                } header: {
                    warmHeader(String(localized: "settings.section.tickleDefaults"))
                }

                // MARK: — About
                Section {
                    WordmarkLockup()
                        .frame(maxWidth: .infinity)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets())
                    LabeledContent(String(localized: "settings.row.version"), value: appVersion)
                    LabeledContent(String(localized: "settings.row.builtBy"), value: "Xaymaca")
                } header: {
                    warmHeader(String(localized: "settings.section.about"))
                }

                // MARK: — Reset
                Section {
                    Button(String(localized: "settings.button.resetOnboarding"), role: .destructive) {
                        hasCompletedOnboarding = false
                    }
                }

                // MARK: — Debug (compiled out in Release)
                #if DEBUG
                Section {
                    NavigationLink("Warm Gallery") {
                        WarmGalleryView()
                    }

                    Button("Reset Onboarding") {
                        hasCompletedOnboarding = false
                        seedMessage = "Onboarding reset — relaunch app"
                    }

                    Button("Load Test Contacts") {
                        Task { @MainActor in
                            do {
                                let result = try SeedDataService.seedTestContacts(context: modelContext)
                                switch (result.imported, result.skipped) {
                                case (_, 0):
                                    seedMessage = "Loaded \(result.imported) test contacts ✓"
                                case (0, _):
                                    seedMessage = "All \(result.skipped) contacts already exist"
                                default:
                                    seedMessage = "Loaded \(result.imported) new, skipped \(result.skipped) duplicates"
                                }
                            } catch {
                                seedMessage = "Seed failed: \(error.localizedDescription)"
                            }
                        }
                    }
                    .foregroundColor(WarmCategory.milestones.palette.accent)

                    Button("Load Demo Data") {
                        Task { @MainActor in
                            do {
                                seedMessage = try SeedDataService.seedDemoData(context: modelContext)
                            } catch {
                                seedMessage = "Demo seed failed: \(error.localizedDescription)"
                            }
                        }
                    }
                    .foregroundColor(WarmCategory.family.palette.accent)

                    Button("Clear All Data", role: .destructive) {
                        showClearConfirm = true
                    }

                    if let msg = seedMessage {
                        Text(msg)
                            .font(.caption)
                            .foregroundColor(palette.ink2)
                    }
                } header: {
                    warmHeader("Debug")
                } footer: {
                    Text("Debug only — not visible in release builds.")
                        .foregroundStyle(palette.ink3)
                }
                .confirmationDialog(
                    "Clear all data?",
                    isPresented: $showClearConfirm,
                    titleVisibility: .visible
                ) {
                    Button("Delete All Data", role: .destructive) {
                        Task { @MainActor in
                            let contacts = (try? modelContext.fetch(FetchDescriptor<Contact>())) ?? []
                            for contact in contacts { modelContext.delete(contact) }
                            let groups = (try? modelContext.fetch(FetchDescriptor<ContactGroup>())) ?? []
                            for group in groups { modelContext.delete(group) }
                            let tickles = (try? modelContext.fetch(FetchDescriptor<TickleReminder>())) ?? []
                            for tickle in tickles { modelContext.delete(tickle) }
                            PhotoStore.deleteAll()
                            ContactPhotoFetcher.clearCache()
                            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
                            try? modelContext.save()
                            seedMessage = "All data cleared ✓"
                        }
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("This will permanently delete all contacts, groups, and tickles. This cannot be undone.")
                }
                #endif
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(palette.paper.ignoresSafeArea())
            .contentMargins(.horizontal, readableSidePadding, for: .scrollContent)
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .task { await fetchNotificationStatus() }
            // Returning from the system Settings app (e.g. after toggling
            // notification permission) doesn't re-run `.task`, so re-read the
            // authorization status when the scene becomes active again.
            .onChange(of: scenePhase) { _, phase in
                if phase == .active {
                    Task { await fetchNotificationStatus() }
                }
            }
            .saveConfirmationToast(message: $importToastMessage, warmth: warmth)
        }
    }

    // MARK: — Helpers

    @ViewBuilder
    private func warmHeader(_ text: String) -> some View {
        WarmEyebrow(text: text, warmth: warmth)
            .textCase(nil)
            .padding(.bottom, 4)
    }

    private func fetchNotificationStatus() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        systemAuthStatus = settings.authorizationStatus
    }

    private func handleNotificationToggle(enabled: Bool) {
        if enabled {
            Task {
                let center = UNUserNotificationCenter.current()
                let granted = (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
                await fetchNotificationStatus()
                if granted {
                    let all = (try? modelContext.fetch(FetchDescriptor<TickleReminder>())) ?? []
                    for reminder in all where reminder.status == .active || reminder.status == .snoozed {
                        TickleScheduler.scheduleNotification(for: reminder)
                    }
                } else {
                    notificationsEnabled = false
                }
            }
        } else {
            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        }
    }
}
