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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.assistant.PupBrain
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.sense.WeaveSense
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyRed
import com.jaikar.spideyos.ui.theme.SpideyWeb
import com.jaikar.spideyos.ui.theme.WebBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext

data class ThreadMessage(
    val id: Long = System.currentTimeMillis(),
    val fromMe: Boolean,
    val body: String,
    val time: String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
    val animateWeb: Boolean = false,
    val reaction: String? = null,
    val isPip: Boolean = false,
)

private val WeaveBubbleShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(16f, 0f)
    lineTo(w - 16f, 0f)
    quadraticBezierTo(w, 0f, w, 16f)
    lineTo(w, h - 20f)
    quadraticBezierTo(w, h, w - 24f, h)
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
            ThreadMessage(fromMe = false, body = "Hey ${settings.userName}! Welcome to ThreadBox — soft weave bubbles."),
            ThreadMessage(fromMe = true, body = "This looks rich already."),
            ThreadMessage(
                fromMe = false,
                body = "I'll sit in the thread like a loyal pup and react when you send.",
                isPip = true,
                reaction = "wag",
            ),
        )
    }
    var input by remember { mutableStateOf("") }
    var pipTyping by remember { mutableStateOf(false) }
    val webProgress = remember { Animatable(0f) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        val last = messages.lastOrNull()
        if (last?.animateWeb == true && last.fromMe) {
            webProgress.snapTo(0f)
            webProgress.animateTo(1f, tween(750, easing = LinearOutSlowInEasing))
        }
    }

    Box(Modifier.fillMaxSize()) {
        WebBackground(intensity = settings.themeIntensity * 0.95f)
        AdaptiveContent {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpideyWeb)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("ThreadBox", color = SpideyWeb, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Message launcher · web-shoot send · WeaveSense", color = SpideyGold, fontSize = 12.sp)
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(SpideyRed.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Pets, null, tint = SpideyWeb, modifier = Modifier.size(22.dp))
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Canvas(Modifier.fillMaxSize()) {
                        val p = webProgress.value
                        if (p > 0f && p < 1f) {
                            val start = Offset(size.width * 0.88f, size.height - 12f)
                            val end = Offset(size.width * 0.55f, size.height * (1f - 0.4f * p))
                            val x = start.x + (end.x - start.x) * p
                            val y = start.y + (end.y - start.y) * p
                            drawLine(SpideyWeb.copy(alpha = 0.65f), start, Offset(x, y), strokeWidth = 4.5f)
                            // secondary web strands
                            drawLine(
                                SpideyGold.copy(alpha = 0.4f),
                                Offset(start.x - 18f, start.y),
                                Offset(x - 10f, y + 8f),
                                strokeWidth = 2f,
                            )
                            drawLine(
                                SpideyGold.copy(alpha = 0.4f),
                                Offset(start.x + 12f, start.y - 6f),
                                Offset(x + 14f, y - 6f),
                                strokeWidth = 2f,
                            )
                            val r = 28f * p
                            for (i in 0 until 10) {
                                val a = Math.PI * 2 * i / 10
                                drawLine(
                                    SpideyGold.copy(alpha = 0.8f),
                                    Offset(x, y),
                                    Offset(x + r * cos(a).toFloat(), y + r * sin(a).toFloat()),
                                    strokeWidth = 2.4f,
                                )
                            }
                            drawCircle(SpideyWeb.copy(alpha = 0.5f), radius = 6f * p, center = Offset(x, y))
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val align = if (msg.fromMe) Alignment.CenterEnd else Alignment.CenterStart
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = if (msg.fromMe) Alignment.End else Alignment.Start,
                            ) {
                                if (msg.isPip) {
                                    Text("Pip", color = SpideyGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Box(Modifier.fillMaxWidth(), contentAlignment = align) {
                                    Column(
                                        Modifier
                                            .fillMaxWidth(0.82f)
                                            .background(
                                                when {
                                                    msg.isPip -> SpideyGold.copy(alpha = 0.18f)
                                                    msg.fromMe -> SpideyRed.copy(alpha = 0.9f)
                                                    else -> SpideyNavy.copy(alpha = 0.92f)
                                                },
                                                WeaveBubbleShape,
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                    ) {
                                        Text(msg.body, color = SpideyWeb, fontSize = 14.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(msg.time, color = SpideyWeb.copy(alpha = 0.55f), fontSize = 10.sp)
                                            msg.reaction?.let {
                                                Icon(
                                                    Icons.Default.Favorite,
                                                    null,
                                                    tint = SpideyGold,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (pipTyping) {
                            item {
                                Text(
                                    "Pip is weaving a reply…",
                                    color = SpideyGold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .background(SpideyNavy, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a rich thread…") },
                        singleLine = true,
                    )
                    IconButton(
                        enabled = input.isNotBlank() && !pipTyping,
                        onClick = {
                            val text = input.trim()
                            input = ""
                            WeaveSense.shoot(context)
                            messages += ThreadMessage(fromMe = true, body = text, animateWeb = true)
                            scope.launch {
                                pipTyping = true
                                delay(700)
                                WeaveSense.wag(context)
                                val reply = PupBrain.chat(settings.userName, text)
                                messages += ThreadMessage(
                                    fromMe = false,
                                    body = reply,
                                    isPip = true,
                                    reaction = "wag",
                                )
                                pipTyping = false
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = SpideyGold)
                    }
                }
            }
        }
    }
}
