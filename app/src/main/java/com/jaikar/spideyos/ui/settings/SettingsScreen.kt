package com.jaikar.spideyos.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.SpideyCredits
import com.jaikar.spideyos.assistant.SpideyOverlayService
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyWeb
import com.jaikar.spideyos.ui.theme.WebBackground
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as SpideyApp).settings
    val scope = rememberCoroutineScope()
    var name by remember(settings.userName) { mutableStateOf(settings.userName) }
    var key by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var intensity by remember(settings.themeIntensity) { mutableFloatStateOf(settings.themeIntensity) }
    var imapHost by remember(settings.imapHost) { mutableStateOf(settings.imapHost) }
    var imapEmail by remember(settings.imapEmail) { mutableStateOf(settings.imapEmail) }
    var imapPass by remember(settings.imapPassword) { mutableStateOf(settings.imapPassword) }

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = intensity)
        AdaptiveContent {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpideyWeb)
                }
                Text("Settings", color = SpideyWeb, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Gemini API key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Text("Theme intensity", color = SpideyGold)
            Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 0.3f..1f)
            SettingSwitch("Spidey voice notifications", settings.spideyVoiceEnabled) {
                scope.launch { repo.setSpideyVoiceEnabled(it) }
            }
            SettingSwitch("Messages module", settings.messagesEnabled) {
                scope.launch { repo.setMessagesEnabled(it) }
            }
            SettingSwitch("Mail module", settings.mailEnabled) {
                scope.launch { repo.setMailEnabled(it) }
            }
            SettingSwitch("Camera module", settings.cameraEnabled) {
                scope.launch { repo.setCameraEnabled(it) }
            }
            Spacer(Modifier.height(12.dp))
            Text("IMAP / Gmail (app password)", color = SpideyGold, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = imapHost,
                onValueChange = { imapHost = it },
                label = { Text("IMAP host") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = imapEmail,
                onValueChange = { imapEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = imapPass,
                onValueChange = { imapPass = it },
                label = { Text("App password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        repo.setUserName(name)
                        repo.setGeminiKey(key)
                        repo.setThemeIntensity(intensity)
                        repo.setImap(imapHost, imapEmail, imapPass)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Notification access") }
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Overlay permission") }
            OutlinedButton(
                onClick = { SpideyOverlayService.start(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Start floating Spidey") }
            Spacer(Modifier.height(16.dp))
            Text(
                SpideyCredits.ABOUT_LINE,
                color = SpideyGold,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                "Unofficial fan project. Not affiliated with Marvel or Sony.\nAdaptive UI for OnePlus · Samsung · Oppo · Vivo · Realme · Redmi · Xiaomi · Poco · Lava.",
                color = SpideyWeb.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
        }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SpideyWeb, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
