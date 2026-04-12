import SwiftUI
import SwiftData

private enum ActiveSheet: Identifiable {
    case edit, addTickle, addToGroup, compose
    var id: Int { hashValue }
}

struct ContactDetailView: View {
    @Bindable var contact: Contact
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @State private var activeSheet: ActiveSheet?
    @State private var showingDeleteConfirm = false

    private let amber = Color(red: 0.96, green: 0.78, blue: 0.25)
    private let cobalt = Color(red: 0.145, green: 0.388, blue: 0.922)

    var body: some View {
        List {
            Section {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        Circle()
                            .fill(Color.indigo.opacity(0.15))
                            .frame(width: 72, height: 72)
                            .overlay(
                                Text(contact.initials)
                                    .font(.system(size: 28, weight: .semibold))
                                    .foregroundStyle(.indigo)
                            )
                        Text(contact.fullName)
                            .font(.title3).fontWeight(.semibold)
                        if !contact.company.isEmpty {
                            Text(contact.company)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        if !contact.jobTitle.isEmpty {
                            Text(contact.jobTitle)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }

            if !contact.phoneNumbers.isEmpty {
                Section(String(localized: "contact.section.phone")) {
                    ForEach(contact.phoneNumbers, id: \.self) { phone in
                        let cleaned = phone.filter { $0.isNumber || $0 == "+" }
                        if let url = URL(string: "tel:\(cleaned)"), !cleaned.isEmpty {
                            Link(phone, destination: url)
                        } else {
                            Text(phone)
                        }
                    }
                }
            }

            if !contact.emails.isEmpty {
                Section(String(localized: "contact.section.email")) {
                    ForEach(contact.emails, id: \.self) { email in
                        if let url = URL(string: "mailto:\(email)") {
                            Link(email, destination: url)
                        } else {
                            Text(email)
                        }
                    }
                }
            }

            if !contact.notes.isEmpty {
                Section(String(localized: "contact.section.notes")) {
                    Text(contact.notes).foregroundStyle(.secondary)
                }
            }

            if !contact.tags.isEmpty {
                Section(String(localized: "contact.section.tags")) {
                    Text(contact.tags.joined(separator: " · "))
                        .foregroundStyle(.secondary)
                }
            }

            if !contact.groups.isEmpty {
                Section(String(localized: "contact.section.groups")) {
                    ForEach(contact.groups) { group in
                        HStack {
                            Text(group.emoji)
                            Text(group.name)
                        }
                    }
                }
            }

            Section {
                HStack(spacing: 12) {
                    Button {
                        activeSheet = .addTickle
                    } label: {
                        Label(String(localized: "contactDetail.button.addTickle"), systemImage: "bell.badge")
                            .foregroundStyle(amber)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.plain)
                    Button {
                        activeSheet = .addToGroup
                    } label: {
                        Label(String(localized: "contactDetail.button.addToGroup"), systemImage: "person.3.fill")
                            .foregroundStyle(cobalt)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.plain)
                }
                Button {
                    activeSheet = .compose
                } label: {
                    Label(String(localized: "contactDetail.button.message"), systemImage: "message.fill")
                        .foregroundStyle(contact.phoneNumbers.isEmpty ? Color.secondary : cobalt)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
                .disabled(contact.phoneNumbers.isEmpty)
            }

            Section {
                Button(role: .destructive) {
                    showingDeleteConfirm = true
                } label: {
                    Label(String(localized: "contactDetail.button.deleteContact"), systemImage: "trash")
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .navigationTitle(contact.fullName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(String(localized: "common.edit")) { activeSheet = .edit }
            }
        }
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .edit:
                ContactEditSheet(contact: contact)
            case .addTickle:
                TickleEditView(contact: contact)
            case .addToGroup:
                AddToGroupSheet(contact: contact)
            case .compose:
                ComposeView(
                    onCancel: { activeSheet = nil },
                    initialContact: contact
                )
            }
        }
        .confirmationDialog(
            String(localized: "contactDetail.deleteConfirm.title \(contact.fullName)"),
            isPresented: $showingDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button(String(localized: "contactDetail.button.deleteContact"), role: .destructive) {
                modelContext.delete(contact)
                try? modelContext.save()
                dismiss()
            }
        } message: {
            Text(String(localized: "common.cannotUndo"))
        }
    }
}

// MARK: - Add to Group Sheet

private struct AddToGroupSheet: View {
    let contact: Contact
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \ContactGroup.name) private var allGroups: [ContactGroup]

    @State private var showingCreateField = false
    @State private var newGroupName = ""

    var body: some View {
        NavigationStack {
            List {
                ForEach(allGroups) { group in
                    let isMember = contact.groups.contains(where: { $0.id == group.id })
                    Button {
                        if isMember {
                            contact.groups.removeAll { $0.id == group.id }
                        } else {
                            contact.groups.append(group)
                        }
                        try? modelContext.save()
                    } label: {
                        HStack {
                            Text(group.emoji)
                            Text(group.name)
                            Spacer()
                            if isMember {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(Color(red: 0.145, green: 0.388, blue: 0.922))
                            }
                        }
                    }
                    .tint(.primary)
                }

                if showingCreateField {
                    let isDuplicateInline = {
                        let trimmed = newGroupName.trimmingCharacters(in: .whitespaces)
                        guard !trimmed.isEmpty else { return false }
                        return allGroups.contains {
                            $0.name.caseInsensitiveCompare(trimmed) == .orderedSame
                        }
                    }()
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            TextField(String(localized: "addToGroup.placeholder.groupName"), text: $newGroupName)
                                .onChange(of: newGroupName) { _, v in
                                    if v.count > 30 { newGroupName = String(v.prefix(30)) }
                                }
                            Button(String(localized: "common.create")) {
                                let trimmed = newGroupName.trimmingCharacters(in: .whitespaces)
                                guard !trimmed.isEmpty else { return }
                                let group = ContactGroup(name: trimmed, emoji: "👥")
                                modelContext.insert(group)
                                contact.groups.append(group)
                                try? modelContext.save()
                                newGroupName = ""
                                showingCreateField = false
                            }
                            .disabled(newGroupName.trimmingCharacters(in: .whitespaces).isEmpty || isDuplicateInline)
                        }
                        HStack {
                            if isDuplicateInline {
                                Text(String(localized: "addToGroup.error.duplicateName"))
                                    .font(.caption)
                                    .foregroundStyle(.red)
                            } else {
                                Spacer()
                            }
                            Text("\(newGroupName.count) / 30")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                } else {
                    Button {
                        showingCreateField = true
                    } label: {
                        Label(String(localized: "addToGroup.button.createNew"), systemImage: "plus")
                            .foregroundStyle(Color(red: 0.145, green: 0.388, blue: 0.922))
                    }
                }
            }
            .navigationTitle(String(localized: "addToGroup.navTitle"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "common.done")) { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
    }
}

// MARK: - Edit Sheet

private struct ContactEditSheet: View {
    let contact: Contact
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \ContactGroup.name) private var allGroups: [ContactGroup]

    @State private var firstName: String
    @State private var lastName: String
    @State private var company: String
    @State private var jobTitle: String
    @State private var notes: String
    @State private var phoneNumbers: [String]
    @State private var emails: [String]
    @State private var tags: [String]
    @State private var selectedGroupIDs: Set<UUID>

    @State private var newPhone = ""
    @State private var newEmail = ""
    @State private var newTag = ""

    init(contact: Contact) {
        self.contact = contact
        _firstName = State(initialValue: contact.firstName)
        _lastName = State(initialValue: contact.lastName)
        _company = State(initialValue: contact.company)
        _jobTitle = State(initialValue: contact.jobTitle)
        _notes = State(initialValue: contact.notes)
        _phoneNumbers = State(initialValue: contact.phoneNumbers)
        _emails = State(initialValue: contact.emails)
        _tags = State(initialValue: contact.tags)
        _selectedGroupIDs = State(initialValue: Set(contact.groups.map { $0.id }))
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

                if !allGroups.isEmpty {
                    Section(String(localized: "contact.section.groups")) {
                        ForEach(allGroups) { group in
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
            .navigationTitle(String(localized: "contactEdit.navTitle"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "common.save")) { save() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private func save() {
        contact.firstName = firstName.trimmingCharacters(in: .whitespaces)
        contact.lastName = lastName.trimmingCharacters(in: .whitespaces)
        contact.company = company.trimmingCharacters(in: .whitespaces)
        contact.jobTitle = jobTitle.trimmingCharacters(in: .whitespaces)
        contact.notes = notes.trimmingCharacters(in: .whitespaces)
        contact.phoneNumbers = phoneNumbers.filter { !$0.isEmpty }
        contact.emails = emails.filter { !$0.isEmpty }
        contact.tags = tags
        contact.groups = allGroups.filter { selectedGroupIDs.contains($0.id) }
        try? modelContext.save()
        dismiss()
    }
}
