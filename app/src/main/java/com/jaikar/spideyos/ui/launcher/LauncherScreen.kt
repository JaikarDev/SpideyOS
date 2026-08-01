package com.jaikar.spideyos.ui.launcher

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.apps.AppCatalog
import com.jaikar.spideyos.assistant.PupBrain
import com.jaikar.spideyos.assistant.SpideyOverlayService
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.sense.WeaveSense
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.adaptive.rememberSpideyWindowInfo
import com.jaikar.spideyos.ui.theme.Fraunces
import com.jaikar.spideyos.ui.theme.Outfit
import com.jaikar.spideyos.ui.theme.PipPuppet
import com.jaikar.spideyos.ui.theme.VaAtmosphere
import com.jaikar.spideyos.ui.theme.VaCoral
import com.jaikar.spideyos.ui.theme.VaGhostButton
import com.jaikar.spideyos.ui.theme.VaIconTile
import com.jaikar.spideyos.ui.theme.VaInk
import com.jaikar.spideyos.ui.theme.VaMint
import com.jaikar.spideyos.ui.theme.VaMuted
import com.jaikar.spideyos.ui.theme.VaPill
import com.jaikar.spideyos.ui.theme.VaPrimaryButton
import com.jaikar.spideyos.ui.theme.VaSectionLabel
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
    onOpenToday: () -> Unit = {},
) {
    val context = LocalContext.current
    val window = rememberSpideyWindowInfo()
    val heroScale = remember { Animatable(0.88f) }
    val time = remember { DateFormat.getTimeFormat(context).format(Date()) }
    val apps = remember { AppCatalog.loadLaunchableApps(context.packageManager) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            WeaveSense.spidySense(context)
            SpideyOverlayService.stop(context)
            SpideyOverlayService.start(context)
        }
    }

    fun startBuddy() {
        WeaveSense.spidySense(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            SpideyOverlayService.stop(context)
            SpideyOverlayService.start(context)
        }
    }

    LaunchedEffect(Unit) {
        heroScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        WeaveSense.wag(context)
    }

    val modules = listOf(
        HomeModule("Buddy", Icons.Default.Pets, VaMint) {
            WeaveSense.tap(context); onOpenAssistant()
        },
        HomeModule("Today", Icons.Default.Today, Color(0xFF5BB8C8)) {
            WeaveSense.tap(context); onOpenToday()
        },
        HomeModule("Inbox", Icons.Default.Email, Color(0xFF6B8CFF)) {
            WeaveSense.sense(context); onOpenMail()
        },
        HomeModule("Chat", Icons.AutoMirrored.Filled.Chat, VaCoral) {
            WeaveSense.shoot(context); onOpenMessages()
        },
        HomeModule("Snap", Icons.Default.CameraAlt, Color(0xFFFF9B5B)) {
            WeaveSense.tap(context); onOpenCamera()
        },
        HomeModule("Nest", Icons.Default.Settings, VaMuted) {
            WeaveSense.tap(context); onOpenSettings()
        },
    )

    Box(Modifier.fillMaxSize()) {
        VaAtmosphere()
        WebShootOpenAnimation()
        AdaptiveContent {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        time,
                        color = VaMuted,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Nest", tint = VaInk)
                    }
                }

                // Hero composition — brand first
                Column(
                    Modifier
                        .fillMaxWidth()
                        .scale(heroScale.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Spidy",
                        color = VaInk,
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                    )
                    Text(
                        "Hey ${settings.userName}",
                        color = VaInk,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your on-phone companion — local, quiet, ready.",
                        color = VaMuted,
                        fontFamily = Outfit,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    PipPuppet(
                        moodText = PupBrain.moodLine(settings.userName),
                        onTap = {
                            WeaveSense.sense(context)
                            onOpenAssistant()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VaPrimaryButton(
                            text = "Talk to Spidy",
                            icon = Icons.Default.Mic,
                            onClick = { startBuddy() },
                        )
                        VaGhostButton("Today", onClick = onOpenToday)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("What did I get?", "Tell a joke", "Flashlight", "Good morning").forEach { tip ->
                            VaPill(tip) { startBuddy() }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                VaSectionLabel("Quick open")
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    modules.forEach { mod ->
                        VaIconTile(mod.title, mod.icon, mod.tint, mod.onClick)
                    }
                }

                Spacer(Modifier.height(28.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VaSectionLabel("Apps on this phone")
                    Text(
                        AppCredits.APP_NAME,
                        color = VaMuted,
                        fontFamily = Outfit,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = window.gridMinCell),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    userScrollEnabled = false,
                ) {
                    items(apps.take(12), key = { it.packageName }) { app ->
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
                                    .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = VaInk.copy(alpha = 0.05f), spotColor = VaInk.copy(alpha = 0.08f))
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .border(1.dp, VaInk.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                val bmp = app.icon
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                    )
                                } else {
                                    Text(
                                        app.label.take(1).uppercase(),
                                        color = VaInk,
                                        fontFamily = Outfit,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                app.label,
                                color = VaInk,
                                fontFamily = Outfit,
                                fontSize = 11.sp,
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
