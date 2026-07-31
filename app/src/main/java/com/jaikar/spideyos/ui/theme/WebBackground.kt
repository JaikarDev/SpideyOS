package com.jaikar.spideyos.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    SpideyBlue,
                    SpideyNavy.copy(alpha = 0.95f),
                    SpideyRed.copy(alpha = 0.35f * intensity),
                ),
            ),
        )
        val cx = w / 2f
        val cy = h * 0.38f
        val maxR = min(w, h) * 0.55f * intensity
        val webColor = SpideyWeb.copy(alpha = 0.22f * intensity)
        val stroke = Stroke(width = 2f)
        for (ring in 1..5) {
            val r = maxR * ring / 5f
            drawCircle(color = webColor, radius = r, center = Offset(cx, cy), style = stroke)
        }
        val spokes = 8
        for (i in 0 until spokes) {
            val angle = Math.PI * 2 * i / spokes
            val x = cx + (maxR * cos(angle)).toFloat()
            val y = cy + (maxR * sin(angle)).toFloat()
            drawLine(webColor, Offset(cx, cy), Offset(x, y), strokeWidth = 2f)
        }
        // corner webs
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                quadraticBezierTo(w * 0.15f, h * 0.05f, w * 0.28f, 0f)
                moveTo(0f, 0f)
                quadraticBezierTo(w * 0.05f, h * 0.18f, 0f, h * 0.3f)
            },
            color = webColor,
            style = Stroke(width = 2.5f),
        )
        drawPath(
            path = Path().apply {
                moveTo(w, 0f)
                quadraticBezierTo(w * 0.85f, h * 0.05f, w * 0.72f, 0f)
                moveTo(w, 0f)
                quadraticBezierTo(w * 0.95f, h * 0.18f, w, h * 0.3f)
            },
            color = webColor,
            style = Stroke(width = 2.5f),
        )
    }
}

@Composable
fun MiniWebBadge(modifier: Modifier = Modifier, color: Color = SpideyWeb) {
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
