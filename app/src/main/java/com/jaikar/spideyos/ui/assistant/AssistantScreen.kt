package com.jaikar.spideyos.ui.assistant

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.assistant.CompanionAction
import com.jaikar.spideyos.assistant.CompanionOrchestrator
import com.jaikar.spideyos.assistant.GeminiBridge
import com.jaikar.spideyos.assistant.PupBrain
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.adaptive.AdaptiveContent
import com.jaikar.spideyos.ui.theme.Fraunces
import com.jaikar.spideyos.ui.theme.Outfit
import com.jaikar.spideyos.ui.theme.PipPuppet
import com.jaikar.spideyos.ui.theme.VaAtmosphere
import com.jaikar.spideyos.ui.theme.VaCloud
import com.jaikar.spideyos.ui.theme.VaInk
import com.jaikar.spideyos.ui.theme.VaMint
import com.jaikar.spideyos.ui.theme.VaMintDeep
import com.jaikar.spideyos.ui.theme.VaMuted
import com.jaikar.spideyos.ui.theme.VaSoft
import kotlinx.coroutines.launch

data class ChatLine(
    val fromUser: Boolean,
    val text: String,
    val meta: String? = null,
)

@Composable
fun AssistantScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
    onOpenMail: () -> Unit = {},
    onOpenMessages: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
) {
    val lines = remember { mutableStateListOf<ChatLine>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val orchestrator = remember(settings.geminiApiKey) {
        CompanionOrchestrator(GeminiBridge { settings.geminiApiKey })
    }
    val liveStatus by orchestrator.liveStatus.collectAsState()

    LaunchedEffect(settings.userName) {
        if (lines.isEmpty()) {
            val mode = if (settings.geminiApiKey.isNotBlank()) {
                "Gemini ready — I'll work in the background while we talk."
            } else {
                "On-device mode — add an optional Gemini key in Settings for smarter chat."
            }
            lines += ChatLine(
                false,
                PupBrain.moodLine(settings.userName) + " $mode",
                meta = "Pip · WeaveHome",
            )
        }
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    Box(Modifier.fillMaxSize()) {
        VaAtmosphere()
        AdaptiveContent {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaInk)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Spidy",
                            color = VaInk,
                            fontFamily = Fraunces,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )
                        Text(
                            if (settings.geminiApiKey.isNotBlank()) {
                                "Gemini + local actions · ${AppCredits.DEVELOPER_NAME}"
                            } else {
                                "On-device companion · ${AppCredits.DEVELOPER_NAME}"
                            },
                            color = VaMuted,
                            fontFamily = Outfit,
                            fontSize = 12.sp,
                        )
                    }
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = VaMint, strokeWidth = 2.dp)
                    }
                }

                PipPuppet(
                    moodText = if (busy) "Working in the background for you…" else PupBrain.moodLine(settings.userName),
                    onTap = {
                        if (!busy) {
                            input = "hi"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )

                AnimatedVisibility(visible = liveStatus.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(VaSoft, RoundedCornerShape(18.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Background", color = VaMintDeep, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        liveStatus.takeLast(4).forEach { s ->
                            Text("• $s", color = VaInk, fontFamily = Outfit, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(lines) { line ->
                        val bg = if (line.fromUser) VaMint else VaCloud
                        val fg = if (line.fromUser) Color.White else VaInk
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = if (line.fromUser) 40.dp else 0.dp,
                                    end = if (line.fromUser) 0.dp else 40.dp,
                                )
                                .background(bg, RoundedCornerShape(22.dp))
                                .padding(14.dp),
                        ) {
                            Text(line.text, color = fg, fontFamily = Outfit, fontSize = 14.sp)
                            line.meta?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = if (line.fromUser) Color.White.copy(alpha = 0.75f) else VaMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Spidy…") },
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
                                val turn = orchestrator.respond(
                                    userName = settings.userName,
                                    history = history,
                                    userMessage = msg,
                                    onNavigate = { action ->
                                        when (action) {
                                            CompanionAction.WATCH_MAIL -> onOpenMail()
                                            CompanionAction.WATCH_MESSAGES -> onOpenMessages()
                                            CompanionAction.PREP_CAMERA -> onOpenCamera()
                                            else -> Unit
                                        }
                                    },
                                )
                                lines += ChatLine(
                                    false,
                                    turn.reply,
                                    meta = buildString {
                                        if (turn.usedGemini) append("Gemini · ")
                                        append("actions: ${turn.actions.joinToString { it.name }}")
                                    },
                                )
                                busy = false
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = VaMint)
                    }
                }
            }
        }
    }
}
