package com.jaikar.spideyos.ui.launcher

import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.apps.AppCatalog
import com.jaikar.spideyos.assistant.PupBrain
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.sense.WeaveSense
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.adaptive.rememberSpideyWindowInfo
import com.jaikar.spideyos.ui.theme.PipPuppet
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyRed
import com.jaikar.spideyos.ui.theme.SpideyWeb
import com.jaikar.spideyos.ui.theme.WebBackground
import com.jaikar.spideyos.ui.theme.WebShootOpenAnimation
import java.util.Date

data class HomeModule(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

@Composable
fun LauncherScreen(
    settings: SpideySettings,
    onOpenAssistant: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenMail: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val window = rememberSpideyWindowInfo()
    val webScale = remember { Animatable(0.2f) }
    val sensePulse = rememberInfiniteTransition(label = "sense")
    val senseGlow by sensePulse.animateFloat(
        0.35f,
        1f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "senseGlow",
    )

    LaunchedEffect(Unit) {
        webScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        WeaveSense.wag(context)
    }

    val modules = listOf(
        HomeModule("Pip", Icons.Default.Pets, SpideyGold) {
            WeaveSense.tap(context); onOpenAssistant()
        },
        HomeModule("ThreadBox", Icons.AutoMirrored.Filled.Chat, SpideyRed) {
            WeaveSense.shoot(context); onOpenMessages()
        },
        HomeModule("Inbox", Icons.Default.Email, Color(0xFF5B8CFF)) {
            WeaveSense.sense(context); onOpenMail()
        },
        HomeModule("SnapBooth", Icons.Default.CameraAlt, Color(0xFFFF8A5B)) {
            WeaveSense.tap(context); onOpenCamera()
        },
        HomeModule("Nest", Icons.Default.Settings, SpideyWeb) {
            WeaveSense.tap(context); onOpenSettings()
        },
    )

    val apps = remember { AppCatalog.loadLaunchableApps(context.packageManager) }
    val time = remember {
        DateFormat.getTimeFormat(context).format(Date())
    }

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = settings.themeIntensity)
        WebShootOpenAnimation()
        AdaptiveContent {
            Column(Modifier.fillMaxSize()) {
                // Unique OS status chrome
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SpideyNavy.copy(alpha = 0.82f))
                        .border(1.dp, SpideyGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(time, color = SpideyWeb, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .scale(0.85f + 0.15f * senseGlow)
                                .background(SpideyGold.copy(alpha = senseGlow), CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("WeaveSense", color = SpideyGold.copy(alpha = senseGlow), fontSize = 11.sp)
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Default.Wifi, null, tint = SpideyWeb, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.BatteryFull, null, tint = SpideyWeb, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    AppCredits.APP_NAME,
                    color = SpideyWeb,
                    fontSize = window.titleSp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.scale(webScale.value),
                )
                Text(
                    "Hey ${settings.userName} — your woven home is live.",
                    color = SpideyGold,
                    fontSize = window.bodySp,
                )
                Text(
                    AppCredits.CREDIT_LINE,
                    color = SpideyWeb.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                )

                Spacer(Modifier.height(6.dp))
                PipPuppet(
                    moodText = PupBrain.moodLine(settings.userName),
                    onTap = {
                        WeaveSense.sense(context)
                        onOpenAssistant()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                // Unique dock — glossy module launcher
                Row(
                    Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(SpideyNavy.copy(alpha = 0.95f), Color(0xFF0A1F26).copy(alpha = 0.98f)),
                            ),
                        )
                        .border(1.dp, SpideyWeb.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    modules.forEach { mod ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = mod.onClick)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .shadow(6.dp, CircleShape)
                                    .background(
                                        Brush.radialGradient(listOf(mod.tint.copy(alpha = 0.95f), mod.tint.copy(alpha = 0.55f))),
                                        CircleShape,
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(mod.icon, mod.title, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(mod.title, color = SpideyWeb, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Apps", color = SpideyWeb.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = window.gridMinCell),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    WeaveSense.tap(context)
                                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                                        context.startActivity(it)
                                    }
                                }
                                .padding(4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(if (window.isTablet) 64.dp else 56.dp)
                                    .shadow(8.dp, RoundedCornerShape(18.dp))
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SpideyNavy)
                                    .border(1.dp, SpideyGold.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                val bmp = app.icon
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(12.dp)),
                                    )
                                } else {
                                    Text(
                                        app.label.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                app.label,
                                color = SpideyWeb,
                                fontSize = if (window.smallestWidthDp < 360) 10.sp else 11.sp,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

