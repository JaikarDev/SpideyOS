package com.jaikar.spideyos.ui.messages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyRed
import com.jaikar.spideyos.ui.theme.SpideyWeb
import com.jaikar.spideyos.ui.theme.WebBackground

data class WebMessage(val fromMe: Boolean, val body: String, val animateWeb: Boolean = false)

private val WebBubbleShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(16f, 0f)
    lineTo(w - 16f, 0f)
    quadraticBezierTo(w, 0f, w, 16f)
    lineTo(w, h - 24f)
    quadraticBezierTo(w, h, w - 28f, h)
    lineTo(w * 0.55f, h)
    lineTo(w * 0.48f, h + 10f)
    lineTo(w * 0.42f, h)
    lineTo(16f, h)
    quadraticBezierTo(0f, h, 0f, h - 16f)
    lineTo(0f, 16f)
    quadraticBezierTo(0f, 0f, 16f, 0f)
    close()
}

@Composable
fun WebMessagesScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
) {
    val messages = remember {
        mutableStateListOf(
            WebMessage(false, "Hey ${settings.userName}! Messages land like webs here."),
            WebMessage(true, "SpideyOS looking sharp."),
            WebMessage(false, "Want me to announce new chats in Spidey voice? Enable Notification Access."),
        )
    }
    var input by remember { mutableStateOf("") }
    val webProgress = remember { Animatable(0f) }

    LaunchedEffect(messages.size) {
        val last = messages.lastOrNull()
        if (last?.animateWeb == true && last.fromMe) {
            webProgress.snapTo(0f)
            webProgress.animateTo(1f, tween(550, easing = LinearOutSlowInEasing))
        }
    }

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = settings.themeIntensity * 0.9f)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpideyWeb)
                }
                Column {
                    Text("Web Messages", color = SpideyWeb, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Spider-web bubbles · send-line animation", color = SpideyGold, fontSize = 12.sp)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Canvas(Modifier.fillMaxSize()) {
                    val p = webProgress.value
                    if (p > 0f) {
                        val start = Offset(size.width * 0.85f, size.height - 8f)
                        val end = Offset(size.width * 0.55f, size.height * (1f - 0.35f * p))
                        drawLine(
                            SpideyWeb.copy(alpha = 0.55f),
                            start,
                            Offset(
                                start.x + (end.x - start.x) * p,
                                start.y + (end.y - start.y) * p,
                            ),
                            strokeWidth = 3f,
                        )
                        // tiny web burst
                        val cx = start.x + (end.x - start.x) * p
                        val cy = start.y + (end.y - start.y) * p
                        val r = 18f * p
                        for (i in 0 until 6) {
                            val a = Math.PI * 2 * i / 6
                            drawLine(
                                SpideyGold.copy(alpha = 0.7f),
                                Offset(cx, cy),
                                Offset(cx + r * kotlin.math.cos(a).toFloat(), cy + r * kotlin.math.sin(a).toFloat()),
                                strokeWidth = 2f,
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    items(messages) { msg ->
                        val align = if (msg.fromMe) Alignment.CenterEnd else Alignment.CenterStart
                        Box(Modifier.fillMaxWidth(), contentAlignment = align) {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.78f)
                                    .background(
                                        if (msg.fromMe) SpideyRed.copy(alpha = 0.9f) else SpideyNavy.copy(alpha = 0.92f),
                                        WebBubbleShape,
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                // decorative mini web lines inside bubble
                                Canvas(Modifier.fillMaxWidth().height(0.dp)) { }
                                Text(msg.body, color = SpideyWeb)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Shoot a web-message…") },
                    singleLine = true,
                )
                IconButton(
                    enabled = input.isNotBlank(),
                    onClick = {
                        messages += WebMessage(true, input.trim(), animateWeb = true)
                        input = ""
                        messages += WebMessage(
                            false,
                            "Spidey here — ${settings.userName}, message sent across the web!",
                        )
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = SpideyGold)
                }
            }
        }
    }
}
