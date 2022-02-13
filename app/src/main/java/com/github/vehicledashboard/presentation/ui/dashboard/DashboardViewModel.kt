package com.github.vehicledashboard.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _tachometerValues: MutableSharedFlow<Pair<Float, Long>> =
        MutableStateFlow(TACHOMETER_ZERO to 0)
    val tachometerValues: Flow<Pair<Float, Long>> = _tachometerValues

    private val _speedometerValues: MutableSharedFlow<Pair<Float, Long>> =
        MutableStateFlow(SPEEDOMETER_START_VALUE to 0)
    val speedometerValues: Flow<Pair<Float, Long>> = _speedometerValues

    private var tachometerJob: Job? = null
    private var speedometerJob: Job? = null

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

    companion object {
        private const val SPEEDOMETER_START_VALUE = 0f
        private const val SPEEDOMETER_STEP = 5f

        private const val TACHOMETER_ZERO = 0f
        private const val TACHOMETER_START_VALUE = 0.8f
        private const val TACHOMETER_STEP = 0.2f

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