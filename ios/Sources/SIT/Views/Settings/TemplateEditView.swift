import SwiftUI
import SwiftData

struct TemplateEditView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    var template: MessageTemplate?
    /// Runs right before the editor dismisses on a successful save, carrying
    /// the localized save-confirmation text (TIC-84). Save now applies and the
    /// sheet dismisses immediately — `TemplateListView`, which remains on
    /// screen afterward, shows the confirmation toast instead of this sheet.
    var onSaved: ((String) -> Void)? = nil

    @State private var title = ""
    @State private var body_ = ""
    @State private var isSaving = false

    private var isEditing: Bool { template != nil }

    /// Save is blocked while a title is empty or a save is already in flight —
    /// without the in-flight guard a double-tap inserts a duplicate template
    /// (same fix as TickleEditView).
    private var canSave: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty && !isSaving
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "templateEdit.section.title")) {
                    TextField(String(localized: "templateEdit.placeholder.title"), text: $title)
                }
                Section(String(localized: "templateEdit.section.message")) {
                    TextEditor(text: $body_)
                        .frame(minHeight: 120)
                }
            }
            .navigationTitle(isEditing ? String(localized: "templateEdit.navTitle.edit") : String(localized: "templateEdit.navTitle.new"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "common.save")) { save() }
                        .disabled(!canSave)
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
        // Flip BEFORE any work so the next tap sees canSave == false — the
        // sheet dismisses immediately below, but stays disabled for whatever
        // sliver of the dismiss animation it's still on screen for, so a
        // rapid double-tap can't insert a duplicate template.
        isSaving = true
        let trimmedTitle = title.trimmingCharacters(in: .whitespaces)
        let trimmedBody = body_.trimmingCharacters(in: .whitespaces)
        if let template {
            template.title = trimmedTitle
            template.body = trimmedBody
        } else {
            let newTemplate = MessageTemplate(title: trimmedTitle, body: trimmedBody)
            modelContext.insert(newTemplate)
        }
        // TIC-84: apply and dismiss immediately — no artificial delay. The
        // confirmation toast is handed to the presenter (TemplateListView),
        // which remains on screen after this sheet is gone.
        let message = isEditing
            ? String(localized: "templateEdit.toast.updated")
            : String(localized: "templateEdit.toast.saved")
        onSaved?(message)
        dismiss()
    }
}
