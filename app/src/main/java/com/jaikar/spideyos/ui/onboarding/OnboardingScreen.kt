package com.jaikar.spideyos.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.jaikar.spideyos.assistant.SpideyOverlayService
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyWeb
import com.jaikar.spideyos.ui.theme.WebBackground
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    settings: SpideySettings,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as SpideyApp).settings
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(settings.userName) }
    var apiKey by remember { mutableStateOf(settings.geminiApiKey) }
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* user may deny; Spidey voice still works via listener */ }

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = settings.themeIntensity)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("SpideyOS", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = SpideyWeb)
            Text("Your web-slinging Android companion", color = SpideyGold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Developed by Jaikar Pothula",
                color = SpideyGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Unofficial fan project — not affiliated with Marvel or Sony.",
                color = SpideyWeb.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API key (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enable Notification Access") }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enable Overlay (Spidey float)") }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    scope.launch {
                        repo.setUserName(name)
                        repo.setGeminiKey(apiKey)
                        repo.setOnboardingDone(true)
                        SpideyOverlayService.start(context)
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Swing in — Hey ${name.ifBlank { "Jaikar" }}!")
            }
        }
    }
}
