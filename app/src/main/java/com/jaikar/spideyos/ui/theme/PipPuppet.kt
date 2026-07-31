package com.jaikar.spideyos.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import kotlin.math.sin

/** Immersive on-home Pip puppet — original mascot, no franchise likeness. */
@Composable
fun PipPuppet(
    moodText: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val breathe = rememberInfiniteTransition(label = "pip")
    val bob by breathe.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val wag by breathe.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "wag",
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .offset(y = (bob * -10f).dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(SpideyNavy.copy(alpha = 0.85f))
                .clickable(onClick = onTap)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(110.dp).scale(1f + bob * 0.03f)) {
                val c = Offset(size.width / 2f, size.height / 2f + 6f)
                // body
                drawCircle(PipMint, radius = size.minDimension * 0.38f, center = c)
                // ears
                val ear = Path().apply {
                    moveTo(c.x - 34f, c.y - 20f)
                    quadraticBezierTo(c.x - 48f, c.y - 55f, c.x - 18f, c.y - 38f)
                    close()
                }
                drawPath(ear, PipMint)
                val ear2 = Path().apply {
                    moveTo(c.x + 34f, c.y - 20f)
                    quadraticBezierTo(c.x + 48f, c.y - 55f, c.x + 18f, c.y - 38f)
                    close()
                }
                drawPath(ear2, PipMint)
                // eyes
                drawCircle(SpideyWeb, radius = 7f, center = Offset(c.x - 14f, c.y - 6f))
                drawCircle(SpideyWeb, radius = 7f, center = Offset(c.x + 14f, c.y - 6f))
                drawCircle(SpideyBlue, radius = 3.2f, center = Offset(c.x - 14f, c.y - 5f))
                drawCircle(SpideyBlue, radius = 3.2f, center = Offset(c.x + 14f, c.y - 5f))
                // nose
                drawCircle(SpideyGold, radius = 5f, center = Offset(c.x, c.y + 8f))
                // tail wag
                val tx = c.x + 42f
                val ty = c.y + 10f + sin(Math.toRadians(wag.toDouble())).toFloat() * 10f
                drawLine(SpideyGold, Offset(c.x + 28f, c.y + 18f), Offset(tx, ty), strokeWidth = 6f)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            AppCredits.MASCOT_NAME,
            color = SpideyGold,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Text(
            moodText,
            color = SpideyWeb,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SpideyNavy.copy(alpha = 0.75f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Text(
            "Tap Pip · local companion · no API",
            color = SpideyWeb.copy(alpha = 0.5f),
            fontSize = 11.sp,
        )
    }
}
