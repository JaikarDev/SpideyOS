package com.jaikar.spideyos.companion.spidy

import android.content.Context
import java.util.Calendar

/** Once-per-day morning / night routine gates (on-device). */
object SpidyRoutines {
    private const val PREFS = "spidy_routines"
    private const val KEY_MORNING_DAY = "morning_day"
    private const val KEY_NIGHT_DAY = "night_day"

    fun isMorningWindow(cal: Calendar = Calendar.getInstance()): Boolean {
        val h = cal.get(Calendar.HOUR_OF_DAY)
        return h in 5..10
    }

    fun isNightWindow(cal: Calendar = Calendar.getInstance()): Boolean {
        val h = cal.get(Calendar.HOUR_OF_DAY)
        return h >= 21 || h < 5
    }

    fun shouldSpeakMorning(context: Context, enabled: Boolean): Boolean {
        if (!enabled || !isMorningWindow()) return false
        val day = dayKey()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_MORNING_DAY, null) == day) return false
        prefs.edit().putString(KEY_MORNING_DAY, day).apply()
        return true
    }

    fun shouldSpeakNight(context: Context, enabled: Boolean): Boolean {
        if (!enabled || !isNightWindow()) return false
        val day = dayKey()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_NIGHT_DAY, null) == day) return false
        prefs.edit().putString(KEY_NIGHT_DAY, day).apply()
        return true
    }

    private fun dayKey(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }
}
