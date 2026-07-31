package com.jaikar.spideyos.ui.vibe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.ui.theme.PipMint
import com.jaikar.spideyos.ui.theme.SpideyBlue
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyWeb
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Cinematic intro gate — WeaveHome vibe before home. */
@Composable
fun VibeSplashScreen(
    onFinished: () -> Unit,
) {
    val title = remember { Animatable(0f) }
    val ring = rememberInfiniteTransition(label = "ring")
    val pulse by ring.animateFloat(
        0.85f,
        1.12f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val spin by ring.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(8000), RepeatMode.Restart),
        label = "spin",
    )

    LaunchedEffect(Unit) {
        title.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(1600)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(SpideyBlue, PipMint.copy(alpha = 0.35f), SpideyBlue)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().scale(pulse)) {
            val c = Offset(size.width / 2f, size.height * 0.42f)
            val maxR = size.minDimension * 0.28f
            for (i in 1..5) {
                drawCircle(
                    color = SpideyWeb.copy(alpha = 0.12f),
                    radius = maxR * i / 5f,
                    center = c,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f),
                )
            }
            for (i in 0 until 12) {
                val a = Math.toRadians((spin + i * 30).toDouble())
                drawLine(
                    SpideyGold.copy(alpha = 0.35f),
                    c,
                    Offset(c.x + maxR * cos(a).toFloat(), c.y + maxR * sin(a).toFloat()),
                    strokeWidth = 2f,
                )
            }
            drawCircle(PipMint.copy(alpha = 0.9f), radius = 36f, center = c)
            drawCircle(SpideyGold, radius = 10f, center = c)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .alpha(title.value)
                .scale(0.92f + 0.08f * title.value),
        ) {
            Text(AppCredits.APP_NAME, color = SpideyWeb, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Pip is weaving your home…", color = SpideyGold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(AppCredits.CREDIT_LINE, color = SpideyWeb.copy(alpha = 0.65f), fontSize = 12.sp)
        }
    }
}
