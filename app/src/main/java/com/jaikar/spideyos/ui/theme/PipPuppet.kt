package com.jaikar.spideyos.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.R

/** In-app SpideyDashPip — same character art as the floating buddy. WeaveHome theme. */
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
            Image(
                painter = painterResource(R.drawable.spideydashpip),
                contentDescription = AppCredits.MASCOT_NAME,
                modifier = Modifier
                    .size(110.dp)
                    .scale(1f + bob * 0.03f),
                contentScale = ContentScale.Fit,
            )
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
            "Tap · local buddy · no API",
            color = SpideyWeb.copy(alpha = 0.5f),
            fontSize = 11.sp,
        )
    }
}
