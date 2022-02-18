package com.github.vehicledashboard.presentation.ui.dashboard

import android.content.res.Configuration
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.vehicledashboard.domain.PI_IN_DEGREES
import com.github.vehicledashboard.domain.calcMajorStepAngle
import com.github.vehicledashboard.domain.cosInRadians
import com.github.vehicledashboard.domain.getHalf
import com.github.vehicledashboard.domain.sinInRadians
import com.github.vehicledashboard.presentation.models.BarLabel
import com.github.vehicledashboard.presentation.models.Meter
import com.github.vehicledashboard.presentation.models.MeterType
import com.github.vehicledashboard.presentation.models.Needle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

class DashboardViewModel : ViewModel() {
    private val _isEngineStarted = MutableStateFlow(false)
    val isEngineStarted: StateFlow<Boolean> =
        _isEngineStarted

    private val _speedometer = MutableStateFlow<Meter?>(null)
    val speedometer: StateFlow<Meter?> =
        _speedometer
    private val _tachometer = MutableStateFlow<Meter?>(null)
    val tachometer: StateFlow<Meter?> =
        _tachometer

    private val _speedometerValues =
        MutableStateFlow<Pair<Float, Long>>(SPEEDOMETER_START_VALUE to 0)
    val speedometerValues: Flow<Pair<Float, Long>> =
        _speedometerValues
    private val _tachometerValues =
        MutableStateFlow<Pair<Float, Long>>(TACHOMETER_ZERO to 0)
    val tachometerValues: Flow<Pair<Float, Long>> =
        _tachometerValues

    private val _speedometerNeedle = MutableStateFlow<Needle?>(null)
    val speedometerNeedle: Flow<Needle?> =
        _speedometerNeedle
    private val _tachometerNeedle = MutableStateFlow<Needle?>(null)
    val tachometerNeedle: Flow<Needle?> =
        _tachometerNeedle

    private var tachometerJob: Job? = null
    private var speedometerJob: Job? = null
    private var tachometerValuesJob: Job? = null
    private var speedometerValuesJob: Job? = null
    private var speedometerNeedleJob: Job? = null
    private var tachometerNeedleJob: Job? = null

    fun onEngineStartStopClick(start: Boolean) {
        if (start) {
            onEngineStartClick()
        } else {
            onEngineStopClick()
        }
    }

    private fun onEngineStartClick() {
        tachometerValuesJob?.cancel()
        tachometerValuesJob = viewModelScope.launch(Dispatchers.Default) {
            _isEngineStarted.emit(true)
            generateNextValues(
                _tachometerValues,
                startValue = TACHOMETER_ZERO,
                step = TACHOMETER_STEP,
                0..4,
                animationDuration = 1000L,
                endDelay = 0L
            )
        }
    }

    private fun onEngineStopClick() {
        tachometerValuesJob?.cancel()
        tachometerValuesJob = viewModelScope.launch(Dispatchers.Default) {
            _isEngineStarted.emit(false)
            generateNextValues(
                _tachometerValues,
                startValue = TACHOMETER_ZERO,
                step = -TACHOMETER_STEP,
                0..0,
                animationDuration = 1000L,
                endDelay = 0L
            )
        }
        stopVehicle()
    }

    fun onGoBreakClick(go: Boolean) {
        if (go) {
            onGoClick()
        } else {
            onBreakClick()
        }
    }

    // todo: move to a repo/service
    private fun onGoClick() {
        tachometerValuesJob?.cancel()
        tachometerValuesJob = viewModelScope.launch(Dispatchers.Default) {
            val downDelay = 2500L
            val upDelay = 2000L
            val durationUp = 1000L
            val durationDown = 1200L
            val firstGearRange = 1..24
            val gearRange = 1..18
            var nextValue = generateNextValues(
                _tachometerValues,
                startValue = TACHOMETER_START_VALUE,
                step = TACHOMETER_STEP,
                firstGearRange,
                animationDuration = durationUp,
                endDelay = downDelay
            )
            nextValue = generateNextValues(
                _tachometerValues,
                startValue = nextValue,
                step = -TACHOMETER_STEP,
                gearRange,
                animationDuration = durationDown,
                endDelay = downDelay
            )
            nextValue = generateNextValues(
                _tachometerValues,
                startValue = nextValue,
                step = TACHOMETER_STEP,
                gearRange,
                animationDuration = durationUp,
                endDelay = upDelay
            )
            nextValue = generateNextValues(
                _tachometerValues,
                startValue = nextValue,
                step = -TACHOMETER_STEP,
                gearRange,
                animationDuration = durationDown,
                endDelay = downDelay
            )
            generateNextValues(
                _tachometerValues,
                startValue = nextValue,
                step = TACHOMETER_STEP,
                gearRange,
                animationDuration = durationUp + 1000,
                endDelay = upDelay
            )
        }

        speedometerValuesJob?.cancel()
        speedometerValuesJob = viewModelScope.launch(Dispatchers.Default) {
            val gearSwitchDelay = 5000L
            val duration = 2500L
            val gearRange = 0..16
            var nextValue = generateNextValues(
                _speedometerValues,
                startValue = SPEEDOMETER_START_VALUE,
                step = SPEEDOMETER_STEP,
                gearRange,
                animationDuration = duration,
                endDelay = gearSwitchDelay
            )
            nextValue = generateNextValues(
                _speedometerValues,
                startValue = nextValue,
                step = SPEEDOMETER_STEP,
                gearRange,
                animationDuration = duration,
                endDelay = gearSwitchDelay
            )
            generateNextValues(
                _speedometerValues,
                startValue = nextValue,
                step = SPEEDOMETER_STEP,
                gearRange,
                animationDuration = duration,
                endDelay = gearSwitchDelay
            )
        }
    }

    private fun onBreakClick() {
        stopVehicle()
        tachometerValuesJob?.cancel()
        tachometerValuesJob = viewModelScope.launch(Dispatchers.Default) {
            generateNextValues(
                _tachometerValues,
                startValue = TACHOMETER_START_VALUE,
                step = -SPEEDOMETER_STEP,
                0..0,
                animationDuration = 2000L,
                endDelay = 0
            )
        }
    }

    private fun stopVehicle() {
        speedometerValuesJob?.cancel()
        speedometerValuesJob = viewModelScope.launch(Dispatchers.Default) {
            generateNextValues(
                _speedometerValues,
                startValue = SPEEDOMETER_START_VALUE,
                step = -SPEEDOMETER_STEP,
                0..0,
                animationDuration = 2000L,
                endDelay = 0
            )
        }
    }

    private suspend fun generateNextValues(
        flow: MutableSharedFlow<Pair<Float, Long>>,
        startValue: Float,
        step: Float,
        gearRange: IntRange,
        animationDuration: Long,
        endDelay: Long
    ): Float =
        (startValue + (gearRange.last - gearRange.first) * step)
            .also { nextValue ->
                flow.emit(nextValue to animationDuration)
                delay(endDelay)
            }

    fun buildMeter(
        meterType: MeterType,
        screenOrientation: Int,
        canvasWidth: Int,
        canvasHeight: Int,
        paddingLeft: Int,
        paddingRight: Int,
        paddingTop: Int,
        paddingBottom: Int,
        majorTickLength: Float,
        majorTickStep: Float,
        minorTickStep: Float,
        barValuePadding: Float,
        barMaxValue: Float
    ) {
        when (meterType) {
            MeterType.SPEEDOMETER -> {
                speedometerJob?.cancel()
                speedometerJob = viewModelScope.launch(Dispatchers.Default) {
                    _speedometer.emit(
                        doBuildMeter(
                            meterType = meterType,
                            screenOrientation = screenOrientation,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            paddingLeft = paddingLeft,
                            paddingRight = paddingRight,
                            paddingTop = paddingTop,
                            paddingBottom = paddingBottom,
                            majorTickLength = majorTickLength,
                            majorTickStep = majorTickStep,
                            minorTickStep = minorTickStep,
                            barValuePadding = barValuePadding,
                            barMaxValue = barMaxValue
                        )
                    )
                }
            }
            MeterType.TACHOMETER -> {
                tachometerJob?.cancel()
                tachometerJob = viewModelScope.launch(Dispatchers.Default) {
                    _tachometer.emit(
                        doBuildMeter(
                            meterType = meterType,
                            screenOrientation = screenOrientation,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            paddingLeft = paddingLeft,
                            paddingRight = paddingRight,
                            paddingTop = paddingTop,
                            paddingBottom = paddingBottom,
                            majorTickLength = majorTickLength,
                            majorTickStep = majorTickStep,
                            minorTickStep = minorTickStep,
                            barValuePadding = barValuePadding,
                            barMaxValue = barMaxValue
                        )
                    )
                }
            }
            else -> throw UnsupportedOperationException()
        }
    }

    private fun doBuildMeter(
        meterType: MeterType,
        screenOrientation: Int,
        canvasWidth: Int,
        canvasHeight: Int,
        paddingLeft: Int,
        paddingRight: Int,
        paddingTop: Int,
        paddingBottom: Int,
        majorTickLength: Float,
        majorTickStep: Float,
        minorTickStep: Float,
        barValuePadding: Float,
        barMaxValue: Float
    ): Meter {

        val borderBox = buildBordersBox(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            paddingLeft = paddingLeft,
            paddingRight = paddingRight,
            paddingTop = paddingTop,
            paddingBottom = paddingBottom,
            meterType = meterType,
            screenOrientation = screenOrientation
        )
        val needleCircleBox = buildBordersBox(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            paddingLeft = paddingLeft,
            paddingRight = paddingRight,
            paddingTop = paddingTop,
            paddingBottom = paddingBottom,
            meterType = meterType,
            screenOrientation = screenOrientation,
            factor = NEEDLE_CIRCLE_RADIUS_COEFFICIENT
        )
        val radius = borderBox.width() * TICKS_RADIUS_COEFFICIENT
        val centerX = borderBox.centerX()
        val centerY = borderBox.centerY()
        val minorTickLength = getHalf(majorTickLength)
        val ticks = buildTicks(
            radius = radius,
            centerX = centerX,
            centerY = centerY,
            minorTickLength = minorTickLength,
            majorTickStep = majorTickStep,
            minorTickStep = minorTickStep,
            barMaxValue = barMaxValue
        )
        val barLabels = buildBarLabels(
            majorTickStep = majorTickStep,
            barMaxValue = barMaxValue
        )
        return Meter(
            borderBox = borderBox,
            needleCircleBox = needleCircleBox,
            ticks = ticks,
            barLabels = barLabels,
            centerX + radius - minorTickLength - barValuePadding,
            centerY + barValuePadding
        )
    }

    private fun buildBordersBox(
        canvasWidth: Int,
        canvasHeight: Int,
        paddingLeft: Int,
        paddingRight: Int,
        paddingTop: Int,
        paddingBottom: Int,
        meterType: MeterType,
        screenOrientation: Int,
        factor: Float = FACTOR_FULL
    ): RectF {
        val smallest =
            min(
                canvasWidth - paddingLeft - paddingRight,
                canvasHeight - paddingTop - paddingBottom
            )
        val startX =
            if (meterType == MeterType.SPEEDOMETER && screenOrientation != Configuration.ORIENTATION_PORTRAIT) {
                smallest
            } else {
                0
            }
        val startY =
            if (meterType == MeterType.SPEEDOMETER && screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                smallest
            } else {
                0
            }
        return RectF(
            startX + paddingLeft.toFloat(),
            startY + paddingTop.toFloat(),
            startX + smallest * factor + paddingRight,
            startY + smallest * factor + paddingBottom
        )
    }

    private fun buildTicks(
        radius: Float,
        centerX: Float,
        centerY: Float,
        minorTickLength: Float,
        majorTickStep: Float,
        minorTickStep: Float,
        barMaxValue: Float
    ): FloatArray {
        var curProgress = 0f

        var currentAngle = BAR_START_ANGLE
        val endAngle = MeterView.ARC_END_ANGLE + currentAngle

        val majorStepAngle =
            calcMajorStepAngle(
                step = majorTickStep,
                angle = MeterView.ARC_END_ANGLE,
                maxVal = barMaxValue
            )
        val minorTicks = (majorTickStep / minorTickStep).toInt()
        val minorStepAngle = majorStepAngle / minorTicks
        val halfMinorStepAngle = getHalf(minorStepAngle)
        val barTickStartPosition = radius - minorTickLength
        val barTickEndPosition = radius + minorTickLength

        val lines = mutableListOf<Float>()
        while (currentAngle <= endAngle) {
            lines.add(centerX + cosInRadians(currentAngle) * barTickStartPosition)
            lines.add(centerY - sinInRadians(currentAngle) * barTickStartPosition)
            lines.add(centerX + cosInRadians(currentAngle) * barTickEndPosition)
            lines.add(centerY - sinInRadians(currentAngle) * barTickEndPosition)

            for (i in 1..minorTicks) {
                val angle = currentAngle + i * minorStepAngle
                if (angle >= endAngle + halfMinorStepAngle) {
                    break
                }
                val cosOfAngle = cosInRadians(angle)
                val sinOfAngle = sinInRadians(angle)
                lines.add(centerX + cosOfAngle * radius)
                lines.add(centerY - sinOfAngle * radius)
                lines.add(centerX + cosOfAngle * barTickEndPosition)
                lines.add(centerY - sinOfAngle * barTickEndPosition)
            }
            currentAngle += majorStepAngle
            curProgress += majorTickStep
        }
        return lines.toFloatArray()
    }

    private fun buildBarLabels(
        majorTickStep: Float,
        barMaxValue: Float,
    ): List<BarLabel> {
        val majorStepAngle =
            calcMajorStepAngle(
                step = majorTickStep,
                angle = MeterView.ARC_END_ANGLE,
                maxVal = barMaxValue
            )

        var curProgress = 0f
        var currentAngle = BAR_START_ANGLE
        val endAngle = MeterView.ARC_END_ANGLE + currentAngle
        val barLabels = mutableListOf<BarLabel>()
        while (currentAngle <= endAngle) {
            val barLabel = getBarLabel(progress = curProgress, majorTickStep = majorTickStep)
            if (barLabel.isNotBlank()) {
                barLabels.add(BarLabel(barLabel, PI_IN_DEGREES + currentAngle))
            }
            currentAngle += majorStepAngle
            curProgress += majorTickStep
        }
        return barLabels
    }

    private fun getBarLabel(progress: Float, majorTickStep: Float): String =
        if (progress % majorTickStep == 0f) {
            BAR_DIGIT_FORMAT.format(progress)
        } else {
            ""
        }

    fun buildNeedle(
        meterType: MeterType,
        viewWidth: Float,
        needleBaseCircleDiameter: Float,
        barMaxValue: Float,
        needleValue: Float,
        centerX: Float,
        centerY: Float
    ) {
        when (meterType) {
            MeterType.SPEEDOMETER -> {
                speedometerNeedleJob?.cancel()
                speedometerNeedleJob = viewModelScope.launch(Dispatchers.Default) {
                    buildNeedle(
                        _speedometerNeedle,
                        viewWidth = viewWidth,
                        circleDiameter = needleBaseCircleDiameter,
                        barMaxValue = barMaxValue,
                        needleValue = needleValue,
                        centerX = centerX,
                        centerY = centerY
                    )
                }
            }
            MeterType.TACHOMETER -> {
                tachometerNeedleJob?.cancel()
                tachometerNeedleJob = viewModelScope.launch(Dispatchers.Default) {
                    buildNeedle(
                        _tachometerNeedle,
                        viewWidth = viewWidth,
                        circleDiameter = needleBaseCircleDiameter,
                        barMaxValue = barMaxValue,
                        needleValue = needleValue,
                        centerX = centerX,
                        centerY = centerY
                    )
                }
            }
            MeterType.UNKNOWN -> throw UnsupportedOperationException()
        }
    }

    private suspend fun buildNeedle(
        flow: MutableSharedFlow<Needle?>,
        viewWidth: Float,
        circleDiameter: Float,
        barMaxValue: Float,
        needleValue: Float,
        centerX: Float,
        centerY: Float
    ) {
        val radius = viewWidth * NEEDLE_RADIUS_COEFFICIENT
        val majorStepAngle = MeterView.ARC_END_ANGLE / barMaxValue
        val angle = BAR_START_ANGLE + needleValue * majorStepAngle
        val circleRadius = getHalf(circleDiameter)
        val cosOfAngle = cosInRadians(angle)
        val sinOfAngle = sinInRadians(angle)
        flow.emit(
            Needle(
                startX = centerX + cosOfAngle * circleRadius,
                startY = centerY - sinOfAngle * circleRadius,
                stopX = centerX + cosOfAngle * radius,
                stopY = centerY - sinOfAngle * radius,
                circleRadius = circleRadius
            )
        )
    }

    companion object {
        private const val FACTOR_FULL = 1f

        private const val NEEDLE_CIRCLE_RADIUS_COEFFICIENT = 0.2f
        private const val NEEDLE_RADIUS_COEFFICIENT = 0.38f

        private const val SPEEDOMETER_START_VALUE = 0f
        private const val SPEEDOMETER_STEP = 5f

        private const val TACHOMETER_ZERO = 0f
        private const val TACHOMETER_START_VALUE = 0.8f
        private const val TACHOMETER_STEP = 0.2f

        private const val BAR_DIGIT_FORMAT = "%.0f"
        private const val BAR_START_ANGLE = -40f

        private const val TICKS_RADIUS_COEFFICIENT = 0.48f
    }
}