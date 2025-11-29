package com.example.aharui.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.aharui.R
import kotlin.math.abs

class LineGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataPoints: List<Float> = emptyList()
    private var labels: List<String> = emptyList()
    private var selectedIndex: Int = -1
    private val points = mutableListOf<Pair<Float, Float>>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_line)
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_point)
        style = Paint.Style.FILL
    }

    private val circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val selectedCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_point)
        style = Paint.Style.FILL
    }

    private val selectedCircleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_line)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_grid)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val tooltipBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_point)
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 4f, Color.argb(50, 0, 0, 0))
    }

    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val tooltipDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val path = Path()
    private val gradientPath = Path()

    fun setData(newData: List<Float>, newLabels: List<String>) {
        this.dataPoints = newData
        this.labels = newLabels
        this.selectedIndex = -1
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Find the closest point to the touch
                val touchX = event.x
                val touchY = event.y

                var closestIndex = -1
                var minDistance = Float.MAX_VALUE
                val touchThreshold = 100f // pixels

                points.forEachIndexed { index, (x, y) ->
                    val distance = abs(touchX - x) + abs(touchY - y)
                    if (distance < minDistance && distance < touchThreshold) {
                        minDistance = distance
                        closestIndex = index
                    }
                }

                if (closestIndex != selectedIndex) {
                    selectedIndex = closestIndex
                    invalidate()
                    performHapticFeedback()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Keep the selection visible
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun performHapticFeedback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        } else {
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty() || labels.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val padding = 60f
        val textPadding = 50f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding - textPadding

        // Draw grid lines
        drawGridLines(canvas, padding, textPadding, chartWidth, chartHeight)

        val maxValue = dataPoints.maxOrNull() ?: 0f
        val minValue = dataPoints.minOrNull() ?: 0f
        val valueRange = if (maxValue == minValue) 1f else maxValue - minValue
        val xStep = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1).toFloat() else 0f

        path.reset()
        gradientPath.reset()
        points.clear()

        // Calculate all points
        dataPoints.forEachIndexed { index, value ->
            val x = padding + index * xStep
            val y = (height - padding - textPadding) - ((value - minValue) / valueRange) * chartHeight
            points.add(Pair(x, y))

            if (index == 0) {
                path.moveTo(x, y)
                gradientPath.moveTo(x, height - padding - textPadding)
                gradientPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                gradientPath.lineTo(x, y)
            }
        }

        // Close gradient path
        if (points.isNotEmpty()) {
            gradientPath.lineTo(points.last().first, height - padding - textPadding)
            gradientPath.close()
        }

        // Draw gradient fill
        gradientPaint.shader = android.graphics.LinearGradient(
            0f, padding,
            0f, height - padding - textPadding,
            intArrayOf(
                ContextCompat.getColor(context, R.color.chart_line),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        gradientPaint.alpha = 60
        canvas.drawPath(gradientPath, gradientPaint)

        // Draw the connecting line
        canvas.drawPath(path, linePaint)

        // Draw data points and labels
        points.forEachIndexed { index, (x, y) ->
            val isSelected = index == selectedIndex

            if (isSelected) {
                // Draw larger selected point with outer ring
                canvas.drawCircle(x, y, 20f, selectedCircleStrokePaint)
                canvas.drawCircle(x, y, 16f, selectedCirclePaint)
            } else {
                // Draw normal points
                canvas.drawCircle(x, y, 14f, circleStrokePaint)
                canvas.drawCircle(x, y, 10f, circlePaint)
            }

            // Draw labels
            if (index < labels.size) {
                val labelPaint = if (isSelected) {
                    Paint(textPaint).apply {
                        color = ContextCompat.getColor(context, R.color.chart_point)
                        isFakeBoldText = true
                    }
                } else {
                    textPaint
                }
                canvas.drawText(labels[index], x, height - padding + 35f, labelPaint)
            }

            // Draw values on first and last points (when not selected)
            if (!isSelected && (index == 0 || index == dataPoints.size - 1)) {
                val valueText = String.format("%.1f", dataPoints[index])
                canvas.drawText(valueText, x, y - 25f, valuePaint)
            }
        }

        // Draw tooltip for selected point
        if (selectedIndex >= 0 && selectedIndex < dataPoints.size) {
            drawTooltip(canvas, selectedIndex)
        }
    }

    private fun drawTooltip(canvas: Canvas, index: Int) {
        val (x, y) = points[index]
        val weight = dataPoints[index]
        val date = if (index < labels.size) labels[index] else ""

        val weightText = String.format("%.1f kg", weight)

        // Calculate tooltip dimensions
        val weightTextWidth = tooltipTextPaint.measureText(weightText)
        val dateTextWidth = tooltipDatePaint.measureText(date)
        val maxTextWidth = maxOf(weightTextWidth, dateTextWidth)

        val tooltipWidth = maxTextWidth + 40f
        val tooltipHeight = 100f
        val cornerRadius = 12f

        // Position tooltip above the point, but adjust if too close to top
        var tooltipX = x
        var tooltipY = y - tooltipHeight - 30f

        // Adjust horizontal position if too close to edges
        if (tooltipX - tooltipWidth / 2 < 20f) {
            tooltipX = tooltipWidth / 2 + 20f
        } else if (tooltipX + tooltipWidth / 2 > width - 20f) {
            tooltipX = width - tooltipWidth / 2 - 20f
        }

        // Adjust vertical position if too close to top
        if (tooltipY < 20f) {
            tooltipY = y + 60f // Show below the point instead
        }

        // Draw tooltip background
        val tooltipRect = RectF(
            tooltipX - tooltipWidth / 2,
            tooltipY,
            tooltipX + tooltipWidth / 2,
            tooltipY + tooltipHeight
        )
        canvas.drawRoundRect(tooltipRect, cornerRadius, cornerRadius, tooltipBackgroundPaint)

        // Draw weight text
        canvas.drawText(
            weightText,
            tooltipX,
            tooltipY + 40f,
            tooltipTextPaint
        )

        // Draw date text
        canvas.drawText(
            date,
            tooltipX,
            tooltipY + 72f,
            tooltipDatePaint
        )
    }

    private fun drawGridLines(canvas: Canvas, padding: Float, textPadding: Float, chartWidth: Float, chartHeight: Float) {
        val numLines = 4
        for (i in 0..numLines) {
            val y = padding + (chartHeight / numLines) * i
            axisPaint.alpha = 40
            canvas.drawLine(padding, y, padding + chartWidth, y, axisPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.text_tertiary)
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "No data available",
            width / 2f,
            height / 2f,
            emptyTextPaint
        )
    }
}