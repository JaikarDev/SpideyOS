package com.jaikar.spideyos.companion.spidy

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.jaikar.spideyos.companion.DashPrivacyGuard

/** Safe on-device automations — panels & torch; no privileged radio hacks. */
object SpidyAutomation {
    @Volatile
    private var torchOn = false

    sealed class Result {
        data class Spoke(val line: String) : Result()
        data class SpokeAndDid(val line: String) : Result()
    }

    fun setTorch(context: Context, on: Boolean): Result {
        return runCatching {
            val cm = context.getSystemService(CameraManager::class.java) ?: return Result.Spoke(
                "Camera torch isn’t available on this phone.",
            )
            val id = cm.cameraIdList.firstOrNull { camId ->
                cm.getCameraCharacteristics(camId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.Spoke("No flashlight on this device.")
            cm.setTorchMode(id, on)
            torchOn = on
            SpidyActivityLog.append(context, "torch", if (on) "on" else "off")
            Result.SpokeAndDid(if (on) "Flashlight on." else "Flashlight off.")
        }.getOrElse {
            Result.Spoke("Couldn’t toggle flashlight — check camera permission.")
        }
    }

    fun toggleTorch(context: Context): Result = setTorch(context, !torchOn)

    fun openWifiPanel(context: Context): Result =
        openPanel(context, Settings.Panel.ACTION_WIFI, "Opening Wi‑Fi panel.")

    fun openBluetoothPanel(context: Context): Result {
        val action = if (Build.VERSION.SDK_INT >= 31) {
            Settings.ACTION_BLUETOOTH_SETTINGS
        } else {
            Settings.ACTION_BLUETOOTH_SETTINGS
        }
        return openSettings(context, action, "Opening Bluetooth settings.")
    }

    fun openBatterySaver(context: Context): Result =
        openSettings(context, Settings.ACTION_BATTERY_SAVER_SETTINGS, "Opening battery saver.")

    fun openDisplay(context: Context): Result =
        openSettings(context, Settings.ACTION_DISPLAY_SETTINGS, "Opening display settings — dark mode lives there.")

    fun openSound(context: Context): Result =
        openSettings(context, Settings.ACTION_SOUND_SETTINGS, "Opening sound settings — silent mode is there.")

    fun openWeather(context: Context, userName: String): Result {
        val q = Uri.encode("weather")
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(web)
            SpidyActivityLog.append(context, "weather", "search")
            Result.SpokeAndDid("Opening weather for you, $userName — I don’t use your location myself.")
        }.getOrElse {
            Result.Spoke("Couldn’t open weather search.")
        }
    }

    fun openWebSearch(context: Context, query: String): Result {
        val q = DashPrivacyGuard.redactPii(query).ifBlank { return Result.Spoke("What should I search?") }
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", q)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            SpidyActivityLog.append(context, "search", q.take(40))
            Result.SpokeAndDid("Searching for “${q.take(40)}”.")
        }.getOrElse {
            Result.Spoke("Search didn’t open.")
        }
    }

    private fun openPanel(context: Context, action: String, line: String): Result {
        if (Build.VERSION.SDK_INT < 29) {
            return openSettings(context, Settings.ACTION_WIFI_SETTINGS, line)
        }
        return runCatching {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            SpidyActivityLog.append(context, "panel", action.substringAfterLast('.'))
            Result.SpokeAndDid(line)
        }.getOrElse {
            openSettings(context, Settings.ACTION_WIFI_SETTINGS, line)
        }
    }

    private fun openSettings(context: Context, action: String, line: String): Result =
        runCatching {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            SpidyActivityLog.append(context, "settings", action.substringAfterLast('.'))
            Result.SpokeAndDid(line)
        }.getOrElse {
            Result.Spoke("Couldn’t open that settings screen.")
        }
}
