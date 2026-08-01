package com.jaikar.spideyos.companion.spidy

import android.content.Context
import com.jaikar.spideyos.companion.DashPrivacyGuard
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device companion memory — preferences + short facts.
 * Never uploads; PII redacted on write.
 */
object SpidyMemory {
    private const val PREFS = "spidy_memory_v1"
    private const val KEY_FACTS = "facts_json"
    private const val KEY_LAST_TOPIC = "last_topic"
    private const val KEY_BIRTHDAY = "birthday"
    private const val KEY_YDAY = "yesterday_summary"
    private const val MAX_FACTS = 24

    fun rememberFact(context: Context, fact: String) {
        val safe = DashPrivacyGuard.redactPii(fact).take(160).ifBlank { return }
        val prefs = prefs(context)
        val arr = JSONArray(prefs.getString(KEY_FACTS, "[]"))
        val next = JSONArray()
        next.put(
            JSONObject()
                .put("t", safe)
                .put("at", System.currentTimeMillis()),
        )
        for (i in 0 until minOf(arr.length(), MAX_FACTS - 1)) {
            next.put(arr.getJSONObject(i))
        }
        prefs.edit()
            .putString(KEY_FACTS, next.toString())
            .putString(KEY_LAST_TOPIC, safe)
            .apply()
    }

    fun setBirthday(context: Context, value: String) {
        prefs(context).edit()
            .putString(KEY_BIRTHDAY, DashPrivacyGuard.redactPii(value).take(40))
            .apply()
    }

    fun birthday(context: Context): String? =
        prefs(context).getString(KEY_BIRTHDAY, null)?.takeIf { it.isNotBlank() }

    fun setYesterdaySummary(context: Context, summary: String) {
        prefs(context).edit()
            .putString(KEY_YDAY, DashPrivacyGuard.redactPii(summary).take(200))
            .apply()
    }

    fun yesterdaySummary(context: Context): String? =
        prefs(context).getString(KEY_YDAY, null)?.takeIf { it.isNotBlank() }

    fun lastTopic(context: Context): String? =
        prefs(context).getString(KEY_LAST_TOPIC, null)?.takeIf { it.isNotBlank() }

    fun speakRecall(context: Context, userName: String): String {
        val facts = facts(context).take(4)
        val topic = lastTopic(context)
        val bday = birthday(context)
        if (facts.isEmpty() && topic == null && bday == null) {
            return "I don’t have memories stored yet, $userName. Say “remember that …” and I’ll keep it on this phone only."
        }
        val parts = mutableListOf<String>()
        if (bday != null) parts += "birthday note: $bday"
        if (topic != null) parts += "last topic: $topic"
        if (facts.isNotEmpty()) parts += "I remember: " + facts.joinToString("; ")
        return "Here’s what I kept on-device for you, $userName. ${parts.joinToString(". ")}."
    }

    fun continueYesterday(context: Context, userName: String): String {
        val y = yesterdaySummary(context)
        return if (y != null) {
            "Continuing from yesterday, $userName: $y"
        } else {
            val topic = lastTopic(context)
            if (topic != null) "Picking up where we left off, $userName: $topic"
            else "Nothing saved from yesterday yet, $userName. Chat a bit and I’ll remember."
        }
    }

    fun facts(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_FACTS, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.getJSONObject(i).optString("t"))
                }
            }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
