package com.jaikar.spideyos.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Width buckets matching Material window size classes. */
enum class SpideyWidthClass { Compact, Medium, Expanded }

enum class SpideyHeightClass { Compact, Medium, Expanded }

/**
 * Auto-adapts UI for phones / large phones / tablets across OEM skins
 * (OnePlus, Samsung, Oppo, Vivo, Realme, Redmi, Xiaomi, Poco, Lava, …).
 */
@Immutable
data class SpideyWindowInfo(
    val widthClass: SpideyWidthClass,
    val heightClass: SpideyHeightClass,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val smallestWidthDp: Int,
    val isLandscape: Boolean,
    val isTablet: Boolean,
    val contentMaxWidth: Dp,
    val horizontalPadding: Dp,
    val titleSp: TextUnit,
    val bodySp: TextUnit,
    val gridMinCell: Dp,
)

@Composable
fun rememberSpideyWindowInfo(): SpideyWindowInfo {
    val config = LocalConfiguration.current
    val w = config.screenWidthDp
    val h = config.screenHeightDp
    val sw = min(w, h)
    return remember(w, h, config.orientation) {
        val widthClass = when {
            w < 600 -> SpideyWidthClass.Compact
            w < 840 -> SpideyWidthClass.Medium
            else -> SpideyWidthClass.Expanded
        }
        val heightClass = when {
            h < 480 -> SpideyHeightClass.Compact
            h < 900 -> SpideyHeightClass.Medium
            else -> SpideyHeightClass.Expanded
        }
        val tablet = sw >= 600
        SpideyWindowInfo(
            widthClass = widthClass,
            heightClass = heightClass,
            screenWidthDp = w,
            screenHeightDp = h,
            smallestWidthDp = sw,
            isLandscape = w > h,
            isTablet = tablet,
            contentMaxWidth = when (widthClass) {
                SpideyWidthClass.Compact -> w.dp
                SpideyWidthClass.Medium -> 720.dp
                SpideyWidthClass.Expanded -> 1000.dp
            },
            horizontalPadding = when {
                tablet && widthClass == SpideyWidthClass.Expanded -> 48.dp
                tablet -> 32.dp
                w < 360 -> 12.dp
                else -> 20.dp
            },
            titleSp = when {
                sw < 360 -> 28.sp
                tablet -> 40.sp
                else -> 34.sp
            },
            bodySp = when {
                sw < 360 -> 14.sp
                tablet -> 17.sp
                else -> 16.sp
            },
            gridMinCell = when {
                sw < 360 -> 72.dp
                tablet -> 100.dp
                else -> 84.dp
            },
        )
    }
}

@Composable
fun spideySafePadding(): PaddingValues {
    val insets = WindowInsets.safeDrawing
        .union(WindowInsets.statusBars)
        .union(WindowInsets.navigationBars)
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
    val dir = LocalLayoutDirection.current
    val extra = rememberSpideyWindowInfo().horizontalPadding
    return PaddingValues(
        start = insets.calculateStartPadding(dir) + extra,
        top = insets.calculateTopPadding() + 8.dp,
        end = insets.calculateEndPadding(dir) + extra,
        bottom = insets.calculateBottomPadding() + 8.dp,
    )
}

/** Centers content and caps width on tablets / foldables / desktop-width windows. */
@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val info = rememberSpideyWindowInfo()
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val maxW = if (info.widthClass == SpideyWidthClass.Compact) {
            maxWidth
        } else {
            minOf(info.contentMaxWidth, maxWidth)
        }
        Box(
            Modifier
                .widthIn(max = maxW)
                .fillMaxWidth()
                .padding(spideySafePadding()),
        ) {
            content()
        }
    }
}
