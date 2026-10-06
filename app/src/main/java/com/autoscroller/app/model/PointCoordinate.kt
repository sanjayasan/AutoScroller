package com.autoscroller.app.model

data class PointCoordinate(
    val x: Float,
    val y: Float
) {
    override fun toString(): String = "(${x.toInt()}, ${y.toInt()})"
}
