package com.jaikar.spideyos.sense

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * WeaveSense — local haptic “danger tingle” / web-shoot feedback.
 * Original fan-made OS feel. No studio or franchise names.
 */
object WeaveSense {
    /** Tingling alert when something important lands. */
    val PATTERN_SENSE = longArrayOf(0, 35, 50, 35, 50, 80, 40)

    /** Web-shoot send burst. */
    val PATTERN_SHOOT = longArrayOf(0, 18, 30, 45, 20, 70)

    /** Highlight notification pulse. */
    val PATTERN_NOTIFY = longArrayOf(0, 60, 40, 60, 40, 120)

    /** Soft success / wag. */
    val PATTERN_WAG = longArrayOf(0, 25, 40, 25)

    /** Open app / dock tap. */
    val PATTERN_TAP = longArrayOf(0, 12)

    fun vibrate(context: Context, pattern: LongArray, amplitude: Int = 180) {
        val vibrator = vibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amps = IntArray(pattern.size) { i ->
                    if (i % 2 == 0) 0 else amplitude.coerceIn(1, 255)
                }
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
            // Some OEMs restrict haptics — fail quiet.
        }
    }

    fun sense(context: Context) = vibrate(context, PATTERN_SENSE, 200)
    fun shoot(context: Context) = vibrate(context, PATTERN_SHOOT, 220)
    fun notify(context: Context) = vibrate(context, PATTERN_NOTIFY, 255)
    fun wag(context: Context) = vibrate(context, PATTERN_WAG, 140)
    fun tap(context: Context) = vibrate(context, PATTERN_TAP, 100)

    private fun vibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

@Composable
fun rememberWeaveSense(): (LongArray) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { pattern -> WeaveSense.vibrate(context, pattern) }
    }
}
