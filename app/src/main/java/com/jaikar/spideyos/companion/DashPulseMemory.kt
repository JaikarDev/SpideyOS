package com.jaikar.spideyos.companion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device only inbox memory — mail & messages Spidey can report later.
 * Stored in app SharedPreferences on this phone; never uploaded.
 * Phone numbers / emails are redacted before save.
 */
object DashPulseMemory {
    data class Pulse(
        val type: String,
        val app: String,
        val from: String?,
        val atMs: Long,
    )

    private const val PREFS = "spidey_pulse_memory"
    private const val KEY = "pulses_json"
    private const val MAX = 40
    private val lock = Any()
    private val pulses = ArrayDeque<Pulse>()
    private var loaded = false

    fun remember(context: Context, type: String, app: String, from: String?) {
        synchronized(lock) {
            ensureLoaded(context)
            val safeFrom = DashPrivacyGuard.redactPii(from).ifBlank { null }
            val safeApp = DashPrivacyGuard.redactPii(app).ifBlank { "App" }
            pulses.addFirst(Pulse(type, safeApp, safeFrom?.take(80), System.currentTimeMillis()))
            while (pulses.size > MAX) pulses.removeLast()
            persist(context)
        }
    }

    fun recent(context: Context, limit: Int = 8): List<Pulse> {
        synchronized(lock) {
            ensureLoaded(context)
            return pulses.take(limit)
        }
    }

    fun summaryForSpeak(context: Context, userName: String): String {
        val list = recent(context, 6)
        if (list.isEmpty()) {
            return "Hey $userName — inbox is quiet right now. No fresh mail or messages on this phone."
        }
        val mail = list.count { it.type.equals("mail", true) }
        val msgs = list.count { !it.type.equals("mail", true) }
        val parts = mutableListOf<String>()
        if (mail > 0) parts += if (mail == 1) "you got mail" else "you got $mail mail alerts"
        if (msgs > 0) parts += if (msgs == 1) "you got a message" else "you got $msgs messages"
        val head = "Hey $userName — here you go: ${parts.joinToString(" and ")}."
        val detail = list.take(3).joinToString(". ") { p ->
            val who = DashPrivacyGuard.redactPii(p.from).takeIf { it.isNotBlank() } ?: p.app
            if (p.type.equals("mail", true)) "Mail from $who" else "${p.app}: $who"
        }
        return DashPrivacyGuard.safeSpeakLine(
            "$head $detail. Kept only on your phone — no numbers, no location, no cloud.",
        )
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?: return
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                pulses.addLast(
                    Pulse(
                        type = o.optString("type"),
                        app = DashPrivacyGuard.redactPii(o.optString("app")).ifBlank { "App" },
                        from = DashPrivacyGuard.redactPii(o.optString("from")).ifBlank { null },
                        atMs = o.optLong("at"),
                    ),
                )
            }
        }
    }

    private fun persist(context: Context) {
        val arr = JSONArray()
        pulses.forEach { p ->
            arr.put(
                JSONObject()
                    .put("type", p.type)
                    .put("app", p.app)
                    .put("from", p.from ?: "")
                    .put("at", p.atMs),
            )
        }
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }
}
