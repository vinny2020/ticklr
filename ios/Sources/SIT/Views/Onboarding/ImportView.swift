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
                            Label("Import from iPhone Contacts", systemImage: "apple.logo")
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
                        Label("Import from LinkedIn CSV", systemImage: "briefcase.fill")
                    }
                    .disabled(isImporting)
                } footer: {
                    Text("LinkedIn: go to linkedin.com → Settings → Data Privacy → Get a copy of your data → Connections.csv")
                }
            }
            .navigationTitle("Import Contacts")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
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
            .alert("Import Error", isPresented: $showingError) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(importError ?? "Unknown error")
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
