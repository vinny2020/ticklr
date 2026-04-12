import SwiftUI
import SwiftData
import UniformTypeIdentifiers

struct ImportView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false

    @State private var isImporting = false
    @State private var showingDocumentPicker = false
    @State private var importError: String?
    @State private var showingError = false

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
        }
    }

    @MainActor
    private func importFromiOS() async {
        isImporting = true
        defer { isImporting = false }
        do {
            try await ContactImportService.importFromiOS(context: modelContext)
            hasCompletedOnboarding = true
            dismiss()
        } catch {
            importError = error.localizedDescription
            showingError = true
        }
    }

    private func importFromLinkedIn(url: URL) {

        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }
        do {
            try LinkedInCSVParser.parse(url: url, context: modelContext)
            hasCompletedOnboarding = true
            dismiss()
        } catch {
            importError = error.localizedDescription
            showingError = true
        }
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
