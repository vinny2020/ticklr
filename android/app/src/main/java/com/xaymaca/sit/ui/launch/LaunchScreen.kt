package com.xaymaca.sit.ui.launch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xaymaca.sit.ui.theme.SITTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// Ticklr blue palette — matches the launcher icon exactly
private val BG_COLOR      = Color(0xFF1C3A62)
private val BUBBLE_FILL   = Color(0xFF5891C3)
private val BUBBLE_STROKE = Color(0xFF1C3762)
private val DOT_COLOR     = Color(0xFF34527A)
private val HAND_COLOR    = Color(0xFF19325A)

@Composable
fun LaunchScreen(onComplete: () -> Unit) {
    var animationStarted by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logo_alpha"
    )

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(2000)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG_COLOR),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            drawTicklrLogo(alpha = alpha)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C3A62)
@Composable
fun LaunchScreenPreview() {
    SITTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BG_COLOR),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawTicklrLogo(alpha = 1f)
            }
        }
    }
}

private fun DrawScope.drawTicklrLogo(alpha: Float) {
    val w = size.width
    val h = size.height
    val pad  = w * 0.05f
    val bx   = pad
    val by   = h * 0.04f
    val bw   = w - pad * 2f
    val bh   = h * 0.52f
    val cr   = bh * 0.20f   // corner radius
    val sw   = w * 0.018f   // stroke width

    // ── Chat bubble ──────────────────────────────────────────────
    val bubblePath = Path().apply {
        addRoundRect(RoundRect(rect = Rect(bx, by, bx + bw, by + bh), radiusX = cr, radiusY = cr))
    }
    drawPath(path = bubblePath, color = BUBBLE_FILL.copy(alpha = alpha))

    // Tail
    val tailCX = bx + bw * 0.35f
    val tailCY = by + bh + h * 0.10f
    val tailPath = Path().apply {
        moveTo(tailCX - bh * 0.13f, by + bh - 2f)
        lineTo(tailCX + bh * 0.13f, by + bh - 2f)
        lineTo(tailCX, tailCY)
        close()
    }
    drawPath(path = tailPath, color = BUBBLE_FILL.copy(alpha = alpha))

    // Bubble + tail stroke
    drawPath(path = bubblePath, color = BUBBLE_STROKE.copy(alpha = alpha), style = Stroke(width = sw))
    drawLine(BUBBLE_STROKE.copy(alpha = alpha),
        start = Offset(tailCX - bh * 0.13f, by + bh - 2f), end = Offset(tailCX, tailCY),
        strokeWidth = sw * 0.8f, cap = StrokeCap.Round)
    drawLine(BUBBLE_STROKE.copy(alpha = alpha),
        start = Offset(tailCX + bh * 0.13f, by + bh - 2f), end = Offset(tailCX, tailCY),
        strokeWidth = sw * 0.8f, cap = StrokeCap.Round)

    // Three dots inside bubble
    val dotR    = w * 0.045f
    val dotY    = by + bh * 0.50f
    val spacing = bw * 0.20f
    val dotCX   = bx + bw / 2f
    for (i in -1..1) {
        drawCircle(color = DOT_COLOR.copy(alpha = alpha), radius = dotR,
            center = Offset(dotCX + i * spacing, dotY))
    }

    // ── Clock ────────────────────────────────────────────────────
    val clockR  = w * 0.22f
    val clockCX = bx + bw * 0.74f
    val clockCY = by + bh + h * 0.04f

    drawCircle(color = BUBBLE_FILL.copy(alpha = alpha), radius = clockR, center = Offset(clockCX, clockCY))
    drawCircle(color = BUBBLE_STROKE.copy(alpha = alpha), radius = clockR,
        center = Offset(clockCX, clockCY), style = Stroke(width = sw))

    // Tick marks
    for (i in 0..11) {
        val angle = Math.toRadians((i * 30 - 90).toDouble())
        val r0 = if (i % 3 == 0) clockR * 0.70f else clockR * 0.80f
        val lw = if (i % 3 == 0) maxOf(2f, clockR / 14f) else maxOf(1f, clockR / 22f)
        drawLine(color = HAND_COLOR.copy(alpha = alpha),
            start = Offset(clockCX + r0 * cos(angle).toFloat(), clockCY + r0 * sin(angle).toFloat()),
            end   = Offset(clockCX + clockR * 0.90f * cos(angle).toFloat(), clockCY + clockR * 0.90f * sin(angle).toFloat()),
            strokeWidth = lw, cap = StrokeCap.Round)
    }

    // Hour hand (~10 o'clock)
    val angH = Math.toRadians(-60.0)
    drawLine(color = HAND_COLOR.copy(alpha = alpha),
        start = Offset(clockCX, clockCY),
        end   = Offset(clockCX + clockR * 0.50f * cos(angH).toFloat(), clockCY + clockR * 0.50f * sin(angH).toFloat()),
        strokeWidth = maxOf(3f, clockR / 8f), cap = StrokeCap.Round)

    // Minute hand (~2 o'clock)
    val angM = Math.toRadians(60.0)
    drawLine(color = HAND_COLOR.copy(alpha = alpha),
        start = Offset(clockCX, clockCY),
        end   = Offset(clockCX + clockR * 0.67f * cos(angM).toFloat(), clockCY + clockR * 0.67f * sin(angM).toFloat()),
        strokeWidth = maxOf(2f, clockR / 11f), cap = StrokeCap.Round)

    // Center pivot dot
    drawCircle(color = HAND_COLOR.copy(alpha = alpha),
        radius = maxOf(3f, clockR / 9f), center = Offset(clockCX, clockCY))
}
