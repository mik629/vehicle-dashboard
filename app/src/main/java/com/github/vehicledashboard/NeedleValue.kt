package com.github.vehicledashboard

import android.os.Parcelable
import com.github.vehicledashboard.domain.models.MeterType
import kotlinx.parcelize.Parcelize

@Parcelize
class NeedleValue(
    val value: Float,
    val animationDuration: Long,
    val endDelay: Long,
    val meterType: MeterType
) : Parcelable