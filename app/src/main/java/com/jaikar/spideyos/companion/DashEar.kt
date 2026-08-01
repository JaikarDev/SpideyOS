package com.jaikar.spideyos.companion

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Always-listen ear for Spidy. Uses online+on-device STT (no forced offline —
 * that breaks on many OnePlus builds). Watchdog keeps the mic session alive.
 */
class DashEar(
    private val context: Context,
    private val onWake: (heard: String) -> Unit,
    private val onCommand: (String) -> Unit,
    private val onStatus: (String) -> Unit = {},
) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var enabled = false
    private var paused = false
    private var commandMode = false
    private var forceCommandOnce = false
    private var restartRunnable: Runnable? = null
    private var resumeSpeakRunnable: Runnable? = null
    private var watchdogRunnable: Runnable? = null
    private var lastWakeAt = 0L
    private var lastTalkAt = 0L
    private var listenGeneration = 0
    private var gotReady = false

    fun start() {
        enabled = true
        paused = false
        commandMode = false
        forceCommandOnce = false
        onStatus("Ears up — talk to me anytime")
        clearTimers()
        main.post { beginListen() }
        armWatchdog()
    }

    fun stop() {
        enabled = false
        paused = false
        commandMode = false
        forceCommandOnce = false
        clearTimers()
        destroyRecognizer()
    }

    /** Force a fresh listen cycle (call after menu / screen on). */
    fun nudge() {
        if (!enabled || paused) return
        destroyRecognizer()
        main.post { beginListen() }
    }

    /** Long-press Spidy: listen for any command without needing the wake word. */
    fun listenNow() {
        if (!enabled) start()
        paused = false
        forceCommandOnce = true
        commandMode = true
        destroyRecognizer()
        onStatus("Listening — talk now")
        main.postDelayed({ if (enabled && !paused) beginListen() }, 200)
    }

    fun pauseForSpeak(ms: Long = 3_500) {
        paused = true
        destroyRecognizer()
        resumeSpeakRunnable?.let { main.removeCallbacks(it) }
        val r = Runnable {
            if (enabled) {
                paused = false
                beginListen()
            }
        }
        resumeSpeakRunnable = r
        main.postDelayed(r, ms)
    }

    fun enterCommandMode() {
        commandMode = true
        destroyRecognizer()
        main.postDelayed({ if (enabled && !paused) beginListen() }, 400)
    }

    private fun clearTimers() {
        restartRunnable?.let { main.removeCallbacks(it) }
        restartRunnable = null
        resumeSpeakRunnable?.let { main.removeCallbacks(it) }
        resumeSpeakRunnable = null
        watchdogRunnable?.let { main.removeCallbacks(it) }
        watchdogRunnable = null
    }

    private fun armWatchdog() {
        watchdogRunnable?.let { main.removeCallbacks(it) }
        val r = Runnable {
            if (!enabled) return@Runnable
            if (!paused) {
                // Soft recycle so OEM speech engines don't die silently.
                destroyRecognizer()
                beginListen()
            }
            armWatchdog()
        }
        watchdogRunnable = r
        main.postDelayed(r, 6_500)
    }

    private fun beginListen() {
        if (!enabled || paused) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus("Enable Google voice typing in system settings")
            scheduleRestart(2_500)
            return
        }
        destroyRecognizer()
        val gen = ++listenGeneration
        gotReady = false
        val r = createRecognizer()
        recognizer = r
        r.setRecognitionListener(makeListener(gen))
        val intent = buildIntent()
        runCatching { r.startListening(intent) }
            .onFailure {
                onStatus("Mic busy — retrying")
                scheduleRestart(1_000)
            }
        // If engine never becomes ready, recycle.
        main.postDelayed({
            if (alive(gen) && !gotReady) {
                destroyRecognizer()
                scheduleRestart(400)
            }
        }, 2_800)
    }

    private fun createRecognizer(): SpeechRecognizer {
        if (Build.VERSION.SDK_INT >= 31) {
            runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }
                .getOrNull()
                ?.let { return it }
        }
        return SpeechRecognizer.createSpeechRecognizer(context)
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Spidy wake words are English — bias US English + device locale.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            // Do NOT set EXTRA_PREFER_OFFLINE — it fails on many OnePlus devices.
        }

    private fun makeListener(gen: Int) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (!alive(gen)) return
            gotReady = true
        }

        override fun onBeginningOfSpeech() {
            if (!alive(gen)) return
            gotReady = true
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            if (!alive(gen)) return
            val texts = allTexts(partialResults)
            if (commandMode || forceCommandOnce) return
            pickWake(texts)?.let { handleWake(it) }
        }

        override fun onResults(results: Bundle?) {
            if (!alive(gen)) return
            val texts = allTexts(results)
            if (commandMode || forceCommandOnce) {
                forceCommandOnce = false
                commandMode = false
                val best = texts.firstOrNull().orEmpty()
                if (best.isNotBlank()) {
                    if (DashCommander.containsWake(best) && DashCommander.stripWake(best).length <= 2) {
                        handleWake(best)
                    } else if (DashCommander.containsWake(best)) {
                        handleWake(best)
                    } else {
                        onCommand(best)
                    }
                }
                scheduleRestart(500)
                return
            }
            val wake = pickWake(texts)
            if (wake != null) {
                handleWake(wake)
            } else {
                val best = texts.firstOrNull { it.length >= 2 }
                if (best != null) handleAnyTalk(best) else scheduleRestart(220)
            }
        }

        override fun onError(error: Int) {
            if (!alive(gen) || paused) return
            val delay = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> 220L
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1_600L
                SpeechRecognizer.ERROR_CLIENT -> 1_000L
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    onStatus("Allow microphone in Nest")
                    4_000L
                }
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                -> 700L
                else -> 800L
            }
            scheduleRestart(delay)
        }
    }

    private fun alive(gen: Int): Boolean =
        enabled && !paused && gen == listenGeneration

    private fun allTexts(bundle: Bundle?): List<String> =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun pickWake(texts: List<String>): String? =
        texts.firstOrNull { DashCommander.containsWake(it) }

    private fun handleAnyTalk(heard: String) {
        val now = System.currentTimeMillis()
        if (now - lastTalkAt < 1_400) {
            scheduleRestart(350)
            return
        }
        lastTalkAt = now
        destroyRecognizer()
        // Any speech (not only Spidy) — buddy reacts + replies.
        onCommand(heard)
        scheduleRestart(1_000)
    }

    private fun handleWake(heard: String) {
        val now = System.currentTimeMillis()
        if (now - lastWakeAt < 1_000) return
        lastWakeAt = now
        lastTalkAt = now
        forceCommandOnce = false
        val remainder = DashCommander.stripWake(heard)
        commandMode = true
        destroyRecognizer()
        onWake(heard)
        if (remainder.isNotBlank() && remainder.length > 2 && !DashCommander.containsWake(remainder)) {
            commandMode = false
            onCommand(remainder)
            scheduleRestart(900)
        } else {
            main.postDelayed({ if (enabled && !paused) beginListen() }, 450)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        restartRunnable?.let { main.removeCallbacks(it) }
        val r = Runnable {
            if (enabled && !paused) beginListen()
        }
        restartRunnable = r
        main.postDelayed(r, delayMs)
    }

    private fun destroyRecognizer() {
        listenGeneration++
        runCatching { recognizer?.setRecognitionListener(null) }
        runCatching { recognizer?.stopListening() }
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}
