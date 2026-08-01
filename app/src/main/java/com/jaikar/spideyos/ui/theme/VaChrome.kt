package com.jaikar.spideyos.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VaAtmosphere(modifier: Modifier = Modifier) {
    val drift = rememberInfiniteTransition(label = "vaAtm")
    val phase by drift.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(VaMist)
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VaSky.copy(alpha = 0.95f), Color.Transparent),
                center = Offset(w * (0.18f + phase * 0.04f), h * 0.12f),
                radius = w * 0.55f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VaSoft.copy(alpha = 0.9f), Color.Transparent),
                center = Offset(w * (0.82f - phase * 0.03f), h * 0.28f),
                radius = w * 0.48f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VaMint.copy(alpha = 0.12f + phase * 0.05f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = w * 0.42f,
            ),
        )
    }
}

@Composable
fun WebBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 0.85f,
) {
    VaAtmosphere(modifier = modifier)
    @Suppress("UNUSED_EXPRESSION")
    intensity
}

@Composable
fun NestSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(VaCloud)
            .border(1.dp, VaLine, RoundedCornerShape(28.dp))
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun VaPill(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (selected) VaMint else VaCloud
    val fg = if (selected) Color.White else VaInk
    Text(
        text = text,
        color = fg,
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = modifier
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) VaMint else VaLine, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun VaPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(VaMint, VaMintDeep)))
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = Color.White, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun VaGhostButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = VaInk,
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = modifier
            .clip(CircleShape)
            .border(1.5.dp, VaLine, CircleShape)
            .background(VaCloud.copy(alpha = 0.65f))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    )
}

@Composable
fun VaHeroOrb(
    size: Dp = 200.dp,
    content: @Composable () -> Unit,
) {
    val breathe = rememberInfiniteTransition(label = "orb")
    val pulse by breathe.animateFloat(
        0.92f, 1.05f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val ring by breathe.animateFloat(
        0.55f, 0.9f,
        infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label = "ring",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Box(
            Modifier
                .size(size)
                .scale(pulse)
                .background(
                    Brush.radialGradient(listOf(VaMint.copy(alpha = 0.22f * ring), Color.Transparent)),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .size(size * 0.82f)
                .border(2.dp, VaMint.copy(alpha = 0.35f * ring), CircleShape),
        )
        Box(
            Modifier
                .size(size * 0.68f)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(VaCloud)
                .border(1.dp, VaSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
fun VaSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = VaMuted,
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        modifier = modifier,
    )
}

@Composable
fun VaIconTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.95f), tint.copy(alpha = 0.7f))))
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, title, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = VaInk, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
fun MiniWebBadge(modifier: Modifier = Modifier, color: Color = VaMint) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2.2f
        drawCircle(color = color.copy(alpha = 0.2f), radius = r, center = c)
        drawCircle(color = color.copy(alpha = 0.45f), radius = r * 0.55f, center = c)
    }
}
