package com.jaikar.spideyos.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jaikar.spideyos.AppCredits
import com.jaikar.spideyos.MainActivity
import com.jaikar.spideyos.R
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.apps.AppCatalog
import com.jaikar.spideyos.apps.InstalledApp
import android.Manifest
import android.content.pm.PackageManager
import com.jaikar.spideyos.companion.DashPrivacyGuard
import com.jaikar.spideyos.companion.DashCommander
import com.jaikar.spideyos.companion.DashEar
import com.jaikar.spideyos.companion.DashEvent
import com.jaikar.spideyos.companion.DashHit
import com.jaikar.spideyos.companion.DashMediaHelper
import com.jaikar.spideyos.companion.DashMood
import com.jaikar.spideyos.companion.DashOverlayHost
import com.jaikar.spideyos.companion.DashSearchCatalog
import com.jaikar.spideyos.companion.DashSearchKind
import com.jaikar.spideyos.companion.DashSpeaker
import com.jaikar.spideyos.companion.DashSuggestScript
import com.jaikar.spideyos.companion.DashVoice
import com.jaikar.spideyos.companion.DashVoiceAction
import com.jaikar.spideyos.companion.SpideyDashPipBus
import com.jaikar.spideyos.companion.SpideyDashPipLifeReceiver
import com.jaikar.spideyos.companion.spidy.SpidyActivityLog
import com.jaikar.spideyos.companion.spidy.SpidyAutomation
import com.jaikar.spideyos.companion.spidy.SpidyMeetings
import com.jaikar.spideyos.companion.spidy.SpidyPersonality
import com.jaikar.spideyos.companion.spidy.SpidyRoutines
import com.jaikar.spideyos.assistant.GeminiBridge
import com.jaikar.spideyos.sense.WeaveSense
import com.jaikar.spideyos.ui.Routes
import com.jaikar.spideyos.ui.companion.SpideyDashPipPanel
import com.jaikar.spideyos.ui.theme.SpideyOSTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

/**
 * A native transparent character host with a separately attached Compose menu.
 * Keeping the mascot in [DashOverlayHost] prevents Compose from drawing an opaque plate behind it.
 */
class SpideyOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    private var windowManager: WindowManager? = null
    private var buddyHost: DashOverlayHost? = null
    private var menuView: ComposeView? = null
    private var menuParams: WindowManager.LayoutParams? = null
    private var lifeReceiver: BroadcastReceiver? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var expanded by mutableStateOf(false)
    private var pane by mutableStateOf<PipSearchScript.Pane>(PipSearchScript.home("friend"))
    private var searchQuery by mutableStateOf("")
    private var searchHits by mutableStateOf<List<DashHit>>(emptyList())
    private var apps by mutableStateOf<List<InstalledApp>>(emptyList())
    private var userName by mutableStateOf("friend")
    private var mood by mutableStateOf<DashMood>(DashMood.Roaming)
    private var facingLeft by mutableStateOf(false)
    private var musicPlaying by mutableStateOf(false)
    private var userDragging by mutableStateOf(false)
    private var isWalking by mutableStateOf(false)
    private var asleep by mutableStateOf(false)
    private var tapPulse by mutableIntStateOf(0)
    private var roamPausedUntil = 0L
    private var routinesEnabled = true
    private var digestEnabled = true
    private var memoryEnabled = true
    private var automationEnabled = true
    private var meetingsEnabled = true
    private var geminiKey: String = ""
    private var lastMusicDanceTrack: String? = null
    private var musicWasPlaying = false
    private val gemini by lazy { GeminiBridge { geminiKey } }

    private lateinit var speaker: DashSpeaker
    private lateinit var voice: DashVoice
    private var ear: DashEar? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        speaker = DashSpeaker(this)
        voice = DashVoice(
            context = this,
            onResult = { heard -> onProactiveTalk(heard) },
            onError = { line -> say(line, 3_000) },
        )
        ear = DashEar(
            context = this,
            onWake = { heard -> onSpidyWake(heard) },
            onCommand = { heard -> onProactiveTalk(heard) },
            onStatus = { line ->
                if (!asleep) buddyHost?.showSpeech(line, 2_200)
            },
        )
        scope.launch { SpideyApp.instance.settings.setBuddyEnabled(true) }
        startAsForeground()

        if (Settings.canDrawOverlays(this)) {
            attachCompanion()
            registerLifeReceiver()
            startRoamLoop()
            startSuggestLoop()
            startEventCollector()
            startMusicWatcher()
            startSpidyEar()
            startCompanionRoutines()
            startMeetingReminderLoop()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun startAsForeground() {
        val notification = buildNotification("${AppCredits.MASCOT_NAME} · say Spidy")
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= 34) {
            val types = if (micGranted) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            ServiceCompat.startForeground(this, NOTIF_ID, notification, types)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun attachCompanion() {
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = manager
        apps = AppCatalog.loadLaunchableApps(packageManager)
        buddyHost = DashOverlayHost(
            context = this,
            windowManager = manager,
            onTap = ::onBuddyTap,
            onDrag = ::onBuddyDrag,
            onLongPress = ::onBuddyLongPress,
        ).also { it.attach() }
        scope.launch {
            SpideyApp.instance.settings.settings.collect { s ->
                userName = s.userName.ifBlank { "friend" }
                routinesEnabled = s.routinesEnabled
                digestEnabled = s.digestEnabled
                memoryEnabled = s.memoryEnabled
                automationEnabled = s.automationEnabled
                meetingsEnabled = s.meetingsEnabled
                geminiKey = s.geminiApiKey
                pane = PipSearchScript.home(userName)
            }
        }
        scope.launch {
            delay(400)
            notifyForeground("Hey $userName — ${AppCredits.MASCOT_NAME} says hi.")
        }
    }

    private fun registerLifeReceiver() {
        lifeReceiver = SpideyDashPipLifeReceiver()
        ContextCompat.registerReceiver(
            this,
            lifeReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun onBuddyDrag(dx: Float, dy: Float) {
        if (asleep) return
        userDragging = true
        isWalking = true
        roamPausedUntil = System.currentTimeMillis() + 4_000
        facingLeft = dx < 0
        buddyHost?.setFacingLeft(facingLeft)
        buddyHost?.setWalking(true)
        buddyHost?.moveBy(dx, dy)
        if (expanded) positionMenuNearBuddy()
    }

    private fun onBuddyTap() {
        if (asleep) {
            SpideyDashPipBus.emit(DashEvent.WakeUp)
            return
        }
        WeaveSense.wag(this)
        tapPulse++
        buddyHost?.pulseTap()
        roamPausedUntil = System.currentTimeMillis() + 12_000
        userDragging = false
        isWalking = false
        buddyHost?.setWalking(false)

        // Tap only activates / closes the menu — no talking.
        if (!expanded) {
            mood = DashMood.Roaming
            expanded = true
            pane = PipSearchScript.home(userName)
            attachMenu()
            ear?.nudge()
        } else {
            collapseMenu()
            mood = DashMood.Roaming
            ear?.nudge()
        }
    }

    private fun attachMenu() {
        if (!expanded || menuView != null) return
        val manager = windowManager ?: return
        val density = resources.displayMetrics.density
        val themedContext = ContextThemeWrapper(this, R.style.Theme_SpideyOverlay)
        val view = ComposeView(themedContext).apply {
            setBackgroundColor(Color.TRANSPARENT)
            background = ColorDrawable(Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@SpideyOverlayService)
            setViewTreeViewModelStoreOwner(this@SpideyOverlayService)
            setViewTreeSavedStateRegistryOwner(this@SpideyOverlayService)
            setContent {
                SpideyOSTheme {
                    SpideyDashPipPanel(
                        expanded = expanded,
                        mood = mood,
                        facingLeft = facingLeft,
                        isWalking = isWalking,
                        tapPulse = tapPulse,
                        showNameTag = expanded,
                        pane = pane,
                        searchQuery = searchQuery,
                        searchHits = searchHits,
                        musicPlaying = musicPlaying,
                        onToggle = ::onBuddyTap,
                        onOption = ::handleOption,
                        onSearchQueryChange = {
                            searchQuery = it
                            refreshSearchHits()
                        },
                        onOpenHit = ::openHit,
                        onWebSearch = ::runWebSearch,
                        onCloseSearch = {
                            pane = PipSearchScript.home(userName)
                            searchQuery = ""
                            searchHits = emptyList()
                            mood = DashMood.Roaming
                            refreshMenuLayout()
                        },
                        onBackFromTip = {
                            pane = PipSearchScript.home(userName)
                            refreshMenuLayout()
                        },
                        onSkipPrev = {
                            WeaveSense.tap(this@SpideyOverlayService)
                            DashMediaHelper.skipPrevious(this@SpideyOverlayService)
                            refreshMusicMood()
                        },
                        onPlayPause = {
                            WeaveSense.tap(this@SpideyOverlayService)
                            DashMediaHelper.playPause(this@SpideyOverlayService)
                            refreshMusicMood()
                        },
                        onSkipNext = {
                            WeaveSense.tap(this@SpideyOverlayService)
                            DashMediaHelper.skipNext(this@SpideyOverlayService)
                            refreshMusicMood()
                        },
                        onDragBy = ::onBuddyDrag,
                    )
                }
            }
        }
        menuView = view
        menuParams = WindowManager.LayoutParams(
            (300 * density).toInt(),
            menuHeight(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            menuFlags(),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            format = PixelFormat.TRANSLUCENT
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        positionMenuNearBuddy()
        manager.addView(view, menuParams)
    }

    private fun collapseMenu() {
        expanded = false
        menuView?.let { runCatching { windowManager?.removeView(it) } }
        menuView = null
        menuParams = null
        searchQuery = ""
        searchHits = emptyList()
        pane = PipSearchScript.home(userName)
    }

    private fun positionMenuNearBuddy() {
        val params = menuParams ?: return
        val hostParams = buddyHost?.layoutParams ?: return
        params.x = (hostParams.x - (96 * resources.displayMetrics.density).toInt()).coerceAtLeast(0)
        params.y = (hostParams.y - menuHeight() + (22 * resources.displayMetrics.density).toInt()).coerceAtLeast(0)
        menuView?.let { runCatching { windowManager?.updateViewLayout(it, params) } }
    }

    private fun menuHeight(): Int {
        val density = resources.displayMetrics.density
        return when {
            pane is PipSearchScript.Pane.Search -> (440 * density).toInt()
            mood is DashMood.GalleryPeek -> (360 * density).toInt()
            mood is DashMood.MusicListen -> (300 * density).toInt()
            else -> (420 * density).toInt()
        }
    }

    private fun menuFlags(): Int =
        if (pane is PipSearchScript.Pane.Search) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }

    private fun refreshMenuLayout() {
        val view = menuView ?: return
        val params = menuParams ?: return
        params.height = menuHeight()
        params.flags = menuFlags()
        positionMenuNearBuddy()
        runCatching { windowManager?.updateViewLayout(view, params) }
    }

    private fun startRoamLoop() {
        scope.launch {
            while (isActive) {
                delay(Random.nextLong(2_400, 4_800))
                if (asleep || expanded || userDragging || System.currentTimeMillis() < roamPausedUntil) continue
                if (mood !is DashMood.Roaming && mood !is DashMood.Walking && mood !is DashMood.MusicListen) continue
                roamStep()
            }
        }
    }

    private suspend fun roamStep() {
        val host = buddyHost ?: return
        if (asleep) return
        val lp = host.layoutParams
        val maxX = (resources.displayMetrics.widthPixels - 100 * resources.displayMetrics.density).toInt().coerceAtLeast(0)
        val maxY = (resources.displayMetrics.heightPixels - 180 * resources.displayMetrics.density).toInt().coerceAtLeast(0)
        val targetX = Random.nextInt(0, maxX + 1)
        val targetY = Random.nextInt((80 * resources.displayMetrics.density).toInt(), maxY + 1)
        facingLeft = targetX < lp.x
        host.setFacingLeft(facingLeft)
        host.setWalking(true)
        isWalking = true
        mood = DashMood.Walking
        val startX = lp.x
        val startY = lp.y
        repeat(Random.nextInt(22, 42)) { index ->
            if (!coroutineContext.isActive || asleep || expanded || (userDragging && System.currentTimeMillis() < roamPausedUntil)) return@repeat
            val t = (index + 1) / 32f
            host.setPosition((startX + (targetX - startX) * t).toInt(), (startY + (targetY - startY) * t).toInt())
            delay(36)
        }
        host.setWalking(false)
        isWalking = false
        userDragging = false
        if (mood is DashMood.Walking) mood = DashMood.Roaming
    }

    private fun startSuggestLoop() {
        // Quiet companion — no random chatter. Speaks only for mail/messages or Listen answers.
    }

    private fun startEventCollector() {
        scope.launch {
            SpideyDashPipBus.events.collect { event ->
                when (event) {
                    is DashEvent.Mail -> if (!asleep) showMail(event)
                    is DashEvent.Message -> if (!asleep) showMessage(event)
                    DashEvent.CameraCaptured -> if (!asleep) showCamera()
                    DashEvent.WaveHi -> Unit // silent — no talk on wave
                    DashEvent.OpenGallery -> if (!asleep) showGallery()
                    DashEvent.OpenMusic -> if (!asleep) showMusic()
                    DashEvent.WakeUp -> goWake()
                    DashEvent.GoSleep -> goSleep()
                    DashEvent.SuggestNow -> Unit
                    DashEvent.StartListen -> if (!asleep) startListening()
                }
            }
        }
    }

    private fun startMusicWatcher() {
        scope.launch {
            while (isActive) {
                delay(2_500)
                val nowPlaying = DashMediaHelper.nowPlayingInfo(this@SpideyOverlayService)
                val playing = DashMediaHelper.activeController(this@SpideyOverlayService)
                    ?.playbackState
                    ?.state == PlaybackState.STATE_PLAYING
                musicPlaying = playing
                if (mood is DashMood.MusicListen && nowPlaying != null) {
                    mood = DashMood.MusicListen((mood as DashMood.MusicListen).line, nowPlaying.title, nowPlaying.artist)
                    if (expanded) refreshMenuLayout()
                }
                // Dance personality when music starts or track changes (bubble; speak lightly once).
                val trackKey = nowPlaying?.title
                if (!asleep && playing && ( !musicWasPlaying || trackKey != lastMusicDanceTrack)) {
                    lastMusicDanceTrack = trackKey
                    val line = SpidyPersonality.dance(userName, trackKey)
                    mood = DashMood.MusicListen(line, nowPlaying?.title, nowPlaying?.artist)
                    buddyHost?.showSpeech(line, 3_200)
                    if (!musicWasPlaying) {
                        SpidyActivityLog.append(this@SpideyOverlayService, "dance", trackKey ?: "music")
                    }
                }
                if (!playing) {
                    lastMusicDanceTrack = null
                }
                musicWasPlaying = playing
            }
        }
    }

    private fun startCompanionRoutines() {
        scope.launch {
            delay(2_800)
            maybeMorningRoutine(fromUnlock = false)
        }
        scope.launch {
            while (isActive) {
                delay(12 * 60_000L)
                maybeNightRoutine()
            }
        }
    }

    private fun maybeMorningRoutine(fromUnlock: Boolean) {
        if (asleep) return
        if (!SpidyRoutines.shouldSpeakMorning(this, routinesEnabled)) return
        val line = SpidyPersonality.morning(userName)
        SpidyActivityLog.append(this, "routine", if (fromUnlock) "morning_unlock" else "morning")
        mood = DashMood.Greeting(line)
        say(line, 5_000)
        scope.launch {
            delay(5_200)
            if (mood is DashMood.Greeting) mood = DashMood.Roaming
        }
    }

    private fun maybeNightRoutine() {
        if (asleep) return
        if (System.currentTimeMillis() < roamPausedUntil) return
        if (!SpidyRoutines.shouldSpeakNight(this, routinesEnabled)) return
        val line = SpidyPersonality.night(userName)
        SpidyActivityLog.append(this, "routine", "night")
        mood = DashMood.Suggest(line)
        say(line, 4_800)
        scope.launch {
            delay(5_000)
            if (mood is DashMood.Suggest) mood = DashMood.Roaming
        }
    }

    /** Speak aloud + bubble above SpideyDashPip only (no top banner). */
    private fun say(line: String, holdMs: Long = 5_200) {
        val text = DashPrivacyGuard.safeSpeakLine(line.trim())
        if (text.isEmpty()) return
        ear?.pauseForSpeak(holdMs.coerceAtLeast(2_800))
        speaker.speak(text)
        buddyHost?.showSpeech(text, holdMs)
    }

    private fun startSpidyEar() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notifyForeground("Mic needed — Nest → Start SpideyDashPip to allow voice")
            buddyHost?.showSpeech("Allow mic in Nest to hear you", 4_000)
            return
        }
        // Upgrade FGS to include microphone so OEM allows continuous listen.
        startAsForeground()
        ear?.stop()
        ear?.start()
        notifyForeground("Ears up — say Hey Spidy (or long-press me)")
    }

    private fun onBuddyLongPress() {
        if (asleep) {
            SpideyDashPipBus.emit(DashEvent.WakeUp)
        }
        WeaveSense.spidySense(this)
        buddyHost?.reactSpidySense()
        collapseMenu()
        mood = DashMood.Listening("Listening — talk now")
        buddyHost?.showSpeech("Listening — talk now", 2_500)
        startSpidyEar()
        ear?.listenNow()
    }

    /** Any speech — show Spidy react icon + reply (proactive). */
    private fun onProactiveTalk(heard: String) {
        if (asleep) {
            asleep = false
            buddyHost?.setSleeping(false)
        }
        WeaveSense.spidySense(this)
        WeaveSense.wag(this)
        roamPausedUntil = System.currentTimeMillis() + 10_000
        buddyHost?.pulseTap()
        buddyHost?.reactSpidySense()
        handleVoiceCommand(heard)
    }

    private fun onSpidyWake(heard: String = "spidy") {
        if (asleep) {
            asleep = false
            buddyHost?.setSleeping(false)
        }
        WeaveSense.spidySense(this)
        WeaveSense.wag(this)
        roamPausedUntil = System.currentTimeMillis() + 12_000
        buddyHost?.pulseTap()
        buddyHost?.reactSpidySense()
        val line = "Hey $userName! How can I help you?"
        mood = DashMood.Listening(line)
        say(line, 3_400)
        scope.launch {
            delay(3_500)
            if (!asleep) ear?.enterCommandMode()
        }
    }

    private fun handleVoiceCommand(heard: String) {
        val action = DashCommander.parse(
            context = this,
            userName = userName,
            heard = heard,
            apps = apps,
            memoryEnabled = memoryEnabled,
            digestEnabled = digestEnabled,
            automationEnabled = automationEnabled,
        )
        roamPausedUntil = System.currentTimeMillis() + 12_000
        when (action) {
            is DashVoiceAction.SpeakOnly -> {
                mood = DashMood.Listening(action.line)
                say(action.line, 5_500)
            }
            is DashVoiceAction.SmartHelp -> handleSmartHelp(action.query)
            is DashVoiceAction.OpenApp -> {
                mood = DashMood.Found(action.line)
                say(action.line, 3_200)
                runCatching {
                    packageManager.getLaunchIntentForPackage(action.packageName)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                }
            }
            is DashVoiceAction.OpenAppsMenu -> {
                say(action.line, 2_800)
                expanded = true
                pane = PipSearchScript.home(userName)
                attachMenu()
                openSearch(DashSearchKind.APPS)
            }
            is DashVoiceAction.OpenSearch -> {
                say(action.line, 2_800)
                expanded = true
                attachMenu()
                pane = PipSearchScript.searchPane(action.kind)
                searchQuery = action.query
                mood = DashMood.Searching("Digging for “${action.query}”…")
                refreshSearchHits()
                refreshMenuLayout()
            }
            is DashVoiceAction.OpenMusic -> {
                say(action.line, 2_800)
                collapseMenu()
                showMusic()
            }
            is DashVoiceAction.OpenGallery -> {
                say(action.line, 2_800)
                collapseMenu()
                showGallery()
            }
            is DashVoiceAction.Sleep -> {
                say(action.line, 2_500)
                scope.launch {
                    delay(1_200)
                    goSleep()
                }
            }
            is DashVoiceAction.Hide -> {
                say(action.line, 1_800)
                scope.launch {
                    SpideyApp.instance.settings.setBuddyEnabled(false)
                    delay(1_000)
                    stop(this@SpideyOverlayService)
                }
            }
        }
        scope.launch {
            delay(5_000)
            if (mood is DashMood.Listening || mood is DashMood.Found) mood = DashMood.Roaming
        }
    }

    private fun handleSmartHelp(query: String) {
        mood = DashMood.Listening("Thinking…")
        buddyHost?.showSpeech("On it…", 2_000)
        scope.launch {
            val online = runCatching {
                if (gemini.hasKey()) gemini.chat(userName, emptyList(), query) else null
            }.getOrNull()?.let { raw ->
                raw.replace(Regex("\\[\\[ACTION:[A-Z_]+]]"), "").trim()
            }
            if (!online.isNullOrBlank()) {
                SpidyActivityLog.append(this@SpideyOverlayService, "internet_reply", query.take(40))
                mood = DashMood.Listening(online)
                say(online, 6_500)
                return@launch
            }
            // No Gemini key — open internet search for whatever they said.
            when (val r = SpidyAutomation.openWebSearch(this@SpideyOverlayService, query)) {
                is SpidyAutomation.Result.SpokeAndDid -> {
                    mood = DashMood.Found(r.line)
                    say("I opened that on the internet for you, $userName.", 4_200)
                }
                is SpidyAutomation.Result.Spoke -> {
                    mood = DashMood.Listening(r.line)
                    say(
                        "Add a Gemini key in Nest for spoken answers, $userName — or say open WhatsApp, meetings, or digest.",
                        5_500,
                    )
                }
            }
        }
    }

    private fun startMeetingReminderLoop() {
        scope.launch {
            while (isActive) {
                delay(45_000)
                if (asleep || !meetingsEnabled) continue
                if (!SpidyMeetings.hasPermission(this@SpideyOverlayService)) continue
                SpidyMeetings.dueReminders(this@SpideyOverlayService).forEach { m ->
                    SpidyMeetings.markReminded(this@SpideyOverlayService, m)
                    val line = SpidyMeetings.remindLine(userName, m)
                    mood = DashMood.Suggest(line)
                    say(line, 5_500)
                    delay(6_000)
                }
            }
        }
    }

    private fun showTransientLine(line: String) {
        buddyHost?.showSpeech(line.trim(), 4_000)
    }

    private suspend fun playGreeting() {
        mood = DashMood.Greeting("Ready when you are, $userName.")
        delay(400)
        if (mood is DashMood.Greeting) mood = DashMood.Roaming
    }

    private fun showMail(event: DashEvent.Mail) {
        scope.launch {
            WeaveSense.notify(this@SpideyOverlayService)
            roamPausedUntil = System.currentTimeMillis() + 8_000
            collapseMenu()
            mood = DashMood.MailPickup(event.speak)
            say(event.speak, 5_500)
            delay(5_500)
            if (mood is DashMood.MailPickup) mood = DashMood.Roaming
        }
    }

    private fun showMessage(event: DashEvent.Message) {
        scope.launch {
            WeaveSense.notify(this@SpideyOverlayService)
            roamPausedUntil = System.currentTimeMillis() + 8_000
            collapseMenu()
            mood = DashMood.MessagePickup(event.speak)
            say(event.speak, 5_500)
            delay(5_500)
            if (mood is DashMood.MessagePickup) mood = DashMood.Roaming
        }
    }

    private fun startListening() {
        collapseMenu()
        val line = "Hey $userName! How can I help you?"
        mood = DashMood.Listening(line)
        say(line, 3_500)
        ear?.enterCommandMode()
        // Also one-shot listen as backup if ear is paused for TTS.
        scope.launch {
            delay(3_600)
            ear?.enterCommandMode()
            delay(200)
            voice.start()
        }
    }

    private fun goSleep() {
        asleep = true
        isWalking = false
        buddyHost?.setWalking(false)
        buddyHost?.setSleeping(true)
        buddyHost?.hideSpeech()
        ear?.stop()
        collapseMenu()
        mood = DashMood.Sleeping(DashSuggestScript.sleepLine(userName))
        notifyForeground("Sleeping… say Spidy or unlock to wake")
    }

    private fun goWake() {
        scope.launch {
            asleep = false
            buddyHost?.setSleeping(false)
            WeaveSense.wag(this@SpideyOverlayService)
            mood = DashMood.Roaming
            startSpidyEar()
            ear?.nudge()
            notifyForeground("Awake — say Hey Spidy")
            delay(1_200)
            maybeMorningRoutine(fromUnlock = true)
        }
    }

    private suspend fun showSuggestion() {
        roamPausedUntil = System.currentTimeMillis() + 4_000
        isWalking = false
        buddyHost?.setWalking(false)
        mood = DashMood.Suggest(DashSuggestScript.forNow(userName))
        // Quiet — no speak unless user asked via Listen.
        delay(1_200)
        if (mood is DashMood.Suggest) mood = DashMood.Roaming
    }

    private fun showCamera() {
        scope.launch {
            WeaveSense.sense(this@SpideyOverlayService)
            mood = DashMood.CameraSnap("Nice shot! Holding your gallery…")
            delay(1_200)
            showGallery()
        }
    }

    private fun showGallery() {
        scope.launch {
            val photo = DashMediaHelper.recentPhoto(this@SpideyOverlayService)
            mood = DashMood.GalleryPeek(
                if (photo != null) "Looking at your photos with you." else "Gallery peek needs photo permission — open Nest to allow.",
                photo,
            )
            if (expanded) refreshMenuLayout()
            delay(6_500)
            if (mood is DashMood.GalleryPeek) mood = DashMood.Roaming
        }
    }

    private fun showMusic() {
        refreshMusicMood()
        val (title, artist) = DashMediaHelper.nowPlaying(this)
        mood = DashMood.MusicListen("Earbuds on — tap prev / play / next.", title, artist)
        if (expanded) refreshMenuLayout()
    }

    private fun refreshMusicMood() {
        val (title, artist) = DashMediaHelper.nowPlaying(this)
        musicPlaying = DashMediaHelper.activeController(this) != null
        if (mood is DashMood.MusicListen) {
            mood = DashMood.MusicListen("Earbuds in — change the track anytime.", title, artist)
        }
    }

    private fun handleOption(option: PipSearchScript.Option) {
        WeaveSense.tap(this)
        when (option.id) {
            "listen" -> startListening()
            "wave" -> {
                collapseMenu()
                scope.launch {
                    buddyHost?.pulseTap()
                    mood = DashMood.TapReact("wave")
                    delay(900)
                    if (mood is DashMood.TapReact) mood = DashMood.Roaming
                }
            }
            "suggest" -> {
                collapseMenu()
                scope.launch { showSuggestion() }
            }
            "search_apps" -> openSearch(DashSearchKind.APPS)
            "search_pics" -> openSearch(DashSearchKind.PICTURES)
            "search_music" -> openSearch(DashSearchKind.MUSIC)
            "search_video" -> openSearch(DashSearchKind.VIDEO)
            "search_files" -> openSearch(DashSearchKind.FILES)
            "search_people" -> openSearch(DashSearchKind.PEOPLE)
            "search_web" -> openSearch(DashSearchKind.WEB)
            "gallery" -> {
                collapseMenu()
                showGallery()
            }
            "music" -> {
                collapseMenu()
                showMusic()
            }
            "modules" -> {
                pane = PipSearchScript.modules()
                refreshMenuLayout()
            }
            "tips" -> {
                pane = PipSearchScript.tips()
                refreshMenuLayout()
            }
            "hide" -> {
                buddyHost?.hideSpeech()
                scope.launch {
                    SpideyApp.instance.settings.setBuddyEnabled(false)
                    delay(200)
                    stop(this@SpideyOverlayService)
                }
            }
            "back" -> {
                pane = PipSearchScript.home(userName)
                refreshMenuLayout()
            }
            "mod_assistant" -> openRoute(Routes.ASSISTANT)
            "mod_messages" -> openRoute(Routes.MESSAGES)
            "mod_mail" -> openRoute(Routes.MAIL)
            "mod_camera" -> openRoute(Routes.CAMERA)
            "mod_settings" -> openRoute(Routes.SETTINGS)
        }
    }

    private fun openSearch(kind: DashSearchKind) {
        pane = PipSearchScript.searchPane(kind)
        searchQuery = ""
        mood = DashMood.Searching("Digging… hang on!")
        refreshSearchHits()
        refreshMenuLayout()
    }

    private fun refreshSearchHits() {
        val kind = (pane as? PipSearchScript.Pane.Search)?.kind ?: return
        searchHits = if (kind == DashSearchKind.WEB) emptyList() else DashSearchCatalog.search(this, kind, searchQuery, apps)
        if (searchQuery.isNotBlank()) {
            mood = if (searchHits.isEmpty()) DashMood.Searching("Digging for “$searchQuery”…") else DashMood.Found("Got ${searchHits.size} — tap one!")
        }
    }

    private fun openHit(hit: DashHit) {
        WeaveSense.sense(this)
        DashSearchCatalog.openHit(this, hit)
        collapseMenu()
        mood = DashMood.Found("Opening ${hit.title}!")
        scope.launch {
            delay(1_400)
            mood = DashMood.Roaming
        }
    }

    private fun runWebSearch() {
        WeaveSense.sense(this)
        DashSearchCatalog.openWeb(this, searchQuery)
        collapseMenu()
        mood = DashMood.Searching("Searching the Internet…")
        scope.launch {
            delay(1_200)
            mood = DashMood.Roaming
        }
    }

    private fun openRoute(route: String) {
        collapseMenu()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_ROUTE, route),
        )
    }

    private fun notifyForeground(content: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification(content))
    }

    private fun buildNotification(content: String): Notification {
        val channelId = "spidey_overlay"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, getString(R.string.overlay_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.overlay_channel_desc)
            },
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WeaveHome · ${AppCredits.MASCOT_NAME}")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_legacy)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifeReceiver?.let { runCatching { unregisterReceiver(it) } }
        lifeReceiver = null
        ear?.stop()
        ear = null
        collapseMenu()
        buddyHost?.detach()
        buddyHost = null
        voice.stop()
        speaker.shutdown()
        store.clear()
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1701

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                android.widget.Toast.makeText(
                    context,
                    "Allow overlay first — Nest → Overlay",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                return
            }
            val micOk = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (!micOk) {
                android.widget.Toast.makeText(
                    context,
                    "Mic needed for voice — allow microphone, then Start again",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
            val intent = Intent(context, SpideyOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            android.widget.Toast.makeText(
                context,
                if (micOk) "Spidy starting — say Hey Spidy or long-press him"
                else "Buddy up without voice until mic is allowed",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }

        fun stop(context: Context) {
            CoroutineScope(Dispatchers.Default).launch {
                runCatching { SpideyApp.instance.settings.setBuddyEnabled(false) }
            }
            context.stopService(Intent(context, SpideyOverlayService::class.java))
            android.widget.Toast.makeText(context, "Spidy hidden", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
