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
import android.widget.ImageView
import android.widget.TextView
import com.autoscroller.app.R
import com.autoscroller.app.model.PointCoordinate

class PinMarkerManager(
    private val context: Context,
    private val windowManager: WindowManager
) {

    private var pinAView: View? = null
    private var pinAParams: WindowManager.LayoutParams? = null
    private var pinACoordinate = PointCoordinate(540f, 1600f)

    private var pinBView: View? = null
    private var pinBParams: WindowManager.LayoutParams? = null
    private var pinBCoordinate = PointCoordinate(540f, 600f)

    var onPointAChanged: ((PointCoordinate) -> Unit)? = null
    var onPointBChanged: ((PointCoordinate) -> Unit)? = null

    val isPinAShown: Boolean get() = pinAView != null
    val isPinBShown: Boolean get() = pinBView != null

    fun togglePinA() {
        if (isPinAShown) {
            hidePinA()
        } else {
            showPinA(pinACoordinate)
        }
    }

    fun togglePinB() {
        if (isPinBShown) {
            hidePinB()
        } else {
            showPinB(pinBCoordinate)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showPinA(initial: PointCoordinate) {
        if (pinAView != null) return
        pinACoordinate = initial

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_pin_marker, null)
        val icon = view.findViewById<ImageView>(R.id.iv_pin_icon)
        val label = view.findViewById<TextView>(R.id.tv_pin_label)

        icon.setImageResource(R.drawable.ic_pin_a)
        label.text = "Point A"

        val params = createLayoutParams(initial.x.toInt() - 72, initial.y.toInt() - 72)
        setupDraggableTouch(view, params) { newX, newY ->
            pinACoordinate = PointCoordinate(newX, newY)
            onPointAChanged?.invoke(pinACoordinate)
        }

        try {
            windowManager.addView(view, params)
            pinAView = view
            pinAParams = params
            onPointAChanged?.invoke(pinACoordinate)
        } catch (_: Exception) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showPinB(initial: PointCoordinate) {
        if (pinBView != null) return
        pinBCoordinate = initial

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_pin_marker, null)
        val icon = view.findViewById<ImageView>(R.id.iv_pin_icon)
        val label = view.findViewById<TextView>(R.id.tv_pin_label)

        icon.setImageResource(R.drawable.ic_pin_b)
        label.text = "Point B"

        val params = createLayoutParams(initial.x.toInt() - 72, initial.y.toInt() - 72)
        setupDraggableTouch(view, params) { newX, newY ->
            pinBCoordinate = PointCoordinate(newX, newY)
            onPointBChanged?.invoke(pinBCoordinate)
        }

        try {
            windowManager.addView(view, params)
            pinBView = view
            pinBParams = params
            onPointBChanged?.invoke(pinBCoordinate)
        } catch (_: Exception) {}
    }

    fun hidePinA() {
        pinAView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            pinAView = null
            pinAParams = null
        }
    }

    fun hidePinB() {
        pinBView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            pinBView = null
            pinBParams = null
        }
    }

    fun setPinsVisibility(visible: Boolean) {
        pinAView?.visibility = if (visible) View.VISIBLE else View.GONE
        pinBView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun removeAll() {
        hidePinA()
        hidePinB()
    }

    private fun createLayoutParams(x: Int, y: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x.coerceAtLeast(0)
            this.y = y.coerceAtLeast(0)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggableTouch(
        view: View,
        params: WindowManager.LayoutParams,
        onCenterUpdated: (Float, Float) -> Unit
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
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
                        windowManager.updateViewLayout(view, params)
                    } catch (_: Exception) {}
                    val centerX = params.x + (view.width / 2f)
                    val centerY = params.y + (view.height / 2f)
                    onCenterUpdated(centerX, centerY)
                    true
                }
                else -> false
            }
        }
    }
}
