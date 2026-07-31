package com.jaikar.spideyos.assistant

import com.jaikar.spideyos.mail.MailDigestStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates a rich conversation turn:
 * 1) Kick off local background work immediately (like a dog getting busy)
 * 2) Optionally ask Gemini if a key exists
 * 3) Merge reply + executed actions
 */
class CompanionOrchestrator(
    private val gemini: GeminiBridge,
) {
    private val _liveStatus = MutableStateFlow<List<String>>(emptyList())
    val liveStatus: StateFlow<List<String>> = _liveStatus.asStateFlow()

    suspend fun respond(
        userName: String,
        history: List<Pair<String, String>>,
        userMessage: String,
        onNavigate: (CompanionAction) -> Unit = {},
    ): CompanionTurn = coroutineScope {
        val localActions = ActionParser.fromUserIntent(userMessage)
        val status = mutableListOf<String>()
        fun push(s: String) {
            status += s
            _liveStatus.value = status.toList()
        }

        // Background work starts immediately
        val bg = async {
            localActions.forEach { action ->
                push(ActionParser.label(action))
                when (action) {
                    CompanionAction.WATCH_MAIL -> {
                        delay(280)
                        PupBrain.remember("mail-check")
                        onNavigate(action)
                    }
                    CompanionAction.WATCH_MESSAGES -> {
                        delay(260)
                        PupBrain.remember("message-check")
                        onNavigate(action)
                    }
                    CompanionAction.PREP_CAMERA -> {
                        delay(300)
                        PupBrain.remember("camera-warm")
                        onNavigate(action)
                    }
                    CompanionAction.HOME_WAG -> {
                        delay(200)
                        onNavigate(action)
                    }
                    CompanionAction.SUMMARIZE_DAY -> {
                        delay(350)
                        PupBrain.remember("day-summary")
                    }
                    CompanionAction.REMEMBER -> {
                        delay(120)
                        PupBrain.remember(userMessage.take(80))
                    }
                }
                delay(90)
            }
            if (localActions.contains(CompanionAction.SUMMARIZE_DAY) ||
                localActions.contains(CompanionAction.WATCH_MAIL)
            ) {
                push("Inbox Pulse notes ready (${MailDigestStore.demoInbox.size} sample threads)")
            }
        }

        val geminiDeferred = async {
            if (gemini.hasKey()) {
                push("Gemini thinking in the background…")
                gemini.chat(userName, history, userMessage)
            } else null
        }

        bg.await()
        val geminiRaw = geminiDeferred.await()

        val (geminiClean, geminiActions) = if (geminiRaw != null) {
            ActionParser.fromModelText(geminiRaw)
        } else {
            "" to emptyList()
        }

        geminiActions.forEach { action ->
            if (action !in localActions) {
                push(ActionParser.label(action))
                onNavigate(action)
                delay(100)
            }
        }

        val localReply = PupBrain.chat(userName, userMessage)
        val reply = when {
            geminiClean.isNotBlank() -> geminiClean
            else -> localReply
        }

        val footer = when {
            geminiRaw != null -> "\n\n— Pip · Gemini assist · background tasks done"
            else -> "\n\n— Pip · on-device · add Gemini key in Settings for smarter chat"
        }

        _liveStatus.value = emptyList()
        CompanionTurn(
            reply = reply + footer,
            actions = (localActions + geminiActions).distinct(),
            statusLines = status.toList(),
            usedGemini = geminiRaw != null,
        )
    }
}
