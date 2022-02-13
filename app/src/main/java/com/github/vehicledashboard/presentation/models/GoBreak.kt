package com.github.vehicledashboard.presentation.models

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.github.vehicledashboard.R

enum class GoBreak(@StringRes val stringId: Int, @ColorRes val colorId: Int) {
    GO(R.string.go, R.color.selector_blue_grey),
    BREAK(R.string.break_, R.color.black)
}

fun vehicleOppositeState(go: Boolean): GoBreak =
    if (go) {
        GoBreak.BREAK
    } else {
        GoBreak.GO
    }