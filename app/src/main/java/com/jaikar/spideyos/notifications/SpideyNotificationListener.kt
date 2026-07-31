package com.jaikar.spideyos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.jaikar.spideyos.R
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.assistant.PupBrain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Pip watches allowed notifications on-device — no cloud AI. */
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

            val pipText = when {
                isMailPackage(pkg) || title.contains("mail", true) || text.contains("mail", true) ->
                    PupBrain.announceMail(settings.userName)
                isMessagePackage(pkg) ->
                    PupBrain.announceMessage(settings.userName, title.ifBlank { null })
                else -> null
            } ?: return@launch

            postPipAlert(pipText, "From your phone · tracked locally by Pip")
        }
    }

    private fun postPipAlert(title: String, body: String) {
        val channelId = "pip_watch"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Pip Watch", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_legacy)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
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
