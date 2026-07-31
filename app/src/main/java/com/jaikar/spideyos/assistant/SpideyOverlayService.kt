package com.jaikar.spideyos.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.jaikar.spideyos.MainActivity
import com.jaikar.spideyos.R
import com.jaikar.spideyos.SpideyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpideyOverlayService : Service() {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var windowManager: WindowManager? = null
    private var bubble: FrameLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
        if (Settings.canDrawOverlays(this)) {
            attachBubble()
        }
    }

    private fun attachBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        // Scale bubble with screen density / size (phones → tablets)
        val sw = resources.configuration.smallestScreenWidthDp
        val base = when {
            sw >= 600 -> 64f
            sw < 360 -> 48f
            else -> 56f
        }
        val size = (base * density).toInt()
        val view = FrameLayout(this).apply {
            setBackgroundColor(0xFFC41E3A.toInt())
            val label = TextView(this@SpideyOverlayService).apply {
                text = "S"
                setTextColor(0xFFE8EEF5.toInt())
                textSize = if (sw >= 600) 26f else 22f
                gravity = Gravity.CENTER
            }
            addView(label, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
            setOnClickListener {
                startActivity(
                    Intent(this@SpideyOverlayService, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        bubble = view
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (12 * density).toInt()
            y = (120 * density).toInt()
        }
        windowManager?.addView(view, params)

        scope.launch {
            val name = SpideyApp.instance.settings.settings.first().userName
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification("Hey $name — Spidey is on watch."))
        }
    }

    private fun buildNotification(content: String = "Spidey assistant overlay active"): Notification {
        val channelId = "spidey_overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.overlay_channel_desc) },
        )
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SpideyOS")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_legacy)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        bubble?.let { runCatching { windowManager?.removeView(it) } }
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1701

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, SpideyOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SpideyOverlayService::class.java))
        }
    }
}
