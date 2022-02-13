package com.github.vehicledashboard.presentation.ui

import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.vehicledashboard.R
import com.github.vehicledashboard.databinding.AppActivityBinding
import com.github.vehicledashboard.presentation.models.engineOppositeState
import com.github.vehicledashboard.presentation.models.vehicleOppositeState
import com.github.vehicledashboard.presentation.ui.dashboard.DashboardViewModel
import com.github.vehicledashboard.presentation.ui.dashboard.MeterView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AppActivity : AppCompatActivity() {

    private lateinit var binding: AppActivityBinding
    private val dashboardViewModel: DashboardViewModel by viewModels()

    private var firstTouchX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = AppActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= 30) {
            binding.root.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            binding.root.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        switchVisibility(true)
        subscribeMeterViewToValuesStream(
            binding.tachometer,
            dashboardViewModel.tachometerValues,
            animationStartDelay = 0L
        )
        subscribeMeterViewToValuesStream(
            binding.speedometer,
            dashboardViewModel.speedometerValues,
            animationStartDelay = 0L
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.isEngineStarted.collect { isEngineStarted ->
                    binding.goBreak.apply {
                        isEnabled = isEngineStarted
                        text = getString(R.string.go)
                    }
                }
            }
        }

        binding.startStop.setOnClickListener {
            val start = binding.startStop.text == getString(R.string.start)
            val engineOppositeState = engineOppositeState(start)
            dashboardViewModel.onEngineStartStopClick(start)
            binding.startStop.apply {
                text = getString(engineOppositeState.stringId)
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        engineOppositeState.colorId
                    )
                )
            }
        }
        binding.goBreak.setOnClickListener {
            val go = binding.goBreak.text == getString(R.string.go)
            dashboardViewModel.onGoBreakClick(go)
            val vehicleOppositeState = vehicleOppositeState(go)
            binding.goBreak.apply {
                text = getString(vehicleOppositeState.stringId)
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        vehicleOppositeState.colorId
                    )
                )
            }

        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                firstTouchX = event.x
                false
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == COUNT_TWO_FINGERS) {
                    switchVisibility(event.x - firstTouchX > 0)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    private fun switchVisibility(isVisible: Boolean) {
        binding.tachometer.apply {
            this.isEnabled = isVisible
            this.elevation = if (isVisible) {
                ELEVATION_DEFAULT
            } else {
                ELEVATION_ZERO
            }
        }
        binding.speedometer.apply {
            this.isEnabled = !isVisible
            this.elevation = if (!isVisible) {
                ELEVATION_DEFAULT
            } else {
                ELEVATION_ZERO
            }
        }
    }

    private fun subscribeMeterViewToValuesStream(
        meterView: MeterView,
        valuesStream: Flow<Pair<Float, Long>>,
        animationStartDelay: Long
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                valuesStream.collect { nextValue ->
                    meterView.setNeedleValue(
                        progress = nextValue.first,
                        duration = nextValue.second,
                        startDelay = animationStartDelay
                    )
                }
            }
        }
    }

    companion object {
        private const val ELEVATION_ZERO = 0f
        private const val ELEVATION_DEFAULT = 2f

        private const val COUNT_TWO_FINGERS = 2
    }
}