import SwiftUI
import SwiftData

struct TemplateEditView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    var template: MessageTemplate?

    @State private var title = ""
    @State private var body_ = ""

    private var isEditing: Bool { template != nil }

    var body: some View {
        NavigationStack {
            Form {
                Section("Title") {
                    TextField("e.g. Checking in", text: $title)
                }
                Section("Message") {
                    TextEditor(text: $body_)
                        .frame(minHeight: 120)
                }
            }
            .navigationTitle(isEditing ? "Edit Template" : "New Template")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .onAppear {
                if let template {
                    title = template.title
                    body_ = template.body
                }
            }
        }
    }

    private func save() {
        let trimmedTitle = title.trimmingCharacters(in: .whitespaces)
        let trimmedBody = body_.trimmingCharacters(in: .whitespaces)
        if let template {
            template.title = trimmedTitle
            template.body = trimmedBody
        } else {
            let newTemplate = MessageTemplate(title: trimmedTitle, body: trimmedBody)
            modelContext.insert(newTemplate)
        }
        dismiss()
    }
}
