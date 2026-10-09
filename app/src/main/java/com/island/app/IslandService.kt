package com.island.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IslandService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private lateinit var lp: WindowManager.LayoutParams
    private var root: FrameLayout? = null
    private var clock: TextView? = null
    private var bg: GradientDrawable? = null

    private val handler = Handler(Looper.getMainLooper())
    private val fmt = SimpleDateFormat("hh:mm", Locale.getDefault())

    private var wDp = 120f
    private var hDp = 34f
    private var xDp = 0f
    private var yDp = 8f

    private val density get() = resources.displayMetrics.density
    private fun px(dp: Float) = (dp * density).toInt()

    private val tick = object : Runnable {
        override fun run() {
            clock?.text = fmt.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        load()
        applyLayout()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("island", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        load()
        removeOverlay()
        createOverlay()
    }

    private fun load() {
        wDp = prefs.getFloat("w", 120f)
        hDp = prefs.getFloat("h", 34f)
        xDp = prefs.getFloat("x", 0f)
        yDp = prefs.getFloat("y", 8f)
    }

    private fun save() {
        prefs.edit()
            .putFloat("w", wDp).putFloat("h", hDp)
            .putFloat("x", xDp).putFloat("y", yDp)
            .apply()
    }

    private fun createOverlay() {
        val drawable = GradientDrawable().apply { setColor(Color.BLACK) }
        bg = drawable

        val tv = TextView(this).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            text = fmt.format(Date())
        }
        clock = tv

        val container = FrameLayout(this).apply {
            background = drawable
            addView(
                tv,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        root = container

        lp = WindowManager.LayoutParams(
            px(wDp),
            px(hDp),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = px(xDp)
            y = px(yDp)
        }

        val scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    wDp = (wDp * d.scaleFactor).coerceIn(60f, 360f)
                    hDp = (hDp * d.scaleFactor).coerceIn(24f, 120f)
                    applyLayout()
                    save()
                    return true
                }
            }
        )
        var lastX = 0f
        var lastY = 0f
        var rebase = false
        container.setOnTouchListener { _, e ->
            scaleDetector.onTouchEvent(e)
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = e.rawX
                    lastY = e.rawY
                    rebase = false
                }
                MotionEvent.ACTION_POINTER_UP -> rebase = true
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress && e.pointerCount == 1) {
                        if (rebase) {
                            lastX = e.rawX
                            lastY = e.rawY
                            rebase = false
                        } else {
                            xDp += (e.rawX - lastX) / density
                            yDp = (yDp + (e.rawY - lastY) / density).coerceAtLeast(0f)
                            lastX = e.rawX
                            lastY = e.rawY
                            applyLayout()
                            save()
                        }
                    }
                }
            }
            true
        }

        try {
            wm.addView(container, lp)
        } catch (_: Exception) {
        }
        applyLayout()
        handler.post(tick)
    }

    private fun applyLayout() {
        val r = root ?: return
        lp.width = px(wDp)
        lp.height = px(hDp)
        lp.x = px(xDp)
        lp.y = px(yDp)
        bg?.cornerRadius = px(hDp) / 2f
        clock?.textSize = hDp * 0.4f
        try {
            wm.updateViewLayout(r, lp)
        } catch (_: Exception) {
        }
    }

    private fun removeOverlay() {
        handler.removeCallbacks(tick)
        root?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        root = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        removeOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}
