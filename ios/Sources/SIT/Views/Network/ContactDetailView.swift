import SwiftUI
import SwiftData
import PhotosUI

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
    @State private var pickerSelection: PhotosPickerItem? = nil
    @State private var photoVersion = UUID()
    @State private var photoSaveError: String? = nil

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }
    private var category: WarmCategory { WarmCategory.resolve(for: contact) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                photoHeader
                    .frame(maxWidth: .infinity)
                    .padding(.top, 8)

                actionChipRow
                    .padding(.horizontal, WarmSpacing.lg)

                detailCards

                if !contact.groups.isEmpty {
                    groupsSection
                }

                hostCard

                deleteButton
                    .padding(.horizontal, WarmSpacing.lg)
                    .padding(.top, 12)
            }
            .padding(.bottom, 32)
        }
        .background(palette.paper.ignoresSafeArea())
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(String(localized: "common.edit")) { activeSheet = .edit }
                    .foregroundStyle(palette.ink)
            }
        }
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .edit:        ContactEditSheet(contact: contact)
            case .addTickle:   TickleEditView(contact: contact)
            case .addToGroup:  AddToGroupSheet(contact: contact)
            case .compose:
                ComposeView(onCancel: { activeSheet = nil },
                            initialContact: contact)
            }
        }
        .confirmationDialog(
            String(localized: "contactDetail.deleteConfirm.title \(contact.fullName)"),
            isPresented: $showingDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button(String(localized: "contactDetail.button.deleteContact"), role: .destructive) {
                PhotoStore.delete(for: contact.id)
                modelContext.delete(contact)
                try? modelContext.save()
                dismiss()
            }
        } message: {
            Text(String(localized: "common.cannotUndo"))
        }
        .onChange(of: pickerSelection) { _, item in
            guard let item else { return }
            Task { await attachPhoto(item) }
        }
    }

    // MARK: - Photo header

    private var photoHeader: some View {
        VStack(spacing: 14) {
            ContactPhotoView(contact: contact, category: category, style: .detail)
                .id(photoVersion)

            PhotosPicker(selection: $pickerSelection,
                         matching: .images,
                         photoLibrary: .shared()) {
                HStack(spacing: 6) {
                    Image(systemName: "plus")
                    Text(String(localized: "warm.contact.addPhoto",
                                defaultValue: "Add a photo"))
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(category.palette.accent)
            }
            .buttonStyle(.plain)

            if let err = photoSaveError {
                Text(err)
                    .font(.caption2)
                    .foregroundStyle(.red)
            }

            VStack(spacing: 4) {
                Text(contact.fullName)
                    .font(WarmHeadingFont.font(size: 28, warmth: warmth))
                    .tracking(WarmHeadingFont.tracking(warmth: warmth))
                    .foregroundStyle(palette.ink)
                    .multilineTextAlignment(.center)
                if !contact.company.isEmpty {
                    Text(contact.company)
                        .font(.system(size: 14))
                        .foregroundStyle(palette.ink2)
                }
                if !contact.jobTitle.isEmpty {
                    Text(contact.jobTitle)
                        .font(.system(size: 12))
                        .foregroundStyle(palette.ink3)
                }
            }
            .padding(.top, 6)
            .padding(.horizontal, WarmSpacing.lg)
        }
    }

    // MARK: - Action chips

    private var actionChipRow: some View {
        let canText = !contact.phoneNumbers.isEmpty
        let canCall = !contact.phoneNumbers.isEmpty
        let canEmail = !contact.emails.isEmpty
        return HStack(spacing: 8) {
            actionChip(
                title: String(localized: "warm.contact.sendTickle", defaultValue: "Send a text"),
                systemImage: "message.fill",
                style: .filled,
                isEnabled: canText
            ) { activeSheet = .compose }

            actionChip(
                title: String(localized: "warm.contact.createTickle", defaultValue: "Create a tickle"),
                systemImage: "bell.fill",
                style: .outline,
                isEnabled: true
            ) { activeSheet = .addTickle }

            actionChip(
                title: String(localized: "warm.contact.call", defaultValue: "Call"),
                systemImage: "phone.fill",
                style: .outline,
                isEnabled: canCall
            ) {
                if let phone = contact.phoneNumbers.first,
                   let url = URL(string: "tel:\(phone.filter { $0.isNumber || $0 == "+" })") {
                    UIApplication.shared.open(url)
                }
            }

            actionChip(
                title: String(localized: "warm.contact.email", defaultValue: "Email"),
                systemImage: "envelope.fill",
                style: .outline,
                isEnabled: canEmail
            ) {
                if let email = contact.emails.first,
                   let url = URL(string: "mailto:\(email)") {
                    UIApplication.shared.open(url)
                }
            }
        }
    }

    private enum ActionChipStyle { case filled, outline }

    private func actionChip(
        title: String,
        systemImage: String,
        style: ActionChipStyle,
        isEnabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.system(size: 16, weight: .semibold))
                Text(title)
                    .font(.system(size: 12, weight: .semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(background(for: style, enabled: isEnabled))
            .foregroundStyle(foreground(for: style, enabled: isEnabled))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(border(for: style, enabled: isEnabled), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1 : 0.5)
    }

    private func background(for style: ActionChipStyle, enabled: Bool) -> Color {
        switch style {
        case .filled:  category.palette.accent
        case .outline: palette.cardBg
        }
    }
    private func foreground(for style: ActionChipStyle, enabled: Bool) -> Color {
        switch style {
        case .filled:  Color(red: 0.98, green: 0.96, blue: 0.89)
        case .outline: category.palette.accent
        }
    }
    private func border(for style: ActionChipStyle, enabled: Bool) -> Color {
        switch style {
        case .filled:  Color.clear
        case .outline: palette.cardBorder
        }
    }

    // MARK: - Detail cards

    @ViewBuilder
    private var detailCards: some View {
        VStack(alignment: .leading, spacing: 10) {
            if !contact.phoneNumbers.isEmpty {
                detailSection(title: String(localized: "contact.section.phone")) {
                    ForEach(contact.phoneNumbers, id: \.self) { phone in
                        let cleaned = phone.filter { $0.isNumber || $0 == "+" }
                        if let url = URL(string: "tel:\(cleaned)"), !cleaned.isEmpty {
                            Link(phone, destination: url)
                                .foregroundStyle(category.palette.accent)
                        } else {
                            Text(phone).foregroundStyle(palette.ink)
                        }
                    }
                }
            }

            if !contact.emails.isEmpty {
                detailSection(title: String(localized: "contact.section.email")) {
                    ForEach(contact.emails, id: \.self) { email in
                        if let url = URL(string: "mailto:\(email)") {
                            Link(email, destination: url)
                                .foregroundStyle(category.palette.accent)
                        } else {
                            Text(email).foregroundStyle(palette.ink)
                        }
                    }
                }
            }

            if let lastConnected = contact.lastContactedAt {
                detailSection(title: String(localized: "warm.contact.lastConnected",
                                            defaultValue: "Last connected")) {
                    Text(lastConnected.formatted(.relative(presentation: .named)))
                        .foregroundStyle(palette.ink2)
                }
            }

            if !contact.notes.isEmpty {
                detailSection(title: String(localized: "contact.section.notes")) {
                    Text(contact.notes).foregroundStyle(palette.ink2)
                }
            }

            if !contact.tags.isEmpty {
                detailSection(title: String(localized: "contact.section.tags")) {
                    Text(contact.tags.joined(separator: " · "))
                        .foregroundStyle(palette.ink2)
                }
            }
        }
        .padding(.horizontal, WarmSpacing.lg)
    }

    @ViewBuilder
    private func detailSection<Content: View>(title: String,
                                              @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            WarmEyebrow(text: title, warmth: warmth)
            VStack(alignment: .leading, spacing: 4) {
                content()
            }
            .padding(WarmSpacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(palette.cardBg)
            .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                    .stroke(palette.cardBorder, lineWidth: 1)
            )
        }
    }

    // MARK: - Groups section

    private var groupsSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            WarmEyebrow(text: String(localized: "contact.section.groups"), warmth: warmth)
            WarmListContainer(warmth: warmth) {
                ForEach(Array(contact.groups.enumerated()), id: \.element.persistentModelID) { idx, group in
                    HStack(spacing: 12) {
                        Text(group.emoji)
                            .font(.system(size: 18))
                            .frame(width: 36, height: 36)
                            .background(palette.paperSurface)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        Text(group.name)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(palette.ink)
                        Spacer()
                    }
                    .padding(.horizontal, WarmSpacing.lg)
                    .padding(.vertical, WarmSpacing.md)
                    if idx < contact.groups.count - 1 {
                        WarmRowDivider(warmth: warmth)
                    }
                }
            }

            Button {
                activeSheet = .addToGroup
            } label: {
                Label(String(localized: "contactDetail.button.addToGroup"),
                      systemImage: "person.3.fill")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(category.palette.accent)
                    .padding(.top, 4)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, WarmSpacing.lg)
    }

    // MARK: - Featured host card

    private var hostCard: some View {
        WarmCard(category: category, variant: .hero, warmth: warmth, showPrompt: true)
            .padding(.horizontal, WarmSpacing.lg)
            .padding(.top, 6)
    }

    private var deleteButton: some View {
        Button(role: .destructive) {
            showingDeleteConfirm = true
        } label: {
            Label(String(localized: "contactDetail.button.deleteContact"),
                  systemImage: "trash")
                .font(.system(size: 14, weight: .semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .foregroundStyle(.red)
    }

    // MARK: - Photo save

    @MainActor
    private func attachPhoto(_ item: PhotosPickerItem) async {
        do {
            guard let data = try await item.loadTransferable(type: Data.self),
                  let image = UIImage(data: data) else {
                photoSaveError = "Couldn't decode the selected image."
                return
            }
            try PhotoStore.save(image, for: contact.id)
            ContactPhotoFetcher.clearCache()
            photoVersion = UUID()
            photoSaveError = nil
            pickerSelection = nil
        } catch {
            photoSaveError = "Save failed: \(error.localizedDescription)"
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
