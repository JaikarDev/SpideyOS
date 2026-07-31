package com.jaikar.spideyos.companion

import java.util.Calendar
import kotlin.random.Random

/** Scripted suggestions — no generative AI. */
object DashSuggestScript {
    fun forNow(userName: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val pool = when (hour) {
            in 5..10 -> listOf(
                "Morning, $userName! Want me to check your mail?",
                "Stretch break? I can open SnapBooth for a quick smile.",
                "Coffee playlist? Tap me and I’ll wear earbuds.",
                "Shall I peek Inbox Pulse for overnight mail?",
            )
            in 11..16 -> listOf(
                "Midday tip: want me to search an app for you?",
                "Messages piling up? I can catch them when they land.",
                "Photo break? Let’s peek your gallery together.",
                "Need ThreadBox? I can hop you there.",
            )
            in 17..21 -> listOf(
                "Evening check — want music with earbuds?",
                "Any mail left? I can pick it up for you.",
                "Snap a sunset in SnapBooth? I’m ready.",
                "Tired eyes? I’ll keep watch while you scroll.",
            )
            else -> listOf(
                "Late night, $userName — I can go quiet if you tap hide.",
                "Night owl mode: I’ll roam soft and suggest less.",
                "Want wind-down music? Tap me for earbuds.",
                "I’ll nap when the screen sleeps — wake me tomorrow!",
            )
        }
        return pool[Random.nextInt(pool.size)]
    }

    fun wakeLine(userName: String): String =
        listOf(
            "Yawn… hi $userName! SpideyDashPip waking up.",
            "Screen’s on — I’m awake and ready to roam!",
            "Good to see you, $userName! What should we do?",
        ).random()

    fun sleepLine(userName: String): String =
        listOf(
            "Screen off… night night, $userName. Zzz…",
            "Going to sleep. Wake me when you unlock.",
            "Tucking in — see you when the phone wakes.",
        ).random()
}
