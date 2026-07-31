package com.jaikar.spideyos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SpideyRed = Color(0xFFC41E3A)
val SpideyBlue = Color(0xFF0B1D36)
val SpideyNavy = Color(0xFF102A43)
val SpideyWeb = Color(0xFFE8EEF5)
val SpideyGold = Color(0xFFF4C430)
val SpideyBlueLight = Color(0xFF1F3A5F)

private val SpideyDarkColors = darkColorScheme(
    primary = SpideyRed,
    onPrimary = SpideyWeb,
    secondary = SpideyGold,
    onSecondary = SpideyBlue,
    background = SpideyBlue,
    onBackground = SpideyWeb,
    surface = SpideyNavy,
    onSurface = SpideyWeb,
    tertiary = SpideyBlueLight,
)

@Composable
fun SpideyOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpideyDarkColors,
        content = content,
    )
}
