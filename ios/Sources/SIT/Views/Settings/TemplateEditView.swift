import SwiftUI
import SwiftData

struct TemplateEditView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    var template: MessageTemplate?

    @State private var title = ""
    @State private var body_ = ""
    @State private var showToast = false
    @State private var toastMessage = ""

    private var isEditing: Bool { template != nil }

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
                        .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .onAppear {
                if let template {
                    title = template.title
                    body_ = template.body
                }
            }
            .overlay(alignment: .bottom) {
                if showToast {
                    Text(verbatim: toastMessage)
                        .font(.subheadline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(red: 0.145, green: 0.388, blue: 0.922))
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .shadow(radius: 4)
                        .padding(.bottom, 16)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: showToast)
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
        toastMessage = isEditing
            ? String(localized: "templateEdit.toast.updated")
            : String(localized: "templateEdit.toast.saved")
        showToast = true
        Task {
            try? await Task.sleep(for: .seconds(2))
            dismiss()
        }
    }
}
