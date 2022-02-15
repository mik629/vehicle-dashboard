package com.github.vehicledashboard.presentation.ui.dashboard

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
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
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.github.vehicledashboard.R
import com.github.vehicledashboard.domain.getHalf
import com.github.vehicledashboard.domain.inBetweenExclusive
import com.github.vehicledashboard.presentation.models.BarLabel
import com.github.vehicledashboard.presentation.models.MeterType
import com.github.vehicledashboard.presentation.models.Needle
import com.github.vehicledashboard.presentation.models.fromId
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

    private var barBackgroundColor: Int = 0

    private var meterType: MeterType = MeterType.UNKNOWN

    private var tickLines: FloatArray = floatArrayOf()
    private var barLabels: List<BarLabel> = listOf()

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
        init()
    }

    private fun init() {
        if (!isInEditMode) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        // todo: maybe move to attrs
        arcPaint.strokeWidth = ARC_STROKE_WIDTH
        ticksPaint.strokeWidth = TICK_STROKE_WIDTH
        needlePaint.strokeWidth = NEEDLE_STROKE_WIDTH
    }

    // todo: optimize?
    fun setNeedleValue(progress: Float, duration: Long, startDelay: Long) {
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner())
        lifecycleOwner
            .lifecycleScope
            .launch {
                when (meterType) {
                    MeterType.SPEEDOMETER -> dashboardViewModel.speedometerTicks
                    MeterType.TACHOMETER -> dashboardViewModel.tachometerTicks
                    MeterType.UNKNOWN -> throw UnsupportedOperationException()
                }
                    .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { ticks ->
                        tickLines = ticks
                        invalidate()
                    }
            }

        lifecycleOwner
            .lifecycleScope
            .launch {
                when (meterType) {
                    MeterType.SPEEDOMETER -> dashboardViewModel.speedometerBarLabels
                    MeterType.TACHOMETER -> dashboardViewModel.tachometerBarLabels
                    MeterType.UNKNOWN -> throw UnsupportedOperationException()
                }
                    .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { bars ->
                        barLabels = bars
                        invalidate()
                    }
            }

        lifecycleOwner
            .lifecycleScope
            .launch {
                when (meterType) {
                    MeterType.SPEEDOMETER -> dashboardViewModel.speedometerNeedle
                    MeterType.TACHOMETER -> dashboardViewModel.tachometerNeedle
                    MeterType.UNKNOWN -> throw UnsupportedOperationException()
                }
                    .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { aNeedle ->
                        needle = aNeedle
                        invalidate()
                    }
            }
    }

    // fixme: refactor
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var width: Int
        var height: Int

        width = if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            widthSize
        } else {
            -1
        }

        height = if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            heightSize
        } else {
            -1
        }
        if (height >= 0 && width >= 0) {
            width = min(height, width)
            height = width / 2
        } else if (width >= 0) {
            height = width / 2
        } else if (height >= 0) {
            width = height * 2
        } else {
            width = 0
            height = 0
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        backgroundPaint.color = if (isEnabled) {
            barBackgroundColor
        } else {
            Color.GRAY
        }

        val oval = getOval(canvas)
        val center = getHalf(min(width, height).toFloat())
        drawBackground(canvas, center)
        drawArc(canvas, oval)

        val centerX = oval.centerX()
        val centerY = oval.centerY()
        val viewWidth = oval.width()
        drawTicks(canvas, centerX = centerX, centerY = centerY, viewWidth = viewWidth)
        drawNeedle(
            canvas,
            centerX = centerX,
            centerY = centerY,
            viewWidth = viewWidth,
            center = center
        )
    }

    private fun drawBackground(canvas: Canvas, center: Float) {
        canvas.drawCircle(center, center, center, backgroundPaint)
    }

    private fun drawArc(canvas: Canvas, oval: RectF) {
        canvas.drawArc(oval, ARC_START_ANGLE, ARC_END_ANGLE, false, arcPaint)
    }

    private fun drawTicks(canvas: Canvas, centerX: Float, centerY: Float, viewWidth: Float) {
        if (barLabels.isEmpty()) {
            dashboardViewModel.buildBarLabels(
                majorTickStep = majorTickStep,
                barMaxValue = barMaxValue,
                meterType = meterType
            )
        } else {
            val radius = viewWidth * TICKS_RADIUS_COEFFICIENT
            val txtX = centerX + radius - getHalf(DEFAULT_MAJOR_TICK_LENGTH) - barValuePadding
            val txtY = centerY + barValuePadding

            for (barLabel in barLabels) {
                canvas.save()
                canvas.rotate(barLabel.rotationAngle, centerX, centerY)
                canvas.rotate(BAR_TEXT_ROTATION, txtX, centerY)
                canvas.drawText(
                    barLabel.label,
                    txtX,
                    txtY,
                    txtPaint
                )
                canvas.restore()
            }
        }

        if (tickLines.isEmpty()) {
            dashboardViewModel.buildTicks(
                viewWidth = viewWidth,
                centerX = centerX,
                centerY = centerY,
                majorTickLength = DEFAULT_MAJOR_TICK_LENGTH,
                majorTickStep = majorTickStep,
                minorTickStep = minorTickStep,
                barMaxValue = barMaxValue,
                meterType = meterType
            )
        } else {
            canvas.drawLines(tickLines, ticksPaint)
        }
    }

    // fixme: move calculations away from UI thread
    private fun drawNeedle(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        viewWidth: Float,
        center: Float
    ) {
        val smallCircle = getOval(canvas, NEEDLE_CIRCLE_RADIUS_COEFFICIENT)
        needle?.let { aNeedle ->
            canvas.drawLine(
                aNeedle.startX,
                aNeedle.startY,
                aNeedle.stopX,
                aNeedle.stopY,
                needlePaint
            )
            canvas.drawCircle(center, center, aNeedle.circleCenter, ticksPaint)
        }
        dashboardViewModel.buildNeedle(
            meterType,
            viewWidth = viewWidth,
            needleBaseCircleDiameter = smallCircle.width(),
            barMaxValue = barMaxValue,
            needleValue = lastNeedleValue,
            centerX = centerX,
            centerY = centerY
        )
    }

    private fun getOval(canvas: Canvas, factor: Float = FACTOR_FULL): RectF {
        val canvasWidth = canvas.width - paddingLeft - paddingRight
        val canvasHeight = canvas.height - paddingTop - paddingBottom
        val smallest = min(canvasWidth, canvasHeight)
        return RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            smallest * factor + paddingRight,
            smallest * factor + paddingBottom
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

        private const val FACTOR_FULL = 1f

        private const val BAR_TEXT_ROTATION = 90f

        private const val NEEDLE_CIRCLE_RADIUS_COEFFICIENT = 0.2f
        const val TICKS_RADIUS_COEFFICIENT = 0.48f
    }
}