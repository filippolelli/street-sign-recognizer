package it.unibo.sistemidigitali.app.kotlin.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.get
import kotlin.math.pow
import kotlin.math.sqrt

class CropAreaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint per disegnare i cerchi
    private val paint = Paint().apply {
        color = Color.WHITE       // Colore dei cerchi
        style = Paint.Style.FILL // Riempimento dei cerchi
        isAntiAlias = true       // Smussa i bordi
    }

    var radius = 10f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var touchedPoint: String? = null
    private val pointThreshold=5
    lateinit var boundaryPoints: MutableMap<String, PointF>

    init {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    performClick()
                    lastTouchX = event.x
                    lastTouchY = event.y
                    boundaryPoints.forEach { (key, point) ->
                        if (sqrt((point.x - event.x).pow(2) + (point.y - event.y).pow(2)) <= radius * pointThreshold) touchedPoint =
                            key
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    var deltaX = (event.x - lastTouchX)
                    var deltaY = (event.y - lastTouchY)
                    val canvasWidth = width.toFloat()
                    val canvasHeight = height.toFloat()

                    if (touchedPoint.isNullOrBlank()) { //move all points
                        boundaryPoints.forEach { (key, point) ->
                            if (point.x+ deltaX <= radius ) deltaX=radius-point.x
                            else if (point.x + deltaX>= canvasWidth - radius) deltaX=canvasWidth-radius-point.x
                            if (point.y+ deltaY <= radius ) deltaY=radius-point.y
                            else if (point.y + deltaY>= canvasHeight - radius) deltaY=canvasHeight-radius-point.y

                        }
                        boundaryPoints.forEach { (key, point) ->
                            point.x += deltaX
                            point.y += deltaY
                        }

                    } else {
                        val (toMoveX, toMoveY) = when (touchedPoint) {
                            "lt" -> "lb" to "rt"
                            "lb" -> "lt" to "rb"
                            "rt" -> "rb" to "lt"
                            else -> "rt" to "lb"
                        }
                        val point=boundaryPoints[touchedPoint]!!
                        if (point.x+ deltaX <= radius ) deltaX=radius-point.x
                        else if (point.x + deltaX>= canvasWidth - radius) deltaX=canvasWidth-radius-point.x
                        if (point.y+ deltaY <= radius ) deltaY=radius-point.y
                        else if (point.y + deltaY>= canvasHeight - radius) deltaY=canvasHeight-radius-point.y

                        boundaryPoints[touchedPoint]?.x = point.x +deltaX
                        boundaryPoints[toMoveX]?.x = boundaryPoints[toMoveX]?.x?.plus(deltaX)!!
                        boundaryPoints[touchedPoint]?.y = point.y +deltaY
                        boundaryPoints[toMoveY]?.y = boundaryPoints[toMoveY]?.y?.plus(deltaY)!!

                    }
                }
                MotionEvent.ACTION_UP -> {
                    touchedPoint = null
                }
            }
            lastTouchX = event.x
            lastTouchY = event.y
            invalidate()
            true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val viewWidth = w.toFloat()
        val viewHeight = h.toFloat()

        boundaryPoints = mutableMapOf(
            "lt" to PointF(radius, radius),
            "lb" to PointF(radius, viewHeight-radius),
            "rt" to PointF(viewWidth - radius, radius),
            "rb" to PointF(viewWidth - radius, viewHeight-radius)
        )



    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        boundaryPoints.forEach { (_, point) ->
            canvas.drawCircle(
                point.x,
                point.y,
                radius,
                paint
            )
        }
        val edges = listOf(
            "lt" to "rt",
            "lt" to "lb",
            "rt" to "rb",
            "lb" to "rb"
        )
        edges.forEach { (start, end) ->
            val startPoint = boundaryPoints[start]!!
            val endPoint = boundaryPoints[end]!!
            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, paint)
        }



    }

}