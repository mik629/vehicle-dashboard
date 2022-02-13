package com.github.vehicledashboard.presentation.models

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.github.vehicledashboard.R

enum class StartStop(@StringRes val stringId: Int, @ColorRes val colorId: Int) {
    START(R.string.start, R.color.green_500),
    STOP(R.string.stop, R.color.red_500);
}

fun engineOppositeState(start: Boolean): StartStop =
    if (start) {
        StartStop.STOP
    } else {
        StartStop.START
    }