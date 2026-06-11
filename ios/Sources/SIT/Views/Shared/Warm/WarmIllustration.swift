import SwiftUI

/// Paper-collage illustration shown on warm cards. One per category.
/// Mid-fidelity port of `assets/design-system/project/warm-redesign/
/// illustrations.jsx`. SVG grain/vignette/paper-shadow filters are
/// intentionally omitted — noted fidelity gap from the implementation
/// plan. Each illustration draws into a 320×180 reference space, scaled
/// by the parent's `.aspectRatio(_:contentMode:)` modifier.
///
/// Text-free rule (June 2026): the artwork contains no drawn strings —
/// the app ships in 21 locales and baked-in English read as unfinished
/// in ar/he/ja/…. Former labels are abstract marks (faux-script bars,
/// dash·diamond·dash, star-on-the-day, check ring), matching the updated
/// design-system source.
struct WarmIllustration: View {
    let category: WarmCategory

    var body: some View {
        Canvas { context, size in
            let ref = CGSize(width: 320, height: 180)
            let scale = max(size.width / ref.width, size.height / ref.height)
            context.scaleBy(x: scale, y: scale)
            context.translateBy(
                x: (size.width / scale - ref.width) / 2,
                y: (size.height / scale - ref.height) / 2
            )

            switch category {
            case .family:     drawFamily(in: &context)
            case .friends:    drawFriends(in: &context)
            case .work:       drawWork(in: &context)
            case .milestones: drawMilestones(in: &context)
            case .community:  drawCommunity(in: &context)
            }
        }
        .accessibilityHidden(true)
    }
}

// MARK: - Family

private func drawFamily(in ctx: inout GraphicsContext) {
    // backing paper
    ctx.fill(Rectangle().path(in: CGRect(x: 0, y: 0, width: 320, height: 180)),
             with: .color(hex("#F4DDD2")))

    // back paper plane (rotated)
    var plane = ctx
    plane.translateBy(x: 28, y: 18)
    plane.rotate(by: .degrees(-5))
    plane.fill(
        RoundedRectangle(cornerRadius: 6).path(in: CGRect(x: 0, y: 0, width: 180, height: 120)),
        with: .color(hex("#EAB8AB"))
    )

    // photo frame (slight rotation)
    var frame = ctx
    frame.translateBy(x: 58, y: 30)
    frame.rotate(by: .degrees(-3))
    // outer frame card
    frame.fill(RoundedRectangle(cornerRadius: 4).path(in: CGRect(x: 0, y: 0, width: 150, height: 110)),
               with: .color(hex("#F8EBE2")))
    // photo backdrop
    frame.fill(Rectangle().path(in: CGRect(x: 8, y: 8, width: 134, height: 80)),
               with: .color(hex("#D88679")))
    // 4 abstract figures (no faces — just heads + bodies)
    let figures: [(cx: CGFloat, cy: CGFloat, headR: CGFloat, bodyW: CGFloat, bodyH: CGFloat, bodyX: CGFloat, bodyY: CGFloat, color: String)] = [
        (40, 56, 14, 28, 20, 26, 68, "#9C3F3C"),
        (70, 58, 11, 22, 21, 59, 67, "#C57867"),
        (98, 60, 10, 20, 20, 88, 68, "#6E3030"),
        (124, 62, 9,  18, 18, 115, 70, "#A14A45"),
    ]
    for fig in figures {
        frame.fill(Circle().path(in: CGRect(x: fig.cx - fig.headR, y: fig.cy - fig.headR, width: fig.headR * 2, height: fig.headR * 2)),
                   with: .color(hex("#F4D9CC")))
        frame.fill(RoundedRectangle(cornerRadius: 4).path(in: CGRect(x: fig.bodyX, y: fig.bodyY, width: fig.bodyW, height: fig.bodyH)),
                   with: .color(hex(fig.color)))
    }

    // dash · diamond · dash keepsake mark under the photo
    var caption = Path()
    caption.move(to: CGPoint(x: 52, y: 99)); caption.addLine(to: CGPoint(x: 66, y: 99))
    caption.move(to: CGPoint(x: 84, y: 99)); caption.addLine(to: CGPoint(x: 98, y: 99))
    frame.stroke(caption, with: .color(hex("#6E3030").opacity(0.8)),
                 style: StrokeStyle(lineWidth: 1.2, lineCap: .round))
    var diamond = frame
    diamond.translateBy(x: 75, y: 99)
    diamond.rotate(by: .degrees(45))
    diamond.fill(RoundedRectangle(cornerRadius: 1).path(in: CGRect(x: -3, y: -3, width: 6, height: 6)),
                 with: .color(hex("#6E3030").opacity(0.8)))

    // heart pin (top right)
    var pin = ctx
    pin.translateBy(x: 220, y: 40)
    pin.rotate(by: .degrees(15))
    pin.fill(Circle().path(in: CGRect(x: -22, y: -22, width: 44, height: 44)),
             with: .color(hex("#FAF4E2")))
    pin.fill(heartPath().offsetBy(dx: -6, dy: -8),
             with: .color(hex("#9C3F3C")))

    // ribbon swoop (bottom)
    var ribbon = Path()
    ribbon.move(to: CGPoint(x: 48, y: 140))
    ribbon.addQuadCurve(to: CGPoint(x: 128, y: 138), control: CGPoint(x: 88, y: 132))
    ribbon.addQuadCurve(to: CGPoint(x: 220, y: 138), control: CGPoint(x: 168, y: 144))
    ctx.stroke(ribbon, with: .color(hex("#9C3F3C").opacity(0.5)),
               style: StrokeStyle(lineWidth: 1.4, lineCap: .round))

    // small leaf sprig (right)
    var leaf = ctx
    leaf.translateBy(x: 238, y: 108)
    var l1 = Path()
    l1.move(to: .zero)
    l1.addQuadCurve(to: CGPoint(x: 24, y: -14), control: CGPoint(x: 10, y: -12))
    l1.addQuadCurve(to: CGPoint(x: 4, y: 4), control: CGPoint(x: 18, y: -2))
    l1.closeSubpath()
    leaf.fill(l1, with: .color(hex("#6E8A6B")))
    var l2 = Path()
    l2.move(to: CGPoint(x: -2, y: 4))
    l2.addQuadCurve(to: CGPoint(x: -26, y: -6), control: CGPoint(x: -12, y: -6))
    l2.addQuadCurve(to: CGPoint(x: -4, y: 10), control: CGPoint(x: -22, y: 8))
    l2.closeSubpath()
    leaf.fill(l2, with: .color(hex("#849D7F")))
}

// MARK: - Friends

private func drawFriends(in ctx: inout GraphicsContext) {
    ctx.fill(Rectangle().path(in: CGRect(x: 0, y: 0, width: 320, height: 180)),
             with: .color(hex("#DDE7F0")))

    // postcard — faux-script bars, no literal copy
    var pc = ctx
    pc.translateBy(x: 26, y: 30)
    pc.rotate(by: .degrees(-7))
    pc.fill(RoundedRectangle(cornerRadius: 5).path(in: CGRect(x: 0, y: 0, width: 124, height: 86)),
            with: .color(hex("#F4EAD7")))
    // dividing line
    var dl = Path()
    dl.move(to: CGPoint(x: 62, y: 12))
    dl.addLine(to: CGPoint(x: 62, y: 74))
    pc.stroke(dl, with: .color(hex("#3F5C7A").opacity(0.45)), lineWidth: 0.8)
    // text lines (faux script)
    pc.fill(RoundedRectangle(cornerRadius: 1).path(in: CGRect(x: 12, y: 22, width: 44, height: 5)),
            with: .color(hex("#3F5C7A").opacity(0.85)))
    for (y, w) in [(38.0, 46.0), (50.0, 50.0), (62.0, 44.0)] {
        pc.fill(RoundedRectangle(cornerRadius: 1).path(in: CGRect(x: 12, y: y, width: w, height: 3)),
                with: .color(hex("#6B5F4F").opacity(0.55)))
    }
    // stamp
    pc.fill(Rectangle().path(in: CGRect(x: 74, y: 12, width: 36, height: 24)),
            with: .color(hex("#C7D4E0")))
    pc.stroke(Rectangle().path(in: CGRect(x: 74, y: 12, width: 36, height: 24)),
              with: .color(hex("#3F5C7A")),
              style: StrokeStyle(lineWidth: 0.6, dash: [2, 1.4]))
    pc.fill(Circle().path(in: CGRect(x: 86.5, y: 18.5, width: 11, height: 11)),
            with: .color(hex("#3F5C7A")))

    // two friends catching up over coffee (faceless; cream heads,
    // identity carried by category blues — never skin tone)
    ctx.fill(Circle().path(in: CGRect(x: 183, y: 65, width: 26, height: 26)),
             with: .color(hex("#F4D9CC")))
    var bodyL = Path()
    bodyL.move(to: CGPoint(x: 178, y: 124))
    bodyL.addLine(to: CGPoint(x: 178, y: 106))
    bodyL.addCurve(to: CGPoint(x: 214, y: 106),
                   control1: CGPoint(x: 178, y: 87),
                   control2: CGPoint(x: 214, y: 87))
    bodyL.addLine(to: CGPoint(x: 214, y: 124))
    bodyL.closeSubpath()
    ctx.fill(bodyL, with: .color(hex("#3F5C7A")))

    ctx.fill(Circle().path(in: CGRect(x: 254, y: 70, width: 24, height: 24)),
             with: .color(hex("#F4D9CC")))
    var bodyR = Path()
    bodyR.move(to: CGPoint(x: 250, y: 124))
    bodyR.addLine(to: CGPoint(x: 250, y: 109))
    bodyR.addCurve(to: CGPoint(x: 282, y: 109),
                   control1: CGPoint(x: 250, y: 92),
                   control2: CGPoint(x: 282, y: 92))
    bodyR.addLine(to: CGPoint(x: 282, y: 124))
    bodyR.closeSubpath()
    ctx.fill(bodyR, with: .color(hex("#7E97B8")))

    // table
    ctx.fill(RoundedRectangle(cornerRadius: 3.5).path(in: CGRect(x: 170, y: 122, width: 124, height: 7)),
             with: .color(hex("#F4EAD7")))

    // mugs on the table (one per friend)
    func mugPath() -> Path {
        var p = Path()
        p.move(to: .zero)
        p.addLine(to: CGPoint(x: 14, y: 0))
        p.addLine(to: CGPoint(x: 14, y: 12))
        p.addQuadCurve(to: CGPoint(x: 10, y: 16), control: CGPoint(x: 14, y: 16))
        p.addLine(to: CGPoint(x: 4, y: 16))
        p.addQuadCurve(to: CGPoint(x: 0, y: 12), control: CGPoint(x: 0, y: 16))
        p.closeSubpath()
        return p
    }
    func handlePath() -> Path {
        var p = Path()
        p.move(to: CGPoint(x: 14, y: 3))
        p.addQuadCurve(to: CGPoint(x: 14, y: 11), control: CGPoint(x: 19, y: 7))
        return p
    }
    var mugL = ctx
    mugL.translateBy(x: 212, y: 106)
    mugL.fill(mugPath(), with: .color(hex("#FAF4E2")))
    mugL.stroke(handlePath(), with: .color(hex("#FAF4E2")), lineWidth: 2.2)
    var steam = Path()
    steam.move(to: CGPoint(x: 4, y: -4))
    steam.addQuadCurve(to: CGPoint(x: 4, y: -10), control: CGPoint(x: 6, y: -7))
    steam.move(to: CGPoint(x: 10, y: -4))
    steam.addQuadCurve(to: CGPoint(x: 10, y: -10), control: CGPoint(x: 8, y: -7))
    mugL.stroke(steam, with: .color(hex("#6B5F4F").opacity(0.5)),
                style: StrokeStyle(lineWidth: 1, lineCap: .round))
    var mugR = ctx
    mugR.translateBy(x: 240, y: 106)
    mugR.scaleBy(x: -1, y: 1)
    mugR.fill(mugPath(), with: .color(hex("#A2693C")))
    mugR.stroke(handlePath(), with: .color(hex("#A2693C")), lineWidth: 2.2)

    // speech bubble between them
    var sb = ctx
    sb.translateBy(x: 204, y: 18)
    sb.rotate(by: .degrees(4))
    var bubble = Path()
    bubble.move(to: .zero)
    bubble.addLine(to: CGPoint(x: 62, y: 0))
    bubble.addQuadCurve(to: CGPoint(x: 70, y: 8), control: CGPoint(x: 70, y: 0))
    bubble.addLine(to: CGPoint(x: 70, y: 26))
    bubble.addQuadCurve(to: CGPoint(x: 62, y: 34), control: CGPoint(x: 70, y: 34))
    bubble.addLine(to: CGPoint(x: 18, y: 34))
    bubble.addLine(to: CGPoint(x: 8, y: 42))
    bubble.addLine(to: CGPoint(x: 8, y: 34))
    bubble.addQuadCurve(to: CGPoint(x: 0, y: 26), control: CGPoint(x: 0, y: 34))
    bubble.addLine(to: CGPoint(x: 0, y: 8))
    bubble.addQuadCurve(to: CGPoint(x: 8, y: 0), control: CGPoint(x: 0, y: 0))
    bubble.closeSubpath()
    sb.fill(bubble, with: .color(hex("#FAF4E2")))
    for x in [20.0, 33.0, 46.0] {
        sb.fill(Circle().path(in: CGRect(x: x - 2.2, y: 14.8, width: 4.4, height: 4.4)),
                with: .color(hex("#3F5C7A")))
    }
}

// MARK: - Work

private func drawWork(in ctx: inout GraphicsContext) {
    ctx.fill(Rectangle().path(in: CGRect(x: 0, y: 0, width: 320, height: 180)),
             with: .color(hex("#D5DDCB")))

    // desk line
    var dl = Path()
    dl.move(to: CGPoint(x: 0, y: 140))
    dl.addLine(to: CGPoint(x: 320, y: 140))
    ctx.stroke(dl, with: .color(hex("#7A8A6E").opacity(0.5)), lineWidth: 0.7)

    // envelope back
    var env = ctx
    env.translateBy(x: 20, y: 26)
    env.rotate(by: .degrees(-6))
    env.fill(RoundedRectangle(cornerRadius: 3).path(in: CGRect(x: 0, y: 0, width: 170, height: 108)),
             with: .color(hex("#EBE0C4")))
    var flap = Path()
    flap.move(to: .zero)
    flap.addLine(to: CGPoint(x: 85, y: 60))
    flap.addLine(to: CGPoint(x: 170, y: 0))
    env.stroke(flap, with: .color(hex("#9CA384")), lineWidth: 1)

    // letter
    var letter = ctx
    letter.translateBy(x: 40, y: 22)
    letter.rotate(by: .degrees(-2))
    letter.fill(RoundedRectangle(cornerRadius: 2).path(in: CGRect(x: 0, y: 0, width: 142, height: 88)),
                with: .color(hex("#FAF4E2")))
    // header bar
    letter.fill(RoundedRectangle(cornerRadius: 1).path(in: CGRect(x: 14, y: 14, width: 50, height: 6)),
                with: .color(hex("#4F6B47")))
    // green underline
    var ul = Path()
    ul.move(to: CGPoint(x: 14, y: 22))
    ul.addLine(to: CGPoint(x: 120, y: 22))
    letter.stroke(ul, with: .color(hex("#4F6B47")), lineWidth: 1)
    // body lines
    let letterLines: [CGFloat] = [96, 110, 84, 102, 70]
    for (i, w) in letterLines.enumerated() {
        let y: CGFloat = 34 + CGFloat(i) * 10
        var line = Path()
        line.move(to: CGPoint(x: 14, y: y))
        line.addLine(to: CGPoint(x: 14 + w, y: y))
        letter.stroke(line, with: .color(hex("#A8B19A")), lineWidth: 0.8)
    }

    // check-in tile — check ring instead of EN-only "Q3 / REMINDER"
    var cal = ctx
    cal.translateBy(x: 216, y: 32)
    cal.rotate(by: .degrees(4))
    cal.fill(RoundedRectangle(cornerRadius: 6).path(in: CGRect(x: 0, y: 0, width: 74, height: 78)),
             with: .color(hex("#FAF4E2")))
    cal.fill(RoundedRectangle(cornerRadius: 6).path(in: CGRect(x: 0, y: 0, width: 74, height: 22)),
             with: .color(hex("#4F6B47")))
    cal.fill(RoundedRectangle(cornerRadius: 1.5).path(in: CGRect(x: 22, y: 9, width: 30, height: 5)),
             with: .color(hex("#FAF4E2").opacity(0.9)))
    cal.stroke(Circle().path(in: CGRect(x: 24, y: 35, width: 26, height: 26)),
               with: .color(hex("#4F6B47")), lineWidth: 2.4)
    var check = Path()
    check.move(to: CGPoint(x: 31, y: 48))
    check.addLine(to: CGPoint(x: 35, y: 52))
    check.addLine(to: CGPoint(x: 43, y: 43))
    cal.stroke(check, with: .color(hex("#4F6B47")),
               style: StrokeStyle(lineWidth: 2.4, lineCap: .round, lineJoin: .round))
    cal.fill(RoundedRectangle(cornerRadius: 1.5).path(in: CGRect(x: 22, y: 66, width: 30, height: 3)),
             with: .color(hex("#6B5F4F").opacity(0.55)))

    // pencil
    var pencil = ctx
    pencil.translateBy(x: 50, y: 138)
    pencil.rotate(by: .degrees(-4))
    pencil.fill(RoundedRectangle(cornerRadius: 1).path(in: CGRect(x: 0, y: 0, width: 120, height: 8)),
                with: .color(hex("#A7791C")))
    // tip
    var tip = Path()
    tip.move(to: CGPoint(x: 120, y: 0))
    tip.addLine(to: CGPoint(x: 132, y: 4))
    tip.addLine(to: CGPoint(x: 120, y: 8))
    tip.closeSubpath()
    pencil.fill(tip, with: .color(hex("#FAF4E2")))
    // graphite
    var graphite = Path()
    graphite.move(to: CGPoint(x: 128, y: 2))
    graphite.addLine(to: CGPoint(x: 132, y: 4))
    graphite.addLine(to: CGPoint(x: 128, y: 6))
    graphite.closeSubpath()
    pencil.fill(graphite, with: .color(hex("#4A3A20")))
    // eraser
    pencil.fill(Rectangle().path(in: CGRect(x: 0, y: 0, width: 14, height: 8)),
                with: .color(hex("#9C3F3C")))
}

// MARK: - Milestones

private func drawMilestones(in ctx: inout GraphicsContext) {
    ctx.fill(Rectangle().path(in: CGRect(x: 0, y: 0, width: 320, height: 180)),
             with: .color(hex("#EDDEB6")))

    // big calendar — abstract header marks + star on the day, no numerals
    var cal = ctx
    cal.translateBy(x: 28, y: 24)
    cal.rotate(by: .degrees(-4))
    cal.fill(RoundedRectangle(cornerRadius: 6).path(in: CGRect(x: 0, y: 0, width: 160, height: 130)),
             with: .color(hex("#FAF4E2")))
    cal.fill(RoundedRectangle(cornerRadius: 6).path(in: CGRect(x: 0, y: 0, width: 160, height: 24)),
             with: .color(hex("#A7791C")))
    cal.fill(RoundedRectangle(cornerRadius: 2).path(in: CGRect(x: 48, y: 9, width: 42, height: 6)),
             with: .color(hex("#FAF4E2").opacity(0.9)))
    cal.fill(Circle().path(in: CGRect(x: 97.4, y: 9.4, width: 5.2, height: 5.2)),
             with: .color(hex("#FAF4E2").opacity(0.9)))
    cal.fill(RoundedRectangle(cornerRadius: 2).path(in: CGRect(x: 108, y: 9, width: 18, height: 6)),
             with: .color(hex("#FAF4E2").opacity(0.9)))
    // grid: 4 rows × 7 cols, highlight the (1,3) cell
    for r in 0..<4 {
        for c in 0..<7 {
            let isHi = (r == 1 && c == 3)
            let x: CGFloat = 6 + CGFloat(c) * 21.5
            let y: CGFloat = 32 + CGFloat(r) * 22
            cal.fill(RoundedRectangle(cornerRadius: 3).path(in: CGRect(x: x, y: y, width: 20, height: 20)),
                     with: .color(hex(isHi ? "#A7791C" : "#F2E7C7")))
        }
    }
    // star on the big day
    var star = Path()
    star.move(to: CGPoint(x: 80.5, y: 58))
    for (dx, dy) in [(2.2, 4.4), (4.9, 0.7), (-3.5, 3.5), (0.8, 4.9),
                     (-4.4, -2.3), (-4.4, 2.3), (0.8, -4.9), (-3.5, -3.5), (4.9, -0.7)] {
        star.addLine(to: CGPoint(x: star.currentPoint!.x + dx, y: star.currentPoint!.y + dy))
    }
    star.closeSubpath()
    cal.fill(star, with: .color(hex("#FAF4E2")))
    // dash marks on a few busy days
    for (x, y) in [(31.0, 40.0), (117.0, 62.0), (52.5, 84.0), (9.5, 106.0)] {
        cal.fill(RoundedRectangle(cornerRadius: 1).path(in: CGRect(x: x, y: y, width: 13, height: 2.5)),
                 with: .color(hex("#6B5F4F").opacity(0.5)))
    }

    // candle
    var candle = ctx
    candle.translateBy(x: 208, y: 40)
    candle.fill(RoundedRectangle(cornerRadius: 2).path(in: CGRect(x: 0, y: 20, width: 14, height: 60)),
                with: .color(hex("#B26342")))
    candle.fill(Rectangle().path(in: CGRect(x: 2, y: 22, width: 10, height: 6)),
                with: .color(hex("#D87A52")))
    // wick
    var wick = Path()
    wick.move(to: CGPoint(x: 7, y: 20))
    wick.addLine(to: CGPoint(x: 7, y: 10))
    candle.stroke(wick, with: .color(hex("#6B5F4F")), lineWidth: 1)
    // flame
    var flame = Path()
    flame.move(to: CGPoint(x: 7, y: 10))
    flame.addQuadCurve(to: CGPoint(x: 7, y: -2), control: CGPoint(x: 11, y: 4))
    flame.addQuadCurve(to: CGPoint(x: 7, y: 10), control: CGPoint(x: 3, y: 4))
    candle.fill(flame, with: .color(hex("#F5C842")))

    // gift box
    var gift = ctx
    gift.translateBy(x: 232, y: 86)
    gift.rotate(by: .degrees(6))
    gift.fill(RoundedRectangle(cornerRadius: 3).path(in: CGRect(x: 0, y: 0, width: 56, height: 46)),
              with: .color(hex("#9C3F3C")))
    gift.fill(Rectangle().path(in: CGRect(x: 0, y: 0, width: 56, height: 14)),
              with: .color(hex("#7B2E2C")))
    gift.fill(Rectangle().path(in: CGRect(x: 22, y: 0, width: 12, height: 46)),
              with: .color(hex("#F5C842")))
    // bow loops
    var bow = Path()
    bow.move(to: CGPoint(x: 22, y: 0))
    bow.addQuadCurve(to: CGPoint(x: 34, y: -14), control: CGPoint(x: 10, y: -14))
    bow.move(to: CGPoint(x: 34, y: 0))
    bow.addQuadCurve(to: CGPoint(x: 22, y: -14), control: CGPoint(x: 46, y: -14))
    gift.stroke(bow, with: .color(hex("#F5C842")), lineWidth: 2.5)

    // confetti
    let confettiRects: [(x: CGFloat, y: CGFloat, s: CGFloat, deg: Double, color: String, alpha: Double)] = [
        (225, 27, 6, 20, "#9C3F3C", 0.8),
        (264, 40, 5, -15, "#4F6B47", 0.7),
        (290, 28, 5, 30, "#3F5C7A", 0.6),
    ]
    for cf in confettiRects {
        var piece = ctx
        piece.translateBy(x: cf.x, y: cf.y)
        piece.rotate(by: .degrees(cf.deg))
        piece.fill(RoundedRectangle(cornerRadius: 1).path(in: CGRect(x: -cf.s / 2, y: -cf.s / 2, width: cf.s, height: cf.s)),
                   with: .color(hex(cf.color).opacity(cf.alpha)))
    }
    ctx.fill(Circle().path(in: CGRect(x: 243, y: 17, width: 6, height: 6)),
             with: .color(hex("#F5C842").opacity(0.9)))
    ctx.fill(Circle().path(in: CGRect(x: 293.4, y: 57.4, width: 5.2, height: 5.2)),
             with: .color(hex("#B26342").opacity(0.8)))
}

// MARK: - Community

private func drawCommunity(in ctx: inout GraphicsContext) {
    ctx.fill(Rectangle().path(in: CGRect(x: 0, y: 0, width: 320, height: 180)),
             with: .color(hex("#F0D4C2")))

    // connecting threads
    var t1 = Path()
    t1.move(to: CGPoint(x: 60, y: 90))
    t1.addQuadCurve(to: CGPoint(x: 160, y: 70), control: CGPoint(x: 100, y: 50))
    t1.addQuadCurve(to: CGPoint(x: 260, y: 80), control: CGPoint(x: 200, y: 110))
    ctx.stroke(t1, with: .color(hex("#B26342").opacity(0.55)),
               style: StrokeStyle(lineWidth: 1.2, lineCap: .round))

    var t2 = Path()
    t2.move(to: CGPoint(x: 60, y: 90))
    t2.addQuadCurve(to: CGPoint(x: 260, y: 80), control: CGPoint(x: 120, y: 130))
    ctx.stroke(t2, with: .color(hex("#B26342").opacity(0.55)),
               style: StrokeStyle(lineWidth: 1.2, lineCap: .round, dash: [2, 4]))

    var t3 = Path()
    t3.move(to: CGPoint(x: 120, y: 40))
    t3.addQuadCurve(to: CGPoint(x: 220, y: 140), control: CGPoint(x: 140, y: 120))
    ctx.stroke(t3, with: .color(hex("#B26342").opacity(0.55)),
               style: StrokeStyle(lineWidth: 1.2, lineCap: .round))

    // 5 paper "person" circles (no faces — head + body silhouette)
    let people: [(cx: CGFloat, cy: CGFloat, r: CGFloat, fill: String, rim: String)] = [
        (60, 90, 28, "#9C3F3C", "#F4D9CC"),
        (130, 50, 24, "#A7791C", "#EDDEB6"),
        (220, 80, 30, "#4F6B47", "#D5DDCB"),
        (168, 130, 22, "#3F5C7A", "#DDE7F0"),
        (268, 134, 20, "#B26342", "#F0D4C2"),
    ]
    for p in people {
        // rim
        ctx.fill(Circle().path(in: CGRect(x: p.cx - p.r - 6, y: p.cy - p.r - 6, width: (p.r + 6) * 2, height: (p.r + 6) * 2)),
                 with: .color(hex(p.rim)))
        // body
        ctx.fill(Circle().path(in: CGRect(x: p.cx - p.r, y: p.cy - p.r, width: p.r * 2, height: p.r * 2)),
                 with: .color(hex(p.fill)))
        // head highlight
        let headR = p.r * 0.28
        ctx.fill(Circle().path(in: CGRect(x: p.cx - headR, y: p.cy - p.r * 0.18 - headR, width: headR * 2, height: headR * 2)),
                 with: .color(hex(p.rim)))
        // shoulder arc
        var sh = Path()
        sh.move(to: CGPoint(x: p.cx - p.r * 0.45, y: p.cy + p.r * 0.35))
        sh.addQuadCurve(to: CGPoint(x: p.cx + p.r * 0.45, y: p.cy + p.r * 0.35),
                        control: CGPoint(x: p.cx, y: p.cy))
        sh.closeSubpath()
        ctx.fill(sh, with: .color(hex(p.rim)))
    }

    // center heart
    ctx.fill(Circle().path(in: CGRect(x: 146, y: 84, width: 28, height: 28)),
             with: .color(hex("#FAF4E2")))
    var heart = ctx
    heart.translateBy(x: 160, y: 98)
    heart.fill(heartPath().offsetBy(dx: -4, dy: -5),
               with: .color(hex("#B26342")))
}

// MARK: - Helpers

private func heartPath() -> Path {
    var p = Path()
    p.move(to: CGPoint(x: 0, y: -3))
    p.addQuadCurve(to: CGPoint(x: 8, y: -3), control: CGPoint(x: 4, y: -10))
    p.addQuadCurve(to: CGPoint(x: 4, y: 9), control: CGPoint(x: 8, y: 4))
    p.addQuadCurve(to: CGPoint(x: 0, y: -3), control: CGPoint(x: 0, y: 4))
    p.closeSubpath()
    return p
}

private extension Path {
    func offsetBy(dx: CGFloat, dy: CGFloat) -> Path {
        var p = Path()
        p.addPath(self, transform: CGAffineTransform(translationX: dx, y: dy))
        return p
    }
}

private func hex(_ value: String) -> Color {
    var h: UInt64 = 0
    Scanner(string: value.replacingOccurrences(of: "#", with: "")).scanHexInt64(&h)
    return Color(
        red:   Double((h & 0xFF0000) >> 16) / 255,
        green: Double((h & 0x00FF00) >> 8) / 255,
        blue:  Double(h & 0x0000FF) / 255
    )
}

#Preview {
    ScrollView {
        VStack(spacing: 16) {
            ForEach(WarmCategory.allCases) { cat in
                VStack(alignment: .leading) {
                    Text(cat.localizedLabel).font(.caption).padding(.leading)
                    WarmIllustration(category: cat)
                        .aspectRatio(16.0 / 9.0, contentMode: .fit)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .padding(.horizontal)
                }
            }
        }
        .padding(.vertical)
    }
    .background(WarmTheme.subtle.paper)
}
