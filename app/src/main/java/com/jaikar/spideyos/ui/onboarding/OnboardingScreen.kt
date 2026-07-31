package com.jaikar.spideyos.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.core.content.ContextCompat
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.assistant.SpideyOverlayService
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.theme.PipPuppet
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
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = settings.themeIntensity)
        AdaptiveContent {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(AppCredits.APP_NAME, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = SpideyWeb)
                Text(AppCredits.TAGLINE, color = SpideyGold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(AppCredits.CREDIT_LINE, color = SpideyGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Original mascot Pip — not affiliated with any movie brand.",
                    color = SpideyWeb.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(16.dp))
                PipPuppet(
                    moodText = "I'll live on your home like a loyal pup.",
                    onTap = { },
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Let Pip watch notifications") }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Let Pip float on home") }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        scope.launch {
                            repo.setUserName(name)
                            repo.setOnboardingDone(true)
                            SpideyOverlayService.start(context)
                            onFinished()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Bring Pip home — Hey ${name.ifBlank { "Jaikar" }}!")
                }
            }
        }
    }
}
