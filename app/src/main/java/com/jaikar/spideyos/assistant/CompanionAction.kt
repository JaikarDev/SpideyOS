package com.jaikar.spideyos.assistant

/** Things Pip / Gemini can do in the background while talking. */
enum class CompanionAction {
    WATCH_MAIL,
    WATCH_MESSAGES,
    PREP_CAMERA,
    HOME_WAG,
    SUMMARIZE_DAY,
    REMEMBER,
}

data class CompanionTurn(
    val reply: String,
    val actions: List<CompanionAction> = emptyList(),
    val statusLines: List<String> = emptyList(),
    val usedGemini: Boolean = false,
)

object ActionParser {
    fun fromUserIntent(message: String): List<CompanionAction> {
        val m = message.lowercase()
        val out = mutableListOf<CompanionAction>()
        if (m.contains("mail") || m.contains("email") || m.contains("inbox")) out += CompanionAction.WATCH_MAIL
        if (m.contains("message") || m.contains("sms") || m.contains("chat") || m.contains("thread")) {
            out += CompanionAction.WATCH_MESSAGES
        }
        if (m.contains("photo") || m.contains("camera") || m.contains("snap") || m.contains("picture")) {
            out += CompanionAction.PREP_CAMERA
        }
        if (m.contains("hi") || m.contains("hey") || m.contains("hello") || m.contains("good ")) {
            out += CompanionAction.HOME_WAG
        }
        if (m.contains("day") || m.contains("summary") || m.contains("what's up") || m.contains("whats up")) {
            out += CompanionAction.SUMMARIZE_DAY
        }
        out += CompanionAction.REMEMBER
        return out.distinct()
    }

    fun fromModelText(text: String): Pair<String, List<CompanionAction>> {
        val actions = mutableListOf<CompanionAction>()
        var clean = text
        val tagRegex = Regex("""\[\[ACTION:([A-Z_]+)]]""")
        tagRegex.findAll(text).forEach { match ->
            runCatching { CompanionAction.valueOf(match.groupValues[1]) }.getOrNull()?.let { actions += it }
            clean = clean.replace(match.value, "")
        }
        return clean.trim() to actions.distinct()
    }

    fun label(action: CompanionAction): String = when (action) {
        CompanionAction.WATCH_MAIL -> "Checking Inbox Pulse in background…"
        CompanionAction.WATCH_MESSAGES -> "Scanning ThreadBox threads…"
        CompanionAction.PREP_CAMERA -> "Warming SnapBooth…"
        CompanionAction.HOME_WAG -> "Pip wagging on home…"
        CompanionAction.SUMMARIZE_DAY -> "Weaving your day notes…"
        CompanionAction.REMEMBER -> "Remembering this for later…"
    }
}
