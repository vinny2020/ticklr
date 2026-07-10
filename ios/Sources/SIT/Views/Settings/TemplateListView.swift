import SwiftUI
import SwiftData

struct TemplateListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \MessageTemplate.title) private var templates: [MessageTemplate]

    @State private var showingAdd = false
    @State private var editingTemplate: MessageTemplate?
    /// Non-nil while a save-confirmation toast is showing after a
    /// TemplateEditView sheet dismissed (TIC-84).
    @State private var saveToastMessage: String?

    var body: some View {
        List {
            ForEach(templates) { template in
                Button {
                    editingTemplate = template
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(template.title)
                            .font(.body)
                            .foregroundStyle(.primary)
                        Text(template.body)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                    .padding(.vertical, 2)
                }
            }
            .onDelete(perform: deleteTemplates)
        }
        .navigationTitle(String(localized: "templateList.navTitle"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showingAdd = true
                } label: {
                    Image(systemName: "plus")
                }
            }
            ToolbarItem(placement: .topBarLeading) {
                EditButton()
            }
        }
        .sheet(isPresented: $showingAdd) {
            TemplateEditView(onSaved: { saveToastMessage = $0 })
        }
        .sheet(item: $editingTemplate) { template in
            TemplateEditView(template: template, onSaved: { saveToastMessage = $0 })
        }
        .saveConfirmationToast(message: $saveToastMessage)
    }

    private func deleteTemplates(at offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(templates[index])
        }
    }
}
