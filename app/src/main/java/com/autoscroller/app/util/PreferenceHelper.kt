package com.autoscroller.app.util

import android.content.Context
import android.content.SharedPreferences
import com.autoscroller.app.model.GestureConfig
import com.autoscroller.app.model.PointCoordinate

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "autoscroller_prefs"
        private const val KEY_START_DELAY = "key_start_delay"
        private const val KEY_LOOP_COUNT = "key_loop_count"
        private const val KEY_SWIPE_DURATION = "key_swipe_duration"
        private const val KEY_PAUSE_INTERVAL = "key_pause_interval"
        private const val KEY_POINT_A_X = "key_point_a_x"
        private const val KEY_POINT_A_Y = "key_point_a_y"
        private const val KEY_POINT_B_X = "key_point_b_x"
        private const val KEY_POINT_B_Y = "key_point_b_y"
    }

    fun loadGestureConfig(defaultConfig: GestureConfig = GestureConfig()): GestureConfig {
        return GestureConfig(
            startDelaySeconds = prefs.getInt(KEY_START_DELAY, defaultConfig.startDelaySeconds),
            loopCount = prefs.getInt(KEY_LOOP_COUNT, defaultConfig.loopCount),
            swipeDurationMs = prefs.getLong(KEY_SWIPE_DURATION, defaultConfig.swipeDurationMs),
            pauseIntervalMs = prefs.getLong(KEY_PAUSE_INTERVAL, defaultConfig.pauseIntervalMs),
            pointA = PointCoordinate(
                x = prefs.getFloat(KEY_POINT_A_X, defaultConfig.pointA.x),
                y = prefs.getFloat(KEY_POINT_A_Y, defaultConfig.pointA.y)
            ),
            pointB = PointCoordinate(
                x = prefs.getFloat(KEY_POINT_B_X, defaultConfig.pointB.x),
                y = prefs.getFloat(KEY_POINT_B_Y, defaultConfig.pointB.y)
            )
        )
    }

    fun saveGestureConfig(config: GestureConfig) {
        prefs.edit()
            .putInt(KEY_START_DELAY, config.startDelaySeconds)
            .putInt(KEY_LOOP_COUNT, config.loopCount)
            .putLong(KEY_SWIPE_DURATION, config.swipeDurationMs)
            .putLong(KEY_PAUSE_INTERVAL, config.pauseIntervalMs)
            .putFloat(KEY_POINT_A_X, config.pointA.x)
            .putFloat(KEY_POINT_A_Y, config.pointA.y)
            .putFloat(KEY_POINT_B_X, config.pointB.x)
            .putFloat(KEY_POINT_B_Y, config.pointB.y)
            .apply()
    }
}
