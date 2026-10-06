package com.autoscroller.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.autoscroller.app.R
import com.autoscroller.app.model.GestureConfig
import com.autoscroller.app.model.PointCoordinate
import com.autoscroller.app.model.ScrollerState

class OverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val gestureConfig: GestureConfig,
    private val pinMarkerManager: PinMarkerManager
) {

    private var expandedView: View? = null
    private var expandedParams: WindowManager.LayoutParams? = null

    private var minimizedView: View? = null
    private var minimizedParams: WindowManager.LayoutParams? = null

    private var isExpanded = true

    var onStartRequested: (() -> Unit)? = null
    var onStopRequested: (() -> Unit)? = null
    var onCloseRequested: (() -> Unit)? = null

    fun initialize() {
        showExpandedView()

        pinMarkerManager.onPointAChanged = { coord ->
            gestureConfig.pointA = coord
            updateCoordTextA(coord)
        }
        pinMarkerManager.onPointBChanged = { coord ->
            gestureConfig.pointB = coord
            updateCoordTextB(coord)
        }
    }

    fun switchToMinimized() {
        if (!isExpanded) return
        removeExpandedView()
        showMinimizedView()
        isExpanded = false
    }

    fun switchToExpanded() {
        if (isExpanded) return
        removeMinimizedView()
        showExpandedView()
        isExpanded = true
    }

    fun updateCountdown(remainingSeconds: Int) {
        val text = "Starting in ${remainingSeconds}s..."
        expandedView?.findViewById<TextView>(R.id.tv_status_message)?.text = text
        minimizedView?.findViewById<TextView>(R.id.tv_pill_status)?.text = "${remainingSeconds}s"
    }

    fun updateProgress(currentLoop: Int, totalLoops: Int) {
        val text = "Loop $currentLoop / $totalLoops"
        expandedView?.findViewById<TextView>(R.id.tv_status_message)?.text = "Running: $text"
        minimizedView?.findViewById<TextView>(R.id.tv_pill_status)?.text = text
    }

    fun updateState(state: ScrollerState) {
        val statusTv = expandedView?.findViewById<TextView>(R.id.tv_status_message)
        val pillTv = minimizedView?.findViewById<TextView>(R.id.tv_pill_status)
        when (state) {
            ScrollerState.IDLE -> {
                statusTv?.text = "Status: Ready"
                pillTv?.text = "Ready"
            }
            ScrollerState.COUNTDOWN -> {
                statusTv?.text = "Counting down..."
            }
            ScrollerState.RUNNING -> {
                statusTv?.text = "Running swipe loop"
            }
            ScrollerState.STOPPED -> {
                statusTv?.text = "Status: Stopped"
                pillTv?.text = "Stopped"
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showExpandedView() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_panel_expanded, null)

        val params = createOverlayLayoutParams(100, 200, WindowManager.LayoutParams.WRAP_CONTENT)
        expandedParams = params
        expandedView = view

        val dragBar = view.findViewById<View>(R.id.header_drag_bar)
        setupDraggable(dragBar, view, params)

        // Minimize & Close buttons
        view.findViewById<ImageButton>(R.id.btn_minimize).setOnClickListener {
            switchToMinimized()
        }
        view.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            onCloseRequested?.invoke()
        }

        // Pin markers toggle
        view.findViewById<View>(R.id.btn_toggle_pin_a).setOnClickListener {
            pinMarkerManager.togglePinA()
        }
        view.findViewById<View>(R.id.btn_toggle_pin_b).setOnClickListener {
            pinMarkerManager.togglePinB()
        }

        // Update coordinate labels
        updateCoordTextA(gestureConfig.pointA)
        updateCoordTextB(gestureConfig.pointB)

        // Delay stepper
        val tvDelay = view.findViewById<TextView>(R.id.tv_delay_val)
        tvDelay.text = "${gestureConfig.startDelaySeconds}s"
        view.findViewById<Button>(R.id.btn_delay_minus).setOnClickListener {
            if (gestureConfig.startDelaySeconds > 0) {
                gestureConfig.startDelaySeconds--
                tvDelay.text = "${gestureConfig.startDelaySeconds}s"
            }
        }
        view.findViewById<Button>(R.id.btn_delay_plus).setOnClickListener {
            if (gestureConfig.startDelaySeconds < 60) {
                gestureConfig.startDelaySeconds++
                tvDelay.text = "${gestureConfig.startDelaySeconds}s"
            }
        }

        // Loops stepper
        val tvLoops = view.findViewById<TextView>(R.id.tv_loops_val)
        tvLoops.text = "${gestureConfig.loopCount}"
        view.findViewById<Button>(R.id.btn_loops_minus).setOnClickListener {
            if (gestureConfig.loopCount > 1) {
                gestureConfig.loopCount -= if (gestureConfig.loopCount > 10) 5 else 1
                tvLoops.text = "${gestureConfig.loopCount}"
            }
        }
        view.findViewById<Button>(R.id.btn_loops_plus).setOnClickListener {
            if (gestureConfig.loopCount < 9999) {
                gestureConfig.loopCount += 5
                tvLoops.text = "${gestureConfig.loopCount}"
            }
        }

        // Duration stepper
        val tvDuration = view.findViewById<TextView>(R.id.tv_duration_val)
        tvDuration.text = "${gestureConfig.swipeDurationMs}ms"
        view.findViewById<Button>(R.id.btn_duration_minus).setOnClickListener {
            if (gestureConfig.swipeDurationMs > 50L) {
                gestureConfig.swipeDurationMs -= 50L
                tvDuration.text = "${gestureConfig.swipeDurationMs}ms"
            }
        }
        view.findViewById<Button>(R.id.btn_duration_plus).setOnClickListener {
            if (gestureConfig.swipeDurationMs < 5000L) {
                gestureConfig.swipeDurationMs += 50L
                tvDuration.text = "${gestureConfig.swipeDurationMs}ms"
            }
        }

        // Interval stepper
        val tvInterval = view.findViewById<TextView>(R.id.tv_interval_val)
        tvInterval.text = "${gestureConfig.pauseIntervalMs}ms"
        view.findViewById<Button>(R.id.btn_interval_minus).setOnClickListener {
            if (gestureConfig.pauseIntervalMs > 100L) {
                gestureConfig.pauseIntervalMs -= 100L
                tvInterval.text = "${gestureConfig.pauseIntervalMs}ms"
            }
        }
        view.findViewById<Button>(R.id.btn_interval_plus).setOnClickListener {
            if (gestureConfig.pauseIntervalMs < 10000L) {
                gestureConfig.pauseIntervalMs += 100L
                tvInterval.text = "${gestureConfig.pauseIntervalMs}ms"
            }
        }

        // Start / Stop buttons
        view.findViewById<Button>(R.id.btn_start).setOnClickListener {
            onStartRequested?.invoke()
        }
        view.findViewById<Button>(R.id.btn_stop).setOnClickListener {
            onStopRequested?.invoke()
        }

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun removeExpandedView() {
        expandedView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            expandedView = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showMinimizedView() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_panel_minimized, null)

        val params = createOverlayLayoutParams(50, 100, WindowManager.LayoutParams.WRAP_CONTENT)
        minimizedParams = params
        minimizedView = view

        setupDraggable(view, view, params)

        view.findViewById<ImageButton>(R.id.btn_pill_expand).setOnClickListener {
            switchToExpanded()
        }

        view.findViewById<ImageButton>(R.id.btn_pill_stop).setOnClickListener {
            onStopRequested?.invoke()
        }

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun removeMinimizedView() {
        minimizedView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            minimizedView = null
        }
    }

    private fun updateCoordTextA(coord: PointCoordinate) {
        expandedView?.findViewById<TextView>(R.id.tv_coord_a)?.text = "A: $coord"
    }

    private fun updateCoordTextB(coord: PointCoordinate) {
        expandedView?.findViewById<TextView>(R.id.tv_coord_b)?.text = "B: $coord"
    }

    private fun createOverlayLayoutParams(x: Int, y: Int, width: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggable(
        dragTrigger: View,
        rootView: View,
        params: WindowManager.LayoutParams
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragTrigger.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(rootView, params)
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }
    }

    fun destroy() {
        removeExpandedView()
        removeMinimizedView()
        pinMarkerManager.removeAll()
    }
}
