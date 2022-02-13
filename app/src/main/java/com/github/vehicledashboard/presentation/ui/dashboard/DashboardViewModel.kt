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
    private val _tachometerValues: MutableSharedFlow<Float> =
        MutableStateFlow(TACHOMETER_START_VALUE)
    val tachometerValues: Flow<Float> = _tachometerValues

    private val _speedometerValues: MutableSharedFlow<Float> =
        MutableStateFlow(SPEEDOMETER_START_VALUE)
    val speedometerValues: Flow<Float> = _speedometerValues

    init {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val delayCoefficient = 10L
                val stepDelay = 50L
                val endDelay = 100L
                val firstGearUpRange = 1..21
                val gearUpRange = 16..32
                val gearDownRange = 1..16
                var nextValue = generateNextValues(
                    _tachometerValues,
                    startValue = TACHOMETER_START_VALUE,
                    step = TACHOMETER_STEP,
                    firstGearUpRange,
                    stepDelay = { i -> i * delayCoefficient },
                    endDelay = endDelay
                )
                nextValue = generateNextValues(
                    _tachometerValues,
                    startValue = nextValue,
                    step = -TACHOMETER_STEP,
                    gearDownRange,
                    stepDelay = { stepDelay },
                    endDelay = endDelay
                )
                nextValue = generateNextValues(
                    _tachometerValues,
                    startValue = nextValue,
                    step = TACHOMETER_STEP,
                    gearUpRange,
                    stepDelay = { i -> i * delayCoefficient },
                    endDelay = endDelay
                )
                nextValue = generateNextValues(
                    _tachometerValues,
                    startValue = nextValue,
                    step = -TACHOMETER_STEP,
                    gearDownRange,
                    stepDelay = { stepDelay },
                    endDelay = endDelay
                )
                generateNextValues(
                    _tachometerValues,
                    startValue = nextValue,
                    step = TACHOMETER_STEP,
                    gearUpRange,
                    stepDelay = { i -> i * delayCoefficient },
                    endDelay = endDelay
                )
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val stepDelay = 200L
                val gearSwitchDelay = 1500L
                var nextValue = generateNextValues(
                    _speedometerValues,
                    startValue = SPEEDOMETER_START_VALUE,
                    step = SPEEDOMETER_STEP,
                    1..12,
                    stepDelay = { stepDelay },
                    endDelay = gearSwitchDelay
                )
                nextValue = generateNextValues(
                    _speedometerValues,
                    startValue = nextValue,
                    step = SPEEDOMETER_STEP,
                    13..30,
                    stepDelay = { stepDelay },
                    endDelay = gearSwitchDelay
                )
                generateNextValues(
                    _speedometerValues,
                    startValue = nextValue,
                    step = SPEEDOMETER_STEP,
                    31..48,
                    stepDelay = { stepDelay },
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
            flow: MutableSharedFlow<Float>,
            startValue: Float,
            step: Float,
            range: IntRange,
            stepDelay: (Int) -> Long,
            endDelay: Long
        ): Float {
            var nextValue = startValue
            for (i in range) {
                nextValue += step
                flow.emit(nextValue)
                delay(stepDelay(i))
            }
            delay(endDelay)
            return nextValue
        }
    }
}