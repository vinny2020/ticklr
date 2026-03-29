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
                            // Changed to "Select" and used the 'person.badge.plus' icon
                            Label("Select iPhone Contacts", systemImage: "person.badge.plus")
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
                        // Changed to "Add" and used 'doc.badge.plus' icon
                        Label("Add LinkedIn Connections", systemImage: "doc.badge.plus")
                    }
                    .disabled(isImporting)
                } footer: {
                    // Reinforced the "local" value prop for the reviewer here
                    Text("Your connections are processed locally on your device. LinkedIn usually emails your download link within 10–30 minutes.")
                }

                Section("How to get your LinkedIn CSV") {
                    VStack(alignment: .leading, spacing: 10) {
                        LinkedInStep(number: "1", text: "Open linkedin.com in Safari (works on iPhone)")
                        LinkedInStep(number: "2", text: "Tap your photo → Settings & Privacy")
                        LinkedInStep(number: "3", text: "Data Privacy → Get a copy of your data")
                        LinkedInStep(number: "4", text: "Select Connections only → Request archive")
                        LinkedInStep(number: "5", text: "Wait for LinkedIn's email (10–30 min)")
                        LinkedInStep(number: "6", text: "Download the zip → open in Files app → tap to unzip")
                        // Updated Step 7 to match the new button label
                        LinkedInStep(number: "7", text: "Come back here and tap Add LinkedIn Connections")
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Build Your Local Network")
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
            Text(text)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
