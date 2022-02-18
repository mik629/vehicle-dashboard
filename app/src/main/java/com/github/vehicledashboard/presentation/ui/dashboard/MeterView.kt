package com.github.vehicledashboard.presentation.ui.dashboard

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.vehicledashboard.R
import com.github.vehicledashboard.domain.inBetweenExclusive
import com.github.vehicledashboard.presentation.models.BarLabel
import com.github.vehicledashboard.presentation.models.Meter
import com.github.vehicledashboard.presentation.models.MeterType
import com.github.vehicledashboard.presentation.models.Needle
import com.github.vehicledashboard.presentation.models.fromId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt


class MeterView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private val dashboardViewModel: DashboardViewModel by lazy {
        ViewModelProvider(
            requireNotNull(findViewTreeViewModelStoreOwner())
        )[DashboardViewModel::class.java]
    }

    private val typeEvaluator =
        TypeEvaluator<Float> { fraction, startValue, endValue -> startValue + fraction * (endValue - startValue) }

    private var barMaxValue: Float = UNSPECIFIED
        set(value) {
            require(value > ZERO)
            field = value
        }

    private var barBackgroundColor: Int = 0
    private var barValuePadding = 0f

    private var majorTickStep = UNSPECIFIED
        set(value) {
            require(inBetweenExclusive(value = value, start = ZERO, end = barMaxValue))
            field = value
        }

    private var minorTickStep = UNSPECIFIED
        set(value) {
            require(inBetweenExclusive(value = value, start = ZERO, end = barMaxValue))
            field = value
        }

    private var meterType: MeterType = MeterType.UNKNOWN

    private var meter: Meter? = null
        set(value) {
            field = value
            invalidate()
        }

    private var lastNeedleValue: Float = ZERO
        set(value) {
            require(value >= ZERO)
            field = min(barMaxValue, value)
            invalidate()
        }

    private var needle: Needle? = null
        set(value) {
            field = value
            invalidate() // fixme: extra redraw
        }

    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.FILL
        }
    private val arcPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.STROKE
        }
    private val ticksPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.STROKE
        }
    private val txtPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    private val needlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.STROKE
        }

    private var screenOrientation: Int = Configuration.ORIENTATION_PORTRAIT

    init {
        val density = resources.displayMetrics.density
        val attributes = context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.MeterView,
            0, 0
        )
        try {
            barBackgroundColor = attributes.getColor(
                R.styleable.MeterView_barBackgroundColor,
                Color.WHITE // todo: replace with theme attr
            )
            backgroundPaint.color = barBackgroundColor
            val barColor = attributes.getColor(
                R.styleable.MeterView_barColor,
                Color.BLACK // todo: replace with theme attr
            )
            arcPaint.color = barColor
            txtPaint.color = barColor
            ticksPaint.color = barColor
            barMaxValue = attributes.getFloat(
                R.styleable.MeterView_barMaxValue,
                UNSPECIFIED
            )
            barValuePadding = attributes.getDimension(
                R.styleable.MeterView_barValuePadding,
                DEFAULT_PADDING_DP * density
            )
            majorTickStep = attributes.getFloat(
                R.styleable.MeterView_barMajorStep,
                UNSPECIFIED
            )
            minorTickStep = attributes.getFloat(
                R.styleable.MeterView_barMinorStep,
                UNSPECIFIED
            )
            val barValueTextSize = attributes.getDimensionPixelSize(
                R.styleable.MeterView_barValueTextSize,
                (DEFAULT_BAR_TEXT_SIZE_SP * density).roundToInt()
            )
            txtPaint.textSize = barValueTextSize.toFloat()
            needlePaint.color = attributes.getColor(
                R.styleable.MeterView_needleColor,
                Color.RED // todo: replace with theme attr
            )
            lastNeedleValue = attributes.getFloat(
                R.styleable.MeterView_needleStartValue,
                ZERO
            )
            meterType = fromId(
                attributes.getInt(
                    R.styleable.MeterView_meterType,
                    MeterType.UNKNOWN.id
                )
            )
        } finally {
            attributes.recycle()
        }

        if (!isInEditMode) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        // todo: maybe move to attrs
        arcPaint.strokeWidth = ARC_STROKE_WIDTH
        ticksPaint.strokeWidth = TICK_STROKE_WIDTH
        needlePaint.strokeWidth = NEEDLE_STROKE_WIDTH
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner())
        lifecycleOwner
            .lifecycleScope
            .launch {
                lifecycleOwner.lifecycle
                    .repeatOnLifecycle(Lifecycle.State.STARTED) {
                        launch {
                            chooseFlow(
                                speedometerFlow = dashboardViewModel.speedometer,
                                tachometerFlow = dashboardViewModel.tachometer
                            ).collect { meterData ->
                                meter = meterData
                            }
                        }
                        launch {
                            chooseFlow(
                                speedometerFlow = dashboardViewModel.speedometerValues,
                                tachometerFlow = dashboardViewModel.tachometerValues
                            ).collect { nextValue ->
                                setNeedleValue(
                                    progress = nextValue.first,
                                    duration = nextValue.second,
                                    startDelay = 0L
                                )
                            }
                        }
                        launch {
                            chooseFlow(
                                speedometerFlow = dashboardViewModel.speedometerNeedle,
                                tachometerFlow = dashboardViewModel.tachometerNeedle
                            ).collect { aNeedle ->
                                needle = aNeedle
                            }
                        }
                    }
            }
    }

    private fun <T> chooseFlow(speedometerFlow: Flow<T>, tachometerFlow: Flow<T>): Flow<T> =
        when (meterType) {
            MeterType.SPEEDOMETER -> speedometerFlow
            MeterType.TACHOMETER -> tachometerFlow
            MeterType.UNKNOWN -> throw UnsupportedOperationException()
        }

    // todo: optimize?
    private fun setNeedleValue(progress: Float, duration: Long, startDelay: Long) {
        require(progress >= 0)
        ValueAnimator.ofObject(
            typeEvaluator,
            lastNeedleValue,
            min(progress, barMaxValue)
        ).apply {
            this.duration = duration
            this.startDelay = startDelay
            this.addUpdateListener { animation ->
                val value = animation.animatedValue as? Float
                if (value != null) {
                    lastNeedleValue = value
                }
            }
        }.also { va ->
            va.start()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig?.let { config ->
            screenOrientation = config.orientation
        }
        meter = null
    }

    private var i = 0

    override fun onDraw(canvas: Canvas) {
        println("onDraw called ${i++}-th time")
        val startTime = System.nanoTime()
        backgroundPaint.color = if (isEnabled) {
            barBackgroundColor
        } else {
            Color.GRAY
        }

        if (meter == null) {
            dashboardViewModel.buildMeter(
                meterType = meterType,
                screenOrientation = screenOrientation,
                canvasWidth = width,
                canvasHeight = height,
                paddingLeft = paddingLeft,
                paddingRight = paddingRight,
                paddingTop = paddingTop,
                paddingBottom = paddingBottom,
                majorTickLength = DEFAULT_MAJOR_TICK_LENGTH,
                majorTickStep = majorTickStep,
                minorTickStep = minorTickStep,
                barValuePadding = barValuePadding,
                barMaxValue = barMaxValue
            )
        }

        meter?.let { meterData ->
            val centerX = meterData.borderBox.centerX()
            val centerY = meterData.borderBox.centerY()
            val viewWidth = meterData.borderBox.width()

            drawBackground(canvas, centerX, centerY)
            drawArc(canvas, meterData.borderBox)
            drawTicks(
                canvas = canvas,
                barLabels = meterData.barLabels,
                tickLines = meterData.ticks,
                centerX = centerX,
                centerY = centerY,
                barLabelX = meterData.barLabelX,
                barLabelY = meterData.barLabelY
            )
            drawNeedle(
                canvas = canvas,
                circleDiameter = meterData.needleCircleBox.width(),
                centerX = centerX,
                centerY = centerY,
                viewWidth = viewWidth
            )
        }
        println("onDraw took: ${(System.nanoTime() - startTime) / 1000000f}")
    }

    private fun drawBackground(canvas: Canvas, centerX: Float, centerY: Float) {
        canvas.drawCircle(centerX, centerY, min(centerX, centerY), backgroundPaint)
    }

    private fun drawArc(canvas: Canvas, borderBox: RectF) {
        canvas.drawArc(borderBox, ARC_START_ANGLE, ARC_END_ANGLE, false, arcPaint)
    }

    private fun drawTicks(
        canvas: Canvas,
        barLabels: List<BarLabel>,
        tickLines: FloatArray,
        centerX: Float,
        centerY: Float,
        barLabelX: Float,
        barLabelY: Float
    ) {
        for (barLabel in barLabels) {
            canvas.save()
            canvas.rotate(barLabel.rotationAngle, centerX, centerY)
            canvas.rotate(BAR_TEXT_ROTATION, barLabelX, centerY)
            canvas.drawText(
                barLabel.label,
                barLabelX,
                barLabelY,
                txtPaint
            )
            canvas.restore()
        }
        canvas.drawLines(tickLines, ticksPaint)
    }

    private fun drawNeedle(
        canvas: Canvas,
        circleDiameter: Float,
        centerX: Float,
        centerY: Float,
        viewWidth: Float
    ) {
        needle?.let { needleData ->
            canvas.drawLine(
                needleData.startX,
                needleData.startY,
                needleData.stopX,
                needleData.stopY,
                needlePaint
            )
            canvas.drawCircle(centerX, centerY, needleData.circleRadius, ticksPaint)
        }

        dashboardViewModel.buildNeedle(
            meterType,
            viewWidth = viewWidth,
            needleBaseCircleDiameter = circleDiameter,
            barMaxValue = barMaxValue,
            needleValue = lastNeedleValue,
            centerX = centerX,
            centerY = centerY
        )
    }

    companion object {
        private const val UNSPECIFIED = -1f
        private const val ZERO = 0f

        private const val DEFAULT_BAR_TEXT_SIZE_SP = 16
        private const val DEFAULT_PADDING_DP = 8

        private const val ARC_STROKE_WIDTH = 5f
        private const val NEEDLE_STROKE_WIDTH = 5f
        private const val TICK_STROKE_WIDTH = 3f
        private const val DEFAULT_MAJOR_TICK_LENGTH = 32f

        private const val ARC_START_ANGLE = 140f
        const val ARC_END_ANGLE = 260f

        private const val BAR_TEXT_ROTATION = 90f
    }
}