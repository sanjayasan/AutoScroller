package com.autoscroller.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.autoscroller.app.AutoScrollerApp
import com.autoscroller.app.R
import com.autoscroller.app.model.GestureConfig
import com.autoscroller.app.model.ScrollerState
import com.autoscroller.app.overlay.OverlayController
import com.autoscroller.app.overlay.PinMarkerManager
import com.autoscroller.app.ui.MainActivity
import com.autoscroller.app.util.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    companion object {
        const val ACTION_STOP_SERVICE = "com.autoscroller.app.ACTION_STOP_SERVICE"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var executionJob: Job? = null

    private lateinit var windowManager: WindowManager
    private lateinit var pinMarkerManager: PinMarkerManager
    private lateinit var overlayController: OverlayController
    private val gestureConfig = GestureConfig()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        pinMarkerManager = PinMarkerManager(this, windowManager)
        overlayController = OverlayController(this, windowManager, gestureConfig, pinMarkerManager)

        startAsForeground()
        setupOverlayCallbacks()
        overlayController.initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopExecution()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, AutoScrollerApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_play)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.btn_stop), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupOverlayCallbacks() {
        overlayController.onStartRequested = {
            startExecution()
        }

        overlayController.onStopRequested = {
            stopExecution()
        }

        overlayController.onCloseRequested = {
            stopExecution()
            stopSelf()
        }
    }

    private fun startExecution() {
        val accessibilityService = ScrollAccessibilityService.instance
        if (accessibilityService == null) {
            VibrationHelper.vibrateMedium(this)
            overlayController.updateState(ScrollerState.STOPPED)
            return
        }

        stopExecution()
        VibrationHelper.vibrateShort(this)

        executionJob = serviceScope.launch {
            // Hide pins during swipe sequence so they don't block clicks/gestures
            pinMarkerManager.setPinsVisibility(false)
            overlayController.switchToMinimized()

            // 1. Countdown phase
            overlayController.updateState(ScrollerState.COUNTDOWN)
            var remaining = gestureConfig.startDelaySeconds
            while (remaining > 0 && isActive) {
                overlayController.updateCountdown(remaining)
                delay(1000L)
                remaining--
            }

            if (!isActive) {
                onExecutionCompleted()
                return@launch
            }

            // 2. Loop Execution phase
            overlayController.updateState(ScrollerState.RUNNING)
            for (loop in 1..gestureConfig.loopCount) {
                if (!isActive) break

                overlayController.updateProgress(loop, gestureConfig.loopCount)

                val success = accessibilityService.executeSwipe(
                    gestureConfig.pointA.x,
                    gestureConfig.pointA.y,
                    gestureConfig.pointB.x,
                    gestureConfig.pointB.y,
                    gestureConfig.swipeDurationMs
                )

                if (!success || !isActive) break

                // Pause interval between swipes
                delay(gestureConfig.pauseIntervalMs)
            }

            onExecutionCompleted()
        }
    }

    private fun stopExecution() {
        executionJob?.cancel()
        executionJob = null
        onExecutionCompleted()
    }

    private fun onExecutionCompleted() {
        VibrationHelper.vibrateShort(this)
        pinMarkerManager.setPinsVisibility(true)
        overlayController.updateState(ScrollerState.IDLE)
        overlayController.switchToExpanded()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopExecution()
        serviceScope.cancel()
        overlayController.destroy()
    }
}
