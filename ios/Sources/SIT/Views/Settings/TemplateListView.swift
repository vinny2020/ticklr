import SwiftUI
import SwiftData

struct TemplateListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \MessageTemplate.title) private var templates: [MessageTemplate]
    @AppStorage("hasSeededDefaultTemplates") private var hasSeededDefaultTemplates = false

    @State private var showingAdd = false
    @State private var editingTemplate: MessageTemplate?

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
        .navigationTitle("Message Templates")
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
            TemplateEditView()
        }
        .sheet(item: $editingTemplate) { template in
            TemplateEditView(template: template)
        }
        .onAppear(perform: seedDefaultIfNeeded)
    }

    private func deleteTemplates(at offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(templates[index])
        }
    }

    private func seedDefaultIfNeeded() {
        guard !hasSeededDefaultTemplates else { return }
        let starter = MessageTemplate(
            title: "Checking in",
            body: "Hey, just wanted to check in! Hope you're doing well 😊"
        )
        modelContext.insert(starter)
        hasSeededDefaultTemplates = true
    }
}
