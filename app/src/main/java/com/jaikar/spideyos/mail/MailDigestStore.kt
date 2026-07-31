package com.jaikar.spideyos.mail

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jaikar.spideyos.R
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.assistant.GeminiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MailItem(
    val from: String,
    val subject: String,
    val preview: String,
    val unread: Boolean = true,
)

object MailDigestStore {
    val demoInbox = listOf(
        MailItem("Daily Bugle", "Exclusive: Web-slinger spotted", "Editors want photos by noon."),
        MailItem("Aunt May", "Dinner Sunday?", "I made your favorite — don’t be late."),
        MailItem("Oscorp HR", "Internship update", "Please review the attached offer."),
        MailItem("Campus Lab", "Spectrometer results", "Raw data ready for your report."),
    )

    fun scheduleDemoPing(context: Context, delayMs: Long = 3_000L) {
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            delay(delayMs)
            postMailNotification(appCtx)
        }
    }

    fun postMailNotification(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            val settings = SpideyApp.instance.settings.settings.first()
            if (!settings.mailEnabled || !settings.spideyVoiceEnabled) return@launch
            val title = GeminiClient { "" }.announceMail(settings.userName)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "spidey_mail"
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Spidey Mail", NotificationManager.IMPORTANCE_DEFAULT),
            )
            nm.notify(
                1901,
                NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_legacy)
                    .setContentTitle(title)
                    .setContentText("Open Mail Digest for the short version, ${settings.userName}.")
                    .setAutoCancel(true)
                    .build(),
            )
        }
    }
}

class MailDigestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        MailDigestStore.postMailNotification(context)
    }
}
