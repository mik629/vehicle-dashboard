package com.github.vehicledashboard.presentation.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BarLabel(
    val label: String,
    val rotationAngle: Float
) : Parcelable
