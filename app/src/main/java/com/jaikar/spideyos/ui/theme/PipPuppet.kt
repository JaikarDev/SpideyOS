package com.jaikar.spideyos.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.R

/** In-app Spidy — hero orb for VA home. */
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

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        VaHeroOrb(size = 188.dp) {
            Image(
                painter = painterResource(R.drawable.spideydashpip),
                contentDescription = AppCredits.MASCOT_NAME,
                modifier = Modifier
                    .size(118.dp)
                    .scale(1f + bob * 0.04f)
                    .clickable(onClick = onTap),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Spidy",
            color = VaInk,
            fontFamily = Fraunces,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
        )
        Text(
            moodText,
            color = VaMuted,
            fontFamily = Outfit,
            fontSize = 13.sp,
        )
    }
}
