package com.jaikar.spideyos.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.assistant.GeminiClient
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyRed
import com.jaikar.spideyos.ui.theme.SpideyWeb
import com.jaikar.spideyos.ui.theme.WebBackground
import kotlinx.coroutines.launch

data class ChatLine(val fromUser: Boolean, val text: String)

@Composable
fun AssistantScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
) {
    val lines = remember { mutableStateListOf<ChatLine>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(settings.userName) {
        if (lines.isEmpty()) {
            lines += ChatLine(false, "Hey ${settings.userName}! Spidey here — what are we swinging into?")
        }
    }

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = settings.themeIntensity)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpideyWeb)
                }
                Column {
                    Text("Spidey Assistant", color = SpideyWeb, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Gemini persona · personalized for ${settings.userName}", color = SpideyGold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(lines) { line ->
                    val bg = if (line.fromUser) SpideyRed.copy(alpha = 0.85f) else SpideyNavy.copy(alpha = 0.9f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (line.fromUser) 48.dp else 0.dp,
                                end = if (line.fromUser) 0.dp else 48.dp,
                            )
                            .background(bg, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                    ) {
                        Text(line.text, color = SpideyWeb)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Spidey…") },
                    enabled = !busy,
                    singleLine = true,
                )
                IconButton(
                    enabled = !busy && input.isNotBlank(),
                    onClick = {
                        val msg = input.trim()
                        input = ""
                        lines += ChatLine(true, msg)
                        busy = true
                        scope.launch {
                            val history = lines.dropLast(1).map {
                                (if (it.fromUser) "user" else "model") to it.text
                            }
                            // Re-read key from settings snapshot
                            val liveClient = GeminiClient { settings.geminiApiKey }
                            val reply = liveClient.chat(settings.userName, history, msg)
                            lines += ChatLine(false, reply)
                            busy = false
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = SpideyGold)
                }
            }
        }
    }
}
