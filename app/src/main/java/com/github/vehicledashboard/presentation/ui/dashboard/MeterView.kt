package com.github.vehicledashboard.presentation.ui.dashboard

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.github.vehicledashboard.R
import com.github.vehicledashboard.domain.PI_IN_DEGREES
import com.github.vehicledashboard.domain.degreesToRadians
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin


class MeterView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {

    private var barMaxValue: Float = UNSPECIFIED
        set(value) {
            require(value > 0)
            field = value
        }

    private var barValuePadding = 0f

    private var majorTickStep = UNSPECIFIED
        set(value) {
            require(value > 0 && value < barMaxValue)
            field = value
        }

    private var minorTickStep = UNSPECIFIED
        set(value) {
            require(value > 0 && value < barMaxValue)
            field = value
        }

    var needleValue: Float = ZERO
        set(value) {
            require(value >= 0)
            field = min(barMaxValue, value)
            invalidate()
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

    private var majorStepAngle = 0f
    private var minorTicks = 0
    private var minorStepAngle = 0f
    private var halfMinorStepAngle = 0f
    private var minorTicksLength = 0f

    private var tickLines: FloatArray = floatArrayOf()

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
            needleValue = attributes.getFloat(
                R.styleable.MeterView_needleStartValue,
                ZERO
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

        majorStepAngle = calcMajorStepAngle()
        minorTicks = (majorTickStep / minorTickStep).toInt()
        minorStepAngle = majorStepAngle / minorTicks
        halfMinorStepAngle = getHalf(minorStepAngle)
        minorTicksLength = getHalf(DEFAULT_MAJOR_TICK_LENGTH)

        invalidate()
    }

    fun setNeedleValue(progress: Float): ValueAnimator {
        return setNeedleValue(progress, 1500, 100)
    }

    private fun setNeedleValue(progress: Float, duration: Long, startDelay: Long): ValueAnimator {
        require(progress > 0)
        return ValueAnimator.ofObject(
            object : TypeEvaluator<Float> {
                override fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
                    val nextValue = startValue + fraction * (endValue - startValue)
                    println("AnimatedValue: $nextValue")
                    return nextValue
                }
            },
            needleValue,
            min(progress, barMaxValue)
        ).apply {
            this.duration = duration
            this.startDelay = startDelay
            this.addUpdateListener { animation ->
                val value = animation.animatedValue as? Float
                if (value != null) {
                    needleValue = value
                }
            }
        }.also { va ->
            va.start()
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

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) // clear canvas

        val oval = getOval(canvas)
        val center = min(width, height) / HALF
        backgroundPaint.color = if (isEnabled) {
            barBackgroundColor
        } else {
            Color.GRAY
        }
        drawBackground(canvas, center)
        drawArc(canvas, oval)
        drawTicks(canvas, oval)
        drawNeedle(canvas, oval, center)
    }

    private fun drawBackground(canvas: Canvas, center: Float) {
        canvas.drawCircle(center, center, center, backgroundPaint)
    }

    private fun drawArc(canvas: Canvas, oval: RectF) {
        canvas.drawArc(oval, ARC_START_ANGLE, ARC_END_ANGLE, false, arcPaint)
    }

    private fun drawTicks(canvas: Canvas, oval: RectF) {
        val radius = oval.width() * TICKS_RADIUS_COEFFICIENT
        var curProgress = 0f
        val centerX = oval.centerX()
        val centerY = oval.centerY()

        var currentAngle = BAR_START_ANGLE
        val endAngle = ARC_END_ANGLE + currentAngle

        while (currentAngle <= endAngle) {
            val barValue = getLabelFor(curProgress)
            if (barValue.isNotBlank()) {
                canvas.save()
                canvas.rotate(PI_IN_DEGREES + currentAngle, centerX, centerY)
                val txtX = centerX + radius - minorTicksLength - barValuePadding
                canvas.rotate(BAR_TEXT_ROTATION, txtX, centerY)
                canvas.drawText(
                    barValue,
                    txtX,
                    centerY + barValuePadding,
                    txtPaint
                )
                canvas.restore()
            }
            currentAngle += majorStepAngle
            curProgress += majorTickStep
        }

        if (tickLines.isEmpty()) {
            tickLines = buildTicks(oval)
        }
        canvas.drawLines(tickLines, ticksPaint)
    }

    private fun buildTicks(oval: RectF): FloatArray {
        val radius = oval.width() * TICKS_RADIUS_COEFFICIENT
        var curProgress = 0f
        val centerX = oval.centerX()
        val centerY = oval.centerY()

        var currentAngle = BAR_START_ANGLE
        val endAngle = ARC_END_ANGLE + currentAngle

        val barTickStartPosition = radius - minorTicksLength
        val barTickEndPosition = radius + minorTicksLength
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
                lines.add(centerX + cosInRadians(angle) * radius)
                lines.add(centerY - sinInRadians(angle) * radius)
                lines.add(centerX + cosInRadians(angle) * barTickEndPosition)
                lines.add(centerY - sinInRadians(angle) * barTickEndPosition)
            }
            currentAngle += majorStepAngle
            curProgress += majorTickStep
        }
        return lines.toFloatArray()
    }

    private fun calcMajorStepAngle() =
        majorTickStep * ARC_END_ANGLE / barMaxValue

    private fun drawNeedle(canvas: Canvas, oval: RectF, center: Float) {
        val radius = oval.width() * NEEDLE_RADIUS_COEFFICIENT
        val smallOval = getOval(canvas, NEEDLE_CIRCLE_RADIUS_COEFFICIENT)
        val majorStepAngle = ARC_END_ANGLE / barMaxValue
        val angle = BAR_START_ANGLE + needleValue * majorStepAngle
        val ovalMiddle = getHalf(smallOval.width())
        canvas.drawLine(
            oval.centerX() + cosInRadians(angle) * ovalMiddle,
            oval.centerY() - sinInRadians(angle) * ovalMiddle,
            oval.centerX() + cosInRadians(angle) * radius,
            oval.centerY() - sinInRadians(angle) * radius,
            needlePaint
        )
        canvas.drawCircle(center, center, ovalMiddle, ticksPaint)
    }

    private fun sinInRadians(currentAngle: Float) =
        sin(degreesToRadians(currentAngle))

    private fun cosInRadians(currentAngle: Float) =
        cos(degreesToRadians(PI_IN_DEGREES - currentAngle))

    private fun getHalf(majorTicksLength: Float) = majorTicksLength / HALF

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

    private fun getLabelFor(progress: Float): String =
        if (progress % majorTickStep == 0f) {
            BAR_DIGIT_FORMAT.format(progress)
        } else {
            ""
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

        private const val BAR_START_ANGLE = -40f
        private const val ARC_START_ANGLE = 140f
        private const val ARC_END_ANGLE = 260f

        private const val HALF = 2f

        private const val FACTOR_FULL = 1f

        private const val BAR_TEXT_ROTATION = 90f
        private const val BAR_DIGIT_FORMAT = "%.0f"

        private const val NEEDLE_RADIUS_COEFFICIENT = 0.38f
        private const val NEEDLE_CIRCLE_RADIUS_COEFFICIENT = 0.2f
        private const val TICKS_RADIUS_COEFFICIENT = 0.48f
    }
}