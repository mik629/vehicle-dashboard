package com.github.vehicledashboard.presentation.ui.dashboard

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.marginBottom
import com.github.vehicledashboard.domain.getHalf
import com.github.vehicledashboard.domain.models.MeterType
import kotlin.math.max
import kotlin.math.min

class CustomViewGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val tachometer: View?
        get() = getChildAt(0)

    private val startButton: View?
        get() = getChildAt(1)

    private val speedometer: View?
        get() = getChildAt(2)

    private val goButton: View?
        get() = getChildAt(3)

    private val closeButton: View?
        get() = getChildAt(4)

    private var screenOrientation: Int = Configuration.ORIENTATION_PORTRAIT

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig?.let { config ->
            screenOrientation = config.orientation
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val tachometerWidth = tachometer?.measuredWidth ?: 0
        val tachometerHeight = tachometer?.measuredHeight ?: 0
        tachometer?.layout(
            0,
            0,
            tachometerWidth,
            tachometerHeight
        )
        layoutButton(
            meterViewWidth = tachometerWidth,
            meterViewHeight = tachometerHeight,
            startButton,
            MeterType.TACHOMETER,
            meterViewPaddingBottom = tachometer?.paddingBottom ?: 0,
            r = r
        )

        val speedometerWidth = speedometer?.measuredWidth ?: 0
        val speedometerHeight = speedometer?.measuredHeight ?: 0
        speedometer?.layout(
            0,
            0,
            speedometerWidth,
            speedometerHeight
        )
        layoutButton(
            meterViewWidth = speedometerWidth,
            meterViewHeight = speedometerHeight,
            goButton,
            MeterType.SPEEDOMETER,
            meterViewPaddingBottom = (speedometer?.paddingBottom ?: 0),
            r = r,
            paddingRight = if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                0
            } else {
                (speedometer?.paddingRight ?: 0)
            }
        )
        closeButton?.layout(
            r - (closeButton?.measuredWidth ?: 0),
            0,
            r,
            closeButton?.measuredHeight ?: 0
        )
    }

    private fun layoutButton(
        meterViewWidth: Int,
        meterViewHeight: Int,
        button: View?,
        meterType: MeterType,
        meterViewPaddingBottom: Int,
        r: Int,
        paddingRight: Int = 0
    ) {
        val smallestDimension = min(meterViewWidth, meterViewHeight)
        val biggestDimension = max(meterViewWidth, meterViewHeight)
        val buttonHeight = button?.measuredHeight ?: 0
        val buttonHalfWidth = getHalf(button?.measuredWidth ?: 0)
        val middle = getHalf(smallestDimension)
        val buttonMarginBottom = (button?.marginBottom ?: 0) + meterViewPaddingBottom
        when (meterType) {
            MeterType.SPEEDOMETER -> {
                val startY = if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    biggestDimension
                } else {
                    smallestDimension
                }
                button?.layout(
                    r - middle - buttonHalfWidth - paddingRight,
                    startY - buttonHeight - buttonMarginBottom,
                    r - middle + buttonHalfWidth - paddingRight,
                    startY - buttonMarginBottom
                )
            }
            MeterType.TACHOMETER -> {
                button?.layout(
                    middle - buttonHalfWidth,
                    smallestDimension - buttonHeight - buttonMarginBottom,
                    middle + buttonHalfWidth,
                    smallestDimension - buttonMarginBottom
                )
            }
            else -> throw UnsupportedOperationException()
        }
    }
}