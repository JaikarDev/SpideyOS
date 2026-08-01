package com.jaikar.spideyos.companion.spidy

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.jaikar.spideyos.companion.DashPrivacyGuard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** On-device calendar meetings — read-only, no location APIs. */
object SpidyMeetings {
    data class Meeting(
        val id: Long,
        val title: String,
        val startMs: Long,
        val endMs: Long,
    )

    private const val PREFS = "spidy_meetings"
    private const val KEY_REMINDED = "reminded_ids"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun upcoming(context: Context, withinHours: Int = 36, limit: Int = 12): List<Meeting> {
        if (!hasPermission(context)) return emptyList()
        val now = System.currentTimeMillis()
        val end = now + TimeUnit.HOURS.toMillis(withinHours.toLong())
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, end)
            it.build()
        }
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
        )
        return runCatching {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                val idIx = c.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIx = c.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIx = c.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIx = c.getColumnIndex(CalendarContract.Instances.END)
                buildList {
                    while (c.moveToNext() && size < limit) {
                        val title = DashPrivacyGuard.redactPii(c.getString(titleIx) ?: "Meeting")
                            .ifBlank { "Meeting" }
                            .take(80)
                        add(
                            Meeting(
                                id = c.getLong(idIx),
                                title = title,
                                startMs = c.getLong(beginIx),
                                endMs = c.getLong(endIx),
                            ),
                        )
                    }
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun today(context: Context): List<Meeting> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayEnd = cal.timeInMillis
        return upcoming(context, withinHours = 48, limit = 30)
            .filter { it.startMs in dayStart until dayEnd || (it.startMs < dayStart && it.endMs > dayStart) }
    }

    fun next(context: Context): Meeting? =
        upcoming(context, withinHours = 72, limit = 1).firstOrNull { it.startMs >= System.currentTimeMillis() - 60_000 }

    fun speakSummary(context: Context, userName: String): String {
        if (!hasPermission(context)) {
            return "Calendar access is off, $userName. Allow calendar in Nest Permissions and I’ll track meetings."
        }
        val list = today(context)
        if (list.isEmpty()) {
            val n = next(context)
            return if (n == null) {
                "No meetings on your calendar today, $userName."
            } else {
                "Nothing else today — next up is “${n.title}” ${formatWhen(n.startMs)}."
            }
        }
        val bits = list.take(4).joinToString(". ") { m ->
            "“${m.title}” at ${formatClock(m.startMs)}"
        }
        return DashPrivacyGuard.safeSpeakLine(
            "You’ve got ${list.size} meeting${if (list.size == 1) "" else "s"} today, $userName. $bits.",
        )
    }

    fun speakNext(context: Context, userName: String): String {
        if (!hasPermission(context)) {
            return "I need calendar permission to see meetings, $userName."
        }
        val n = next(context) ?: return "You’re clear — no upcoming meetings I can see, $userName."
        return "Next meeting: “${n.title}”, ${formatWhen(n.startMs)}, $userName."
    }

    /** Meetings starting soon that we haven’t reminded yet. */
    fun dueReminders(context: Context, windowMin: Int = 12): List<Meeting> {
        if (!hasPermission(context)) return emptyList()
        val now = System.currentTimeMillis()
        val soon = now + TimeUnit.MINUTES.toMillis(windowMin.toLong())
        val reminded = remindedIds(context)
        return upcoming(context, withinHours = 6, limit = 20)
            .filter { it.startMs in (now + 30_000)..soon && it.id !in reminded }
    }

    fun markReminded(context: Context, meeting: Meeting) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = remindedIds(context).toMutableSet()
        set += meeting.id
        // Keep set small
        if (set.size > 40) {
            val keep = upcoming(context, 72, 20).map { it.id }.toSet()
            set.retainAll(keep)
            set += meeting.id
        }
        prefs.edit().putStringSet(KEY_REMINDED, set.map { it.toString() }.toSet()).apply()
        SpidyActivityLog.append(context, "meeting_remind", meeting.title.take(40))
    }

    fun remindLine(userName: String, meeting: Meeting): String =
        "Heads up, $userName — “${meeting.title}” starts ${formatWhen(meeting.startMs)}."

    private fun remindedIds(context: Context): Set<Long> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_REMINDED, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun formatClock(ms: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

    private fun formatWhen(ms: Long): String {
        val mins = TimeUnit.MILLISECONDS.toMinutes(ms - System.currentTimeMillis())
        return when {
            mins <= 1 -> "right now"
            mins < 60 -> "in $mins minutes"
            else -> "at ${formatClock(ms)}"
        }
    }
}
