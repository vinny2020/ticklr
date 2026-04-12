import SwiftUI
import SwiftData

struct AddContactView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \ContactGroup.name) private var groups: [ContactGroup]

    @State private var firstName = ""
    @State private var lastName = ""
    @State private var company = ""
    @State private var jobTitle = ""
    @State private var notes = ""
    @State private var phoneNumbers: [String] = []
    @State private var emails: [String] = []
    @State private var tags: [String] = []
    @State private var selectedGroupIDs: Set<UUID> = []

    @State private var newPhone = ""
    @State private var newEmail = ""
    @State private var newTag = ""

    private var canSave: Bool {
        !firstName.trimmingCharacters(in: .whitespaces).isEmpty ||
        !lastName.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "contact.section.name")) {
                    TextField(String(localized: "contact.placeholder.firstName"), text: $firstName)
                    TextField(String(localized: "contact.placeholder.lastName"), text: $lastName)
                }

                Section(String(localized: "contact.section.work")) {
                    TextField(String(localized: "contact.placeholder.company"), text: $company)
                    TextField(String(localized: "contact.placeholder.jobTitle"), text: $jobTitle)
                }

                Section(String(localized: "contact.section.phone")) {
                    ForEach(phoneNumbers.indices, id: \.self) { i in
                        TextField(String(localized: "contact.placeholder.phoneNumber"), text: $phoneNumbers[i])
                            .keyboardType(.phonePad)
                    }
                    .onDelete { phoneNumbers.remove(atOffsets: $0) }
                    HStack {
                        TextField(String(localized: "contact.placeholder.addPhone"), text: $newPhone).keyboardType(.phonePad)
                        Button(String(localized: "common.add")) {
                            let v = newPhone.trimmingCharacters(in: .whitespaces)
                            guard !v.isEmpty else { return }
                            phoneNumbers.append(v)
                            newPhone = ""
                        }
                        .disabled(newPhone.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                }

                Section(String(localized: "contact.section.email")) {
                    ForEach(emails.indices, id: \.self) { i in
                        TextField(String(localized: "contact.placeholder.emailAddress"), text: $emails[i])
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                    }
                    .onDelete { emails.remove(atOffsets: $0) }
                    HStack {
                        TextField(String(localized: "contact.placeholder.addEmail"), text: $newEmail)
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                        Button(String(localized: "common.add")) {
                            let v = newEmail.trimmingCharacters(in: .whitespaces)
                            guard !v.isEmpty else { return }
                            emails.append(v)
                            newEmail = ""
                        }
                        .disabled(newEmail.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                }

                Section(String(localized: "contact.section.notes")) {
                    TextEditor(text: $notes)
                        .frame(minHeight: 80)
                }

                Section(String(localized: "contact.section.tags")) {
                    ForEach(tags, id: \.self) { tag in Text(tag) }
                        .onDelete { tags.remove(atOffsets: $0) }
                    HStack {
                        TextField(String(localized: "contact.placeholder.addTag"), text: $newTag)
                        Button(String(localized: "common.add")) {
                            let v = newTag.trimmingCharacters(in: .whitespaces)
                            guard !v.isEmpty else { return }
                            tags.append(v)
                            newTag = ""
                        }
                        .disabled(newTag.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                }

                if !groups.isEmpty {
                    Section(String(localized: "contact.section.groups")) {
                        ForEach(groups) { group in
                            Button {
                                if selectedGroupIDs.contains(group.id) {
                                    selectedGroupIDs.remove(group.id)
                                } else {
                                    selectedGroupIDs.insert(group.id)
                                }
                            } label: {
                                HStack {
                                    Text(group.emoji)
                                    Text(group.name)
                                    Spacer()
                                    if selectedGroupIDs.contains(group.id) {
                                        Image(systemName: "checkmark").foregroundStyle(.indigo)
                                    }
                                }
                            }
                            .tint(.primary)
                        }
                    }
                }
            }
            .navigationTitle(String(localized: "addContact.navTitle"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "common.save")) { save() }
                        .disabled(!canSave)
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private func save() {
        let contact = Contact(
            firstName: firstName.trimmingCharacters(in: .whitespaces),
            lastName: lastName.trimmingCharacters(in: .whitespaces),
            phoneNumbers: phoneNumbers.filter { !$0.isEmpty },
            emails: emails.filter { !$0.isEmpty },
            company: company.trimmingCharacters(in: .whitespaces),
            jobTitle: jobTitle.trimmingCharacters(in: .whitespaces),
            notes: notes.trimmingCharacters(in: .whitespaces),
            tags: tags,
            importSource: .manual
        )
        contact.groups = groups.filter { selectedGroupIDs.contains($0.id) }
        modelContext.insert(contact)
        try? modelContext.save()
        dismiss()
    }
}
