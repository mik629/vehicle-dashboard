package com.github.vehicledashboard.presentation.models

enum class MeterType(val id: Int) {
    SPEEDOMETER(0),
    TACHOMETER(1),
    UNKNOWN(-1)
}

fun fromId(id: Int): MeterType =
    when (id) {
        MeterType.SPEEDOMETER.id -> MeterType.SPEEDOMETER
        MeterType.TACHOMETER.id -> MeterType.TACHOMETER
        else -> MeterType.UNKNOWN
    }