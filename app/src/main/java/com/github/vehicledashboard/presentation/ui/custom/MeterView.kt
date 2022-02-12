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

    private var barMaxValue: Int = -1
        set(value) {
            require(value > 0)
            field = value
            invalidate()
        }

    private var barValuePadding = 0f

    private var majorTickStep = UNSPECIFIED
        set(value) {
            require(value in 1 until barMaxValue)
            field = value
            invalidate()
        }

    private var minorTickStep = UNSPECIFIED
        set(value) {
            require(value in 1 until barMaxValue)
            field = value
            invalidate()
        }

    private var needleValue: Int = 0
        set(value) {
            require(value >= 0)
            field = min(barMaxValue, value)
            invalidate()
        }

    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.FILL
        }
    private val needlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
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
    private val arcPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
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
            barMaxValue = attributes.getInt(
                R.styleable.MeterView_barMaxValue,
                UNSPECIFIED
            )
            barValuePadding = attributes.getDimension(
                R.styleable.MeterView_barValuePadding,
                0f
            )
            majorTickStep = attributes.getInt(
                R.styleable.MeterView_barMajorStep,
                UNSPECIFIED
            )
            minorTickStep = attributes.getInt(
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

    fun setSpeed(progress: Int, duration: Long, startDelay: Long): ValueAnimator {
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
            val value = animation.animatedValue as? Int
            if (value != null) {
                needleValue = value
            }
        }
        va.start()
        return va
    }

    fun setSpeed(progress: Int, animate: Boolean): ValueAnimator {
        return setSpeed(progress, 1500, 200)
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

        // Clear canvas
        canvas.drawColor(Color.BLUE)

        drawBackground(canvas)
        drawArc(canvas)
        drawTicks(canvas)

        // Draw Needle
        drawNeedle(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val middle = calculateCanvasMiddle(canvas)
        canvas.drawCircle(middle, middle, middle, backgroundPaint)
    }

    private fun calculateCanvasMiddle(canvas: Canvas) =
        min(canvas.width, canvas.height) / 2f

    private fun drawArc(canvas: Canvas) {
        canvas.drawArc(getOval(canvas, 1f), START_ANGLE, END_ANGLE, false, arcPaint)
    }

    private fun drawTicks(canvas: Canvas) {
        val minorTicks = (majorTickStep / minorTickStep.toFloat()).toInt()
        val majorTicksLength = 30f
        val minorTicksLength = majorTicksLength / 2
        val oval = getOval(canvas, 1f)
        val radius = oval.width() * 0.48f
        var currentAngle = BAR_START_ANGLE
        val endAngle = END_ANGLE + currentAngle
        var curProgress = 0
        while (currentAngle <= endAngle) {
            canvas.drawLine(
                (oval.centerX() + cos((180 - currentAngle) / 180 * Math.PI) * (radius - majorTicksLength / 2)).toFloat(),
                (oval.centerY() - sin(currentAngle / 180 * Math.PI) * (radius - majorTicksLength / 2)).toFloat(),
                (oval.centerX() + cos((180 - currentAngle) / 180 * Math.PI) * (radius + majorTicksLength / 2)).toFloat(),
                (oval.centerY() - sin(currentAngle / 180 * Math.PI) * (radius + majorTicksLength / 2)).toFloat(),
                ticksPaint
            )
            for (i in 1..minorTicks) {
                val angle = currentAngle + i * minorTickStep
                if (angle >= endAngle + minorTickStep / 2) {
                    break
                }
                canvas.drawLine(
                    (oval.centerX() + cos((180 - angle) / 180 * Math.PI) * radius).toFloat(),
                    (oval.centerY() - sin(angle / 180 * Math.PI) * radius).toFloat(),
                    (oval.centerX() + cos((180 - angle) / 180 * Math.PI) * (radius + minorTicksLength)).toFloat(),
                    (oval.centerY() - sin(angle / 180 * Math.PI) * (radius + minorTicksLength)).toFloat(),
                    ticksPaint
                )
            }
            val barValue = getLabelFor(curProgress)
            if (barValue.isNotBlank()) {
                canvas.save()
                canvas.rotate(180 + currentAngle, oval.centerX(), oval.centerY())
                val txtX = oval.centerX() + radius - majorTicksLength / 2 - barValuePadding
                val txtY = oval.centerY()
                canvas.rotate(+90f, txtX, txtY)
                canvas.drawText(
                    barValue, txtX, txtY,
                    txtPaint
                )
                canvas.restore()
            }
            currentAngle += majorTickStep
            curProgress += majorTickStep
        }
    }

    private fun drawNeedle(canvas: Canvas) {
        val oval = getOval(canvas, 1f)
        val radius = oval.width() * 0.4f
        val smallOval = getOval(canvas, 0.2f)
        val angle = BAR_START_ANGLE + needleValue
        canvas.drawLine(
            (oval.centerX() + Math.cos((180 - angle) / 180 * Math.PI) * smallOval.width() * 0.5f).toFloat(),
            (oval.centerY() - Math.sin(angle / 180 * Math.PI) * smallOval.width() * 0.5f).toFloat(),
            (oval.centerX() + Math.cos((180 - angle) / 180 * Math.PI) * radius).toFloat(),
            (oval.centerY() - Math.sin(angle / 180 * Math.PI) * radius).toFloat(),
            needlePaint
        )
        val middle = calculateCanvasMiddle(canvas)
        canvas.drawCircle(middle, middle, smallOval.width() / 2, ticksPaint)
    }

    private fun getOval(canvas: Canvas, factor: Float): RectF {
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

    private fun getLabelFor(progress: Int): String =
        if (progress % majorTickStep == 0) {
            progress.toString()
        } else {
            ""
        }

    companion object {
        private const val UNSPECIFIED = -1
        private const val DEFAULT_LABEL_TEXT_SIZE_SP = 12
        private const val ARC_STROKE_WIDTH = 5f
        private const val NEEDLE_STROKE_WIDTH = 5f
        private const val TICK_STROKE_WIDTH = 3f

        private const val BAR_START_ANGLE = -40f
        private const val START_ANGLE = 140f
        private const val END_ANGLE = 260f
    }
}