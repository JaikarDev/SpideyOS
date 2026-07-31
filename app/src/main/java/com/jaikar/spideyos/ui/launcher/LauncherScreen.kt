package com.jaikar.spideyos.ui.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.assistant.PupBrain
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.adaptive.SpideyWidthClass
import com.jaikar.spideyos.ui.adaptive.rememberSpideyWindowInfo
import com.jaikar.spideyos.ui.theme.MiniWebBadge
import com.jaikar.spideyos.ui.theme.PipPuppet
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyRed
import com.jaikar.spideyos.ui.theme.SpideyWeb
import com.jaikar.spideyos.ui.theme.WebBackground
import com.jaikar.spideyos.ui.theme.WebShootOpenAnimation

data class HomeModule(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

data class InstalledApp(
    val label: String,
    val packageName: String,
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
    LaunchedEffect(Unit) {
        webScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    val modules = listOf(
        HomeModule("Pip", Icons.Default.Pets, onOpenAssistant),
        HomeModule("ThreadBox", Icons.Default.Chat, onOpenMessages),
        HomeModule("Inbox Pulse", Icons.Default.Email, onOpenMail),
        HomeModule("SnapBooth", Icons.Default.CameraAlt, onOpenCamera),
        HomeModule("Settings", Icons.Default.Settings, onOpenSettings),
    )

    val apps = remember { loadLaunchableApps(context.packageManager) }

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = settings.themeIntensity)
        WebShootOpenAnimation()
        AdaptiveContent {
            Column(Modifier.fillMaxSize()) {
                Text(
                    AppCredits.APP_NAME,
                    color = SpideyWeb,
                    fontSize = window.titleSp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.scale(webScale.value),
                )
                Text(
                    "Hey ${settings.userName} — Pip is home.",
                    color = SpideyGold,
                    fontSize = window.bodySp,
                )
                Text(
                    AppCredits.CREDIT_LINE,
                    color = SpideyWeb.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                PipPuppet(
                    moodText = PupBrain.moodLine(settings.userName),
                    onTap = onOpenAssistant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text("Nest modules", color = SpideyWeb.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                if (window.widthClass == SpideyWidthClass.Compact && !window.isLandscape) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modules.forEach { mod -> ModuleChip(mod, Modifier.weight(1f)) }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(if (window.isTablet) 140.dp else 120.dp),
                    ) {
                        items(modules) { mod -> ModuleChip(mod, Modifier.fillMaxWidth()) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Apps on this phone", color = SpideyWeb.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = window.gridMinCell),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(apps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                                        context.startActivity(it)
                                    }
                                }
                                .padding(6.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(if (window.isTablet) 60.dp else 52.dp)
                                    .clip(CircleShape)
                                    .background(SpideyRed.copy(alpha = 0.85f))
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    app.label.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (window.isTablet) 22.sp else 20.sp,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
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

@Composable
private fun ModuleChip(mod: HomeModule, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SpideyNavy.copy(alpha = 0.75f))
            .clickable(onClick = mod.onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            MiniWebBadge(Modifier.size(36.dp), SpideyRed)
            Icon(mod.icon, contentDescription = mod.title, tint = SpideyWeb, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(mod.title, color = SpideyWeb, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun loadLaunchableApps(pm: PackageManager): List<InstalledApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    return resolved
        .map {
            InstalledApp(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
