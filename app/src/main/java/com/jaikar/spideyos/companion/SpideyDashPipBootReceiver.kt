package com.jaikar.spideyos.companion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.assistant.SpideyOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Boots SpideyDashPip on your phone’s existing OS (overlay — not a launcher theme).
 */
class SpideyDashPipBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = SpideyApp.instance.settings.settings.first()
                if (settings.buddyEnabled && Settings.canDrawOverlays(context)) {
                    SpideyOverlayService.start(context.applicationContext)
                    SpideyDashPipBus.emit(DashEvent.WakeUp)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
