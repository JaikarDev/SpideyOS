package com.jaikar.spideyos.companion

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.jaikar.spideyos.R
import kotlin.math.abs

/**
 * Native transparent overlay host — ImageView buddy + speech bubble above him (no Compose plate).
 */
class DashOverlayHost(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onTap: () -> Unit,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onLongPress: () -> Unit = {},
) {
    private val density = context.resources.displayMetrics.density
    private val characterSize = (100 * density).toInt()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hideBubbleRunnable: Runnable? = null
    private var bubbleLiftPx = 0
    private var longPressRunnable: Runnable? = null
    private var longPressFired = false

    val root: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = ColorDrawable(Color.TRANSPARENT)
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren = false
        clipToPadding = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val bubble: TextView = TextView(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f * density
            setColor(0xF016353F.toInt())
            setStroke((1.5f * density).toInt(), 0xFF7DFFC8.toInt())
        }
        setTextColor(0xFFE8F6F0.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(
            (14 * density).toInt(),
            (10 * density).toInt(),
            (14 * density).toInt(),
            (10 * density).toInt(),
        )
        maxWidth = (220 * density).toInt()
        maxLines = 4
        visibility = View.GONE
        alpha = 0f
        elevation = 0f
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val image: ImageView = ImageView(context).apply {
        setImageResource(R.drawable.spideydashpip)
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_CENTER
        setBackgroundColor(Color.TRANSPARENT)
        background = null
        setPadding(0, 0, 0, 0)
        elevation = 0f
        outlineProvider = null
        clipToOutline = false
        // Keep PNG alpha crisp on OEM overlays (no gray plate).
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }
    private var facingLeftFlag = false
    private var walkAnim: ObjectAnimator? = null
    private var bobAnim: ObjectAnimator? = null
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var dragging = false

    val layoutParams: WindowManager.LayoutParams

    init {
        root.addView(
            bubble,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (6 * density).toInt()
            },
        )
        root.addView(
            image,
            LinearLayout.LayoutParams(characterSize, characterSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            format = PixelFormat.TRANSLUCENT
            x = (context.resources.displayMetrics.widthPixels - (110 * density).toInt()).coerceAtLeast(0)
            y = (160 * density).toInt()
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    dragging = false
                    longPressFired = false
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    val lp = Runnable {
                        if (!dragging) {
                            longPressFired = true
                            onLongPress()
                        }
                    }
                    longPressRunnable = lp
                    mainHandler.postDelayed(lp, 480)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    if (!dragging && (abs(dx) > 10f || abs(dy) > 10f)) {
                        dragging = true
                        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    }
                    if (dragging) {
                        onDrag(dx, dy)
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    longPressRunnable = null
                    if (!dragging && !longPressFired) onTap()
                    dragging = false
                    longPressFired = false
                    true
                }
                else -> false
            }
        }
        startIdleBob()
    }

    fun attach() {
        windowManager.addView(root, layoutParams)
    }

    fun detach() {
        hideBubbleRunnable?.let { mainHandler.removeCallbacks(it) }
        hideBubbleRunnable = null
        walkAnim?.cancel()
        bobAnim?.cancel()
        runCatching { windowManager.removeView(root) }
    }

    /** Popup speech above SpideyDashPip (and keep character position stable). */
    fun showSpeech(line: String, holdMs: Long = 5_200) {
        val text = line.trim()
        if (text.isEmpty()) return
        hideBubbleRunnable?.let { mainHandler.removeCallbacks(it) }
        bubble.text = text
        bubble.visibility = View.VISIBLE
        bubble.alpha = 0f
        bubble.scaleX = 0.86f
        bubble.scaleY = 0.86f
        bubble.measure(
            View.MeasureSpec.makeMeasureSpec((220 * density).toInt(), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val lift = bubble.measuredHeight + (6 * density).toInt()
        if (bubbleLiftPx == 0) {
            bubbleLiftPx = lift
            layoutParams.y = (layoutParams.y - lift).coerceAtLeast(0)
            runCatching { windowManager.updateViewLayout(root, layoutParams) }
        }
        bubble.animate().cancel()
        bubble.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()
        val hide = Runnable { hideSpeech() }
        hideBubbleRunnable = hide
        mainHandler.postDelayed(hide, holdMs)
    }

    fun hideSpeech() {
        hideBubbleRunnable?.let { mainHandler.removeCallbacks(it) }
        hideBubbleRunnable = null
        if (bubble.visibility != View.VISIBLE && bubbleLiftPx == 0) return
        bubble.animate().cancel()
        bubble.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(160)
            .withEndAction {
                bubble.visibility = View.GONE
                if (bubbleLiftPx != 0) {
                    layoutParams.y += bubbleLiftPx
                    bubbleLiftPx = 0
                    runCatching { windowManager.updateViewLayout(root, layoutParams) }
                }
            }
            .start()
    }

    fun moveBy(dx: Float, dy: Float) {
        val maxX = (context.resources.displayMetrics.widthPixels - root.width.coerceAtLeast(1)).coerceAtLeast(0)
        val maxY = (context.resources.displayMetrics.heightPixels - root.height.coerceAtLeast(1)).coerceAtLeast(0)
        layoutParams.x = (layoutParams.x + dx.toInt()).coerceIn(0, maxX)
        layoutParams.y = (layoutParams.y + dy.toInt()).coerceIn(0, maxY)
        windowManager.updateViewLayout(root, layoutParams)
    }

    fun setPosition(x: Int, y: Int) {
        layoutParams.x = x.coerceAtLeast(0)
        layoutParams.y = y.coerceAtLeast(0)
        windowManager.updateViewLayout(root, layoutParams)
    }

    fun setFacingLeft(left: Boolean) {
        facingLeftFlag = left
        image.scaleX = if (left) -1f else 1f
    }

    fun setWalking(walking: Boolean) {
        if (walking) {
            bobAnim?.pause()
            if (walkAnim?.isRunning != true) {
                walkAnim?.cancel()
                walkAnim = ObjectAnimator.ofFloat(image, View.TRANSLATION_Y, 0f, -14f * density, 0f).apply {
                    duration = 280
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        } else {
            walkAnim?.cancel()
            image.translationY = 0f
            bobAnim?.resume()
        }
    }

    fun pulseTap() {
        val face = if (facingLeftFlag) -1f else 1f
        image.animate().cancel()
        image.animate()
            .scaleX(0.82f * face)
            .scaleY(0.82f)
            .setDuration(80)
            .withEndAction {
                image.animate()
                    .scaleX(1.12f * face)
                    .scaleY(1.12f)
                    .setDuration(120)
                    .withEndAction {
                        image.animate().scaleX(1f * face).scaleY(1f).setDuration(100).start()
                    }
                    .start()
            }
            .start()
    }

    /** Bigger bounce when you say Spidy / hey Spidy. */
    fun reactSpidySense() {
        val face = if (facingLeftFlag) -1f else 1f
        image.animate().cancel()
        image.animate()
            .scaleX(0.72f * face)
            .scaleY(0.72f)
            .rotation(-12f)
            .setDuration(90)
            .withEndAction {
                image.animate()
                    .scaleX(1.22f * face)
                    .scaleY(1.22f)
                    .rotation(10f)
                    .setDuration(140)
                    .withEndAction {
                        image.animate()
                            .scaleX(1f * face)
                            .scaleY(1f)
                            .rotation(0f)
                            .setDuration(160)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    fun setSleeping(sleeping: Boolean) {
        image.alpha = if (sleeping) 0.7f else 1f
        image.rotation = if (sleeping) -16f else 0f
        if (sleeping) {
            walkAnim?.cancel()
            bobAnim?.pause()
        } else {
            bobAnim?.resume()
            image.rotation = 0f
        }
    }

    private fun startIdleBob() {
        bobAnim = ObjectAnimator.ofFloat(image, View.TRANSLATION_Y, 0f, -8f * density, 0f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}

/** Thin top ticker — now playing / alerts. Fully translucent window. */
class DashTopInfoBar(
    private val context: Context,
    private val windowManager: WindowManager,
) {
    private val density = context.resources.displayMetrics.density
    private val text = TextView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        background = ColorDrawable(0xCC16353F.toInt())
        setTextColor(0xFFE8F6F0.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
        maxLines = 2
        visibility = View.GONE
    }
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        format = PixelFormat.TRANSLUCENT
        y = (36 * density).toInt()
    }
    private var attached = false

    fun attach() {
        if (attached) return
        windowManager.addView(text, params)
        attached = true
    }

    fun detach() {
        if (!attached) return
        runCatching { windowManager.removeView(text) }
        attached = false
    }

    fun show(message: String) {
        if (!attached) attach()
        text.text = message
        text.visibility = View.VISIBLE
    }

    fun hide() {
        text.visibility = View.GONE
    }
}
