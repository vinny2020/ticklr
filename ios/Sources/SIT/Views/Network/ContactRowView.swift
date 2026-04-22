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

            VStack(alignment: .leading, spacing: 1) {
                Text(contact.fullName)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.primary)
                if !contact.company.isEmpty {
                    Text(contact.company)
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                }
            }

            Spacer(minLength: 8)

            HStack(spacing: 6) {
                if !contact.phoneNumbers.isEmpty {
                    Image(systemName: "phone.fill")
                        .font(.system(size: 16))
                        .foregroundStyle(.secondary.opacity(0.6))
                }
                if !contact.emails.isEmpty {
                    Image(systemName: "envelope.fill")
                        .font(.system(size: 16))
                        .foregroundStyle(.secondary.opacity(0.6))
                }
                if !contact.company.isEmpty {
                    Image(systemName: "briefcase.fill")
                        .font(.system(size: 16))
                        .foregroundStyle(.secondary.opacity(0.6))
                }
            }
        }
        .padding(.vertical, 4)
    }
}
