package com.github.vehicledashboard.presentation.ui.custom

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.github.vehicledashboard.R
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

    private var needleValue: Float = ZERO
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
        }
    private val needlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.STROKE
        }

    init {
        val density = resources.displayMetrics.density
        val attributes = context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.MeterView,
            0, 0
        )
        try {
            backgroundPaint.color = attributes.getColor(
                R.styleable.MeterView_barBackgroundColor,
                Color.WHITE // todo: replace with theme attr
            )
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
                (DEFAULT_LABEL_TEXT_SIZE_SP * density).roundToInt()
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
        invalidate()
    }

    fun setNeedleValue(progress: Float, duration: Long, startDelay: Long): ValueAnimator {
        require(progress > 0)
        val va = ValueAnimator.ofObject(
            object : TypeEvaluator<Float> {
                override fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
                    return startValue + fraction * (endValue - startValue)
                }
            },
            needleValue,
            min(progress, barMaxValue)
        )
        va.duration = duration
        va.startDelay = startDelay
        va.addUpdateListener { animation ->
            val value = animation.animatedValue as? Float
            if (value != null) {
                needleValue = value
            }
        }
        va.start()
        return va
    }

    fun setNeedleValue(progress: Float, animate: Boolean): ValueAnimator {
        return setNeedleValue(progress, 1500, 200)
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

        canvas.drawColor(Color.TRANSPARENT) // clear canvas

        val oval = getOval(canvas)
        val center = min(width, height) / HALF
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
        val majorStepAngle = calcMajorStepAngle()
        val minorTicks = (majorTickStep / minorTickStep).toInt()
        val minorStepAngle = majorStepAngle / minorTicks
        val halfMinorStepAngle = getHalf(minorStepAngle)
        val minorTicksLength = getHalf(30f)
        val radius = oval.width() * 0.48f
        var curProgress = 0f
        val centerX = oval.centerX()
        val centerY = oval.centerY()

        var currentAngle = BAR_START_ANGLE
        val endAngle = ARC_END_ANGLE + currentAngle
        while (currentAngle <= endAngle) {
            canvas.drawLine(
                centerX + cosInRadians(currentAngle) * (radius - minorTicksLength),
                centerY - sinInRadians(currentAngle) * (radius - minorTicksLength),
                centerX + cosInRadians(currentAngle) * (radius + minorTicksLength),
                centerY - sinInRadians(currentAngle) * (radius + minorTicksLength),
                ticksPaint
            )
            for (i in 1..minorTicks) {
                val angle = currentAngle + i * minorStepAngle
                if (angle >= endAngle + halfMinorStepAngle) {
                    break
                }
                canvas.drawLine(
                    centerX + cosInRadians(angle) * radius,
                    centerY - sinInRadians(angle) * radius,
                    centerX + cosInRadians(angle) * (radius + minorTicksLength),
                    centerY - sinInRadians(angle) * (radius + minorTicksLength),
                    ticksPaint
                )
            }
            val barValue = getLabelFor(curProgress)
            if (barValue.isNotBlank()) {
                canvas.save()
                canvas.rotate(PI_IN_DEGREES + currentAngle, centerX, centerY)
                val txtX = centerX + radius - minorTicksLength - barValuePadding
                canvas.rotate(BAR_TXT_ROTATION, txtX, centerY)
                canvas.drawText(
                    barValue, txtX, centerY,
                    txtPaint
                )
                canvas.restore()
            }
            currentAngle += majorStepAngle
            curProgress += majorTickStep
        }
    }

    private fun calcMajorStepAngle() =
        majorTickStep * ARC_END_ANGLE / barMaxValue

    private fun drawNeedle(canvas: Canvas, oval: RectF, center: Float) {
        val radius = oval.width() * 0.4f
        val smallOval = getOval(canvas, 0.2f)
        val majorStepAngle = calcMajorStepAngle()
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
        sin(currentAngle / PI_IN_DEGREES * Math.PI).toFloat()

    private fun cosInRadians(currentAngle: Float) =
        cos((PI_IN_DEGREES - currentAngle) / PI_IN_DEGREES * Math.PI).toFloat()

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
            "%.0f".format(progress)
        } else {
            ""
        }

    companion object {
        private const val UNSPECIFIED = -1f
        private const val ZERO = 0f

        private const val DEFAULT_LABEL_TEXT_SIZE_SP = 12
        private const val DEFAULT_PADDING_DP = 16

        private const val ARC_STROKE_WIDTH = 5f
        private const val NEEDLE_STROKE_WIDTH = 5f
        private const val TICK_STROKE_WIDTH = 3f

        private const val BAR_START_ANGLE = -40f
        private const val ARC_START_ANGLE = 140f
        private const val ARC_END_ANGLE = 260f

        private const val PI_IN_DEGREES = 180

        private const val HALF = 2f

        private const val FACTOR_FULL = 1f

        private const val BAR_TXT_ROTATION = 90f
    }
}