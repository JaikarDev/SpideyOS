package com.jaikar.spideyos.ui.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.assistant.PupBrain
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.mail.MailDigestStore
import com.jaikar.spideyos.mail.MailItem
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyWeb
import com.jaikar.spideyos.ui.theme.WebBackground

@Composable
fun MailDigestScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var summary by remember { mutableStateOf<String?>(null) }
    val inbox = MailDigestStore.demoInbox

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = settings.themeIntensity)
        AdaptiveContent {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpideyWeb)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Inbox Pulse", color = SpideyWeb, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Pip announces mail on-device for ${settings.userName}", color = SpideyGold, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        MailDigestStore.scheduleDemoPing(context, 3_000L)
                        summary = PupBrain.announceMail(settings.userName) + " Demo ping in ~3s."
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Simulate “you've got mail”")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val bullets = inbox.joinToString("\n") { "• ${it.from}: ${it.subject}" }
                        summary = "Short version, ${settings.userName}:\n$bullets\n\n*tail wag* Want a reply draft tip?"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ask Pip for short version")
                }
                summary?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        color = SpideyWeb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpideyNavy.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(inbox) { mail: MailItem ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(SpideyNavy.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .clickable {
                                    summary = "Pip skim: “${mail.subject}” from ${mail.from}. ${mail.preview}"
                                }
                                .padding(12.dp),
                        ) {
                            Text(mail.from, color = SpideyGold, fontWeight = FontWeight.SemiBold)
                            Text(mail.subject, color = SpideyWeb, fontWeight = FontWeight.Medium)
                            Text(mail.preview, color = SpideyWeb.copy(alpha = 0.75f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
