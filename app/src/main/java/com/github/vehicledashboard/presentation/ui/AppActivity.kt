package com.github.vehicledashboard.presentation.ui

import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
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
            dashboardViewModel.tachometerValues
        )
        subscribeMeterViewToValuesStream(
            binding.speedometer,
            dashboardViewModel.speedometerValues
        )

        lifecycleScope.launch {
            dashboardViewModel.isEngineStarted
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { isEngineStarted ->
                    binding.goBreak.apply {
                        isEnabled = isEngineStarted
                        text = getString(R.string.go)
                    }
                }
        }

        binding.startStop.setOnClickListener {
            val start = binding.startStop.text == getString(R.string.start)
            val engineOppositeState = engineOppositeState(start)
            dashboardViewModel.onEngineStartStopClick(start)
            fillButton(
                binding.startStop,
                getString(engineOppositeState.stringId),
                engineOppositeState.colorId
            )
        }
        binding.goBreak.setOnClickListener {
            val go = binding.goBreak.text == getString(R.string.go)
            dashboardViewModel.onGoBreakClick(go)
            val vehicleOppositeState = vehicleOppositeState(go)
            fillButton(
                binding.goBreak,
                getString(vehicleOppositeState.stringId),
                vehicleOppositeState.colorId
            )
        }
    }

    private fun fillButton(button: Button, buttonText: String, @ColorRes colorId: Int) {
        button.apply {
            text = buttonText
            setTextColor(
                ContextCompat.getColor(
                    context,
                    colorId
                )
            )
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
                    val movedRight = event.x - firstTouchX > 0
                    switchVisibility(movedRight)
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
            this.elevation = getElevation(isVisible)
        }
        binding.speedometer.apply {
            this.isEnabled = !isVisible
            this.elevation = getElevation(!isVisible)
        }
    }

    private fun getElevation(isVisible: Boolean) =
        if (isVisible) {
            ELEVATION_DEFAULT
        } else {
            ELEVATION_ZERO
        }

    private fun subscribeMeterViewToValuesStream(
        meterView: MeterView,
        valuesStream: Flow<Pair<Float, Long>>,
        animationStartDelay: Long = 0L
    ) {
        lifecycleScope.launch {
            valuesStream.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { nextValue ->
                    meterView.setNeedleValue(
                        progress = nextValue.first,
                        duration = nextValue.second,
                        startDelay = animationStartDelay
                    )
                }
        }
    }

    companion object {
        private const val ELEVATION_ZERO = 0f
        private const val ELEVATION_DEFAULT = 2f

        private const val COUNT_TWO_FINGERS = 2
    }
}