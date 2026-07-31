package com.jaikar.spideyos.ui.companion

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.apps.InstalledApp
import com.jaikar.spideyos.assistant.PipSearchScript
import com.jaikar.spideyos.ui.theme.PipMint
import com.jaikar.spideyos.ui.theme.SpideyBlue
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyWeb
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Rover-style Search Companion UI in WeaveHome colors.
 * Scripted menus + local app filter — no generative AI.
 */
@Composable
fun PipSearchCompanionPanel(
    expanded: Boolean,
    pane: PipSearchScript.Pane,
    searchQuery: String,
    searchResults: List<InstalledApp>,
    onToggle: () -> Unit,
    onOption: (PipSearchScript.Option) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onCloseSearch: () -> Unit,
    onBackFromTip: () -> Unit,
    onDragBy: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = 300.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.94f, animationSpec = tween(140)),
        ) {
            SpeechBubble {
                when (pane) {
                    is PipSearchScript.Pane.Menu -> MenuPane(pane, onOption)
                    is PipSearchScript.Pane.SearchApps -> SearchPane(
                        prompt = pane.prompt,
                        query = searchQuery,
                        results = searchResults,
                        onQueryChange = onSearchQueryChange,
                        onLaunchApp = onLaunchApp,
                        onClose = onCloseSearch,
                    )
                    is PipSearchScript.Pane.Tip -> TipPane(pane, onBackFromTip)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        PipBuddyFace(onTap = onToggle, onDragBy = onDragBy)
    }
}

@Composable
private fun SpeechBubble(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SpideyNavy.copy(alpha = 0.96f))
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun MenuPane(
    pane: PipSearchScript.Pane.Menu,
    onOption: (PipSearchScript.Option) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(pane.title, color = SpideyGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(pane.subtitle, color = SpideyWeb.copy(alpha = 0.65f), fontSize = 11.sp)
        pane.options.forEach { opt ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpideyBlue.copy(alpha = 0.55f))
                    .clickable { onOption(opt) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    when (opt.id) {
                        "search_apps" -> Icons.Default.Search
                        "hide" -> Icons.Default.Close
                        else -> Icons.Default.Apps
                    },
                    contentDescription = null,
                    tint = PipMint,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(opt.label, color = SpideyWeb, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (opt.hint.isNotBlank()) {
                        Text(opt.hint, color = SpideyWeb.copy(alpha = 0.55f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPane(
    prompt: String,
    query: String,
    results: List<InstalledApp>,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onClose: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Search apps",
                color = SpideyGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SpideyWeb,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onClose),
            )
        }
        Text(prompt, color = SpideyWeb.copy(alpha = 0.65f), fontSize = 11.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SpideyBlue.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, null, tint = PipMint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = SpideyWeb, fontSize = 14.sp),
                cursorBrush = SolidColor(PipMint),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Filter apps…", color = SpideyWeb.copy(alpha = 0.4f), fontSize = 14.sp)
                    }
                    inner()
                },
            )
        }
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(results, key = { it.packageName }) { app ->
                AppRow(app, onClick = { onLaunchApp(app) })
            }
            if (results.isEmpty()) {
                item {
                    Text(
                        "No matches — try another name.",
                        color = SpideyWeb.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TipPane(pane: PipSearchScript.Pane.Tip, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "How Pip helps",
                color = SpideyGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SpideyWeb,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onBack),
            )
        }
        pane.lines.forEach { line ->
            Text("• $line", color = SpideyWeb, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpideyBlue.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.icon)
        Spacer(Modifier.width(10.dp))
        Text(
            app.label,
            color = SpideyWeb,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AppIcon(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp)),
        )
    } else {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(PipMint.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Apps, null, tint = SpideyWeb, modifier = Modifier.size(16.dp))
        }
    }
}

/** Compact Pip face for the floating companion — drag to move, tap to open menu. */
@Composable
fun PipBuddyFace(
    onTap: () -> Unit,
    onDragBy: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val breathe = rememberInfiniteTransition(label = "buddy")
    val bob by breathe.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val wag by breathe.animateFloat(
        -10f,
        10f,
        infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "wag",
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .offset(y = (bob * -4f).dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(SpideyNavy.copy(alpha = 0.92f))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragged = false
                        drag(down.id) { change ->
                            val delta = change.positionChange()
                            if (hypot(delta.x.toDouble(), delta.y.toDouble()) > 0.5) {
                                dragged = true
                                change.consume()
                                onDragBy(delta.x, delta.y)
                            }
                        }
                        if (!dragged) onTap()
                    }
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(56.dp).scale(1f + bob * 0.04f)) {
                val c = Offset(size.width / 2f, size.height / 2f + 3f)
                drawCircle(PipMint, radius = size.minDimension * 0.4f, center = c)
                val ear = Path().apply {
                    moveTo(c.x - 18f, c.y - 10f)
                    quadraticBezierTo(c.x - 26f, c.y - 30f, c.x - 8f, c.y - 20f)
                    close()
                }
                drawPath(ear, PipMint)
                val ear2 = Path().apply {
                    moveTo(c.x + 18f, c.y - 10f)
                    quadraticBezierTo(c.x + 26f, c.y - 30f, c.x + 8f, c.y - 20f)
                    close()
                }
                drawPath(ear2, PipMint)
                drawCircle(SpideyWeb, radius = 4.5f, center = Offset(c.x - 7f, c.y - 3f))
                drawCircle(SpideyWeb, radius = 4.5f, center = Offset(c.x + 7f, c.y - 3f))
                drawCircle(SpideyBlue, radius = 2f, center = Offset(c.x - 7f, c.y - 2.5f))
                drawCircle(SpideyBlue, radius = 2f, center = Offset(c.x + 7f, c.y - 2.5f))
                drawCircle(SpideyGold, radius = 3f, center = Offset(c.x, c.y + 5f))
                val ty = c.y + 6f + sin(Math.toRadians(wag.toDouble())).toFloat() * 6f
                drawLine(
                    SpideyGold,
                    Offset(c.x + 16f, c.y + 10f),
                    Offset(c.x + 24f, ty),
                    strokeWidth = 4f,
                )
            }
        }
        Text(
            AppCredits.MASCOT_NAME,
            color = SpideyGold,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SpideyNavy.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
