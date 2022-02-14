package com.github.vehicledashboard.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.vehicledashboard.domain.PI_IN_DEGREES
import com.github.vehicledashboard.domain.calcMajorStepAngle
import com.github.vehicledashboard.domain.cosInRadians
import com.github.vehicledashboard.domain.getHalf
import com.github.vehicledashboard.domain.sinInRadians
import com.github.vehicledashboard.presentation.models.BarLabel
import com.github.vehicledashboard.presentation.models.MeterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel : ViewModel() {
    private val _isEngineStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEngineStarted: StateFlow<Boolean> =
        _isEngineStarted

    private val _speedometerTicks: MutableStateFlow<FloatArray> = MutableStateFlow(floatArrayOf())
    val speedometerTicks: StateFlow<FloatArray> =
        _speedometerTicks

    private val _tachometerTicks: MutableStateFlow<FloatArray> = MutableStateFlow(floatArrayOf())
    val tachometerTicks: StateFlow<FloatArray> =
        _tachometerTicks

    private val _speedometerBarLabels: MutableStateFlow<List<BarLabel>> = MutableStateFlow(listOf())
    val speedometerBarLabels: StateFlow<List<BarLabel>> =
        _speedometerBarLabels

    private val _tachometerBarLabels: MutableStateFlow<List<BarLabel>> = MutableStateFlow(listOf())
    val tachometerBarLabels: StateFlow<List<BarLabel>> =
        _tachometerBarLabels

    private val _tachometerValues: MutableSharedFlow<Pair<Float, Long>> =
        MutableStateFlow(TACHOMETER_ZERO to 0)
    val tachometerValues: Flow<Pair<Float, Long>> = _tachometerValues

    private val _speedometerValues: MutableSharedFlow<Pair<Float, Long>> =
        MutableStateFlow(SPEEDOMETER_START_VALUE to 0)
    val speedometerValues: Flow<Pair<Float, Long>> = _speedometerValues

    private var tachometerJob: Job? = null
    private var speedometerJob: Job? = null
    private var speedometerTicksJob: Job? = null
    private var tachometerTicksJob: Job? = null
    private var speedometerBarLabelsJob: Job? = null
    private var tachometerBarLabelsJob: Job? = null

    fun onEngineStartStopClick(start: Boolean) {
        if (start) {
            onEngineStartClick()
        } else {
            onEngineStopClick()
        }
    }

    private fun onEngineStartClick() {
        tachometerJob?.cancel()
        tachometerJob = viewModelScope.launch {
            withContext(Dispatchers.Default) {
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
    }

    private fun onEngineStopClick() {
        tachometerJob?.cancel()
        tachometerJob = viewModelScope.launch {
            withContext(Dispatchers.Default) {
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

    private fun onGoClick() {
        tachometerJob?.cancel()
        tachometerJob = viewModelScope.launch {
            withContext(Dispatchers.Default) {
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
        }

        speedometerJob?.cancel()
        speedometerJob = viewModelScope.launch {
            withContext(Dispatchers.Default) {
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
    }

    private fun onBreakClick() {
        stopVehicle()
        tachometerJob?.cancel()
        tachometerJob = viewModelScope.launch {
            withContext(Dispatchers.Default) {
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
    }

    private fun stopVehicle() {
        speedometerJob?.cancel()
        speedometerJob = viewModelScope.launch {
            withContext(Dispatchers.Default) {
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
    }

    fun buildTicks(
        viewWidth: Float,
        centerX: Float,
        centerY: Float,
        majorTickLength: Float,
        majorTickStep: Float,
        minorTickStep: Float,
        barMaxValue: Float,
        meterType: MeterType
    ) {
        when (meterType) {
            MeterType.SPEEDOMETER -> {
                speedometerTicksJob?.cancel()
                speedometerTicksJob = viewModelScope.launch(Dispatchers.Default) {
                    buildTicks(
                        _speedometerTicks,
                        viewWidth = viewWidth,
                        majorTickLength = majorTickLength,
                        majorTickStep = majorTickStep,
                        barMaxValue = barMaxValue,
                        minorTickStep = minorTickStep,
                        centerX = centerX,
                        centerY = centerY
                    )
                }
            }
            MeterType.TACHOMETER -> {
                tachometerTicksJob?.cancel()
                tachometerTicksJob = viewModelScope.launch(Dispatchers.Default) {
                    buildTicks(
                        _tachometerTicks,
                        viewWidth = viewWidth,
                        majorTickLength = majorTickLength,
                        majorTickStep = majorTickStep,
                        barMaxValue = barMaxValue,
                        minorTickStep = minorTickStep,
                        centerX = centerX,
                        centerY = centerY
                    )
                }
            }
            MeterType.UNKNOWN -> throw UnsupportedOperationException()
        }
    }

    private suspend fun buildTicks(
        flow: MutableStateFlow<FloatArray>,
        viewWidth: Float,
        majorTickLength: Float,
        majorTickStep: Float,
        barMaxValue: Float,
        minorTickStep: Float,
        centerX: Float,
        centerY: Float
    ) {
        val radius = viewWidth * MeterView.TICKS_RADIUS_COEFFICIENT
        var curProgress = 0f

        var currentAngle = MeterView.BAR_START_ANGLE
        val endAngle = MeterView.ARC_END_ANGLE + currentAngle

        val minorTickLength = getHalf(majorTickLength)
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
        flow.emit(lines.toFloatArray())
    }

    fun buildBarLabels(
        majorTickStep: Float,
        barMaxValue: Float,
        meterType: MeterType
    ) {
        when (meterType) {
            MeterType.SPEEDOMETER -> {
                speedometerBarLabelsJob?.cancel()
                speedometerBarLabelsJob = viewModelScope.launch(Dispatchers.Default) {
                    buildBarLabels(
                        _speedometerBarLabels,
                        majorTickStep = majorTickStep,
                        barMaxValue = barMaxValue
                    )
                }
            }
            MeterType.TACHOMETER -> {
                tachometerBarLabelsJob?.cancel()
                tachometerBarLabelsJob = viewModelScope.launch(Dispatchers.Default) {
                    buildBarLabels(
                        _tachometerBarLabels,
                        majorTickStep = majorTickStep,
                        barMaxValue = barMaxValue
                    )
                }
            }
            MeterType.UNKNOWN -> throw UnsupportedOperationException()
        }
    }

    private suspend fun buildBarLabels(
        flow: MutableStateFlow<List<BarLabel>>,
        majorTickStep: Float,
        barMaxValue: Float,
    ) {
        val majorStepAngle =
            calcMajorStepAngle(
                step = majorTickStep,
                angle = MeterView.ARC_END_ANGLE,
                maxVal = barMaxValue
            )

        var curProgress = 0f
        var currentAngle = MeterView.BAR_START_ANGLE
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
        flow.emit(barLabels)
    }

    private fun getBarLabel(progress: Float, majorTickStep: Float): String =
        if (progress % majorTickStep == 0f) {
            BAR_DIGIT_FORMAT.format(progress)
        } else {
            ""
        }

    companion object {
        private const val SPEEDOMETER_START_VALUE = 0f
        private const val SPEEDOMETER_STEP = 5f

        private const val TACHOMETER_ZERO = 0f
        private const val TACHOMETER_START_VALUE = 0.8f
        private const val TACHOMETER_STEP = 0.2f

        private const val BAR_DIGIT_FORMAT = "%.0f"

        suspend fun generateNextValues(
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
    }
}