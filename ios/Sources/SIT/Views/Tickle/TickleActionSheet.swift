import SwiftUI
import SwiftData

/// Connect-action sheet shown when a tickle row is tapped (TIC-36).
/// Surfaces the contact's available channels (Compose message / Call / Email) plus
/// the secondary tickle actions (Mark done / Snooze / Edit). Compose opens the in-app
/// Compose screen (templates); Call/Email hand off to system composers. No contact
/// data leaves the device.
///
/// All actions dismiss the sheet; Compose and Edit additionally signal the parent
/// (via `onCompose` / `onEdit`) to present a follow-up sheet once this one is gone,
/// which avoids the "present while dismissing" race.
///
/// On iPad (TIC-46) the same content renders in a `NavigationSplitView` detail pane
/// rather than a modal sheet. Pass `dismissesOnAction: false` there so taps don't try
/// to dismiss a pane that was never presented — the parent drives the pane via the
/// callbacks and its own selection state instead.
struct TickleActionSheet: View {
    let reminder: TickleReminder
    var warmth: Warmth = .subtle
    /// When false (iPad detail pane), actions run their callback/URL but never call
    /// `dismiss()` — there's no sheet to dismiss. Defaults to true (phone bottom sheet).
    var dismissesOnAction: Bool = true
    let onCompose: () -> Void
    let onMarkDone: () -> Void
    /// Snooze the reminder by the chosen number of days (TIC-87 duration choice).
    let onSnooze: (Int) -> Void
    let onEdit: () -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    /// Drives the snooze-duration confirmation dialog (1 day / 3 days / 1 week).
    @State private var showingSnoozeOptions = false

    /// Dismiss only when presented as a sheet; a no-op in the iPad detail pane.
    private func dismissIfNeeded() { if dismissesOnAction { dismiss() } }

    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    private var category: WarmCategory {
        if let contact = reminder.contact { return WarmCategory.resolve(for: contact) }
        if let group = reminder.group, let cat = WarmCategory.from(groupId: group.id) { return cat }
        return .community
    }

    private var phones: [String] { reminder.contact?.phoneNumbers ?? [] }
    private var emails: [String] { reminder.contact?.emails ?? [] }
    /// Compose requires both a number to text and a device that can send text
    /// (false on iPad/Simulator/devices without Messages configured).
    private var canCompose: Bool { !phones.isEmpty && MessageComposerView.canSendMessages() }

    private var displayName: String {
        if let contact = reminder.contact { return contact.fullName }
        if let group = reminder.group { return group.displayName }
        return String(localized: "tickleRow.unknown")
    }

    /// Estimated content height, kept in sync with the rows rendered below so the
    /// parent's `.presentationDetents([.height(...)])` snugly fits the sheet.
    static func estimatedHeight(for reminder: TickleReminder) -> CGFloat {
        let phones = reminder.contact?.phoneNumbers ?? []
        let emails = reminder.contact?.emails ?? []
        var rows = 3 // mark done, snooze, edit
        if !phones.isEmpty && MessageComposerView.canSendMessages() { rows += 1 } // compose
        if !phones.isEmpty { rows += 1 } // call
        if !emails.isEmpty { rows += 1 } // email
        let header: CGFloat = 92
        let rowHeight: CGFloat = 56
        let bottomPadding: CGFloat = 28
        return header + CGFloat(rows) * rowHeight + bottomPadding
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header

            if canCompose {
                actionRow(icon: "square.and.pencil",
                          title: String(localized: "tickleAction.compose", defaultValue: "Compose message"),
                          tint: category.palette.accent) {
                    onCompose()
                    dismissIfNeeded()
                }
            }

            if !phones.isEmpty {
                actionRow(icon: "phone.fill",
                          title: String(localized: "warm.contact.call", defaultValue: "Call"),
                          tint: category.palette.accent) {
                    if let phone = phones.first,
                       let url = URL(string: "tel:\(phone.filter { $0.isNumber || $0 == "+" })") {
                        openURL(url)
                    }
                    dismissIfNeeded()
                }
            }

            if !emails.isEmpty {
                actionRow(icon: "envelope.fill",
                          title: String(localized: "contact.section.email", defaultValue: "Email"),
                          tint: category.palette.accent) {
                    if let email = emails.first, let url = URL(string: "mailto:\(email)") {
                        openURL(url)
                    }
                    dismissIfNeeded()
                }
            }

            WarmRowDivider(warmth: warmth)
                .padding(.vertical, 6)

            actionRow(icon: "checkmark.circle.fill",
                      title: String(localized: "common.done", defaultValue: "Done"),
                      tint: palette.ink2) {
                onMarkDone()
                dismissIfNeeded()
            }

            actionRow(icon: "zzz",
                      title: String(localized: "tickleList.action.snooze", defaultValue: "Snooze"),
                      tint: palette.ink2) {
                showingSnoozeOptions = true
            }

            actionRow(icon: "pencil",
                      title: String(localized: "common.edit", defaultValue: "Edit"),
                      tint: palette.ink2) {
                onEdit()
                dismissIfNeeded()
            }

            Spacer(minLength: 0)
        }
        .padding(.top, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.paper)
        .confirmationDialog(
            String(localized: "tickleAction.snooze.title", defaultValue: "Snooze for how long?"),
            isPresented: $showingSnoozeOptions,
            titleVisibility: .visible
        ) {
            Button(String(localized: "tickleAction.snooze.oneDay", defaultValue: "1 day")) {
                onSnooze(1)
                dismissIfNeeded()
            }
            Button(String(localized: "tickleAction.snooze.threeDays", defaultValue: "3 days")) {
                onSnooze(3)
                dismissIfNeeded()
            }
            Button(String(localized: "tickleAction.snooze.oneWeek", defaultValue: "1 week")) {
                onSnooze(7)
                dismissIfNeeded()
            }
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            avatar
            VStack(alignment: .leading, spacing: 2) {
                Text(displayName)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(palette.ink)
                    .lineLimit(1)
                Text(reminder.frequency.localizedName)
                    .font(.system(size: 13))
                    .foregroundStyle(palette.ink2)
            }
            Spacer()
        }
        .padding(.horizontal, WarmSpacing.lg)
        .padding(.bottom, 14)
    }

    @ViewBuilder
    private var avatar: some View {
        if let contact = reminder.contact {
            ContactPhotoView(contact: contact, category: category, style: .list)
        } else {
            ZStack {
                Circle()
                    .fill(category.palette.accent)
                    .frame(width: 36, height: 36)
                if reminder.group != nil {
                    Text(reminder.group?.emoji ?? "👥").font(.system(size: 18))
                } else {
                    Image(systemName: "bell.fill")
                        .foregroundStyle(Color(red: 0.98, green: 0.96, blue: 0.89))
                }
            }
        }
    }

    private func actionRow(icon: String,
                           title: String,
                           tint: Color,
                           action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(tint)
                    .frame(width: 38, height: 38)
                    .background(tint.opacity(0.14))
                    .clipShape(Circle())
                Text(title)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(palette.ink)
                Spacer()
            }
            .padding(.horizontal, WarmSpacing.lg)
            .padding(.vertical, 9)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
