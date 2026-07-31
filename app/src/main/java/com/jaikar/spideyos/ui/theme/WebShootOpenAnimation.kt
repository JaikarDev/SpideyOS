package com.jaikar.spideyos.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin

/**
 * One-shot web-shoot burst when the launcher (or a screen) opens.
 */
@Composable
fun WebShootOpenAnimation(
    modifier: Modifier = Modifier,
    triggerKey: Any = Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(850, easing = LinearOutSlowInEasing))
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        if (p <= 0f || p >= 1f) return@Canvas
        val cx = size.width / 2f
        val cy = size.height * 0.36f
        val maxR = size.minDimension * 0.55f * p
        val alpha = (1f - p) * 0.75f
        val color = SpideyWeb.copy(alpha = alpha)
        val gold = SpideyGold.copy(alpha = alpha)
        for (i in 0 until 12) {
            val a = Math.PI * 2 * i / 12.0
            val x = cx + maxR * cos(a).toFloat()
            val y = cy + maxR * sin(a).toFloat()
            drawLine(color, Offset(cx, cy), Offset(x, y), strokeWidth = 3.5f, cap = StrokeCap.Round)
        }
        drawCircle(color = gold, radius = 10f + 28f * p, center = Offset(cx, cy), alpha = alpha)
        // corner shoots
        drawLine(color, Offset(0f, 0f), Offset(size.width * 0.35f * p, size.height * 0.22f * p), 3f)
        drawLine(color, Offset(size.width, 0f), Offset(size.width - size.width * 0.35f * p, size.height * 0.22f * p), 3f)
    }
}
