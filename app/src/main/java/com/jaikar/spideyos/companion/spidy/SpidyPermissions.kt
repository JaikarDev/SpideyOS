package com.jaikar.spideyos.companion.spidy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.jaikar.spideyos.data.SpideySettings

data class SpidyCapability(
    val id: String,
    val title: String,
    val detail: String,
    val granted: Boolean,
    val system: Boolean = true,
)

/** Single source for Nest permission dashboard. */
object SpidyPermissions {
    fun snapshot(context: Context, settings: SpideySettings): List<SpidyCapability> {
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val photos = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
        return listOf(
            SpidyCapability(
                "overlay",
                "Display over apps",
                "Lets Spidy float on your home screen",
                Settings.canDrawOverlays(context),
            ),
            SpidyCapability(
                "mic",
                "Microphone",
                "Wake word + voice commands (on-device STT)",
                mic,
            ),
            SpidyCapability(
                "notifications",
                "Notification access",
                "Mail / WhatsApp pulse — enable in system settings",
                settings.spideyVoiceEnabled, // proxy; real binder checked in Nest buttons
                system = true,
            ),
            SpidyCapability(
                "photos",
                "Photos",
                "Gallery peek only — no hidden albums auto-scan",
                photos,
            ),
            SpidyCapability(
                "routines",
                "Morning / night routines",
                "One greeting per window, on-device",
                settings.routinesEnabled,
                system = false,
            ),
            SpidyCapability(
                "digest",
                "Smart notification digest",
                "Batched “you received …” summaries",
                settings.digestEnabled,
                system = false,
            ),
            SpidyCapability(
                "memory",
                "Companion memory",
                "Remember facts you say — local only",
                settings.memoryEnabled,
                system = false,
            ),
            SpidyCapability(
                "automation",
                "Device actions",
                "Flashlight + open settings panels",
                settings.automationEnabled,
                system = false,
            ),
            SpidyCapability(
                "meetings",
                "Meetings & reminders",
                "Read calendar + speak reminders before events",
                settings.meetingsEnabled && (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                        PackageManager.PERMISSION_GRANTED
                    ),
                system = false,
            ),
        )
    }

    const val PRIVACY_PLEDGE =
        "Spidy is local-first: no location, no phone numbers, no hidden/sensitive files, " +
            "and companion memory stays on this phone unless you clear it."
}
