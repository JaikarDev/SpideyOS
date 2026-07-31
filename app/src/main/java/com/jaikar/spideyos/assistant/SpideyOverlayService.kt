package com.jaikar.spideyos.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
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
import com.jaikar.spideyos.MainActivity
import com.jaikar.spideyos.R
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.apps.AppCatalog
import com.jaikar.spideyos.apps.InstalledApp
import com.jaikar.spideyos.sense.WeaveSense
import com.jaikar.spideyos.ui.Routes
import com.jaikar.spideyos.ui.companion.PipSearchCompanionPanel
import com.jaikar.spideyos.ui.theme.SpideyOSTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Floating Pip Search Companion — drag Pip to move, tap for scripted speech-bubble menus.
 * Local app search only. No generative AI.
 */
class SpideyOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var expanded by mutableStateOf(false)
    private var pane by mutableStateOf<PipSearchScript.Pane>(PipSearchScript.home("friend"))
    private var searchQuery by mutableStateOf("")
    private var apps by mutableStateOf<List<InstalledApp>>(emptyList())
    private var userName by mutableStateOf("friend")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val notification = buildNotification("Pip Search Companion is watching")
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
        if (Settings.canDrawOverlays(this)) {
            attachCompanion()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun attachCompanion() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        apps = AppCatalog.loadLaunchableApps(packageManager)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@SpideyOverlayService)
            setViewTreeViewModelStoreOwner(this@SpideyOverlayService)
            setViewTreeSavedStateRegistryOwner(this@SpideyOverlayService)
            setContent {
                SpideyOSTheme {
                    PipSearchCompanionPanel(
                        expanded = expanded,
                        pane = pane,
                        searchQuery = searchQuery,
                        searchResults = AppCatalog.filter(apps, searchQuery),
                        onToggle = {
                            WeaveSense.tap(this@SpideyOverlayService)
                            expanded = !expanded
                            if (expanded) {
                                pane = PipSearchScript.home(userName)
                                searchQuery = ""
                            }
                            refreshWindowFlags()
                            resizeToContent()
                        },
                        onOption = { handleOption(it) },
                        onSearchQueryChange = { searchQuery = it },
                        onLaunchApp = { launchApp(it) },
                        onCloseSearch = {
                            pane = PipSearchScript.home(userName)
                            searchQuery = ""
                            refreshWindowFlags()
                            resizeToContent()
                        },
                        onBackFromTip = {
                            pane = PipSearchScript.home(userName)
                            resizeToContent()
                        },
                        onDragBy = { dx, dy -> moveBy(dx, dy) },
                    )
                }
            }
        }
        composeView = view

        val collapsedW = (128 * density).toInt()
        val collapsedH = (118 * density).toInt()
        val params = WindowManager.LayoutParams(
            collapsedW,
            collapsedH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            collapsedFlags(),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - collapsedW - (16 * density).toInt())
                .coerceAtLeast(0)
            y = (120 * density).toInt()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        layoutParams = params
        windowManager?.addView(view, params)

        scope.launch {
            userName = SpideyApp.instance.settings.settings.first().userName.ifBlank { "friend" }
            pane = PipSearchScript.home(userName)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification("Hey $userName — drag Pip · tap to search."))
        }
    }

    private fun moveBy(dx: Float, dy: Float) {
        val lp = layoutParams ?: return
        val view = composeView ?: return
        val maxX = (resources.displayMetrics.widthPixels - view.width).coerceAtLeast(0)
        val maxY = (resources.displayMetrics.heightPixels - view.height).coerceAtLeast(0)
        lp.x = (lp.x + dx.toInt()).coerceIn(0, maxX)
        lp.y = (lp.y + dy.toInt()).coerceIn(0, maxY)
        windowManager?.updateViewLayout(view, lp)
    }

    private fun handleOption(opt: PipSearchScript.Option) {
        WeaveSense.tap(this)
        when (opt.id) {
            "search_apps" -> {
                pane = PipSearchScript.Pane.SearchApps()
                searchQuery = ""
                refreshWindowFlags()
                resizeToContent()
            }
            "modules" -> {
                pane = PipSearchScript.modules()
                resizeToContent()
            }
            "tips" -> {
                pane = PipSearchScript.tips()
                resizeToContent()
            }
            "hide" -> stop(this)
            "back" -> {
                pane = PipSearchScript.home(userName)
                resizeToContent()
            }
            "mod_assistant" -> openRoute(Routes.ASSISTANT)
            "mod_messages" -> openRoute(Routes.MESSAGES)
            "mod_mail" -> openRoute(Routes.MAIL)
            "mod_camera" -> openRoute(Routes.CAMERA)
            "mod_settings" -> openRoute(Routes.SETTINGS)
        }
    }

    private fun openRoute(route: String) {
        expanded = false
        refreshWindowFlags()
        resizeToContent()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
                .putExtra(MainActivity.EXTRA_ROUTE, route),
        )
    }

    private fun launchApp(app: InstalledApp) {
        WeaveSense.sense(this)
        val launch = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
        expanded = false
        searchQuery = ""
        pane = PipSearchScript.home(userName)
        refreshWindowFlags()
        resizeToContent()
    }

    private fun collapsedFlags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

    private fun expandedFlags(): Int {
        val needsIme = pane is PipSearchScript.Pane.SearchApps
        return if (needsIme) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }
    }

    private fun refreshWindowFlags() {
        val lp = layoutParams ?: return
        val view = composeView ?: return
        lp.flags = if (expanded) expandedFlags() else collapsedFlags()
        windowManager?.updateViewLayout(view, lp)
    }

    private fun resizeToContent() {
        val lp = layoutParams ?: return
        val view = composeView ?: return
        val density = resources.displayMetrics.density
        if (expanded) {
            lp.width = (300 * density).toInt()
            lp.height = when (pane) {
                is PipSearchScript.Pane.SearchApps -> (420 * density).toInt()
                is PipSearchScript.Pane.Tip -> (280 * density).toInt()
                else -> (360 * density).toInt()
            }
        } else {
            lp.width = (128 * density).toInt()
            lp.height = (118 * density).toInt()
        }
        windowManager?.updateViewLayout(view, lp)
    }

    private fun buildNotification(content: String = "Pip Search Companion active"): Notification {
        val channelId = "spidey_overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.overlay_channel_desc) },
        )
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WeaveHome · Pip")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_legacy)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let { runCatching { windowManager?.removeView(it) } }
        composeView = null
        store.clear()
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1701

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, SpideyOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SpideyOverlayService::class.java))
        }
    }
}
