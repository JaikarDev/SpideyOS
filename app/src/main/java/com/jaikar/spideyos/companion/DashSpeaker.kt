package com.jaikar.spideyos.companion

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** SpideyDashPip speaks aloud — mail, messages, greetings. */
class DashSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.US
            tts?.setSpeechRate(1.05f)
            tts?.setPitch(1.12f)
            pending?.let {
                pending = null
                speak(it)
            }
        }
    }

    fun speak(text: String) {
        val line = text.trim()
        if (line.isEmpty()) return
        if (!ready) {
            pending = line
            return
        }
        tts?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "spideydashpip-${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
