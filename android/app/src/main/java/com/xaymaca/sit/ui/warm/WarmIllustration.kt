package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.xaymaca.sit.ui.theme.WarmCategory

/**
 * Paper-collage illustration for each category. Canvas-drawn port of
 * assets/design-system/project/warm-redesign/illustrations.jsx. Mirrors
 * the iOS WarmIllustration — Family/Friends/Work/Milestones/Community.
 *
 * Known fidelity gaps from the spec (intentional, matching iOS):
 *  - No SVG feTurbulence paper grain
 *  - No radial vignette
 *  - No per-shape Gaussian-blur paper-shadow
 */
@Composable
fun WarmIllustration(
    category: WarmCategory,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        // Reference space matches the SVG viewBox 320x180; scale to fit.
        val sx = size.width / 320f
        val sy = size.height / 180f
        val s = maxOf(sx, sy)
        translate(
            left = (size.width / s - 320f) / 2f * s,
            top = (size.height / s - 180f) / 2f * s,
        ) {
            // SwiftUI's scaleBy isn't exposed cleanly here; we instead
            // multiply geometry by `s` inside each draw call via the
            // helper at the bottom.
            when (category) {
                WarmCategory.Family -> drawFamily(s)
                WarmCategory.Friends -> drawFriends(s)
                WarmCategory.Work -> drawWork(s)
                WarmCategory.Milestones -> drawMilestones(s)
                WarmCategory.Community -> drawCommunity(s)
            }
        }
    }
}

// ── Family ──────────────────────────────────────────────────────────

private fun DrawScope.drawFamily(s: Float) {
    rect(Color(0xFFF4DDD2), 0f, 0f, 320f, 180f, s)
    // back paper plane
    pushRotated(28f, 18f, -5f, s) {
        rect(Color(0xFFEAB8AB), 0f, 0f, 180f, 120f, s, radius = 6f)
    }
    // photo frame
    pushRotated(58f, 30f, -3f, s) {
        rect(Color(0xFFF8EBE2), 0f, 0f, 150f, 110f, s, radius = 4f)
        rect(Color(0xFFD88679), 8f, 8f, 134f, 80f, s)
        // 4 abstract figures
        val figs = listOf(
            FigureSpec(40f, 56f, 14f, 28f, 20f, 26f, 68f, Color(0xFF9C3F3C)),
            FigureSpec(70f, 58f, 11f, 22f, 21f, 59f, 67f, Color(0xFFC57867)),
            FigureSpec(98f, 60f, 10f, 20f, 20f, 88f, 68f, Color(0xFF6E3030)),
            FigureSpec(124f, 62f, 9f, 18f, 18f, 115f, 70f, Color(0xFFA14A45)),
        )
        for (f in figs) {
            circle(Color(0xFFF4D9CC), f.cx, f.cy, f.headR, s)
            rect(f.color, f.bodyX, f.bodyY, f.bodyW, f.bodyH, s, radius = 4f)
        }
        // dash · diamond · dash keepsake mark under the photo
        val markColor = Color(0xFF6E3030).copy(alpha = 0.8f)
        strokeLine(markColor, 1.2f, 52f, 99f, 66f, 99f, s)
        strokeLine(markColor, 1.2f, 84f, 99f, 98f, 99f, s)
        pushRotated(75f, 99f, 45f, s) {
            rect(markColor, -3f, -3f, 6f, 6f, s, radius = 1f)
        }
    }
    // heart pin
    pushRotated(220f, 40f, 15f, s) {
        circle(Color(0xFFFAF4E2), 0f, 0f, 22f, s)
        path(Color(0xFF9C3F3C), s) {
            heartPath(this, -6f, -8f)
        }
    }
    // ribbon swoop
    strokeQuadPath(Color(0xFF9C3F3C).copy(alpha = 0.5f), 1.4f, s) {
        moveTo(48f, 140f)
        quadraticBezierTo(88f, 132f, 128f, 138f)
        quadraticBezierTo(168f, 144f, 220f, 138f)
    }
}

// ── Friends ─────────────────────────────────────────────────────────

private fun DrawScope.drawFriends(s: Float) {
    rect(Color(0xFFDDE7F0), 0f, 0f, 320f, 180f, s)
    // postcard — faux-script bars, no literal copy
    pushRotated(26f, 30f, -7f, s) {
        rect(Color(0xFFF4EAD7), 0f, 0f, 124f, 86f, s, radius = 5f)
        strokeLine(Color(0xFF3F5C7A).copy(alpha = 0.45f), 0.8f, 62f, 12f, 62f, 74f, s)
        rect(Color(0xFF3F5C7A).copy(alpha = 0.85f), 12f, 22f, 44f, 5f, s, radius = 1f)
        for ((y, w) in listOf(38f to 46f, 50f to 50f, 62f to 44f)) {
            rect(Color(0xFF6B5F4F).copy(alpha = 0.55f), 12f, y, w, 3f, s, radius = 1f)
        }
        // stamp
        rect(Color(0xFFC7D4E0), 74f, 12f, 36f, 24f, s)
        strokeRectDashed(Color(0xFF3F5C7A), 0.6f, 74f, 12f, 36f, 24f, s)
        circle(Color(0xFF3F5C7A), 92f, 24f, 5.5f, s)
    }
    // two friends catching up over coffee (faceless; cream heads,
    // identity carried by category blues — never skin tone)
    circle(Color(0xFFF4D9CC), 196f, 78f, 13f, s)
    path(Color(0xFF3F5C7A), s) {
        moveTo(178f, 124f)
        lineTo(178f, 106f)
        cubicTo(178f, 87f, 214f, 87f, 214f, 106f)
        lineTo(214f, 124f)
        close()
    }
    circle(Color(0xFFF4D9CC), 266f, 82f, 12f, s)
    path(Color(0xFF7E97B8), s) {
        moveTo(250f, 124f)
        lineTo(250f, 109f)
        cubicTo(250f, 92f, 282f, 92f, 282f, 109f)
        lineTo(282f, 124f)
        close()
    }
    // table
    rect(Color(0xFFF4EAD7), 170f, 122f, 124f, 7f, s, radius = 3.5f)
    // mugs on the table (one per friend; right mug mirrored)
    pushTranslated(212f, 106f, s) {
        mug(Color(0xFFFAF4E2), s)
        strokePath(Color(0xFF6B5F4F).copy(alpha = 0.5f), 1f, s) {
            moveTo(4f, -4f); quadraticBezierTo(6f, -7f, 4f, -10f)
            moveTo(10f, -4f); quadraticBezierTo(8f, -7f, 10f, -10f)
        }
    }
    pushTranslated(240f, 106f, s) {
        scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero) {
            mug(Color(0xFFA2693C), s)
        }
    }
    // speech bubble between them
    pushRotated(204f, 18f, 4f, s) {
        path(Color(0xFFFAF4E2), s) {
            moveTo(0f, 0f)
            lineTo(62f, 0f)
            quadraticBezierTo(70f, 0f, 70f, 8f)
            lineTo(70f, 26f)
            quadraticBezierTo(70f, 34f, 62f, 34f)
            lineTo(18f, 34f)
            lineTo(8f, 42f)
            lineTo(8f, 34f)
            quadraticBezierTo(0f, 34f, 0f, 26f)
            lineTo(0f, 8f)
            quadraticBezierTo(0f, 0f, 8f, 0f)
            close()
        }
        for (x in listOf(20f, 33f, 46f)) {
            circle(Color(0xFF3F5C7A), x, 17f, 2.2f, s)
        }
    }
}

/** Small table mug drawn in local space; body + handle. */
private fun DrawScope.mug(color: Color, s: Float) {
    path(color, s) {
        moveTo(0f, 0f)
        lineTo(14f, 0f)
        lineTo(14f, 12f)
        quadraticBezierTo(14f, 16f, 10f, 16f)
        lineTo(4f, 16f)
        quadraticBezierTo(0f, 16f, 0f, 12f)
        close()
    }
    strokePath(color, 2.2f, s) {
        moveTo(14f, 3f)
        quadraticBezierTo(19f, 7f, 14f, 11f)
    }
}

// ── Work ────────────────────────────────────────────────────────────

private fun DrawScope.drawWork(s: Float) {
    rect(Color(0xFFD5DDCB), 0f, 0f, 320f, 180f, s)
    strokeLine(Color(0xFF7A8A6E).copy(alpha = 0.5f), 0.7f, 0f, 140f, 320f, 140f, s)
    // envelope back
    pushRotated(20f, 26f, -6f, s) {
        rect(Color(0xFFEBE0C4), 0f, 0f, 170f, 108f, s, radius = 3f)
        strokePath(Color(0xFF9CA384), 1f, s) {
            moveTo(0f, 0f)
            lineTo(85f, 60f)
            lineTo(170f, 0f)
        }
    }
    // letter
    pushRotated(40f, 22f, -2f, s) {
        rect(Color(0xFFFAF4E2), 0f, 0f, 142f, 88f, s, radius = 2f)
        rect(Color(0xFF4F6B47), 14f, 14f, 50f, 6f, s, radius = 1f)
        strokeLine(Color(0xFF4F6B47), 1f, 14f, 22f, 120f, 22f, s)
        val widths = listOf(96f, 110f, 84f, 102f, 70f)
        for ((i, w) in widths.withIndex()) {
            val y = 34f + i * 10f
            strokeLine(Color(0xFFA8B19A), 0.8f, 14f, y, 14f + w, y, s)
        }
    }
    // check-in tile — check ring (text-free, matches iOS + design system)
    pushRotated(216f, 32f, 4f, s) {
        rect(Color(0xFFFAF4E2), 0f, 0f, 74f, 78f, s, radius = 6f)
        rect(Color(0xFF4F6B47), 0f, 0f, 74f, 22f, s, radius = 6f)
        rect(Color(0xFFFAF4E2).copy(alpha = 0.9f), 22f, 9f, 30f, 5f, s, radius = 1.5f)
        drawCircle(
            color = Color(0xFF4F6B47),
            radius = 13f * s,
            center = Offset(37f * s, 48f * s),
            style = Stroke(width = 2.4f * s),
        )
        strokeRoundPath(Color(0xFF4F6B47), 2.4f, s) {
            moveTo(31f, 48f)
            lineTo(35f, 52f)
            lineTo(43f, 43f)
        }
        rect(Color(0xFF6B5F4F).copy(alpha = 0.55f), 22f, 66f, 30f, 3f, s, radius = 1.5f)
    }
    // pencil
    pushRotated(50f, 138f, -4f, s) {
        rect(Color(0xFFA7791C), 0f, 0f, 120f, 8f, s, radius = 1f)
        path(Color(0xFFFAF4E2), s) {
            moveTo(120f, 0f)
            lineTo(132f, 4f)
            lineTo(120f, 8f)
            close()
        }
        path(Color(0xFF4A3A20), s) {
            moveTo(128f, 2f)
            lineTo(132f, 4f)
            lineTo(128f, 6f)
            close()
        }
        rect(Color(0xFF9C3F3C), 0f, 0f, 14f, 8f, s)
    }
}

// ── Milestones ──────────────────────────────────────────────────────

private fun DrawScope.drawMilestones(s: Float) {
    rect(Color(0xFFEDDEB6), 0f, 0f, 320f, 180f, s)
    // big calendar — abstract header marks + star on the day, no numerals
    pushRotated(28f, 24f, -4f, s) {
        rect(Color(0xFFFAF4E2), 0f, 0f, 160f, 130f, s, radius = 6f)
        rect(Color(0xFFA7791C), 0f, 0f, 160f, 24f, s, radius = 6f)
        val headerMark = Color(0xFFFAF4E2).copy(alpha = 0.9f)
        rect(headerMark, 48f, 9f, 42f, 6f, s, radius = 2f)
        circle(headerMark, 100f, 12f, 2.6f, s)
        rect(headerMark, 108f, 9f, 18f, 6f, s, radius = 2f)
        // grid 4 rows × 7 cols, highlight (1,3)
        for (r in 0 until 4) {
            for (c in 0 until 7) {
                val isHi = r == 1 && c == 3
                val x = 6f + c * 21.5f
                val y = 32f + r * 22f
                rect(
                    color = if (isHi) Color(0xFFA7791C) else Color(0xFFF2E7C7),
                    x = x,
                    y = y,
                    w = 20f,
                    h = 20f,
                    s = s,
                    radius = 3f,
                )
            }
        }
        // star on the big day
        path(Color(0xFFFAF4E2), s) {
            moveTo(80.5f, 58f)
            var px = 80.5f; var py = 58f
            for ((dx, dy) in listOf(
                2.2f to 4.4f, 4.9f to 0.7f, -3.5f to 3.5f, 0.8f to 4.9f,
                -4.4f to -2.3f, -4.4f to 2.3f, 0.8f to -4.9f, -3.5f to -3.5f, 4.9f to -0.7f,
            )) {
                px += dx; py += dy
                lineTo(px, py)
            }
            close()
        }
        // dash marks on a few busy days
        for ((x, y) in listOf(31f to 40f, 117f to 62f, 52.5f to 84f, 9.5f to 106f)) {
            rect(Color(0xFF6B5F4F).copy(alpha = 0.5f), x, y, 13f, 2.5f, s, radius = 1f)
        }
    }
    // confetti
    for ((spec, color) in listOf(
        Triple(225f, 27f, 20f) to Color(0xFF9C3F3C).copy(alpha = 0.8f),
        Triple(264f, 40f, -15f) to Color(0xFF4F6B47).copy(alpha = 0.7f),
        Triple(290f, 28f, 30f) to Color(0xFF3F5C7A).copy(alpha = 0.6f),
    )) {
        val (cx, cy, deg) = spec
        pushRotated(cx, cy, deg, s) {
            rect(color, -3f, -3f, 6f, 6f, s, radius = 1f)
        }
    }
    circle(Color(0xFFF5C842).copy(alpha = 0.9f), 246f, 20f, 3f, s)
    circle(Color(0xFFB26342).copy(alpha = 0.8f), 296f, 60f, 2.6f, s)
    // candle
    pushTranslated(208f, 40f, s) {
        rect(Color(0xFFB26342), 0f, 20f, 14f, 60f, s, radius = 2f)
        rect(Color(0xFFD87A52), 2f, 22f, 10f, 6f, s)
        strokeLine(Color(0xFF6B5F4F), 1f, 7f, 20f, 7f, 10f, s)
        path(Color(0xFFF5C842), s) {
            moveTo(7f, 10f)
            quadraticBezierTo(11f, 4f, 7f, -2f)
            quadraticBezierTo(3f, 4f, 7f, 10f)
            close()
        }
    }
    // gift box
    pushRotated(232f, 86f, 6f, s) {
        rect(Color(0xFF9C3F3C), 0f, 0f, 56f, 46f, s, radius = 3f)
        rect(Color(0xFF7B2E2C), 0f, 0f, 56f, 14f, s)
        rect(Color(0xFFF5C842), 22f, 0f, 12f, 46f, s)
        strokePath(Color(0xFFF5C842), 2.5f, s) {
            moveTo(22f, 0f)
            quadraticBezierTo(10f, -14f, 34f, -14f)
            moveTo(34f, 0f)
            quadraticBezierTo(46f, -14f, 22f, -14f)
        }
    }
}

// ── Community ───────────────────────────────────────────────────────

private fun DrawScope.drawCommunity(s: Float) {
    rect(Color(0xFFF0D4C2), 0f, 0f, 320f, 180f, s)
    // connecting threads
    strokeQuadPath(Color(0xFFB26342).copy(alpha = 0.55f), 1.2f, s) {
        moveTo(60f, 90f)
        quadraticBezierTo(100f, 50f, 160f, 70f)
        quadraticBezierTo(200f, 110f, 260f, 80f)
    }
    strokeQuadPath(Color(0xFFB26342).copy(alpha = 0.55f), 1.2f, s) {
        moveTo(120f, 40f)
        quadraticBezierTo(140f, 120f, 220f, 140f)
    }
    val people = listOf(
        PersonSpec(60f, 90f, 28f, Color(0xFF9C3F3C), Color(0xFFF4D9CC)),
        PersonSpec(130f, 50f, 24f, Color(0xFFA7791C), Color(0xFFEDDEB6)),
        PersonSpec(220f, 80f, 30f, Color(0xFF4F6B47), Color(0xFFD5DDCB)),
        PersonSpec(168f, 130f, 22f, Color(0xFF3F5C7A), Color(0xFFDDE7F0)),
        PersonSpec(268f, 134f, 20f, Color(0xFFB26342), Color(0xFFF0D4C2)),
    )
    for (p in people) {
        circle(p.rim, p.cx, p.cy, p.r + 6f, s)
        circle(p.fill, p.cx, p.cy, p.r, s)
        val headR = p.r * 0.28f
        circle(p.rim, p.cx, p.cy - p.r * 0.18f, headR, s)
    }
    // center heart
    circle(Color(0xFFFAF4E2), 160f, 98f, 14f, s)
    pushTranslated(160f, 98f, s) {
        path(Color(0xFFB26342), s) {
            heartPath(this, -4f, -5f)
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────

private data class FigureSpec(
    val cx: Float, val cy: Float, val headR: Float,
    val bodyW: Float, val bodyH: Float,
    val bodyX: Float, val bodyY: Float,
    val color: Color,
)

private data class PersonSpec(
    val cx: Float, val cy: Float, val r: Float,
    val fill: Color, val rim: Color,
)

private fun DrawScope.rect(
    color: Color, x: Float, y: Float, w: Float, h: Float, s: Float, radius: Float = 0f,
) {
    if (radius > 0f) {
        drawRoundRect(
            color = color,
            topLeft = Offset(x * s, y * s),
            size = Size(w * s, h * s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * s, radius * s),
        )
    } else {
        drawRect(color, Offset(x * s, y * s), Size(w * s, h * s))
    }
}

private fun DrawScope.circle(color: Color, cx: Float, cy: Float, r: Float, s: Float) {
    drawCircle(color = color, radius = r * s, center = Offset(cx * s, cy * s))
}

private fun DrawScope.ellipse(color: Color, x: Float, y: Float, w: Float, h: Float, s: Float) {
    drawOval(color = color, topLeft = Offset(x * s, y * s), size = Size(w * s, h * s))
}

private fun DrawScope.strokeLine(color: Color, w: Float, x1: Float, y1: Float, x2: Float, y2: Float, s: Float) {
    drawLine(color = color, start = Offset(x1 * s, y1 * s), end = Offset(x2 * s, y2 * s), strokeWidth = w * s)
}

private fun DrawScope.strokeRectDashed(
    color: Color, w: Float, x: Float, y: Float, width: Float, height: Float, s: Float,
) {
    val path = Path().apply {
        addRect(androidx.compose.ui.geometry.Rect(x * s, y * s, (x + width) * s, (y + height) * s))
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = w * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * s, 1.4f * s))),
    )
}

private inline fun DrawScope.pushTranslated(dx: Float, dy: Float, s: Float, block: DrawScope.() -> Unit) {
    translate(dx * s, dy * s) { block() }
}

private inline fun DrawScope.pushRotated(dx: Float, dy: Float, degrees: Float, s: Float, block: DrawScope.() -> Unit) {
    translate(dx * s, dy * s) {
        rotate(degrees = degrees, pivot = Offset.Zero) { block() }
    }
}

private inline fun DrawScope.path(color: Color, s: Float, build: Path.() -> Unit) {
    val p = Path().apply(build)
    // Scale path by multiplying coordinates we pass into it.
    drawPath(p.scaled(s), color = color)
}

private inline fun DrawScope.strokePath(color: Color, w: Float, s: Float, build: Path.() -> Unit) {
    val p = Path().apply(build)
    drawPath(p.scaled(s), color = color, style = Stroke(width = w * s))
}

private inline fun DrawScope.strokeQuadPath(color: Color, w: Float, s: Float, build: Path.() -> Unit) {
    val p = Path().apply(build)
    drawPath(p.scaled(s), color = color, style = Stroke(width = w * s))
}

private inline fun DrawScope.strokeRoundPath(color: Color, w: Float, s: Float, build: Path.() -> Unit) {
    val p = Path().apply(build)
    drawPath(
        p.scaled(s),
        color = color,
        style = Stroke(width = w * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun heartPath(p: Path, dx: Float, dy: Float) {
    p.moveTo(dx + 0f, dy + -3f)
    p.quadraticBezierTo(dx + 4f, dy + -10f, dx + 8f, dy + -3f)
    p.quadraticBezierTo(dx + 8f, dy + 4f, dx + 4f, dy + 9f)
    p.quadraticBezierTo(dx + 0f, dy + 4f, dx + 0f, dy + -3f)
    p.close()
}

private fun Path.scaled(s: Float): Path {
    val m = android.graphics.Matrix().apply { setScale(s, s) }
    val result = Path()
    result.addPath(this)
    result.asAndroidPath().transform(m)
    return result
}
