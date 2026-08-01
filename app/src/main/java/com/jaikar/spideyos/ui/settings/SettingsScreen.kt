package com.jaikar.spideyos.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.assistant.SpideyOverlayService
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.theme.Fraunces
import com.jaikar.spideyos.ui.theme.Outfit
import com.jaikar.spideyos.ui.theme.VaAtmosphere
import com.jaikar.spideyos.ui.theme.VaGhostButton
import com.jaikar.spideyos.ui.theme.NestSurface
import com.jaikar.spideyos.ui.theme.VaInk
import com.jaikar.spideyos.ui.theme.VaMint
import com.jaikar.spideyos.ui.theme.VaMuted
import com.jaikar.spideyos.ui.theme.VaPrimaryButton
import com.jaikar.spideyos.ui.theme.VaSectionLabel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
    onOpenToday: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as SpideyApp).settings
    val scope = rememberCoroutineScope()
    var name by remember(settings.userName) { mutableStateOf(settings.userName) }
    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var intensity by remember(settings.themeIntensity) { mutableFloatStateOf(settings.themeIntensity) }
    val photoPerm = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            SpideyOverlayService.stop(context)
            SpideyOverlayService.start(context)
        }
    }

    Box(Modifier.fillMaxSize()) {
        VaAtmosphere()
        AdaptiveContent {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = VaInk)
                    }
                    Text(
                        "Nest",
                        color = VaInk,
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    )
                }
                Text(
                    "Spidy floats on your phone’s OS. Local-first — no location, no numbers.",
                    color = VaMuted,
                    fontFamily = Outfit,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(16.dp))

                NestSurface(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Optional Gemini key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Atmosphere", color = VaMuted, fontFamily = Outfit, fontSize = 12.sp)
                    Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 0.3f..1f)
                    VaPrimaryButton(
                        text = "Save profile",
                        onClick = {
                            scope.launch {
                                repo.setUserName(name)
                                repo.setGeminiKey(geminiKey)
                                repo.setThemeIntensity(intensity)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(16.dp))
                VaSectionLabel("Companion")
                Spacer(Modifier.height(8.dp))
                NestSurface(modifier = Modifier.fillMaxWidth()) {
                    NestSwitch("Watch notifications", settings.spideyVoiceEnabled) {
                        scope.launch { repo.setSpideyVoiceEnabled(it) }
                    }
                    NestSwitch("Morning / night routines", settings.routinesEnabled) {
                        scope.launch { repo.setRoutinesEnabled(it) }
                    }
                    NestSwitch("Smart digest", settings.digestEnabled) {
                        scope.launch { repo.setDigestEnabled(it) }
                    }
                    NestSwitch("Memory", settings.memoryEnabled) {
                        scope.launch { repo.setMemoryEnabled(it) }
                    }
                    NestSwitch("Device actions", settings.automationEnabled) {
                        scope.launch { repo.setAutomationEnabled(it) }
                    }
                    NestSwitch("Meetings & reminders", settings.meetingsEnabled) {
                        scope.launch { repo.setMeetingsEnabled(it) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                VaSectionLabel("Modules")
                Spacer(Modifier.height(8.dp))
                NestSurface(modifier = Modifier.fillMaxWidth()) {
                    NestSwitch("ThreadBox", settings.messagesEnabled) {
                        scope.launch { repo.setMessagesEnabled(it) }
                    }
                    NestSwitch("Inbox Pulse", settings.mailEnabled) {
                        scope.launch { repo.setMailEnabled(it) }
                    }
                    NestSwitch("SnapBooth", settings.cameraEnabled) {
                        scope.launch { repo.setCameraEnabled(it) }
                    }
                }

                Spacer(Modifier.height(18.dp))
                VaPrimaryButton(
                    text = "Start SpideyDashPip",
                    icon = Icons.Default.Mic,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            SpideyOverlayService.stop(context)
                            SpideyOverlayService.start(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaGhostButton("Today", onClick = onOpenToday)
                    VaGhostButton("Permissions", onClick = onOpenPermissions)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaGhostButton("Notify access") {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    VaGhostButton("Overlay") {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaGhostButton("Photos") {
                        if (ContextCompat.checkSelfPermission(context, photoPerm) != PackageManager.PERMISSION_GRANTED) {
                            photoLauncher.launch(photoPerm)
                        }
                    }
                    VaGhostButton("Hide buddy") { SpideyOverlayService.stop(context) }
                }

                Spacer(Modifier.height(22.dp))
                Text(AppCredits.ABOUT_LINE, color = VaMuted, fontFamily = Outfit, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                VaSectionLabel("Coming next")
                Text(
                    "Knowledge · Calendar · Vision · Plugins",
                    color = VaMuted,
                    fontFamily = Outfit,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun NestSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = VaInk, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = VaMint, checkedThumbColor = Color.White),
        )
    }
}
