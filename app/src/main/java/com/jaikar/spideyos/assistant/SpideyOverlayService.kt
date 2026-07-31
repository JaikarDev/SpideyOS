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
import com.jaikar.spideyos.companion.DashCuteLines
import com.jaikar.spideyos.companion.DashEvent
import com.jaikar.spideyos.companion.DashHit
import com.jaikar.spideyos.companion.DashMediaHelper
import com.jaikar.spideyos.companion.DashMood
import com.jaikar.spideyos.companion.DashOverlayHost
import com.jaikar.spideyos.companion.DashSearchCatalog
import com.jaikar.spideyos.companion.DashSearchKind
import com.jaikar.spideyos.companion.DashSpeaker
import com.jaikar.spideyos.companion.DashSuggestScript
import com.jaikar.spideyos.companion.DashTopInfoBar
import com.jaikar.spideyos.companion.DashVoice
import com.jaikar.spideyos.companion.DashVoiceReply
import com.jaikar.spideyos.companion.SpideyDashPipBus
import com.jaikar.spideyos.companion.SpideyDashPipLifeReceiver
import com.jaikar.spideyos.sense.WeaveSense
import com.jaikar.spideyos.ui.Routes
import com.jaikar.spideyos.ui.companion.SpideyDashPipPanel
import com.jaikar.spideyos.ui.theme.SpideyOSTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
    private var topBar: DashTopInfoBar? = null
    private var menuView: ComposeView? = null
    private var menuParams: WindowManager.LayoutParams? = null
    private var lifeReceiver: BroadcastReceiver? = null
    private var alertJob: Job? = null

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

    private lateinit var speaker: DashSpeaker
    private lateinit var voice: DashVoice

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        speaker = DashSpeaker(this)
        voice = DashVoice(
            context = this,
            onResult = { heard -> replyToVoice(heard) },
            onError = { line -> showTransientLine(line) },
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
            scope.launch {
                delay(900)
                playGreeting()
            }
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun startAsForeground() {
        val notification = buildNotification("${AppCredits.MASCOT_NAME} is awake on your home")
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun attachCompanion() {
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = manager
        apps = AppCatalog.loadLaunchableApps(packageManager)
        topBar = DashTopInfoBar(this, manager).also { it.attach() }
        buddyHost = DashOverlayHost(
            context = this,
            windowManager = manager,
            onTap = ::onBuddyTap,
            onDrag = ::onBuddyDrag,
        ).also { it.attach() }
        scope.launch {
            userName = SpideyApp.instance.settings.settings.first().userName.ifBlank { "friend" }
            pane = PipSearchScript.home(userName)
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

        if (!expanded) {
            mood = DashMood.TapReact(DashCuteLines.tap())
            scope.launch {
                delay(380)
                expanded = true
                pane = PipSearchScript.home(userName)
                mood = DashMood.Greeting("What can I find for you?")
                attachMenu()
            }
        } else {
            collapseMenu()
            mood = DashMood.TapReact("Okay! I’ll keep roaming~")
            scope.launch {
                delay(1_100)
                if (mood is DashMood.TapReact) mood = DashMood.Roaming
            }
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
                if (mood !is DashMood.Roaming && mood !is DashMood.Walking && mood !is DashMood.MusicListen && mood !is DashMood.Suggest) continue
                if (mood !is DashMood.MusicListen && Random.nextFloat() < 0.18f) {
                    playGreeting()
                } else {
                    roamStep()
                }
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
        scope.launch {
            while (isActive) {
                delay(Random.nextLong(45_000, 90_000))
                if (!asleep && !expanded && !userDragging && mood !is DashMood.MailPickup && mood !is DashMood.MessagePickup) {
                    showSuggestion()
                }
            }
        }
    }

    private fun startEventCollector() {
        scope.launch {
            SpideyDashPipBus.events.collect { event ->
                when (event) {
                    is DashEvent.Mail -> if (!asleep) showMail(event)
                    is DashEvent.Message -> if (!asleep) showMessage(event)
                    DashEvent.CameraCaptured -> if (!asleep) showCamera()
                    DashEvent.WaveHi -> if (!asleep) playGreeting()
                    DashEvent.OpenGallery -> if (!asleep) showGallery()
                    DashEvent.OpenMusic -> if (!asleep) showMusic()
                    DashEvent.WakeUp -> goWake()
                    DashEvent.GoSleep -> goSleep()
                    DashEvent.SuggestNow -> if (!asleep) showSuggestion()
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
                musicPlaying = DashMediaHelper.activeController(this@SpideyOverlayService)
                    ?.playbackState
                    ?.state == PlaybackState.STATE_PLAYING
                if (nowPlaying != null && musicPlaying) {
                    showNowPlaying(nowPlaying.appLabel, nowPlaying.title, nowPlaying.artist)
                } else if (alertJob?.isActive != true) {
                    topBar?.hide()
                }
                if (mood is DashMood.MusicListen && nowPlaying != null) {
                    mood = DashMood.MusicListen((mood as DashMood.MusicListen).line, nowPlaying.title, nowPlaying.artist)
                }
            }
        }
    }

    private fun showNowPlaying(app: String, title: String?, artist: String?) {
        val track = listOfNotNull(title, artist).joinToString(" — ").ifBlank { "now playing" }
        topBar?.show("♪ $app · $track")
    }

    private fun showTransientLine(line: String) {
        alertJob?.cancel()
        topBar?.show(line)
        alertJob = scope.launch {
            delay(5_000)
            val now = DashMediaHelper.nowPlayingInfo(this@SpideyOverlayService)
            val playing = DashMediaHelper.activeController(this@SpideyOverlayService)
                ?.playbackState
                ?.state == PlaybackState.STATE_PLAYING
            if (now != null && playing) showNowPlaying(now.appLabel, now.title, now.artist) else topBar?.hide()
        }
    }

    private suspend fun playGreeting() {
        val line = "Hi $userName! SpideyDashPip checking in."
        WeaveSense.wag(this)
        speaker.speak(line)
        mood = DashMood.Greeting(line)
        showTransientLine(line)
        delay(3_200)
        if (mood is DashMood.Greeting) mood = DashMood.Roaming
    }

    private fun showMail(event: DashEvent.Mail) {
        scope.launch {
            WeaveSense.notify(this@SpideyOverlayService)
            roamPausedUntil = System.currentTimeMillis() + 8_000
            collapseMenu()
            mood = DashMood.MailPickup(event.speak)
            speaker.speak(event.speak)
            showTransientLine(event.speak)
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
            speaker.speak(event.speak)
            showTransientLine(event.speak)
            delay(5_500)
            if (mood is DashMood.MessagePickup) mood = DashMood.Roaming
        }
    }

    private fun startListening() {
        collapseMenu()
        val line = "I'm listening, $userName."
        mood = DashMood.Listening(line)
        speaker.speak(line)
        showTransientLine(line)
        voice.start()
    }

    private fun replyToVoice(heard: String) {
        val reply = DashVoiceReply.reply(userName, heard)
        mood = DashMood.Listening(reply)
        speaker.speak(reply)
        showTransientLine(reply)
        scope.launch {
            delay(4_000)
            if (mood is DashMood.Listening) mood = DashMood.Roaming
        }
    }

    private fun goSleep() {
        asleep = true
        isWalking = false
        buddyHost?.setWalking(false)
        buddyHost?.setSleeping(true)
        collapseMenu()
        mood = DashMood.Sleeping(DashSuggestScript.sleepLine(userName))
        notifyForeground("Sleeping… unlock to wake SpideyDashPip")
    }

    private fun goWake() {
        scope.launch {
            asleep = false
            buddyHost?.setSleeping(false)
            WeaveSense.wag(this@SpideyOverlayService)
            val line = DashSuggestScript.wakeLine(userName)
            mood = DashMood.Greeting(line)
            speaker.speak(line)
            showTransientLine(line)
            notifyForeground("Awake on your phone's home screen")
            delay(3_400)
            if (mood is DashMood.Greeting) mood = DashMood.Roaming
            delay(1_200)
            if (!asleep) showSuggestion()
        }
    }

    private suspend fun showSuggestion() {
        roamPausedUntil = System.currentTimeMillis() + 7_000
        isWalking = false
        buddyHost?.setWalking(false)
        mood = DashMood.Suggest(DashSuggestScript.forNow(userName))
        showTransientLine((mood as DashMood.Suggest).line)
        delay(5_800)
        if (mood is DashMood.Suggest) mood = DashMood.Roaming
    }

    private fun showCamera() {
        scope.launch {
            WeaveSense.sense(this@SpideyOverlayService)
            mood = DashMood.CameraSnap("Nice shot! Holding your gallery…")
            showTransientLine((mood as DashMood.CameraSnap).line)
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
            if (expanded) refreshMenuLayout() else showTransientLine((mood as DashMood.GalleryPeek).line)
            delay(6_500)
            if (mood is DashMood.GalleryPeek) mood = DashMood.Roaming
        }
    }

    private fun showMusic() {
        refreshMusicMood()
        val (title, artist) = DashMediaHelper.nowPlaying(this)
        mood = DashMood.MusicListen("Earbuds on — tap prev / play / next.", title, artist)
        if (expanded) refreshMenuLayout() else showTransientLine((mood as DashMood.MusicListen).line)
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
                scope.launch { playGreeting() }
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
                val line = "Wave off — see you later, $userName!"
                speaker.speak(line)
                scope.launch {
                    SpideyApp.instance.settings.setBuddyEnabled(false)
                    delay(1_200)
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
        alertJob?.cancel()
        collapseMenu()
        buddyHost?.detach()
        buddyHost = null
        topBar?.detach()
        topBar = null
        voice.stop()
        speaker.shutdown()
        store.clear()
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1701

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, SpideyOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            CoroutineScope(Dispatchers.Default).launch {
                runCatching { SpideyApp.instance.settings.setBuddyEnabled(false) }
            }
            context.stopService(Intent(context, SpideyOverlayService::class.java))
        }
    }
}
