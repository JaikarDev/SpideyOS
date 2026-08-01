package com.jaikar.spideyos.companion

import android.content.Context
import com.jaikar.spideyos.apps.InstalledApp
import com.jaikar.spideyos.companion.spidy.SpidyAutomation
import com.jaikar.spideyos.companion.spidy.SpidyDigest
import com.jaikar.spideyos.companion.spidy.SpidyMemory
import com.jaikar.spideyos.companion.spidy.SpidyMeetings
import com.jaikar.spideyos.companion.spidy.SpidyPersonality
import com.jaikar.spideyos.companion.spidy.SpidyActivityLog

/** Local voice command router — companion core, on-device. */
sealed class DashVoiceAction {
    data class SpeakOnly(val line: String) : DashVoiceAction()
    data class OpenApp(val packageName: String, val label: String, val line: String) : DashVoiceAction()
    data class OpenAppsMenu(val line: String) : DashVoiceAction()
    data class OpenSearch(val kind: DashSearchKind, val query: String, val line: String) : DashVoiceAction()
    data class OpenMusic(val line: String) : DashVoiceAction()
    data class OpenGallery(val line: String) : DashVoiceAction()
    data class Sleep(val line: String) : DashVoiceAction()
    data class Hide(val line: String) : DashVoiceAction()
    /** Unmatched speech — reply with internet (Gemini) or open web search. */
    data class SmartHelp(val query: String) : DashVoiceAction()
}

object DashCommander {
    private val WAKE_PHRASES = listOf(
        "hey spidy", "hey spidey", "hey speedy", "hey spider", "hey speady",
        "hi spidy", "hi spidey", "hi speedy", "hi spider",
        "yo spidy", "yo spidey", "yo speedy",
        "ok spidy", "okay spidy", "ok spidey", "okay spidey", "ok speedy",
        "spidy", "spidey", "spider", "speedy", "speady", "spee dy",
    )

    private val NAME_ROOTS = listOf(
        "spidy", "spidey", "spider", "speedy", "speady", "spidie", "spidi",
        "speedi", "speedie", "spyder", "spid",
    )

    fun isHeyWake(text: String): Boolean {
        val t = normalize(text)
        return t.contains("hey ") || t.startsWith("hey") ||
            t.contains("hi ") || t.startsWith("hi ") ||
            t.contains("yo ") || t.startsWith("yo ")
    }

    fun containsWake(text: String): Boolean {
        val t = normalize(text)
        if (t.isEmpty()) return false
        if (WAKE_PHRASES.any { phrase -> t == phrase || t.contains(phrase) }) return true
        val tokens = t.split(' ').filter { it.isNotBlank() }
        if (tokens.any { tokenLooksLikeSpidy(it) }) return true
        val compact = t.replace(" ", "")
        return NAME_ROOTS.any { compact.contains(it) }
    }

    fun stripWake(text: String): String {
        var t = normalize(text)
        WAKE_PHRASES.sortedByDescending { it.length }.forEach { phrase ->
            t = t.replace(phrase, " ")
        }
        t.split(' ').filter { tokenLooksLikeSpidy(it) }.forEach { tok ->
            t = t.replace(tok, " ")
        }
        return t.trim().replace(Regex("\\s+"), " ")
    }

    private fun normalize(raw: String): String =
        raw.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun tokenLooksLikeSpidy(token: String): Boolean {
        if (token.length < 4) return false
        if (NAME_ROOTS.any { token == it || token.startsWith(it) }) return true
        if (token.startsWith("spi") || token.startsWith("spe") || token.startsWith("spy")) {
            return token.contains("d") || token.contains("der") || token.endsWith("dy") ||
                token.endsWith("dey") || token.endsWith("die") || token.endsWith("dee")
        }
        return false
    }

    fun parse(
        context: Context,
        userName: String,
        heard: String,
        apps: List<InstalledApp>,
        memoryEnabled: Boolean = true,
        digestEnabled: Boolean = true,
        automationEnabled: Boolean = true,
    ): DashVoiceAction {
        val raw = heard.trim()
        val h = stripWake(raw).lowercase().ifBlank { raw.lowercase() }

        when {
            h.isBlank() || h in listOf("hi", "hello", "hey", "yo") ->
                return DashVoiceAction.SpeakOnly(SpidyPersonality.wave(userName))

            matches(h, "good morning", "morning") ->
                return DashVoiceAction.SpeakOnly(SpidyPersonality.morning(userName))

            matches(h, "good night", "goodnight", "night night") ->
                return DashVoiceAction.SpeakOnly(SpidyPersonality.night(userName))

            matches(h, "joke", "tell me a joke", "make me laugh", "funny") -> {
                SpidyActivityLog.append(context, "joke", "told")
                return DashVoiceAction.SpeakOnly(SpidyPersonality.joke())
            }

            isDigestAsk(h) && digestEnabled -> {
                SpidyActivityLog.append(context, "digest", "spoke")
                return DashVoiceAction.SpeakOnly(SpidyDigest.speak(context, userName))
            }

            isInboxAsk(h) ->
                return DashVoiceAction.SpeakOnly(
                    if (digestEnabled) SpidyDigest.speak(context, userName)
                    else DashPulseMemory.summaryForSpeak(context, userName),
                )

            h.startsWith("remember that ") || h.startsWith("remember ") -> {
                if (!memoryEnabled) {
                    return DashVoiceAction.SpeakOnly("Memory is off in Nest, $userName.")
                }
                val fact = h.removePrefix("remember that ").removePrefix("remember ").trim()
                SpidyMemory.rememberFact(context, fact)
                SpidyActivityLog.append(context, "memory", "saved")
                return DashVoiceAction.SpeakOnly("Got it — I’ll remember that on this phone only, $userName.")
            }

            matches(h, "what do you remember", "what you remember", "recall", "my memories") -> {
                if (!memoryEnabled) return DashVoiceAction.SpeakOnly("Memory is off in Nest, $userName.")
                return DashVoiceAction.SpeakOnly(SpidyMemory.speakRecall(context, userName))
            }

            matches(h, "continue yesterday", "what we discussed yesterday", "continue what we", "pick up where") -> {
                if (!memoryEnabled) return DashVoiceAction.SpeakOnly("Memory is off in Nest, $userName.")
                return DashVoiceAction.SpeakOnly(SpidyMemory.continueYesterday(context, userName))
            }

            matches(h, "happy birthday", "my birthday") ->
                return DashVoiceAction.SpeakOnly(SpidyPersonality.birthday(userName))

            automationEnabled && matches(h, "flashlight on", "torch on", "light on") ->
                return speakAuto(SpidyAutomation.setTorch(context, true))

            automationEnabled && matches(h, "flashlight off", "torch off", "light off") ->
                return speakAuto(SpidyAutomation.setTorch(context, false))

            automationEnabled && matches(h, "flashlight", "torch", "flash light") ->
                return speakAuto(SpidyAutomation.toggleTorch(context))

            automationEnabled && matches(h, "wifi", "wi fi", "wi-fi") ->
                return speakAuto(SpidyAutomation.openWifiPanel(context))

            automationEnabled && matches(h, "bluetooth") ->
                return speakAuto(SpidyAutomation.openBluetoothPanel(context))

            automationEnabled && matches(h, "battery saver", "battery") ->
                return speakAuto(SpidyAutomation.openBatterySaver(context))

            automationEnabled && matches(h, "dark mode", "display settings", "brightness") ->
                return speakAuto(SpidyAutomation.openDisplay(context))

            automationEnabled && matches(h, "silent", "sound settings", "volume") ->
                return speakAuto(SpidyAutomation.openSound(context))

            matches(h, "weather", "forecast", "temperature") ->
                return speakAuto(SpidyAutomation.openWeather(context, userName))

            matches(h, "open apps", "show apps", "list apps", "apps menu", "app list", "all apps") ->
                return DashVoiceAction.OpenAppsMenu("Opening your apps, $userName — pick one.")

            h.startsWith("open ") || h.startsWith("launch ") || h.startsWith("start ") -> {
                val name = h.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()
                if (name in listOf("apps", "app", "application", "applications", "my apps")) {
                    return DashVoiceAction.OpenAppsMenu("Here are your apps, $userName.")
                }
                if (name in listOf("gallery", "photos", "pictures", "camera roll")) {
                    return DashVoiceAction.OpenGallery("Opening your photos, $userName.")
                }
                if (name in listOf("music", "spotify", "youtube music", "earbuds")) {
                    return DashVoiceAction.OpenMusic("Music controls ready, $userName.")
                }
                val hit = findApp(apps, name)
                return if (hit != null) {
                    SpidyActivityLog.append(context, "open_app", hit.label)
                    DashVoiceAction.OpenApp(
                        hit.packageName,
                        hit.label,
                        "Opening ${hit.label} for you, $userName.",
                    )
                } else {
                    DashVoiceAction.OpenSearch(
                        DashSearchKind.APPS,
                        name,
                        "Searching apps for “$name”, $userName.",
                    )
                }
            }

            matches(h, "music", "song", "spotify", "youtube music", "play music", "earbuds") ->
                return DashVoiceAction.OpenMusic("Earbuds up, $userName — here's now playing.")

            matches(h, "photo", "photos", "gallery", "pictures", "pics") ->
                return DashVoiceAction.OpenGallery("Peeking at your gallery, $userName.")

            matches(h, "sleep", "go to sleep", "nap", "quiet") ->
                return DashVoiceAction.Sleep("Okay $userName — I'll nap. Say Spidy when you need me.")

            matches(h, "hide", "go away", "dismiss", "bye bye") ->
                return DashVoiceAction.Hide("Wave off, $userName. Call Spidy anytime.")

            matches(h, "who are you", "your name", "what are you") ->
                return DashVoiceAction.SpeakOnly(
                    "I'm Spidy — your on-phone companion. Local-first, privacy-first, $userName.",
                )

            matches(h, "privacy", "secure", "security", "location", "my number", "phone number", "protect") ->
                return DashVoiceAction.SpeakOnly(DashPrivacyGuard.privacyPledge(userName))

            matches(h, "help", "what can you do", "commands", "how can you help") ->
                return DashVoiceAction.SpeakOnly(
                    "Ask me anything, $userName. I can open apps, check meetings, digest notifications, " +
                        "flashlight, or answer using the internet. How can I help?",
                )

            matches(h, "my meetings", "meetings today", "what meetings", "calendar today", "schedule today") ->
                return DashVoiceAction.SpeakOnly(SpidyMeetings.speakSummary(context, userName))

            matches(h, "next meeting", "upcoming meeting", "when is my meeting", "do i have a meeting") ->
                return DashVoiceAction.SpeakOnly(SpidyMeetings.speakNext(context, userName))

            matches(h, "remind me about meetings", "meeting reminder", "remind meetings") -> {
                val due = SpidyMeetings.dueReminders(context, 60)
                return if (due.isEmpty()) {
                    DashVoiceAction.SpeakOnly(SpidyMeetings.speakNext(context, userName))
                } else {
                    val m = due.first()
                    SpidyMeetings.markReminded(context, m)
                    DashVoiceAction.SpeakOnly(SpidyMeetings.remindLine(userName, m))
                }
            }

            h.startsWith("search ") || h.startsWith("google ") || h.startsWith("look up ") -> {
                val q = h.removePrefix("search ").removePrefix("google ").removePrefix("look up ").trim()
                return speakAuto(SpidyAutomation.openWebSearch(context, q))
            }

            else -> {
                val maybeApp = findApp(apps, h)
                if (maybeApp != null) {
                    SpidyActivityLog.append(context, "open_app", maybeApp.label)
                    return DashVoiceAction.OpenApp(
                        maybeApp.packageName,
                        maybeApp.label,
                        "On it — opening ${maybeApp.label} for you, $userName!",
                    )
                }
                // Strip leading "open" leftovers and retry app match on shorter phrase
                val openish = h.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()
                if (openish != h) {
                    findApp(apps, openish)?.let { hit ->
                        SpidyActivityLog.append(context, "open_app", hit.label)
                        return DashVoiceAction.OpenApp(
                            hit.packageName,
                            hit.label,
                            "Opening ${hit.label} for you, $userName.",
                        )
                    }
                }
                if (memoryEnabled) {
                    SpidyMemory.rememberFact(context, "talked about: ${DashPrivacyGuard.redactPii(raw).take(80)}")
                }
                return DashVoiceAction.SmartHelp(DashPrivacyGuard.redactPii(raw).take(160).ifBlank { h })
            }
        }
    }

    private fun speakAuto(result: SpidyAutomation.Result): DashVoiceAction =
        when (result) {
            is SpidyAutomation.Result.Spoke -> DashVoiceAction.SpeakOnly(result.line)
            is SpidyAutomation.Result.SpokeAndDid -> DashVoiceAction.SpeakOnly(result.line)
        }

    private fun matches(h: String, vararg keys: String): Boolean =
        keys.any { key -> h == key || h.contains(key) }

    private fun isDigestAsk(h: String): Boolean =
        matches(
            h,
            "digest",
            "what did i get",
            "what i got",
            "notifications",
            "notification summary",
            "you received",
            "summary of notifications",
            "what came in",
        )

    private fun isInboxAsk(h: String): Boolean {
        val inboxWords = listOf("mail", "email", "inbox", "message", "messages", "whatsapp", "instagram", "telegram")
        val askWords = listOf("got", "any", "check", "tell", "read", "show", "do i", "have", "new", "status", "pulse")
        val hitInbox = inboxWords.any { h.contains(it) }
        if (!hitInbox) return false
        return askWords.any { h.contains(it) } ||
            h in listOf("mail", "email", "messages", "message", "inbox") ||
            h.startsWith("mail") || h.startsWith("message")
    }

    private fun findApp(apps: List<InstalledApp>, spoken: String): InstalledApp? {
        val q = spoken.trim().lowercase()
        if (q.isBlank()) return null
        val aliases = mapOf(
            "whatsapp" to listOf("whatsapp"),
            "instagram" to listOf("instagram"),
            "gmail" to listOf("gmail", "google mail"),
            "youtube" to listOf("youtube"),
            "youtube music" to listOf("youtube music"),
            "spotify" to listOf("spotify"),
            "chrome" to listOf("chrome"),
            "maps" to listOf("maps", "google maps"),
            "camera" to listOf("camera"),
            "settings" to listOf("settings"),
            "messages" to listOf("messages", "messaging"),
            "telegram" to listOf("telegram"),
            "discord" to listOf("discord"),
            "tiktok" to listOf("tiktok", "tik tok"),
            "amazon music" to listOf("amazon music"),
        )
        aliases.forEach { (label, keys) ->
            if (keys.any { q == it || q.contains(it) }) {
                apps.firstOrNull { app ->
                    keys.any { app.label.contains(it, true) || app.packageName.contains(it.replace(" ", ""), true) }
                }?.let { return it }
                apps.firstOrNull { it.label.contains(label, true) }?.let { return it }
            }
        }
        return apps.firstOrNull { it.label.equals(spoken, true) }
            ?: apps.firstOrNull { it.label.startsWith(spoken, true) }
            ?: apps.firstOrNull { it.label.contains(spoken, true) }
            ?: apps.minByOrNull { levenshtein(it.label.lowercase(), q) }
                ?.takeIf { levenshtein(it.label.lowercase(), q) <= 2 && q.length >= 3 }
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in a.indices) {
            cur[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                cur[j + 1] = minOf(cur[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            for (j in prev.indices) prev[j] = cur[j]
        }
        return prev[b.length]
    }
}
