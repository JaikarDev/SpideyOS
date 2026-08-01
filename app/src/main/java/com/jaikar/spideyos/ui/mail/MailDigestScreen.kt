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
import com.jaikar.spideyos.ui.theme.Fraunces
import com.jaikar.spideyos.ui.theme.Outfit
import com.jaikar.spideyos.ui.theme.VaAtmosphere
import com.jaikar.spideyos.ui.theme.VaCloud
import com.jaikar.spideyos.ui.theme.VaInk
import com.jaikar.spideyos.ui.theme.VaMintDeep
import com.jaikar.spideyos.ui.theme.VaMuted
import com.jaikar.spideyos.ui.theme.VaSoft

@Composable
fun MailDigestScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var summary by remember { mutableStateOf<String?>(null) }
    val inbox = MailDigestStore.demoInbox

    Box(Modifier.fillMaxSize()) {
        VaAtmosphere()
        AdaptiveContent {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = VaInk)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Inbox Pulse", color = VaInk, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Spidy announces mail on-device for ${settings.userName}", color = VaMuted, fontFamily = Outfit, fontSize = 12.sp)
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
                        color = VaInk,
                        fontFamily = Outfit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VaCloud, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(inbox) { mail: MailItem ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(VaCloud, RoundedCornerShape(18.dp))
                                .clickable {
                                    summary = "Spidy skim: “${mail.subject}” from ${mail.from}. ${mail.preview}"
                                }
                                .padding(14.dp),
                        ) {
                            Text(mail.from, color = VaMintDeep, fontFamily = Outfit, fontWeight = FontWeight.SemiBold)
                            Text(mail.subject, color = VaInk, fontFamily = Outfit, fontWeight = FontWeight.Medium)
                            Text(mail.preview, color = VaMuted, fontFamily = Outfit, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
