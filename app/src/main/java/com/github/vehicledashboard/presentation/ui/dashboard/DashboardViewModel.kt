package com.github.vehicledashboard.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel : ViewModel() {
    private val _tachometerValues: MutableSharedFlow<Pair<Float, Long>> =
        MutableStateFlow(TACHOMETER_START_VALUE to 0)
    val tachometerValues: Flow<Pair<Float, Long>> = _tachometerValues

    private val _speedometerValues: MutableSharedFlow<Pair<Float, Long>> =
        MutableStateFlow(SPEEDOMETER_START_VALUE to 0)
    val speedometerValues: Flow<Pair<Float, Long>> = _speedometerValues

    init {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val downDelay = 2500L
                val upDelay = 2000L
                val durationUp = 1000L
                val durationDown = 1200L
                val firstGearRange = 1..24
                val gearRange = 1..16
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

        viewModelScope.launch {
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

    companion object {
        private const val SPEEDOMETER_START_VALUE = 0f
        private const val SPEEDOMETER_STEP = 5f

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