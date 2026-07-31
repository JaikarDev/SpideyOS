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
import com.jaikar.spideyos.assistant.PupBrain
import com.jaikar.spideyos.sense.WeaveSense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Pip watches notifications and fires WeaveSense highlights. */
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

            val isMail = isMailPackage(pkg) || title.contains("mail", true) || text.contains("mail", true)
            val isMsg = isMessagePackage(pkg)
            val pipText = when {
                isMail -> PupBrain.announceMail(settings.userName)
                isMsg -> PupBrain.announceMessage(settings.userName, title.ifBlank { null })
                else -> null
            } ?: return@launch

            WeaveSense.notify(this@SpideyNotificationListener)
            postHighlight(
                title = "✦ WeaveSense · $pipText",
                body = buildString {
                    append("Highlighted for ${settings.userName}\n")
                    if (title.isNotBlank()) append(title).append('\n')
                    append(text.ifBlank { "Open ThreadBox or Inbox Pulse" })
                },
                urgent = isMail || isMsg,
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
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
                    .setSummaryText("WeaveHome · Pip highlight"),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF3DBE8B.toInt())
            .setColorized(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
    }

    private fun ensureChannels(nm: NotificationManager) {
        val high = NotificationChannel(
            "weave_sense_high",
            "WeaveSense Highlights",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Rich highlighted alerts from Pip"
            enableVibration(true)
            vibrationPattern = WeaveSense.PATTERN_NOTIFY
            enableLights(true)
            lightColor = Color.parseColor("#3DBE8B")
            if (Build.VERSION.SDK_INT >= 29) setAllowBubbles(true)
        }
        val normal = NotificationChannel(
            "weave_sense",
            "WeaveSense",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            enableVibration(true)
            vibrationPattern = WeaveSense.PATTERN_SENSE
            lightColor = Color.parseColor("#F4C430")
        }
        nm.createNotificationChannel(high)
        nm.createNotificationChannel(normal)
    }

    private fun isMailPackage(pkg: String): Boolean {
        val mail = listOf("gmail", "mail", "outlook", "yahoo.mobile.client.android.mail", "samsung.android.email")
        return mail.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isMessagePackage(pkg: String): Boolean {
        val msg = listOf("messaging", "sms", "mms", "whatsapp", "telegram", "signal", "messages")
        return msg.any { pkg.contains(it, ignoreCase = true) }
    }
}
