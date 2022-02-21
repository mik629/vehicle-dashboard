package com.github.vehicledashboard.presentation.models

import android.graphics.RectF
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Meter(
    val borderBox: RectF,
    val needleCircleBox: RectF,
    val ticks: FloatArray,
    val barLabels: List<BarLabel>,
    val barLabelX: Float,
    val barLabelY: Float,
) : Parcelable