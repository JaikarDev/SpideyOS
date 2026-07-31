package com.jaikar.spideyos.assistant

import com.jaikar.spideyos.AppCredits
import java.util.Calendar
import kotlin.random.Random

/**
 * Fully on-device companion brain — no cloud AI required.
 * Pip behaves like a loyal house pet with WeaveSense.
 */
object PupBrain {
    private val recentNotes = ArrayDeque<String>(12)

    fun remember(note: String) {
        synchronized(recentNotes) {
            if (recentNotes.size >= 12) recentNotes.removeFirst()
            recentNotes.addLast(note)
        }
    }

    fun moodLine(userName: String): String {
        val notes = synchronized(recentNotes) { recentNotes.toList() }
        return when {
            notes.any { it.contains("mail", true) } ->
                "*ears up* Mail scent, $userName. Want me to wag you over to Inbox Pulse?"
            notes.any { it.contains("message", true) } ->
                "*tail thump* Fresh threads for you, $userName."
            notes.any { it.contains("photo", true) } ->
                "*proud pup* Nice snap energy, $userName."
            else -> timeGreeting(userName)
        }
    }

    fun chat(userName: String, message: String): String {
        val m = message.lowercase().trim()
        remember("chat:$m")
        return when {
            m.contains("who made") || m.contains("developer") || m.contains("who built") || m.contains("jaikar") ->
                "WeaveHome and I (${AppCredits.MASCOT_NAME}) were built by ${AppCredits.DEVELOPER_NAME}. I'm just the house puppet — he made the nest."
            m.contains("sense") || m.contains("tingle") || m.contains("vibrate") || m.contains("buzz") ->
                "*WeaveSense tingle* I buzz your hand when mail or threads land, $userName — like a tiny warning web."
            m.contains("spider") || m.contains("marvel") || m.contains("sony") || m.contains("disney") ->
                "I don't use those names, $userName. This is WeaveHome — an original woven home by Jaikar Pothula."
            m.contains("api") || m.contains("gemini") || m.contains("ai ") || m == "ai" || m.contains("chatgpt") ->
                "I'm Pip — local puppet first. Optional Gemini only makes chat smarter while I run background weaves."
            m.contains("mail") || m.contains("email") || m.contains("inbox") ->
                "Woof — mail trail! Open Inbox Pulse and I'll announce: \"Pip here — $userName, you've got mail!\""
            m.contains("message") || m.contains("chat") || m.contains("sms") || m.contains("text") || m.contains("thread") ->
                "ThreadBox is your message launcher — web-shoot sends + WeaveSense buzz."
            m.contains("photo") || m.contains("camera") || m.contains("picture") || m.contains("snap") || m.contains("video") ->
                "SnapBooth is ready for stills and motion. I'll bounce when you land a keeper."
            m.contains("hi") || m.contains("hello") || m.contains("hey") || m.contains("good morning") ||
                m.contains("good night") || m.contains("good evening") ->
                "${timeGreeting(userName)} WeaveSense is armed on your home."
            m.contains("sit") || m.contains("stay") || m.contains("good boy") || m.contains("good pup") ||
                m.contains("good dog") ->
                "*sits proudly* Always, $userName. Loyal house pup mode: ON."
            m.contains("track") || m.contains("watch") || m.contains("guard") || m.contains("help") || m.contains("launcher") ->
                "Unique WeaveHome launcher + ThreadBox message launcher. I highlight what matters with WeaveSense."
            m.contains("name") ->
                "I'm ${AppCredits.MASCOT_NAME}. You're $userName. This nest is ${AppCredits.APP_NAME}."
            m.contains("thank") ->
                "*happy wiggle* Anytime, $userName."
            m.isBlank() ->
                "*tilts head* Say sense, mail, messages, camera — or just hi."
            else -> petReply(userName, m)
        }
    }

    fun announceMail(userName: String): String {
        remember("mail")
        return "Pip here — $userName, you've got mail!"
    }

    fun announceMessage(userName: String, from: String?): String {
        remember("message")
        return if (from.isNullOrBlank()) "Hey $userName — new message just landed!"
        else "Hey $userName — message from $from!"
    }

    fun reactToPhoto(userName: String): String {
        remember("photo")
        return listOf(
            "*tail spin* Nice shot, $userName!",
            "Frame looks sharp — proud of you, $userName.",
            "SnapBooth win. Keep that one, $userName.",
        ).random()
    }

    private fun timeGreeting(userName: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Morning, $userName! Pip's awake and guarding the home screen."
            in 12..16 -> "Hey $userName — afternoon patrol with your home puppet."
            in 17..21 -> "Evening, $userName. I'm curled up on your launcher if you need me."
            else -> "Late night, $userName. I'll keep quiet watch — still here."
        }
    }

    private fun petReply(userName: String, m: String): String {
        val bits = listOf(
            "*nudges your hand* I heard you, $userName. Try sense, mail, messages, or SnapBooth.",
            "I'm a simple home pup with WeaveSense — point me at mail, threads, or the camera.",
            "Woof. Noted. Want the vibe of your day? Say hi, mail, or messages.",
            "Woven into WeaveHome for you, $userName — loyal, local, original.",
        )
        return bits[Random.nextInt(bits.size)]
    }
}
