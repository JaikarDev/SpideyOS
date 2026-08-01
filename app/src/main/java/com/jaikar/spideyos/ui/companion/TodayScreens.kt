package com.jaikar.spideyos.ui.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.assistant.SpideyOverlayService
import com.jaikar.spideyos.companion.DashPulseMemory
import com.jaikar.spideyos.companion.spidy.SpidyActivityLog
import com.jaikar.spideyos.companion.spidy.SpidyCapability
import com.jaikar.spideyos.companion.spidy.SpidyPermissions
import com.jaikar.spideyos.companion.spidy.SpidyPersonality
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.theme.Fraunces
import com.jaikar.spideyos.ui.theme.Outfit
import com.jaikar.spideyos.ui.theme.VaAtmosphere
import com.jaikar.spideyos.ui.theme.VaGhostButton
import com.jaikar.spideyos.ui.theme.NestSurface
import com.jaikar.spideyos.ui.theme.VaInk
import com.jaikar.spideyos.ui.theme.VaMint
import com.jaikar.spideyos.ui.theme.VaMintDeep
import com.jaikar.spideyos.ui.theme.VaMuted
import com.jaikar.spideyos.ui.theme.VaPrimaryButton
import com.jaikar.spideyos.ui.theme.VaSectionLabel
import com.jaikar.spideyos.ui.theme.VaSoft
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PermissionDashboardScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as SpideyApp).settings
    val scope = rememberCoroutineScope()
    val caps = remember(settings) { SpidyPermissions.snapshot(context, settings) }
    val photoPerm = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val calPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
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
                        "Permissions",
                        color = VaInk,
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                }
                Text(
                    SpidyPermissions.PRIVACY_PLEDGE,
                    color = VaMuted,
                    fontFamily = Outfit,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(18.dp))
                caps.forEach { cap ->
                    NestSurface(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (cap.granted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                null,
                                tint = if (cap.granted) VaMint else VaMuted,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cap.title, color = VaInk, fontFamily = Outfit, fontWeight = FontWeight.SemiBold)
                                Text(cap.detail, color = VaMuted, fontFamily = Outfit, fontSize = 12.sp)
                            }
                            if (!cap.system) {
                                Switch(
                                    checked = when (cap.id) {
                                        "routines" -> settings.routinesEnabled
                                        "digest" -> settings.digestEnabled
                                        "memory" -> settings.memoryEnabled
                    "automation" -> settings.automationEnabled
                    "meetings" -> settings.meetingsEnabled
                    else -> cap.granted
                },
                                    onCheckedChange = { on ->
                                        scope.launch {
                                            when (cap.id) {
                                                "routines" -> repo.setRoutinesEnabled(on)
                                                "digest" -> repo.setDigestEnabled(on)
                                                "memory" -> repo.setMemoryEnabled(on)
                                    "automation" -> repo.setAutomationEnabled(on)
                                    "meetings" -> repo.setMeetingsEnabled(on)
                                }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = VaMint,
                                        checkedThumbColor = Color.White,
                                    ),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaGhostButton("Overlay") {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                    VaGhostButton("Mic") {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaGhostButton("Notify") {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    VaGhostButton("Photos") {
                        if (ContextCompat.checkSelfPermission(context, photoPerm) != PackageManager.PERMISSION_GRANTED) {
                            photoLauncher.launch(photoPerm)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaGhostButton("Calendar") {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            calPermLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                VaSectionLabel("Coming next")
                Spacer(Modifier.height(8.dp))
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
fun TodayScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val pulses = remember { DashPulseMemory.recent(context, 20) }
    val mail = pulses.count { it.type.equals("mail", true) }
    val msgs = pulses.size - mail
    val actions = remember { SpidyActivityLog.recent(context, 8) }
    val greeting = remember(settings.userName) { SpidyPersonality.wave(settings.userName) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            SpideyOverlayService.stop(context)
            SpideyOverlayService.start(context)
        }
    }
    val calLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val meetings = remember {
        com.jaikar.spideyos.companion.spidy.SpidyMeetings.today(context)
            .ifEmpty { com.jaikar.spideyos.companion.spidy.SpidyMeetings.upcoming(context, 24, 6) }
    }
    val hasCal = remember {
        com.jaikar.spideyos.companion.spidy.SpidyMeetings.hasPermission(context)
    }

    fun startBuddy() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
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
                    Column {
                        Text(
                            "Today",
                            color = VaInk,
                            fontFamily = Fraunces,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        )
                        Text("with Spidy", color = VaMuted, fontFamily = Outfit, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    greeting,
                    color = VaInk,
                    fontFamily = Outfit,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(18.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatChip("Mail", "$mail", Modifier.weight(1f))
                    StatChip("Messages", "$msgs", Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))
                VaSectionLabel("Meetings")
                Spacer(Modifier.height(8.dp))
                if (!hasCal) {
                    NestSurface(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Allow calendar so Spidy can show meetings and remind you.",
                            color = VaMuted,
                            fontFamily = Outfit,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        VaGhostButton("Allow calendar") {
                            calLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    }
                } else if (meetings.isEmpty()) {
                    NestSurface(modifier = Modifier.fillMaxWidth()) {
                        Text("No upcoming meetings on this phone’s calendar.", color = VaMuted, fontFamily = Outfit)
                    }
                } else {
                    meetings.take(5).forEach { m ->
                        NestSurface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(m.title, color = VaInk, fontFamily = Outfit, fontWeight = FontWeight.SemiBold)
                            Text(
                                timeFmt.format(Date(m.startMs)),
                                color = VaMintDeep,
                                fontFamily = Outfit,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                VaPrimaryButton(
                    text = "Start SpideyDashPip",
                    icon = Icons.Default.Mic,
                    onClick = { startBuddy() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaGhostButton("Stop buddy") { SpideyOverlayService.stop(context) }
                    VaGhostButton("Permissions", onClick = onOpenPermissions)
                }

                Spacer(Modifier.height(22.dp))
                VaSectionLabel("What Spidy did")
                Spacer(Modifier.height(10.dp))
                if (actions.isEmpty()) {
                    NestSurface(modifier = Modifier.fillMaxWidth()) {
                        Text("Quiet so far — talk to Spidy and actions show up here.", color = VaMuted, fontFamily = Outfit)
                    }
                } else {
                    actions.forEach { e ->
                        NestSurface(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(VaMint),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.action, color = VaInk, fontFamily = Outfit, fontWeight = FontWeight.SemiBold)
                                    Text(e.detail, color = VaMuted, fontFamily = Outfit, fontSize = 12.sp)
                                }
                                Text(
                                    timeFmt.format(Date(e.atMs)),
                                    color = VaMuted,
                                    fontFamily = Outfit,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                VaSectionLabel("Coming next")
                Spacer(Modifier.height(8.dp))
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
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    NestSurface(modifier = modifier) {
        Text(label, color = VaMuted, fontFamily = Outfit, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = VaMintDeep,
            fontFamily = Fraunces,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
        )
        Box(
            Modifier
                .padding(top = 8.dp)
                .height(4.dp)
                .fillMaxWidth(0.35f)
                .clip(CircleShape)
                .background(VaSoft),
        )
    }
}
