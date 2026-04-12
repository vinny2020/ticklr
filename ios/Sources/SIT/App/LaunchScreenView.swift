import SwiftUI

struct LaunchScreenView: View {
    var body: some View {
        ZStack {
            Color(red: 0.04, green: 0.11, blue: 0.16)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                Image("AppIcon-Preview")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 110, height: 110)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))

                VStack(spacing: 6) {
                    Text("Ticklr")
                        .font(.system(size: 38, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color(red: 0.96, green: 0.78, blue: 0.25))
                        .tracking(-1)

                    Text("YOUR PEOPLE MATTER")
                        .font(.system(size: 10, weight: .regular))
                        .foregroundStyle(Color(red: 0.96, green: 0.78, blue: 0.25).opacity(0.5))
                        .tracking(4)
                }

                Text(String(localized: "launch.tagline"))
                    .font(.system(size: 13, weight: .regular))
                    .foregroundStyle(.white.opacity(0.3))
                    .padding(.top, 4)
            }
        }
    }
}

#Preview {
    LaunchScreenView()
}
