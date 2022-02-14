package com.github.vehicledashboard.domain

import kotlin.math.cos
import kotlin.math.sin

const val PI = kotlin.math.PI.toFloat()
const val PI_IN_DEGREES = 180

fun sinInRadians(currentAngle: Float): Float =
    sin(degreesToRadians(currentAngle))

fun cosInRadians(currentAngle: Float): Float =
    cos(degreesToRadians(PI_IN_DEGREES - currentAngle))

private fun degreesToRadians(degrees: Float): Float =
    degrees / PI_IN_DEGREES * PI

fun calcMajorStepAngle(step: Float, angle: Float, maxVal: Float): Float =
    step * angle / maxVal

fun inBetweenExclusive(value: Float, start: Float, end: Float): Boolean =
    value > start && value < end

fun getHalf(value: Float) =
    value / 2