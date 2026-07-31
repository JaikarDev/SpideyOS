package com.jaikar.spideyos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.jaikar.spideyos.R
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.companion.DashEvent
import com.jaikar.spideyos.companion.DashNotifyApps
import com.jaikar.spideyos.companion.SpideyDashPipBus
import com.jaikar.spideyos.sense.WeaveSense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Watches notifications — SpideyDashPip speaks mail / WhatsApp / Instagram / etc. */
class SpideyNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return
        scope.launch {
            val settings = SpideyApp.instance.settings.settings.first()
            if (!settings.spideyVoiceEnabled) return@launch

            val extras = sbn.notification.extras
            val title = extras?.getCharSequence("android.title")?.toString().orEmpty()
            val text = extras?.getCharSequence("android.text")?.toString().orEmpty()
            val pkg = sbn.packageName.orEmpty()
            val kind = DashNotifyApps.classify(pkg) ?: run {
                if (title.contains("mail", true) || text.contains("mail", true)) {
                    DashNotifyApps.Kind(DashNotifyApps.Type.MAIL, "Mail")
                } else null
            } ?: return@launch

            val speak = DashNotifyApps.speakLine(settings.userName, kind, title.ifBlank { null })
            when (kind.type) {
                DashNotifyApps.Type.MAIL ->
                    SpideyDashPipBus.emit(DashEvent.Mail(from = title.ifBlank { null }, speak = speak))
                else ->
                    SpideyDashPipBus.emit(
                        DashEvent.Message(
                            from = title.ifBlank { null },
                            appName = kind.appName,
                            speak = speak,
                        ),
                    )
            }

            WeaveSense.notify(this@SpideyNotificationListener)
            postHighlight(
                title = "✦ SpideyDashPip · $speak",
                body = buildString {
                    append(kind.appName)
                    if (title.isNotBlank()) append(" · ").append(title)
                    append('\n')
                    append(text.ifBlank { "Tap SpideyDashPip for options" })
                },
                urgent = true,
            )
        }
    }

    private fun postHighlight(title: String, body: String, urgent: Boolean) {
        val channelId = if (urgent) "weave_sense_high" else "weave_sense"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(nm)
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_legacy)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF3DBE8B.toInt())
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
    }

    private fun ensureChannels(nm: NotificationManager) {
        nm.createNotificationChannel(
            NotificationChannel("weave_sense_high", "SpideyDashPip Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Spoken mail & message alerts"
                enableVibration(true)
                vibrationPattern = WeaveSense.PATTERN_NOTIFY
                enableLights(true)
                lightColor = Color.parseColor("#3DBE8B")
                if (Build.VERSION.SDK_INT >= 29) setAllowBubbles(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel("weave_sense", "SpideyDashPip", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = WeaveSense.PATTERN_SENSE
            },
        )
    }
}
