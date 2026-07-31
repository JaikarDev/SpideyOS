package com.jaikar.spideyos.ui.companion

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.R
import com.jaikar.spideyos.assistant.PipSearchScript
import com.jaikar.spideyos.companion.DashHit
import com.jaikar.spideyos.companion.DashMood
import com.jaikar.spideyos.companion.DashSearchKind
import com.jaikar.spideyos.ui.theme.PipMint
import com.jaikar.spideyos.ui.theme.SpideyBlue
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyWeb
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
/**
 * Rover-style Search Companion panel for SpideyDashPip.
 * Floats on stock OS — WeaveHome bubble colors only.
 */
@Composable
fun SpideyDashPipPanel(
    expanded: Boolean,
    mood: DashMood,
    facingLeft: Boolean,
    isWalking: Boolean,
    tapPulse: Int,
    showNameTag: Boolean,
    pane: PipSearchScript.Pane,
    searchQuery: String,
    searchHits: List<DashHit>,
    musicPlaying: Boolean,
    onToggle: () -> Unit,
    onOption: (PipSearchScript.Option) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenHit: (DashHit) -> Unit,
    onWebSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onBackFromTip: () -> Unit,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onDragBy: (Float, Float) -> Unit,
    showActor: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val speech = moodSpeech(mood)
    val sleeping = mood is DashMood.Sleeping
    Column(
        modifier = modifier.widthIn(max = 300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = (expanded || speech != null) && !sleeping,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.86f, animationSpec = tween(240, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.9f, animationSpec = tween(140)),
        ) {
            SpeechBubble {
                when {
                    expanded && pane is PipSearchScript.Pane.Menu -> MenuPane(pane, onOption)
                    expanded && pane is PipSearchScript.Pane.Search -> RoverSearchPane(
                        pane = pane,
                        query = searchQuery,
                        hits = searchHits,
                        onQueryChange = onSearchQueryChange,
                        onOpenHit = onOpenHit,
                        onWebSearch = onWebSearch,
                        onClose = onCloseSearch,
                    )
                    expanded && pane is PipSearchScript.Pane.Tip -> TipPane(pane, onBackFromTip)
                    mood is DashMood.MusicListen || (expanded && musicPlaying) -> MusicPane(
                        title = (mood as? DashMood.MusicListen)?.trackTitle,
                        artist = (mood as? DashMood.MusicListen)?.artist,
                        line = speech ?: "Earbuds in — change the track?",
                        playing = musicPlaying,
                        onSkipPrev = onSkipPrev,
                        onPlayPause = onPlayPause,
                        onSkipNext = onSkipNext,
                        onOpenMenu = onToggle,
                    )
                    speech != null -> ReactionPane(speech, mood)
                    else -> Unit
                }
            }
        }
        if (sleeping) {
            Text(
                (mood as DashMood.Sleeping).line,
                color = SpideyWeb.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(SpideyNavy.copy(alpha = 0.82f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(2.dp))
        // Native DashOverlayHost draws the buddy; skip Compose actor to avoid grey plate / double mascot.
        if (showActor) {
            SpideyDashPipActor(
                mood = mood,
                facingLeft = facingLeft,
                isWalking = isWalking && !sleeping,
                tapPulse = tapPulse,
                showNameTag = showNameTag,
                onTap = onToggle,
                onDragBy = onDragBy,
            )
        }
    }
}

private fun moodSpeech(mood: DashMood): String? = when (mood) {
    is DashMood.Greeting -> mood.line
    is DashMood.Suggest -> mood.line
    is DashMood.Sleeping -> mood.line
    is DashMood.MailPickup -> mood.line
    is DashMood.MessagePickup -> mood.line
    is DashMood.GalleryPeek -> mood.line
    is DashMood.MusicListen -> mood.line
    is DashMood.CameraSnap -> mood.line
    is DashMood.Searching -> mood.line
    is DashMood.Found -> mood.line
    is DashMood.TapReact -> mood.line
    is DashMood.Listening -> mood.line
    DashMood.Roaming, DashMood.Walking -> null
}

@Composable
private fun SpeechBubble(content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(SpideyNavy.copy(alpha = 0.94f), SpideyBlue.copy(alpha = 0.92f)),
                    ),
                )
                .padding(14.dp),
        ) { content() }
        // Cute pointer toward SpideyDashPip
        Canvas(Modifier.size(16.dp, 10.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path, SpideyNavy.copy(alpha = 0.95f))
        }
    }
}

@Composable
private fun ReactionPane(line: String, mood: DashMood) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when (mood) {
                is DashMood.MailPickup -> Icons.Default.Email
                is DashMood.MessagePickup -> Icons.Default.Email
                is DashMood.GalleryPeek, is DashMood.CameraSnap -> Icons.Default.Photo
                is DashMood.MusicListen -> Icons.Default.Headset
                is DashMood.Greeting, is DashMood.Suggest, is DashMood.TapReact, is DashMood.Listening -> Icons.Default.Favorite
                is DashMood.Found -> Icons.Default.Star
                is DashMood.Searching -> Icons.Default.Search
                else -> Icons.Default.Apps
            },
            contentDescription = null,
            tint = SpideyGold,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(AppCredits.MASCOT_NAME, color = SpideyGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(line, color = SpideyWeb, fontSize = 13.sp)
            if (mood is DashMood.GalleryPeek && mood.photo != null) {
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = mood.photo.asImageBitmap(),
                    contentDescription = "Gallery peek",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun MusicPane(
    title: String?,
    artist: String?,
    line: String,
    playing: Boolean,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(AppCredits.MASCOT_NAME + " · earbuds on", color = SpideyGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(line, color = SpideyWeb, fontSize = 12.sp)
        if (!title.isNullOrBlank()) {
            Text(title, color = SpideyWeb, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (!artist.isNullOrBlank()) {
                Text(artist, color = SpideyWeb.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.SkipPrevious, "Previous", tint = PipMint, modifier = Modifier.size(28.dp).clickable(onClick = onSkipPrev))
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                "Play pause",
                tint = SpideyGold,
                modifier = Modifier.size(34.dp).clickable(onClick = onPlayPause),
            )
            Icon(Icons.Default.SkipNext, "Next", tint = PipMint, modifier = Modifier.size(28.dp).clickable(onClick = onSkipNext))
        }
        Text(
            "More options",
            color = SpideyWeb.copy(alpha = 0.55f),
            fontSize = 11.sp,
            modifier = Modifier.clickable(onClick = onOpenMenu),
        )
    }
}

@Composable
private fun MenuPane(pane: PipSearchScript.Pane.Menu, onOption: (PipSearchScript.Option) -> Unit) {
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
                    when {
                        opt.id.startsWith("search_") -> Icons.Default.Search
                        opt.id == "gallery" || opt.id == "search_pics" -> Icons.Default.Photo
                        opt.id == "music" -> Icons.Default.Headset
                        opt.id == "wave" -> Icons.Default.WavingHand
                        opt.id == "hide" -> Icons.Default.Close
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
private fun RoverSearchPane(
    pane: PipSearchScript.Pane.Search,
    query: String,
    hits: List<DashHit>,
    onQueryChange: (String) -> Unit,
    onOpenHit: (DashHit) -> Unit,
    onWebSearch: () -> Unit,
    onClose: () -> Unit,
) {
    val title = when (pane.kind) {
        DashSearchKind.APPS -> "Search apps"
        DashSearchKind.PICTURES -> "Search pictures"
        DashSearchKind.MUSIC -> "Search music"
        DashSearchKind.VIDEO -> "Search video"
        DashSearchKind.FILES -> "Search files"
        DashSearchKind.PEOPLE -> "Search people"
        DashSearchKind.WEB -> "Search the Internet"
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = SpideyGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SpideyWeb, modifier = Modifier.size(22.dp).clickable(onClick = onClose))
        }
        Text(pane.prompt, color = SpideyWeb.copy(alpha = 0.65f), fontSize = 11.sp)
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
                    if (query.isEmpty()) Text("Dig here…", color = SpideyWeb.copy(alpha = 0.4f), fontSize = 14.sp)
                    inner()
                },
            )
        }
        if (pane.kind == DashSearchKind.WEB) {
            Text(
                "Search web",
                color = SpideyGold,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PipMint.copy(alpha = 0.25f))
                    .clickable(onClick = onWebSearch)
                    .padding(12.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(hits, key = { it.id }) { hit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpideyBlue.copy(alpha = 0.45f))
                            .clickable { onOpenHit(hit) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hit.thumb != null) {
                            Image(hit.thumb.asImageBitmap(), null, Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)))
                        } else {
                            Box(Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(PipMint.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Search, null, tint = SpideyWeb, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(hit.title, color = SpideyWeb, fontSize = 13.sp, maxLines = 1)
                            Text(hit.subtitle, color = SpideyWeb.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
                if (hits.isEmpty()) {
                    item {
                        Text(
                            "Still digging… try another name, or allow Photos/Contacts in Nest.",
                            color = SpideyWeb.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TipPane(pane: PipSearchScript.Pane.Tip, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("How SpideyDashPip helps", color = SpideyGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SpideyWeb, modifier = Modifier.size(22.dp).clickable(onClick = onBack))
        }
        pane.lines.forEach { line -> Text("• $line", color = SpideyWeb, fontSize = 12.sp) }
    }
}

@Composable
fun SpideyDashPipActor(
    mood: DashMood,
    facingLeft: Boolean,
    isWalking: Boolean,
    tapPulse: Int,
    showNameTag: Boolean,
    onTap: () -> Unit,
    onDragBy: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val breathe = rememberInfiniteTransition(label = "dash")
    val idleBob by breathe.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val walkCycle by breathe.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(260, easing = LinearEasing), RepeatMode.Restart),
        label = "walk",
    )
    val wave by breathe.animateFloat(
        -22f, 22f,
        infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Reverse),
        label = "wave",
    )
    val pulse by breathe.animateFloat(
        0.96f, 1.05f,
        infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulse",
    )
    val zzz by breathe.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "zzz",
    )
    val sparkle by breathe.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "sparkle",
    )

    val tapScale = remember { Animatable(1f) }
    val tapSpin = remember { Animatable(0f) }
    val sparkRise = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tapPulse) {
        if (tapPulse == 0) return@LaunchedEffect
        sparkRise.snapTo(0f)
        launch {
            tapScale.snapTo(0.78f)
            tapScale.animateTo(1.18f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow))
            tapScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
        }
        launch {
            tapSpin.snapTo(0f)
            tapSpin.animateTo(14f, tween(90))
            tapSpin.animateTo(-10f, tween(110))
            tapSpin.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
        launch {
            sparkRise.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
    }

    val waving = mood is DashMood.Greeting || mood is DashMood.TapReact
    val listening = mood is DashMood.MusicListen
    val sleeping = mood is DashMood.Sleeping
    val digging = mood is DashMood.Searching
    val found = mood is DashMood.Found
    val dig = if (digging) sin(walkCycle * Math.PI * 2).toFloat() else 0f
    val hop = when {
        digging -> dig * -7f
        isWalking -> sin(walkCycle * Math.PI * 2).toFloat()
        found || mood is DashMood.TapReact -> idleBob * 1.8f
        else -> idleBob
    }
    val lean = when {
        digging -> dig * 16f
        isWalking -> sin(walkCycle * Math.PI * 2).toFloat() * 10f
        else -> 0f
    }
    val squashY = when {
        digging -> 1f - kotlin.math.abs(dig) * 0.14f
        isWalking -> 1f - kotlin.math.abs(sin(walkCycle * Math.PI * 2)).toFloat() * 0.1f
        found -> 1f + idleBob * 0.1f
        else -> 1f
    }
    val stretchX = when {
        digging -> 1f + kotlin.math.abs(dig) * 0.12f
        isWalking -> 1f + kotlin.math.abs(sin(walkCycle * Math.PI * 2)).toFloat() * 0.08f
        else -> 1f
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .offset(y = (if (sleeping) 6f else hop * -12f).dp)
                .size(104.dp)
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
                        if (!dragged) {
                            scope.launch {
                                tapScale.snapTo(0.85f)
                                tapScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                            }
                            onTap()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // Soft aura — circular only, never a rectangle
            Canvas(Modifier.size(100.dp).graphicsLayer { alpha = if (sleeping) 0.08f else 0.22f }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PipMint.copy(alpha = 0.55f), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                    center = center,
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .size(width = (40 + if (isWalking || digging) 12 else 0).dp, height = 11.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = if (sleeping) 0.12f else 0.22f)),
            )
            Image(
                painter = painterResource(R.drawable.spideydashpip),
                contentDescription = AppCredits.MASCOT_NAME,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        val face = if (facingLeft) -1f else 1f
                        val s = tapScale.value
                        scaleX = face * stretchX * s * (if (listening) pulse else 1f) * (if (sleeping) 0.92f else 1f)
                        scaleY = squashY * s * (if (listening) pulse else 1f) * (if (sleeping) 0.88f else 1f)
                        rotationZ = tapSpin.value + when {
                            sleeping -> -16f + zzz * 5f
                            digging -> lean
                            waving -> wave * 0.45f
                            found -> wave * 0.3f
                            isWalking -> lean
                            else -> sin(idleBob * Math.PI).toFloat() * 3f
                        }
                        alpha = if (sleeping) 0.75f else 1f
                    },
                contentScale = ContentScale.Fit,
            )
            // Tap sparkles
            if (sparkRise.value > 0.02f && sparkRise.value < 0.98f) {
                val rise = sparkRise.value
                listOf(-28f to -18f, 0f to -30f, 26f to -16f, -16f to -34f, 18f to -36f).forEachIndexed { i, (ox, oy) ->
                    Icon(
                        if (i % 2 == 0) Icons.Default.Favorite else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i % 2 == 0) Color(0xFFFF6B8A) else SpideyGold,
                        modifier = Modifier
                            .offset(
                                x = (ox + cos((sparkle + i) * 4f) * 4f).dp,
                                y = (oy - rise * 36f).dp,
                            )
                            .size((10 + (1f - rise) * 6).dp)
                            .graphicsLayer { alpha = (1f - rise) * 0.95f },
                    )
                }
            }
            if (digging) {
                Text("…", color = SpideyGold, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 16.dp))
            }
            if (found) {
                Text("!", color = SpideyGold, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp))
            }
            if (sleeping) {
                Text(
                    "ZzZ",
                    color = SpideyGold,
                    fontSize = (12 + zzz * 5).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = ((-6) - zzz * 10).dp)
                        .graphicsLayer { alpha = 0.5f + zzz * 0.45f },
                )
            }
            if (mood is DashMood.MailPickup) {
                Icon(Icons.Default.Email, null, tint = SpideyGold,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(4.dp, 2.dp).size(26.dp)
                        .clip(CircleShape).background(SpideyNavy.copy(alpha = 0.88f)).padding(4.dp).rotate(-12f))
            }
            if (mood is DashMood.MessagePickup) {
                Icon(Icons.Default.Email, null, tint = PipMint,
                    modifier = Modifier.align(Alignment.BottomStart).offset((-4).dp, 2.dp).size(26.dp)
                        .clip(CircleShape).background(SpideyNavy.copy(alpha = 0.88f)).padding(4.dp))
            }
            if (mood is DashMood.GalleryPeek || mood is DashMood.CameraSnap) {
                Icon(Icons.Default.Photo, null, tint = SpideyGold,
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                        .clip(RoundedCornerShape(6.dp)).background(SpideyNavy.copy(alpha = 0.88f)).padding(3.dp))
            }
            if (listening) {
                Icon(Icons.Default.Headset, "Earbuds", tint = SpideyGold,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-4).dp).size(22.dp)
                        .clip(CircleShape).background(SpideyNavy.copy(alpha = 0.85f)).padding(2.dp))
            }
        }
        AnimatedVisibility(visible = showNameTag) {
            Text(
                AppCredits.MASCOT_NAME,
                color = SpideyGold,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpideyNavy.copy(alpha = 0.78f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
