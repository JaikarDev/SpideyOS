package com.jaikar.spideyos.companion.spidy

import java.util.Calendar

/** Local personality lines — companion feel, not chatbot. */
object SpidyPersonality {
    fun morning(userName: String): String = listOf(
        "Good morning, $userName! Spidy’s awake — want mail, messages, or music?",
        "Morning, $userName! I slept on your home screen. Ready when you are.",
        "Hey $userName — new day. Say digest if you want what you got overnight.",
    ).random()

    fun night(userName: String): String = listOf(
        "Night night, $userName. I’ll keep quiet watch. Long-press me if you need me.",
        "Winding down, $userName. Silent mode tip: say battery saver if you want the panel.",
        "Sweet dreams, $userName. Spidy’s napping nearby.",
    ).random()

    fun dance(userName: String, track: String?): String {
        val t = track?.take(40)?.ifBlank { null }
        return if (t != null) "Dancing to “$t”, $userName!" else "Music’s on — Spidy’s dancing for you, $userName!"
    }

    fun celebrate(userName: String): String =
        "Nice one, $userName! Spidy’s celebrating with you."

    fun birthday(userName: String): String =
        "Happy birthday, $userName! Spidy’s throwing a tiny on-phone party."

    fun joke(): String = listOf(
        "Why did the spider get a website? To catch more bugs!",
        "I told my phone a joke… it hung up. Classic.",
        "What’s Spidy’s favorite band? The Rolling Codes.",
        "Why don’t secrets last on phones? Too many notifications!",
        "I’m not Google… I’m Spidy. I live here, with you.",
    ).random()

    fun wave(userName: String): String =
        "Wave! Hey $userName — what should we do?"
}
