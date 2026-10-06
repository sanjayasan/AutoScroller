package com.autoscroller.app.model

data class GestureConfig(
    var startDelaySeconds: Int = 3,
    var loopCount: Int = 20,
    var swipeDurationMs: Long = 350L,
    var pauseIntervalMs: Long = 1500L,
    var pointA: PointCoordinate = PointCoordinate(540f, 1600f),
    var pointB: PointCoordinate = PointCoordinate(540f, 600f)
)
