package com.test

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import android.widget.Scroller
import androidx.core.math.MathUtils.clamp
import kotlin.math.*

class WheelScrollView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val scroller = Scroller(context)
    private var tracker: VelocityTracker? = null
    private val itemCount = 9
    private var itemHeight = 0
    private var lastPointY = 0
    private var scrollOffsetY = 0
    private var drawnCenterY = 0f
    private val minimumVelocity: Int
    private val maximumVelocity: Int
    private val camera = Camera()
    private val matrixRotate = Matrix()
    private val matrixDepth = Matrix()
    private val paint = Paint().apply {
        textAlign = Paint.Align.CENTER
        textSize = 24.toPx()
    }
    private val selectedPadding = 8.toPx()
    private val selectedRectF = RectF()
    private val selectedPaint = Paint().apply {
        color = Color.parseColor("#DDDDDD")
    }
    private val mHandler = Handler(Looper.getMainLooper())
    private val mRunnable = object : Runnable {
        override fun run() {
            if (scroller.computeScrollOffset()) {
                scrollOffsetY = scroller.currY
                postInvalidate()
                mHandler.postDelayed(this, 16)
            }
        }
    }
    private val mData = ArrayList<String>()
    val currentPosition get() = -scrollOffsetY / itemHeight % mData.size

    init {
        val conf = ViewConfiguration.get(context)
        minimumVelocity = conf.scaledMinimumFlingVelocity
        maximumVelocity = conf.scaledMaximumFlingVelocity
    }

    fun setData(data: List<String>): WheelScrollView {
        mData.clear()
        mData.addAll(data)
        invalidate()
        return this
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val textHeight = paint.fontMetrics.let { it.bottom - it.top }.toInt()
        val maxHeight = textHeight * itemCount + 12.toPx() * (itemCount - 1)
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            (2 * sinDegree(90f) / Math.PI * maxHeight).toInt()
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        itemHeight = h / itemCount
        drawnCenterY = h / 2f - (paint.ascent() + paint.descent()) / 2
        selectedRectF.set(
            paddingLeft + selectedPadding,
            h / 2f - itemHeight / 2f - selectedPadding,
            width - paddingRight - selectedPadding,
            h / 2f + itemHeight / 2f + selectedPadding
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                tracker = VelocityTracker.obtain()
                tracker!!.addMovement(event)
                lastPointY = event.y.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                tracker!!.addMovement(event)
                val move = event.y - lastPointY
                if (abs(move) >= 1) {
                    scrollOffsetY += move.toInt()
                    lastPointY = event.y.toInt()
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                tracker!!.addMovement(event)
                tracker!!.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                val velocity = tracker!!.yVelocity.toInt()
                if (abs(velocity) > minimumVelocity) {
                    scroller.fling(0, scrollOffsetY, 0, velocity, 0, 0, Int.MIN_VALUE, Int.MAX_VALUE)
                    scroller.finalY = scroller.finalY + computeDistanceToEndPoint(scroller.finalY % itemHeight)
                } else {
                    scroller.startScroll(0, scrollOffsetY, 0, computeDistanceToEndPoint(scrollOffsetY % itemHeight))
                }
                mHandler.post(mRunnable)
                tracker!!.recycle()
                tracker = null
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (tracker != null) {
                    tracker!!.recycle()
                    tracker = null
                }
            }
        }
        return true
    }

    private fun computeDistanceToEndPoint(remainder: Int): Int {
        return if (abs(remainder) > itemHeight / 2) {
            if (scrollOffsetY < 0) {
                -itemHeight - remainder
            } else {
                itemHeight - remainder
            }
        } else {
            -remainder
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(selectedRectF, selectedPadding, selectedPadding, selectedPaint)
        var drawnOffsetPos = -itemCount / 2 - 1
        var drawnDataPos = -scrollOffsetY / itemHeight + drawnOffsetPos
        val drawnCount = drawnDataPos + itemCount + 2
        while (drawnDataPos < drawnCount) {
            val position = (drawnDataPos % mData.size).let { if (it < 0) it + mData.size else it }
            val drawnItemCenterY = drawnCenterY + drawnOffsetPos * itemHeight + scrollOffsetY % itemHeight
            val ratio = (drawnCenterY - abs(drawnCenterY - drawnItemCenterY)) / drawnCenterY
            val unit = if (drawnItemCenterY > drawnCenterY) 1 else if (drawnItemCenterY < drawnCenterY) -1 else 0
            val degree = clamp((ratio - 1) * 90f * unit, -90f, 90f)
            val distanceToCenter = computeYCoordinateAtAngle(degree)
            val transX = width / 2f
            val transY = height / 2f - distanceToCenter
            camera.save()
            camera.rotateX(degree)
            camera.getMatrix(matrixRotate)
            camera.restore()
            matrixRotate.preTranslate(-transX, -transY)
            matrixRotate.postTranslate(transX, transY)
            camera.save()
            camera.translate(0f, 0f, computeDepth(degree))
            camera.getMatrix(matrixDepth)
            camera.restore()
            matrixDepth.preTranslate(-transX, -transY)
            matrixDepth.postTranslate(transX, transY)
            matrixRotate.postConcat(matrixDepth)
            paint.alpha = max(((drawnCenterY - abs(drawnCenterY - drawnItemCenterY)) * 1f / drawnCenterY * 255).toInt(), 0)
            canvas.save()
            canvas.concat(matrixRotate)
            canvas.drawText(mData[position], transX, drawnCenterY - distanceToCenter, paint)
            canvas.restore()
            drawnDataPos++
            drawnOffsetPos++
        }
    }

    private fun computeYCoordinateAtAngle(degree: Float): Float {
        return sinDegree(degree) / sinDegree(90f) * height / 2
    }

    private fun sinDegree(degree: Float): Float {
        return sin(Math.toRadians(degree.toDouble())).toFloat()
    }

    private fun computeDepth(degree: Float): Float {
        return (height / 2 - cos(Math.toRadians(degree.toDouble())) * height / 2).toFloat()
    }
}