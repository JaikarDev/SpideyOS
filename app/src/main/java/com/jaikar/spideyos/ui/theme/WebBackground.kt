package com.jaikar.spideyos.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun WebBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 0.85f,
) {
    val drift = rememberInfiniteTransition(label = "weaveBg")
    val phase by drift.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "phase",
    )
    val glow by drift.animateFloat(
        0.18f,
        0.32f,
        infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "glow",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    SpideyBlue,
                    SpideyNavy.copy(alpha = 0.95f),
                    SpideyRed.copy(alpha = 0.28f * intensity + glow * 0.4f * intensity),
                ),
            ),
        )
        val cx = w / 2f + (phase - 0.5f) * 28f
        val cy = h * 0.38f + (0.5f - phase) * 18f
        val maxR = min(w, h) * 0.55f * intensity * (0.96f + phase * 0.08f)
        val webColor = SpideyWeb.copy(alpha = glow * intensity)
        val stroke = Stroke(width = 2.2f)
        for (ring in 1..5) {
            val r = maxR * ring / 5f
            drawCircle(color = webColor, radius = r, center = Offset(cx, cy), style = stroke)
        }
        val spokes = 8
        for (i in 0 until spokes) {
            val angle = Math.PI * 2 * i / spokes + phase * 0.15
            val x = cx + (maxR * cos(angle)).toFloat()
            val y = cy + (maxR * sin(angle)).toFloat()
            drawLine(webColor, Offset(cx, cy), Offset(x, y), strokeWidth = 2f)
        }
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                quadraticTo(w * 0.15f, h * 0.05f * (1f + phase * 0.3f), w * 0.28f, 0f)
                moveTo(0f, 0f)
                quadraticTo(w * 0.05f, h * 0.18f, 0f, h * 0.3f)
            },
            color = webColor,
            style = Stroke(width = 2.5f),
        )
        drawPath(
            path = Path().apply {
                moveTo(w, 0f)
                quadraticTo(w * 0.85f, h * 0.05f, w * 0.72f, 0f)
                moveTo(w, 0f)
                quadraticTo(w * 0.95f, h * 0.18f, w, h * 0.3f)
            },
            color = webColor,
            style = Stroke(width = 2.5f),
        )
    }
}

@Composable
fun MiniWebBadge(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = SpideyWeb) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2.2f
        val stroke = Stroke(width = 2f)
        drawCircle(color = color.copy(alpha = 0.35f), radius = r, center = c, style = stroke)
        drawCircle(color = color.copy(alpha = 0.35f), radius = r * 0.55f, center = c, style = stroke)
        for (i in 0 until 6) {
            val a = Math.PI * 2 * i / 6
            drawLine(
                color.copy(alpha = 0.5f),
                c,
                Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()),
                strokeWidth = 2f,
            )
        }
    }
}
