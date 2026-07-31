package com.jaikar.spideyos.companion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** On-device voice listen — responds with the user's name (no cloud AI required). */
class DashVoice(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit = {},
) {
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Voice not available on this phone")
            return
        }
        stop()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) {
                onError("Didn't catch that — tap Listen again")
            }
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isBlank()) onError("Hmm, silence. Try again?")
                else onResult(text)
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        r.startListening(intent)
    }

    fun stop() {
        runCatching {
            recognizer?.cancel()
            recognizer?.destroy()
        }
        recognizer = null
    }
}

object DashVoiceReply {
    fun reply(userName: String, heard: String): String {
        val h = heard.lowercase()
        return when {
            h.contains("mail") || h.contains("email") ->
                "Hey $userName — I'll watch your mail and say when it lands!"
            h.contains("message") || h.contains("whatsapp") || h.contains("instagram") ->
                "Got it $userName — I'll shout when WhatsApp or Instagram pings you."
            h.contains("music") || h.contains("song") || h.contains("spotify") || h.contains("youtube") ->
                "Okay $userName — earbuds ready for Spotify, YouTube Music, and more."
            h.contains("hello") || h.contains("hi") || h.contains("hey") ->
                "Hey $userName! SpideyDashPip here — what should we find?"
            h.contains("name") ->
                "You're $userName — and I'm SpideyDashPip, your search buddy."
            h.contains("sleep") || h.contains("bye") ->
                "Night night, $userName. I'll nap when the screen sleeps."
            else ->
                "Hey $userName, I heard “$heard”. Tap me to search apps, photos, or the web!"
        }
    }
}
