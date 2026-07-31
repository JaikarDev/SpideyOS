package com.jaikar.spideyos.companion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Screen on → wake buddy · screen off → sleep. Lives on stock OEM home. */
class SpideyDashPipLifeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> SpideyDashPipBus.emit(DashEvent.GoSleep)
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT -> SpideyDashPipBus.emit(DashEvent.WakeUp)
        }
    }
}
