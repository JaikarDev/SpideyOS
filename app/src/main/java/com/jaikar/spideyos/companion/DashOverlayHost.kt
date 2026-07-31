package com.jaikar.spideyos.companion

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.jaikar.spideyos.R
import kotlin.math.abs

/**
 * Native transparent overlay host — ImageView only (no Compose plate / grey box).
 */
class DashOverlayHost(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onTap: () -> Unit,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
) {
    private val density = context.resources.displayMetrics.density
    val root: FrameLayout = FrameLayout(context).apply {
        background = ColorDrawable(Color.TRANSPARENT)
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren = false
        clipToPadding = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val image: ImageView = ImageView(context).apply {
        setImageResource(R.drawable.spideydashpip)
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_CENTER
        setBackgroundColor(Color.TRANSPARENT)
        background = null
        elevation = 0f
        outlineProvider = null
        clipToOutline = false
    }
    private var facingLeftFlag = false
    private var walkAnim: ObjectAnimator? = null
    private var bobAnim: ObjectAnimator? = null
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var dragging = false

    val layoutParams: WindowManager.LayoutParams

    init {
        val size = (100 * density).toInt()
        root.addView(image, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
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
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    if (!dragging && (abs(dx) > 10f || abs(dy) > 10f)) dragging = true
                    if (dragging) {
                        onDrag(dx, dy)
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!dragging) onTap()
                    dragging = false
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
        walkAnim?.cancel()
        bobAnim?.cancel()
        runCatching { windowManager.removeView(root) }
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
