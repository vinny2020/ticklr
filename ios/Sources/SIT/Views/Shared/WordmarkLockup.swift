import SwiftUI

struct WordmarkLockup: View {
    var wordmarkSize: CGFloat = 48
    var taglineSize: CGFloat = 11

    private let amber = Color(red: 0.96, green: 0.78, blue: 0.25)
    private let navy = Color(red: 0.039, green: 0.086, blue: 0.157)

    var body: some View {
        VStack(spacing: 12) {
            Text("Ticklr")
                .font(.custom("BebasNeue-Regular", size: wordmarkSize))
                .tracking(-wordmarkSize * 0.02)
                .foregroundStyle(amber)

            Text("YOUR PEOPLE MATTER")
                .font(.system(size: taglineSize, weight: .regular))
                .tracking(taglineSize * 0.4)
                .foregroundStyle(amber.opacity(0.5))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .background(navy, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}

#Preview("Light") {
    WordmarkLockup()
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.light)
}

#Preview("Dark") {
    WordmarkLockup()
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}
