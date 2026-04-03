import SwiftUI

struct ContactRowView: View {
    let contact: Contact

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color.indigo.opacity(0.15))
                .frame(width: 44, height: 44)
                .overlay(
                    Text(contact.initials)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.indigo)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(contact.fullName)
                    .font(.body)
                    .fontWeight(.medium)
                if !contact.company.isEmpty {
                    Text(contact.company)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            // Reachability icons — muted, trailing
            HStack(spacing: 4) {
                if !contact.phoneNumbers.isEmpty {
                    Image(systemName: "phone.fill")
                        .font(.caption)
                        .foregroundStyle(.secondary.opacity(0.6))
                }
                if !contact.emails.isEmpty {
                    Image(systemName: "envelope.fill")
                        .font(.caption)
                        .foregroundStyle(.secondary.opacity(0.6))
                }
            }

            Image(systemName: importIcon(for: contact.importSource))
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 2)
    }

    private func importIcon(for source: ImportSource) -> String {
        switch source {
        case .ios: return "apple.logo"
        case .linkedin: return "briefcase.fill"
        case .manual: return "pencil"
        }
    }
}
