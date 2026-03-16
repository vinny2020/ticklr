import SwiftUI

struct TickleRowView: View {
    let reminder: TickleReminder
    let onComplete: () -> Void

    private let amber = Color(red: 0.96, green: 0.78, blue: 0.25)

    private var isDue: Bool {
        reminder.nextDueDate <= Date() && reminder.status == .active
    }

    private var displayName: String {
        if let contact = reminder.contact { return contact.fullName }
        if let group = reminder.group { return group.name }
        return "Unknown"
    }

    private var avatarText: String {
        if let contact = reminder.contact { return contact.initials }
        if let group = reminder.group { return group.emoji }
        return "?"
    }

    private var isGroupAvatar: Bool { reminder.contact == nil && reminder.group != nil }

    private var dueDateLabel: String {
        let cal = Calendar.current
        let now = Date()
        if cal.isDateInToday(reminder.nextDueDate) { return "Today" }
        if cal.isDateInYesterday(reminder.nextDueDate) { return "Yesterday" }
        if reminder.nextDueDate < now {
            let days = cal.dateComponents([.day], from: reminder.nextDueDate, to: now).day ?? 0
            return "\(days)d overdue"
        }
        if let days = cal.dateComponents([.day], from: now, to: reminder.nextDueDate).day, days < 8 {
            return "In \(days)d"
        }
        return reminder.nextDueDate.formatted(date: .abbreviated, time: .omitted)
    }

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(isDue ? amber.opacity(0.15) : Color.indigo.opacity(0.12))
                .frame(width: 44, height: 44)
                .overlay(
                    Text(avatarText)
                        .font(.system(size: isGroupAvatar ? 22 : 16, weight: .semibold))
                        .foregroundStyle(isDue ? amber : .indigo)
                )

            VStack(alignment: .leading, spacing: 3) {
                Text(displayName)
                    .font(.body).fontWeight(.medium)
                HStack(spacing: 6) {
                    Text(reminder.frequency.rawValue)
                        .font(.caption2).fontWeight(.medium)
                        .padding(.horizontal, 6).padding(.vertical, 2)
                        .background(isDue ? amber.opacity(0.15) : Color(.systemGray5))
                        .foregroundStyle(isDue ? amber : .secondary)
                        .clipShape(Capsule())
                    Text(dueDateLabel)
                        .font(.caption)
                        .foregroundStyle(isDue ? amber : .secondary)
                }
                if !reminder.note.isEmpty {
                    Text(reminder.note)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer()

            Button(action: onComplete) {
                Image(systemName: isDue ? "checkmark.circle.fill" : "checkmark.circle")
                    .font(.title2)
                    .foregroundStyle(isDue ? amber : Color(.systemGray3))
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 2)
    }
}
