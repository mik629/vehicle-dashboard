package com.github.vehicledashboard.domain

const val PI = kotlin.math.PI.toFloat()
const val PI_IN_DEGREES = 180

fun degreesToRadians(degrees: Float): Float =
    degrees / PI_IN_DEGREES * PI