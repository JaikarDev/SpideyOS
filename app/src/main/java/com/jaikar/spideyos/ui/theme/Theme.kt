package com.jaikar.spideyos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.R

/** Overlay / cinematic tokens (dark glass for floating buddy panels). */
val SpideyRed = Color(0xFF3DBE8B)
val SpideyBlue = Color(0xFF0F2A32)
val SpideyNavy = Color(0xFF16353F)
val SpideyWeb = Color(0xFFE8F6F0)
val SpideyGold = Color(0xFFF4C430)
val SpideyBlueLight = Color(0xFF24505C)
val PipMint = SpideyRed

/** Nest VA product shell — cool mist, mint brand (not purple / not cream). */
val VaInk = Color(0xFF0E1A1F)
val VaMist = Color(0xFFF2F7F6)
val VaCloud = Color(0xFFFFFFFF)
val VaMint = Color(0xFF2EC4A0)
val VaMintDeep = Color(0xFF1A8F75)
val VaSoft = Color(0xFFD9EEE8)
val VaSky = Color(0xFFE7F3F8)
val VaMuted = Color(0xFF5C6F74)
val VaLine = Color(0x140E1A1F)
val VaCoral = Color(0xFFFF7A59)

val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

val Fraunces = FontFamily(
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold),
)

private val NestColors = lightColorScheme(
    primary = VaMint,
    onPrimary = Color.White,
    secondary = VaMintDeep,
    onSecondary = Color.White,
    background = VaMist,
    onBackground = VaInk,
    surface = VaCloud,
    onSurface = VaInk,
    tertiary = VaCoral,
    onTertiary = Color.White,
    outline = VaLine,
)

private val NestType = Typography(
    displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, color = VaInk),
    displayMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 36.sp, color = VaInk),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, color = VaInk),
    titleLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VaInk),
    titleMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = VaInk),
    bodyLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp, color = VaInk),
    bodyMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = VaMuted),
    labelLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = VaInk),
)

@Composable
fun SpideyOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NestColors,
        typography = NestType,
        content = content,
    )
}
