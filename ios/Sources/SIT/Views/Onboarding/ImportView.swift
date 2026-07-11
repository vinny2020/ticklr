import SwiftUI
import SwiftData
import UniformTypeIdentifiers
import Contacts

struct ImportView: View {
    /// Where this import is happening from (TIC-85). `.onboarding` is only
    /// used from `OnboardingView`'s sheet: a success both completes onboarding
    /// and requests the one-shot Network-tab landing (with the result toast
    /// attached). `.recurring` covers every re-entry — Settings → Import
    /// Contacts, Network "+" → Get started — where the presenting screen
    /// stays on screen and shows the toast itself via `onImportFinished`.
    enum Context {
        case onboarding
        case recurring
    }

    var context: Context = .recurring
    /// Called with the result message on a successful `.recurring` import,
    /// right before `dismiss()` — mirrors `TickleEditView`'s `onSaved` pattern.
    /// Unused in `.onboarding` context (that path posts through
    /// `PostOnboardingLanding` instead, since the presenter — `OnboardingView`
    /// — is about to be torn down, not left on screen).
    var onImportFinished: ((String) -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false

    @State private var isImporting = false
    @State private var showingDocumentPicker = false
    @State private var importError: String?
    @State private var showingError = false
    /// Drives the system multi-select contact picker (TIC-93) — the
    /// alongside-the-bulk-import "Choose contacts…" affordance.
    @State private var showingContactPicker = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button {
                        Task { await importFromiOS() }
                    } label: {
                        HStack {
                            Label(String(localized: "import.button.iphoneContacts"), systemImage: "person.badge.plus")
                            Spacer()
                            if isImporting {
                                ProgressView()
                            }
                        }
                    }
                    .disabled(isImporting)

                    // TIC-93: alongside the all-at-once sweep above, let the
                    // user hand-pick specific contacts via the system picker.
                    Button {
                        showingContactPicker = true
                    } label: {
                        Label(String(localized: "import.button.chooseContacts"), systemImage: "checklist")
                    }
                    .disabled(isImporting)

                    Button {
                        showingDocumentPicker = true
                    } label: {
                        Label(String(localized: "import.button.linkedinConnections"), systemImage: "doc.badge.plus")
                    }
                    .disabled(isImporting)
                } footer: {
                    Text(String(localized: "import.footer.description"))
                }

                Section(String(localized: "import.section.linkedinGuide")) {
                    VStack(alignment: .leading, spacing: 10) {
                        LinkedInStep(number: "1", text: String(localized: "import.step.1"))
                        LinkedInStep(number: "2", text: String(localized: "import.step.2"))
                        LinkedInStep(number: "3", text: String(localized: "import.step.3"))
                        LinkedInStep(number: "4", text: String(localized: "import.step.4"))
                        LinkedInStep(number: "5", text: String(localized: "import.step.5"))
                        LinkedInStep(number: "6", text: String(localized: "import.step.6"))
                        LinkedInStep(number: "7", text: String(localized: "import.step.7"))
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle(String(localized: "import.navTitle"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "common.done")) { dismiss() }
                }
            }
            .fileImporter(
                isPresented: $showingDocumentPicker,
                allowedContentTypes: [.commaSeparatedText, .text],
                allowsMultipleSelection: false
            ) { result in
                switch result {
                case .success(let urls):
                    guard let url = urls.first else { return }
                    importFromLinkedIn(url: url)
                case .failure(let error):
                    importError = error.localizedDescription
                    showingError = true
                }
            }
            .alert(String(localized: "import.alert.error.title"), isPresented: $showingError) {
                Button(String(localized: "common.ok"), role: .cancel) {}
            } message: {
                Text(verbatim: importError ?? "")
            }
            .sheet(isPresented: $showingContactPicker) {
                ContactPickerRepresentable(isPresented: $showingContactPicker) { contacts in
                    importSelected(contacts)
                }
                .ignoresSafeArea()
            }
        }
    }

    @MainActor
    private func importFromiOS() async {
        isImporting = true
        defer { isImporting = false }
        do {
            let result = try await ContactImportService.importFromiOS(context: modelContext)
            finishImport(imported: result.imported, skipped: result.skipped)
        } catch {
            importError = error.localizedDescription
            showingError = true
        }
    }

    /// Routes the picker's selection through the same dedup/insert core the
    /// bulk import uses (TIC-93): `CNContact` → `ImportedFields` (shared
    /// mapping) → `applyImport`. Synchronous — a hand-picked selection is a
    /// handful of contacts, not the whole address book, so there's no need
    /// for the bulk path's off-main-actor enumeration.
    @MainActor
    private func importSelected(_ contacts: [CNContact]) {
        guard !contacts.isEmpty else { return }
        let fields = contacts.map(ContactImportService.importedFields(from:))
        let result = ContactImportService.applyImport(fields: fields, context: modelContext)
        try? modelContext.save()
        finishImport(imported: result.imported, skipped: result.skipped)
    }

    private func importFromLinkedIn(url: URL) {
        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }
        do {
            let result = try LinkedInCSVParser.parse(url: url, context: modelContext)
            finishImport(imported: result.imported, skipped: result.skipped)
        } catch {
            importError = error.localizedDescription
            showingError = true
        }
    }

    /// Shared success tail for both import sources (TIC-85): builds the
    /// result toast once, then routes it per `context` — see the `Context`
    /// doc comment above for why onboarding and recurring diverge here.
    @MainActor
    private func finishImport(imported: Int, skipped: Int) {
        let message = ImportFeedback.message(imported: imported, skipped: skipped)
        switch context {
        case .onboarding:
            hasCompletedOnboarding = true
            PostOnboardingLanding.requestNetworkLanding(toast: message)
        case .recurring:
            onImportFinished?(message)
        }
        dismiss()
    }
}

private struct LinkedInStep: View {
    let number: String
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Text(number)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(.white)
                .frame(width: 20, height: 20)
                .background(Color.indigo)
                .clipShape(Circle())
            Text(verbatim: text)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
