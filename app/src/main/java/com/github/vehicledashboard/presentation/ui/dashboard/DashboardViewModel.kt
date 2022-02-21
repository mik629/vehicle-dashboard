package com.github.vehicledashboard.presentation.ui.dashboard

import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.vehicledashboard.NeedleValue
import com.github.vehicledashboard.domain.PI_IN_DEGREES
import com.github.vehicledashboard.domain.calcMajorStepAngle
import com.github.vehicledashboard.domain.cosInRadians
import com.github.vehicledashboard.domain.getHalf
import com.github.vehicledashboard.domain.models.EngineMode
import com.github.vehicledashboard.domain.models.MeterType
import com.github.vehicledashboard.domain.sinInRadians
import com.github.vehicledashboard.presentation.models.BarLabel
import com.github.vehicledashboard.presentation.models.GoBreak
import com.github.vehicledashboard.presentation.models.Meter
import com.github.vehicledashboard.presentation.models.Needle
import com.github.vehicledashboard.presentation.models.StartStop
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

    private val _startStopOppositeState = MutableStateFlow(StartStop.START)
    val startStopOppositeState: StateFlow<StartStop> =
        _startStopOppositeState

    private val _goBreakOppositeState = MutableStateFlow(GoBreak.GO)
    val goBreakOppositeState: StateFlow<GoBreak> =
        _goBreakOppositeState

    private val _speedometer = MutableStateFlow<Meter?>(null)
    val speedometer: StateFlow<Meter?> =
        _speedometer
    private val _tachometer = MutableStateFlow<Meter?>(null)
    val tachometer: StateFlow<Meter?> =
        _tachometer

    private val _speedometerValues =
        MutableStateFlow<NeedleValue?>(null)
    val speedometerValues: Flow<NeedleValue?> =
        _speedometerValues
    private val _tachometerValues =
        MutableStateFlow<NeedleValue?>(null)
    val tachometerValues: Flow<NeedleValue?> =
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

    fun onEngineStartStopClick(
        start: Boolean,
        engineOppositeState: StartStop,
        needleValueGenerator: (meterType: String, engineMode: String) -> List<NeedleValue>
    ) {
        val engineMode = if (start) {
            EngineMode.START.name
        } else {
            EngineMode.STOP.name
        }
        tachometerValuesJob?.cancel()
        tachometerValuesJob = viewModelScope.launch(Dispatchers.IO) {
            _isEngineStarted.emit(start)
            _startStopOppositeState.value = engineOppositeState
            postNeedleValues(
                needleValueGenerator(
                    MeterType.TACHOMETER.name,
                    engineMode
                )
            )
        }
        if (!start) {
            speedometerValuesJob?.cancel()
            speedometerValuesJob = viewModelScope.launch(Dispatchers.IO) {
                _goBreakOppositeState.value = GoBreak.GO
                postNeedleValues(
                    needleValueGenerator(
                        MeterType.SPEEDOMETER.name,
                        engineMode
                    )
                )
            }
        }
    }

    fun onGoBreakClick(
        go: Boolean,
        vehicleOppositeState: GoBreak,
        needleValueGenerator: (meterType: String, engineMode: String) -> List<NeedleValue>
    ) {
        val engineMode = if (go) {
            EngineMode.GO.name
        } else {
            EngineMode.BREAK.name
        }
        speedometerValuesJob?.cancel()
        speedometerValuesJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                postNeedleValues(
                    needleValueGenerator(
                        MeterType.SPEEDOMETER.name,
                        engineMode
                    )
                )
            } catch (t: Throwable) {
                Log.e(
                    "NeedleValuesGenerator",
                    "Interaction with generator service failed",
                    t
                ) // fixme: better handling with ui notification
            }
        }

        tachometerValuesJob?.cancel()
        tachometerValuesJob = viewModelScope.launch(Dispatchers.IO) {
            _goBreakOppositeState.value = vehicleOppositeState
            try {
                postNeedleValues(
                    needleValueGenerator(
                        MeterType.TACHOMETER.name,
                        engineMode
                    )
                )
            } catch (t: Throwable) {
                Log.e(
                    "NeedleValuesGenerator",
                    "Interaction with generator service failed",
                    t
                ) // fixme: better handling with ui notification
            }
        }
    }

    private suspend fun postNeedleValues(needleValues: List<NeedleValue>) {
        when (needleValues.random().meterType) {
            MeterType.SPEEDOMETER -> {
                postNeedleValueWithDelay(needleValues, _speedometerValues)
            }
            MeterType.TACHOMETER -> {
                postNeedleValueWithDelay(needleValues, _tachometerValues)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    private suspend fun postNeedleValueWithDelay(
        needleValues: List<NeedleValue>,
        flow: MutableSharedFlow<NeedleValue?>
    ) {
        for (needleValue in needleValues) {
            flow.emit(needleValue)
            delay(needleValue.endDelay)
        }
    }

    fun buildMeter(
        meterType: MeterType,
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
            paddingBottom = paddingBottom
        )
        val needleCircleBox = buildBordersBox(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            paddingLeft = paddingLeft,
            paddingRight = paddingRight,
            paddingTop = paddingTop,
            paddingBottom = paddingBottom,
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
        factor: Float = FACTOR_FULL
    ): RectF {
        val smallest =
            min(
                canvasWidth - paddingLeft - paddingRight,
                canvasHeight - paddingTop - paddingBottom
            )
        return RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            smallest * factor + paddingRight,
            smallest * factor + paddingBottom
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

        private const val BAR_DIGIT_FORMAT = "%.0f"
        private const val BAR_START_ANGLE = -40f

        private const val TICKS_RADIUS_COEFFICIENT = 0.48f
    }
}