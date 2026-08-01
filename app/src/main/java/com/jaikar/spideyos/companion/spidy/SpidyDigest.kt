package com.jaikar.spideyos.companion.spidy

import android.content.Context
import com.jaikar.spideyos.companion.DashPrivacyGuard
import com.jaikar.spideyos.companion.DashPulseMemory

/** Batched “You received …” digest from on-device pulse memory. */
object SpidyDigest {
    fun speak(context: Context, userName: String): String {
        val list = DashPulseMemory.recent(context, 20)
        if (list.isEmpty()) {
            return "Quiet inbox, $userName — no fresh mail or messages stored on this phone."
        }
        val byApp = linkedMapOf<String, Int>()
        var mail = 0
        var msgs = 0
        list.forEach { p ->
            val app = DashPrivacyGuard.redactPii(p.app).ifBlank { "App" }
            byApp[app] = (byApp[app] ?: 0) + 1
            if (p.type.equals("mail", true)) mail++ else msgs++
        }
        val bullets = byApp.entries.take(6).joinToString(", ") { (app, n) ->
            if (n == 1) "1 $app" else "$n $app"
        }
        return DashPrivacyGuard.safeSpeakLine(
            "You received, $userName: $bullets. " +
                "That’s $mail mail and $msgs message alerts kept on-device only.",
        )
    }

    fun counts(context: Context): Pair<Int, Int> {
        val list = DashPulseMemory.recent(context, 40)
        val mail = list.count { it.type.equals("mail", true) }
        val msgs = list.size - mail
        return mail to msgs
    }
}
