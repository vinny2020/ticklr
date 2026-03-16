import SwiftUI

struct LaunchScreenView: View {
    @State private var animatePulse = false

    var body: some View {
        ZStack {
            Color(red: 0.04, green: 0.11, blue: 0.16)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                PulseLogoMark(animate: $animatePulse)
                    .frame(width: 100, height: 100)

                VStack(spacing: 6) {
                    Text("SIT")
                        .font(.system(size: 38, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color(red: 0.96, green: 0.78, blue: 0.25))
                        .tracking(-1)

                    Text("STAY IN TOUCH")
                        .font(.system(size: 10, weight: .regular))
                        .foregroundStyle(Color(red: 0.96, green: 0.78, blue: 0.25).opacity(0.5))
                        .tracking(4)
                }

                Text("Your network. Your rules.")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundStyle(.white.opacity(0.3))
                    .padding(.top, 4)
            }
        }
        .onAppear { animatePulse = true }
    }
}

struct PulseLogoMark: View {
    @Binding var animate: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 22)
                .fill(Color(red: 0.145, green: 0.235, blue: 0.92))
                .frame(width: 88, height: 64)

            BubbleTailShape()
                .fill(Color(red: 0.145, green: 0.235, blue: 0.92))
                .frame(width: 28, height: 18)
                .offset(x: -30, y: 36)

            PulseWaveShape(animate: animate)
                .stroke(
                    Color(red: 0.96, green: 0.78, blue: 0.25),
                    style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round)
                )
                .frame(width: 72, height: 40)
                .offset(y: -2)
                .animation(.easeOut(duration: 0.8).delay(0.2), value: animate)
        }
    }
}

struct BubbleTailShape: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: 0, y: 0))
        p.addLine(to: CGPoint(x: rect.width * 0.6, y: 0))
        p.addLine(to: CGPoint(x: 0, y: rect.height))
        p.closeSubpath()
        return p
    }
}

struct PulseWaveShape: Shape {
    var animate: Bool

    func path(in rect: CGRect) -> Path {
        let w = rect.width
        let mid = rect.midY
        var p = Path()
        p.move(to: CGPoint(x: 0, y: mid))
        p.addLine(to: CGPoint(x: w * 0.12, y: mid))
        p.addLine(to: CGPoint(x: w * 0.22, y: animate ? mid - rect.height * 0.55 : mid))
        p.addLine(to: CGPoint(x: w * 0.32, y: animate ? mid + rect.height * 0.55 : mid))
        p.addLine(to: CGPoint(x: w * 0.42, y: animate ? mid - rect.height * 0.35 : mid))
        p.addLine(to: CGPoint(x: w * 0.50, y: animate ? mid + rect.height * 0.20 : mid))
        p.addLine(to: CGPoint(x: w * 0.58, y: mid))
        p.addLine(to: CGPoint(x: w, y: mid))
        return p
    }
}

#Preview {
    LaunchScreenView()
}
