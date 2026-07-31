package com.jaikar.spideyos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SpideyRed = Color(0xFF3DBE8B) // mint accent (legacy name kept for fewer file renames)
val SpideyBlue = Color(0xFF0F2A32)
val SpideyNavy = Color(0xFF16353F)
val SpideyWeb = Color(0xFFE8F6F0)
val SpideyGold = Color(0xFFF4C430)
val SpideyBlueLight = Color(0xFF24505C)
val PipMint = SpideyRed

private val WeaveColors = darkColorScheme(
    primary = PipMint,
    onPrimary = SpideyBlue,
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
        colorScheme = WeaveColors,
        content = content,
    )
}
