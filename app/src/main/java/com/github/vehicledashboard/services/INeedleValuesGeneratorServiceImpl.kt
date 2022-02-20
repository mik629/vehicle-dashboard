package com.github.vehicledashboard.services

import android.util.Log
import com.github.vehicledashboard.INeedleValuesGeneratorService
import com.github.vehicledashboard.NeedleValue
import com.github.vehicledashboard.domain.models.EngineMode
import com.github.vehicledashboard.domain.models.MeterType

class INeedleValuesGeneratorServiceImpl : INeedleValuesGeneratorService.Stub() {
    override fun generateNextValues(
        meterType: String,
        engineMode: String
    ): List<NeedleValue> {
        try {
            return when (EngineMode.valueOf(engineMode)) {
                EngineMode.START -> {
                    require(MeterType.valueOf(meterType) == MeterType.TACHOMETER)
                    listOf(
                        generateNextValue(
                            meterType = meterType,
                            startValue = TACHOMETER_ZERO,
                            step = TACHOMETER_STEP,
                            gearStart = 0,
                            gearEnd = TACHOMETER_START_POSITION,
                            animationDuration = ANIMATION_DURATION_UP,
                            endDelay = 0L
                        )
                    )
                }
                EngineMode.STOP -> {
                    listOf(
                        when (MeterType.valueOf(meterType)) {
                            MeterType.SPEEDOMETER ->
                                generateNextValue(
                                    meterType = meterType,
                                    startValue = SPEEDOMETER_START_VALUE,
                                    step = -SPEEDOMETER_STEP,
                                    gearStart = 0,
                                    gearEnd = 0,
                                    animationDuration = FULL_STOP_DURATION,
                                    endDelay = 0L
                                )
                            MeterType.TACHOMETER ->
                                generateNextValue(
                                    meterType = meterType,
                                    startValue = TACHOMETER_ZERO,
                                    step = -TACHOMETER_STEP,
                                    gearStart = 0,
                                    gearEnd = 0,
                                    animationDuration = ANIMATION_DURATION_DOWN,
                                    endDelay = 0L
                                )
                            else -> throw UnsupportedOperationException()
                        }
                    )
                }
                EngineMode.GO -> {
                    when (MeterType.valueOf(meterType)) {
                        MeterType.SPEEDOMETER -> generateSpeedometerValues(meterType)
                        MeterType.TACHOMETER -> generateTachometerValues(meterType)
                        MeterType.UNKNOWN -> throw UnsupportedOperationException()
                    }
                }
                EngineMode.BREAK -> {
                    listOf(
                        when (MeterType.valueOf(meterType)) {
                            MeterType.SPEEDOMETER -> generateNextValue(
                                meterType = meterType,
                                startValue = SPEEDOMETER_START_VALUE,
                                step = -SPEEDOMETER_STEP,
                                gearStart = 0,
                                gearEnd = 0,
                                animationDuration = FULL_STOP_DURATION,
                                endDelay = 0L
                            )
                            MeterType.TACHOMETER -> generateNextValue(
                                meterType = meterType,
                                startValue = TACHOMETER_START_VALUE,
                                step = -TACHOMETER_STEP,
                                gearStart = 0,
                                gearEnd = 0,
                                animationDuration = ANIMATION_DURATION_DOWN,
                                endDelay = 0L
                            )
                            MeterType.UNKNOWN -> throw UnsupportedOperationException()
                        }
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e("NeedleValues", "Needle values generation for $meterType failed", t)
            return listOf()
        }
    }

    private fun generateTachometerValues(meterType: String): List<NeedleValue> {
        require(MeterType.valueOf(meterType) == MeterType.TACHOMETER)

        val downDelay = 2500L
        val upDelay = 2000L
        val firstGearStart = 1
        val firstGearEnd = 24
        val gearStart = 1
        val gearEnd = 18
        val tachometerValues = mutableListOf<NeedleValue>()
        var nextValue = generateNextValue(
            startValue = TACHOMETER_START_VALUE,
            step = TACHOMETER_STEP,
            gearStart = firstGearStart,
            gearEnd = firstGearEnd,
            animationDuration = ANIMATION_DURATION_UP,
            endDelay = downDelay,
            meterType = meterType
        ).also { value ->
            tachometerValues.add(value)
        }
        nextValue = generateNextValue(
            startValue = nextValue.value,
            step = -TACHOMETER_STEP,
            gearStart = gearStart,
            gearEnd = gearEnd,
            animationDuration = ANIMATION_DURATION_DOWN,
            endDelay = downDelay,
            meterType = meterType
        ).also { value ->
            tachometerValues.add(value)
        }
        nextValue = generateNextValue(
            startValue = nextValue.value,
            step = TACHOMETER_STEP,
            gearStart = gearStart,
            gearEnd = gearEnd,
            animationDuration = ANIMATION_DURATION_UP,
            endDelay = upDelay,
            meterType = meterType
        ).also { value ->
            tachometerValues.add(value)
        }
        nextValue = generateNextValue(
            startValue = nextValue.value,
            step = -TACHOMETER_STEP,
            gearStart = gearStart,
            gearEnd = gearEnd,
            animationDuration = ANIMATION_DURATION_DOWN,
            endDelay = downDelay,
            meterType = meterType
        ).also { value ->
            tachometerValues.add(value)
        }
        generateNextValue(
            startValue = nextValue.value,
            step = TACHOMETER_STEP,
            gearStart = gearStart,
            gearEnd = gearEnd,
            animationDuration = ANIMATION_DURATION_UP + 1000,
            endDelay = upDelay,
            meterType = meterType
        ).also { value ->
            tachometerValues.add(value)
        }
        return tachometerValues
    }

    private fun generateSpeedometerValues(meterType: String): List<NeedleValue> {
        require(MeterType.valueOf(meterType) == MeterType.SPEEDOMETER)

        val gearSwitchDelay = 5000L
        val duration = 2500L
        val gearStart = 0
        val gearEnd = 16
        val speedometerValues = mutableListOf<NeedleValue>()
        var nextValue = generateNextValue(
            startValue = SPEEDOMETER_START_VALUE,
            step = SPEEDOMETER_STEP,
            gearStart = gearStart,
            gearEnd = gearEnd,
            animationDuration = duration,
            endDelay = gearSwitchDelay,
            meterType = meterType
        ).also { value ->
            speedometerValues.add(value)
        }
        nextValue = generateNextValue(
            startValue = nextValue.value,
            step = SPEEDOMETER_STEP,
            gearStart = gearStart,
            gearEnd = gearEnd,
            animationDuration = duration,
            endDelay = gearSwitchDelay,
            meterType = meterType
        ).also { value ->
            speedometerValues.add(value)
        }
        generateNextValue(
            startValue = nextValue.value,
            step = SPEEDOMETER_STEP,
            gearStart = gearStart,
            gearEnd = gearEnd,
            animationDuration = duration,
            endDelay = gearSwitchDelay,
            meterType = meterType
        ).also { value ->
            speedometerValues.add(value)
        }
        return speedometerValues
    }

    private fun generateNextValue(
        meterType: String,
        startValue: Float,
        step: Float,
        gearStart: Int,
        gearEnd: Int,
        animationDuration: Long,
        endDelay: Long,
    ) =
        NeedleValue(
            value = startValue + (gearEnd - gearStart) * step,
            animationDuration = animationDuration,
            endDelay = endDelay,
            meterType = MeterType.valueOf(meterType)
        )

    companion object {
        private const val ANIMATION_DURATION_UP = 1000L
        private const val ANIMATION_DURATION_DOWN = 1200L
        private const val FULL_STOP_DURATION = 2400L

        private const val SPEEDOMETER_START_VALUE = 0f
        private const val SPEEDOMETER_STEP = 5f

        private const val TACHOMETER_ZERO = 0f
        private const val TACHOMETER_START_POSITION = 4
        private const val TACHOMETER_START_VALUE = 0.8f
        private const val TACHOMETER_STEP = 0.2f
    }
}