package com.jaikar.spideyos.companion.spidy

import android.content.Context
import com.jaikar.spideyos.companion.DashPrivacyGuard
import org.json.JSONArray
import org.json.JSONObject

/** Transparent on-device activity log for Nest. */
object SpidyActivityLog {
    data class Entry(val action: String, val detail: String, val atMs: Long)

    private const val PREFS = "spidy_activity_log"
    private const val KEY = "log_json"
    private const val MAX = 30

    fun append(context: Context, action: String, detail: String) {
        val prefs = prefs(context)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        val next = JSONArray()
        next.put(
            JSONObject()
                .put("a", DashPrivacyGuard.redactPii(action).take(40))
                .put("d", DashPrivacyGuard.redactPii(detail).take(80))
                .put("t", System.currentTimeMillis()),
        )
        for (i in 0 until minOf(arr.length(), MAX - 1)) next.put(arr.getJSONObject(i))
        prefs.edit().putString(KEY, next.toString()).apply()
    }

    fun recent(context: Context, limit: Int = 12): List<Entry> {
        val raw = prefs(context).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until minOf(arr.length(), limit)) {
                    val o = arr.getJSONObject(i)
                    add(Entry(o.optString("a"), o.optString("d"), o.optLong("t")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
