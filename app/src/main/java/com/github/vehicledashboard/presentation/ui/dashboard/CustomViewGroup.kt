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
        val tachometerWidth = getOrDefault(value = tachometer?.measuredWidth, defaultValue = 0)
        val tachometerHeight = getOrDefault(value = tachometer?.measuredHeight, defaultValue = 0)
        tachometer?.layout(
            0,
            0,
            tachometerWidth,
            tachometerHeight
        )
        layoutButton(
            meterViewWidth = tachometerWidth,
            meterViewHeight = tachometerHeight,
            button = startButton,
            meterType = MeterType.TACHOMETER,
            meterViewPaddingBottom = getOrDefault(
                value = tachometer?.paddingBottom,
                defaultValue = 0
            ),
            r = r
        )

        val speedometerWidth = getOrDefault(value = speedometer?.measuredWidth, defaultValue = 0)
        val speedometerHeight = getOrDefault(value = speedometer?.measuredHeight, defaultValue = 0)
        speedometer?.layout(
            0,
            0,
            speedometerWidth,
            speedometerHeight
        )
        layoutButton(
            meterViewWidth = speedometerWidth,
            meterViewHeight = speedometerHeight,
            button = goButton,
            meterType = MeterType.SPEEDOMETER,
            meterViewPaddingBottom = getOrDefault(
                value = speedometer?.paddingBottom,
                defaultValue = 0
            ),
            r = r,
            paddingRight = if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                0
            } else {
                getOrDefault(value = speedometer?.paddingRight, defaultValue = 0)
            }
        )
        closeButton?.layout(
            r - getOrDefault(value = closeButton?.measuredWidth, defaultValue = 0),
            0,
            r,
            getOrDefault(value = closeButton?.measuredHeight, defaultValue = 0)
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
        val buttonHeight = getOrDefault(value = button?.measuredHeight, defaultValue = 0)
        val buttonHalfWidth = getHalf(getOrDefault(value = button?.measuredWidth, defaultValue = 0))
        val middle = getHalf(smallestDimension)
        val buttonMarginBottom =
            getOrDefault(value = button?.marginBottom, defaultValue = 0) + meterViewPaddingBottom
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

    private fun getOrDefault(value: Int?, defaultValue: Int): Int =
        value ?: defaultValue
}