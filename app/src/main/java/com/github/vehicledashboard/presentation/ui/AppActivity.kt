package com.github.vehicledashboard.presentation.ui

import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import com.github.vehicledashboard.databinding.AppActivityBinding

class AppActivity : AppCompatActivity() {

    private lateinit var binding: AppActivityBinding

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
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                firstTouchX = event.x
                false
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2) {
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
                ELEVATION_TWO
            } else {
                ELEVATION_ZERO
            }
        }
        binding.speedometer.apply {
            this.isEnabled = !isVisible
            this.elevation = if (!isVisible) {
                ELEVATION_TWO
            } else {
                ELEVATION_ZERO
            }
        }
    }

    companion object {
        private const val ELEVATION_ZERO = 0f
        private const val ELEVATION_TWO = 2f
    }
}