package com.jaikar.spideyos.companion

import kotlin.random.Random

/** Cute one-liners when you tap SpideyDashPip. */
object DashCuteLines {
    private val taps = listOf(
        "Hehe — you poked me!",
        "Boop! I’m listening~",
        "Hiya! What should we find?",
        "Web-tastic tap!",
        "I’m cute AND helpful — ask away!",
        "Ready to dig! ✨",
        "That tickled… got a job for me?",
        "Yep yep! Search buddy online!",
    )

    fun tap(): String = taps[Random.nextInt(taps.size)]
}
