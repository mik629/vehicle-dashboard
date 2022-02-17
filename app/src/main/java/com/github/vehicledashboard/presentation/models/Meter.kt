package com.github.vehicledashboard.presentation.models

import android.graphics.RectF

class Meter(
    val borderBox: RectF,
    val needleCircleBox: RectF,
    val ticks: FloatArray,
    val barLabels: List<BarLabel>
)