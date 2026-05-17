import SwiftUI

struct TickleRowView: View {
    let reminder: TickleReminder
    let onComplete: () -> Void
    var warmth: Warmth = .subtle

    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    private var isDue: Bool {
        reminder.nextDueDate <= Date() && reminder.status == .active
    }

    private var category: WarmCategory {
        if let contact = reminder.contact {
            return WarmCategory.resolve(for: contact)
        }
        if let group = reminder.group, let cat = WarmCategory.from(groupId: group.id) {
            return cat
        }
        return .community
    }

    private var displayName: String {
        if let contact = reminder.contact { return contact.fullName }
        if let group = reminder.group { return group.name }
        return String(localized: "tickleRow.unknown")
    }

    private var isGroupAvatar: Bool { reminder.contact == nil && reminder.group != nil }

    private var dueDateLabel: String {
        let cal = Calendar.current
        let now = Date()
        if cal.isDateInToday(reminder.nextDueDate) { return String(localized: "tickleRow.due.today") }
        if cal.isDateInYesterday(reminder.nextDueDate) { return String(localized: "tickleRow.due.yesterday") }
        if reminder.nextDueDate < now {
            let days = cal.dateComponents([.day], from: reminder.nextDueDate, to: now).day ?? 0
            return String(localized: "tickleRow.due.overdue \(days)")
        }
        if let days = cal.dateComponents([.day], from: now, to: reminder.nextDueDate).day, days < 8 {
            return String(localized: "tickleRow.due.upcoming \(days)")
        }
        return reminder.nextDueDate.formatted(date: .abbreviated, time: .omitted)
    }

    var body: some View {
        HStack(spacing: 12) {
            avatar

            VStack(alignment: .leading, spacing: 3) {
                Text(displayName)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(palette.ink)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    Text(reminder.frequency.localizedName)
                        .font(.system(size: 11, weight: .semibold))
                        .padding(.horizontal, 7).padding(.vertical, 2)
                        .background(isDue ? category.palette.accent : category.palette.accentTint)
                        .foregroundStyle(isDue ? Color(red: 0.98, green: 0.96, blue: 0.89) : category.palette.accent)
                        .clipShape(Capsule())
                    Text(verbatim: dueDateLabel)
                        .font(.system(size: 12))
                        .foregroundStyle(isDue ? category.palette.accent : palette.ink2)
                }
                if !reminder.note.isEmpty {
                    Text(reminder.note)
                        .font(.system(size: 12))
                        .foregroundStyle(palette.ink3)
                        .lineLimit(1)
                }
            }

            Spacer()

            Button(action: onComplete) {
                Image(systemName: isDue ? "checkmark.circle.fill" : "checkmark.circle")
                    .font(.system(size: 24))
                    .foregroundStyle(isDue ? category.palette.accent : palette.ink3)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, WarmSpacing.lg)
        .padding(.vertical, WarmSpacing.md)
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
                if isGroupAvatar {
                    Text(reminder.group?.emoji ?? "👥")
                        .font(.system(size: 18))
                } else {
                    Image(systemName: "bell.fill")
                        .foregroundStyle(Color(red: 0.98, green: 0.96, blue: 0.89))
                }
            }
            .overlay(
                Circle()
                    .stroke(isDue ? category.palette.accent : Color.clear, lineWidth: 2)
                    .padding(-3)
            )
        }
    }
}
