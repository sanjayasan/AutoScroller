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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.autoscroller.app.R
import com.autoscroller.app.model.GestureConfig
import com.autoscroller.app.model.PointCoordinate
import com.autoscroller.app.model.ScrollerState
import com.autoscroller.app.util.PreferenceHelper
import java.util.Locale
import kotlin.math.abs

class OverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val gestureConfig: GestureConfig,
    private val pinMarkerManager: PinMarkerManager,
    private val preferenceHelper: PreferenceHelper
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

        pinMarkerManager.setInitialCoordinates(gestureConfig.pointA, gestureConfig.pointB)
        pinMarkerManager.onPointAChanged = { coord ->
            gestureConfig.pointA = coord
            preferenceHelper.saveGestureConfig(gestureConfig)
            updateCoordTextA(coord)
        }
        pinMarkerManager.onPointBChanged = { coord ->
            gestureConfig.pointB = coord
            preferenceHelper.saveGestureConfig(gestureConfig)
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

    private fun formatInterval(ms: Long, keepDecimal: Boolean = false): String {
        val nonNegative = ms.coerceAtLeast(0L)
        val sec = nonNegative / 1000.0
        return if (keepDecimal) {
            String.format(Locale.US, "%.1fs", sec)
        } else {
            if (nonNegative % 1000L == 0L) {
                "${nonNegative / 1000L}s"
            } else {
                String.format(Locale.US, "%.1fs", sec)
            }
        }
    }

    fun updateStartupCountdown(remainingSeconds: Int) {
        val text = "Starting in ${remainingSeconds}s..."
        expandedView?.findViewById<TextView>(R.id.tv_status_message)?.text = text
        minimizedView?.findViewById<TextView>(R.id.tv_pill_timer)?.text =
            formatInterval(gestureConfig.pauseIntervalMs)
        minimizedView?.findViewById<TextView>(R.id.tv_pill_loops)?.text =
            "Start in ${remainingSeconds}s"
    }

    fun updatePauseIntervalCountdown(remainingMs: Long) {
        minimizedView?.findViewById<TextView>(R.id.tv_pill_timer)?.text =
            formatInterval(remainingMs, keepDecimal = true)
    }

    fun updateProgress(currentLoop: Int, totalLoops: Int) {
        val text = "Loop $currentLoop / $totalLoops"
        expandedView?.findViewById<TextView>(R.id.tv_status_message)?.text = "Running: $text"
        minimizedView?.findViewById<TextView>(R.id.tv_pill_timer)?.text =
            formatInterval(gestureConfig.pauseIntervalMs)
        minimizedView?.findViewById<TextView>(R.id.tv_pill_loops)?.text =
            "Loop $currentLoop/$totalLoops"
    }

    fun updateState(state: ScrollerState) {
        val statusTv = expandedView?.findViewById<TextView>(R.id.tv_status_message)
        val pillTimerTv = minimizedView?.findViewById<TextView>(R.id.tv_pill_timer)
        val pillLoopsTv = minimizedView?.findViewById<TextView>(R.id.tv_pill_loops)
        when (state) {
            ScrollerState.IDLE -> {
                statusTv?.text = "Status: Ready"
                pillTimerTv?.text = formatInterval(gestureConfig.pauseIntervalMs)
                pillLoopsTv?.text = "Loop 0/${gestureConfig.loopCount}"
            }
            ScrollerState.COUNTDOWN -> {
                statusTv?.text = "Counting down..."
                pillTimerTv?.text = formatInterval(gestureConfig.pauseIntervalMs)
                pillLoopsTv?.text = "Starting..."
            }
            ScrollerState.RUNNING -> {
                statusTv?.text = "Running swipe loop"
            }
            ScrollerState.STOPPED -> {
                statusTv?.text = "Status: Stopped"
                pillTimerTv?.text = formatInterval(gestureConfig.pauseIntervalMs)
                pillLoopsTv?.text = "Stopped"
            }
        }
    }

    private fun syncMinimizedPill() {
        minimizedView?.findViewById<TextView>(R.id.tv_pill_timer)?.text =
            formatInterval(gestureConfig.pauseIntervalMs)
        minimizedView?.findViewById<TextView>(R.id.tv_pill_loops)?.text =
            "Loop 0/${gestureConfig.loopCount}"
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

        // Dismiss keyboard when clicking outside input fields
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                dismissActiveFocus()
            }
            false
        }
        dragBar.setOnClickListener {
            dismissActiveFocus()
        }

        // Minimize & Close buttons
        view.findViewById<ImageButton>(R.id.btn_minimize).setOnClickListener {
            dismissActiveFocus()
            switchToMinimized()
        }
        view.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dismissActiveFocus()
            onCloseRequested?.invoke()
        }

        // Pin markers toggle
        view.findViewById<View>(R.id.btn_toggle_pin_a).setOnClickListener {
            dismissActiveFocus()
            pinMarkerManager.togglePinA()
        }
        view.findViewById<View>(R.id.btn_toggle_pin_b).setOnClickListener {
            dismissActiveFocus()
            pinMarkerManager.togglePinB()
        }

        // Update coordinate labels
        updateCoordTextA(gestureConfig.pointA)
        updateCoordTextB(gestureConfig.pointB)

        // Delay stepper and direct keyboard input
        val etDelay = view.findViewById<EditText>(R.id.et_delay_val)
        setupEditableField(
            editText = etDelay,
            min = 0,
            max = 60,
            getValue = { gestureConfig.startDelaySeconds },
            onValueChanged = {
                gestureConfig.startDelaySeconds = it
                preferenceHelper.saveGestureConfig(gestureConfig)
            }
        )
        view.findViewById<Button>(R.id.btn_delay_minus).setOnClickListener {
            dismissActiveFocus()
            gestureConfig.startDelaySeconds = (gestureConfig.startDelaySeconds - 1).coerceAtLeast(0)
            etDelay.setText(gestureConfig.startDelaySeconds.toString())
            preferenceHelper.saveGestureConfig(gestureConfig)
        }
        view.findViewById<Button>(R.id.btn_delay_plus).setOnClickListener {
            dismissActiveFocus()
            gestureConfig.startDelaySeconds = (gestureConfig.startDelaySeconds + 1).coerceAtMost(60)
            etDelay.setText(gestureConfig.startDelaySeconds.toString())
            preferenceHelper.saveGestureConfig(gestureConfig)
        }

        // Loop count stepper (fixed to +/- 1) and direct keyboard input
        val etLoops = view.findViewById<EditText>(R.id.et_loops_val)
        setupEditableField(
            editText = etLoops,
            min = 1,
            max = 9999,
            getValue = { gestureConfig.loopCount },
            onValueChanged = {
                gestureConfig.loopCount = it
                preferenceHelper.saveGestureConfig(gestureConfig)
                syncMinimizedPill()
            }
        )
        view.findViewById<Button>(R.id.btn_loops_minus).setOnClickListener {
            dismissActiveFocus()
            gestureConfig.loopCount = (gestureConfig.loopCount - 1).coerceAtLeast(1)
            etLoops.setText(gestureConfig.loopCount.toString())
            preferenceHelper.saveGestureConfig(gestureConfig)
            syncMinimizedPill()
        }
        view.findViewById<Button>(R.id.btn_loops_plus).setOnClickListener {
            dismissActiveFocus()
            gestureConfig.loopCount = (gestureConfig.loopCount + 1).coerceAtMost(9999)
            etLoops.setText(gestureConfig.loopCount.toString())
            preferenceHelper.saveGestureConfig(gestureConfig)
            syncMinimizedPill()
        }

        // Swipe duration stepper and direct keyboard input
        val etDuration = view.findViewById<EditText>(R.id.et_duration_val)
        setupEditableField(
            editText = etDuration,
            min = 50,
            max = 5000,
            getValue = { gestureConfig.swipeDurationMs.toInt() },
            onValueChanged = {
                gestureConfig.swipeDurationMs = it.toLong()
                preferenceHelper.saveGestureConfig(gestureConfig)
            }
        )
        view.findViewById<Button>(R.id.btn_duration_minus).setOnClickListener {
            dismissActiveFocus()
            gestureConfig.swipeDurationMs = (gestureConfig.swipeDurationMs - 50L).coerceAtLeast(50L)
            etDuration.setText(gestureConfig.swipeDurationMs.toString())
            preferenceHelper.saveGestureConfig(gestureConfig)
        }
        view.findViewById<Button>(R.id.btn_duration_plus).setOnClickListener {
            dismissActiveFocus()
            gestureConfig.swipeDurationMs = (gestureConfig.swipeDurationMs + 50L).coerceAtMost(5000L)
            etDuration.setText(gestureConfig.swipeDurationMs.toString())
            preferenceHelper.saveGestureConfig(gestureConfig)
        }

        // Pause interval stepper and direct keyboard input
        val etInterval = view.findViewById<EditText>(R.id.et_interval_val)
        setupEditableField(
            editText = etInterval,
            min = 100,
            max = 10000,
            getValue = { gestureConfig.pauseIntervalMs.toInt() },
            onValueChanged = {
                gestureConfig.pauseIntervalMs = it.toLong()
                preferenceHelper.saveGestureConfig(gestureConfig)
                syncMinimizedPill()
            }
        )
        view.findViewById<Button>(R.id.btn_interval_minus).setOnClickListener {
            dismissActiveFocus()
            gestureConfig.pauseIntervalMs = (gestureConfig.pauseIntervalMs - 100L).coerceAtLeast(100L)
            etInterval.setText(gestureConfig.pauseIntervalMs.toString())
            preferenceHelper.saveGestureConfig(gestureConfig)
            syncMinimizedPill()
        }
        view.findViewById<Button>(R.id.btn_interval_plus).setOnClickListener {
            dismissActiveFocus()
            gestureConfig.pauseIntervalMs = (gestureConfig.pauseIntervalMs + 100L).coerceAtMost(10000L)
            etInterval.setText(gestureConfig.pauseIntervalMs.toString())
            preferenceHelper.saveGestureConfig(gestureConfig)
            syncMinimizedPill()
        }

        // Start / Stop buttons
        view.findViewById<Button>(R.id.btn_start).setOnClickListener {
            dismissActiveFocus()
            onStartRequested?.invoke()
        }
        view.findViewById<Button>(R.id.btn_stop).setOnClickListener {
            dismissActiveFocus()
            onStopRequested?.invoke()
        }

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun setupEditableField(
        editText: EditText,
        min: Int,
        max: Int,
        getValue: () -> Int,
        onValueChanged: (Int) -> Unit
    ) {
        editText.setText(getValue().toString())

        fun commit() {
            val text = editText.text.toString().trim()
            val parsed = text.toIntOrNull() ?: getValue()
            val clamped = parsed.coerceIn(min, max)
            onValueChanged(clamped)
            editText.setText(clamped.toString())
        }

        fun hideKeyboard() {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(editText.windowToken, 0)
            editText.clearFocus()
            setOverlayFocusable(false)
        }

        editText.setOnClickListener {
            setOverlayFocusable(true)
            editText.requestFocus()
            editText.selectAll()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                setOverlayFocusable(true)
                editText.post {
                    editText.selectAll()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                }
            } else {
                commit()
                setOverlayFocusable(false)
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commit()
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun dismissActiveFocus() {
        val currentFocus = expandedView?.findFocus()
        if (currentFocus is EditText) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
            setOverlayFocusable(false)
        }
    }

    private fun setOverlayFocusable(focusable: Boolean) {
        expandedParams?.let { params ->
            if (focusable) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            } else {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            try {
                windowManager.updateViewLayout(expandedView, params)
            } catch (_: Exception) {}
        }
    }

    private fun removeExpandedView() {
        expandedView?.let {
            dismissActiveFocus()
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

        syncMinimizedPill()

        setupDraggable(view, view, params) {
            switchToExpanded()
        }

        view.findViewById<ImageButton>(R.id.btn_pill_menu).setOnClickListener {
            switchToExpanded()
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
        params: WindowManager.LayoutParams,
        onSingleTap: (() -> Unit)? = null
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false

        dragTrigger.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        hasMoved = true
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager.updateViewLayout(rootView, params)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        onSingleTap?.invoke()
                    }
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
