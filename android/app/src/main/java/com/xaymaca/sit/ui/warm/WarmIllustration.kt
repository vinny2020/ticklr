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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    // postcard
    pushRotated(34f, 28f, -7f, s) {
        rect(Color(0xFFF4EAD7), 0f, 0f, 140f, 96f, s, radius = 5f)
        strokeLine(Color(0xFF3F5C7A).copy(alpha = 0.45f), 0.8f, 70f, 14f, 70f, 82f, s)
        val widths = listOf(56f, 60f, 68f, 60f)
        for ((i, w) in widths.withIndex()) {
            val y = 28f + i * 14f
            val color = if (i == 0) Color(0xFF3F5C7A).copy(alpha = 0.85f) else Color(0xFF6B5F4F).copy(alpha = 0.55f)
            val h = if (i == 0) 5f else 3f
            rect(color, 14f, y, w, h, s, radius = 1f)
        }
        // stamp
        rect(Color(0xFFC7D4E0), 84f, 14f, 40f, 26f, s)
        strokeRectDashed(Color(0xFF3F5C7A), 0.6f, 84f, 14f, 40f, 26f, s)
        circle(Color(0xFF3F5C7A), 104f, 27f, 6f, s)
    }
    // mug A
    pushTranslated(176f, 50f, s) {
        path(Color(0xFFF4EAD7), s) {
            moveTo(0f, 0f)
            lineTo(36f, 0f)
            lineTo(36f, 34f)
            quadraticBezierTo(36f, 42f, 28f, 42f)
            lineTo(8f, 42f)
            quadraticBezierTo(0f, 42f, 0f, 34f)
            close()
        }
        strokePath(Color(0xFFF4EAD7), 4f, s) {
            moveTo(36f, 8f)
            quadraticBezierTo(48f, 17f, 36f, 26f)
        }
        ellipse(Color(0xFFA2693C), 0f, -1f, 36f, 6f, s)
    }
    // mug B
    pushTranslated(232f, 58f, s) {
        path(Color(0xFF3F5C7A), s) {
            moveTo(0f, 0f)
            lineTo(30f, 0f)
            lineTo(30f, 28f)
            quadraticBezierTo(30f, 35f, 23f, 35f)
            lineTo(7f, 35f)
            quadraticBezierTo(0f, 35f, 0f, 28f)
            close()
        }
        strokePath(Color(0xFF3F5C7A), 3.5f, s) {
            moveTo(30f, 6f)
            quadraticBezierTo(40f, 14f, 30f, 22f)
        }
        ellipse(Color(0xFF9D7A52), 0f, -1f, 30f, 5f, s)
    }
    // speech bubble
    pushRotated(212f, 12f, 8f, s) {
        path(Color(0xFFFAF4E2), s) {
            moveTo(0f, 0f)
            lineTo(66f, 0f)
            quadraticBezierTo(74f, 0f, 74f, 8f)
            lineTo(74f, 28f)
            quadraticBezierTo(74f, 36f, 66f, 36f)
            lineTo(16f, 36f)
            lineTo(6f, 44f)
            lineTo(6f, 36f)
            quadraticBezierTo(0f, 36f, 0f, 28f)
            lineTo(0f, 8f)
            quadraticBezierTo(0f, 0f, 6f, 0f)
            close()
        }
        for (x in listOf(18f, 32f, 46f)) {
            circle(Color(0xFF3F5C7A), x, 18f, 2.2f, s)
        }
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
    // calendar tile
    pushRotated(216f, 32f, 4f, s) {
        rect(Color(0xFFFAF4E2), 0f, 0f, 74f, 78f, s, radius = 6f)
        rect(Color(0xFF4F6B47), 0f, 0f, 74f, 22f, s, radius = 6f)
        // Q3 text is the calendar's headline — Canvas .drawText isn't
        // trivial in Compose; skipped for v1 (icon-only milestone tile).
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
    // big calendar
    pushRotated(28f, 24f, -4f, s) {
        rect(Color(0xFFFAF4E2), 0f, 0f, 160f, 130f, s, radius = 6f)
        rect(Color(0xFFA7791C), 0f, 0f, 160f, 24f, s, radius = 6f)
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
    }
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
