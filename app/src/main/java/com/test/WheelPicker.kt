package com.test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import android.widget.Scroller
import kotlin.collections.ArrayList
import kotlin.math.*

class WheelPicker(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var onItemSelectedListener: IOnItemSelectedListener? = null
    private val dataList = ArrayList<String>()

    private val scroller = Scroller(context)
    private var tracker: VelocityTracker? = null
    private val rectDrawn = Rect()
    private val rectCurrentItem = Rect()
    private val camera = Camera()
    private val matrixRotate = Matrix()
    private val matrixDepth = Matrix()

    private val mMaxAngle = 90f
    private val mVisibleItemCount = 9
    private val mDrawnItemCount = mVisibleItemCount + 2
    private val mHalfDrawnItemCount = mDrawnItemCount / 2
    private var mTextMaxWidth = 0
    private var mTextMaxHeight = 0
    private val mItemTextColor = Color.parseColor("#FF888888")
    private val mSelectedItemTextColor = Color.parseColor("#FF000000")
    private val mItemSpace = 12.toPx().toInt()
    private var mItemHeight = 0
    private var mItemAlign = Paint.Align.CENTER
    private var mHalfItemHeight = 0
    private var mHalfWheelHeight = 0
    private var selectedItemPosition = 0
    private var currentItemPosition = 0
    private var minFlingY = 0
    private var maxFlingY = 0
    private val minimumVelocity: Int
    private val maximumVelocity: Int
    private var wheelCenterX = 0
    private var wheelCenterY = 0
    private var drawnCenterX = 0
    private var drawnCenterY = 0
    private var scrollOffsetY = 0
    private val textMaxWidthPosition = -1
    private var lastPointY = 0
    private var downPointY = 0
    private val touchSlop: Int

    private var hasAtmospheric = true
    private var isCyclic = false
    private var isCurved = true
    private var isClick = false
    private var isForceFinishScroll = false
    private val mHandler = Handler(Looper.getMainLooper())
    private val mRunnable = object : Runnable {
        override fun run() {
            val itemCount = dataList.size
            if (itemCount == 0) return
            if (scroller.isFinished && !isForceFinishScroll) {
                if (mItemHeight == 0) return
                var position = (-scrollOffsetY / mItemHeight + selectedItemPosition) % itemCount
                if (position < 0) position += itemCount
                currentItemPosition = position
                onItemSelectedListener?.onItemSelected(currentItemPosition)
            }
            if (scroller.computeScrollOffset()) {
                scrollOffsetY = scroller.currY
                postInvalidate()
                mHandler.postDelayed(this, 16)
            }
        }
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 24.toPx()
    }
    private val selectedRectF = RectF()
    private val selectedPaint = Paint().apply {
        color = Color.parseColor("#DDDDDD")
    }

    init {
        val conf = ViewConfiguration.get(context)
        minimumVelocity = conf.scaledMinimumFlingVelocity
        maximumVelocity = conf.scaledMaximumFlingVelocity
        touchSlop = conf.scaledTouchSlop
    }

    fun setData(data: List<String>): WheelPicker {
        dataList.clear()
        dataList.addAll(data)
        return this
    }

    private fun getItem(position: Int): String {
        return dataList[abs(position % dataList.size)]
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        notifyDataSetChanged()
    }

    private fun computeTextSize() {
        mTextMaxWidth = 0
        if (textMaxWidthPosition > -1 && textMaxWidthPosition < dataList.size) {
            mTextMaxWidth = paint.measureText(getItem(textMaxWidthPosition)).toInt()
        } else {
            for (i in 0 until dataList.size) {
                mTextMaxWidth = max(mTextMaxWidth, paint.measureText(getItem(i)).toInt())
            }
        }
        mTextMaxHeight = paint.fontMetrics.let { it.bottom - it.top }.toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val modeWidth = MeasureSpec.getMode(widthMeasureSpec)
        val modeHeight = MeasureSpec.getMode(heightMeasureSpec)
        val sizeWidth = MeasureSpec.getSize(widthMeasureSpec)
        val sizeHeight = MeasureSpec.getSize(heightMeasureSpec)
        var resultWidth = mTextMaxWidth
        var resultHeight = mTextMaxHeight * mVisibleItemCount + mItemSpace * (mVisibleItemCount - 1)
        if (isCurved) {
            resultHeight = (2 * sinDegree(mMaxAngle) / (Math.PI * mMaxAngle / 90f) * resultHeight).toInt()
        }
        resultWidth += paddingLeft + paddingRight
        resultHeight += paddingTop + paddingBottom
        resultWidth = measureSize(modeWidth, sizeWidth, resultWidth)
        resultHeight = measureSize(modeHeight, sizeHeight, resultHeight)
        setMeasuredDimension(resultWidth, resultHeight)
    }

    private fun measureSize(mode: Int, sizeExpect: Int, sizeActual: Int): Int {
        return when (mode) {
            MeasureSpec.EXACTLY -> sizeExpect
            MeasureSpec.AT_MOST -> min(sizeActual, sizeExpect)
            else -> sizeActual
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        rectDrawn.set(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)
        wheelCenterX = rectDrawn.centerX()
        wheelCenterY = rectDrawn.centerY()
        computeDrawnCenter()
        mHalfWheelHeight = rectDrawn.height() / 2
        mItemHeight = rectDrawn.height() / mVisibleItemCount
        mHalfItemHeight = mItemHeight / 2
        computeFlingLimitY()
        computeCurrentItemRect()

        val padding = 8.toPx()
        selectedRectF.set(
            paddingLeft + padding,
            wheelCenterY - mItemHeight / 2f - padding,
            width - paddingRight - padding,
            wheelCenterY + mItemHeight / 2f + padding
        )
    }

    private fun computeDrawnCenter() {
        drawnCenterX = when (mItemAlign) {
            Paint.Align.LEFT -> rectDrawn.left
            Paint.Align.RIGHT -> rectDrawn.right
            else -> wheelCenterX
        }
        drawnCenterY = (wheelCenterY - (paint.ascent() + paint.descent()) / 2).toInt()
    }

    private fun computeFlingLimitY() {
        val currentItemOffset = selectedItemPosition * mItemHeight
        minFlingY = if (isCyclic) Int.MIN_VALUE else -mItemHeight * dataList.lastIndex + currentItemOffset
        maxFlingY = if (isCyclic) Int.MAX_VALUE else currentItemOffset
    }

    private fun computeCurrentItemRect() {
        if (mSelectedItemTextColor == -1) return
        rectCurrentItem.set(rectDrawn.left, wheelCenterY - mHalfItemHeight, rectDrawn.right, wheelCenterY + mHalfItemHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (mItemHeight - mHalfDrawnItemCount <= 0) {
            return
        }
        canvas.drawRoundRect(selectedRectF, 8.toPx(), 8.toPx(), selectedPaint)
        val drawnDataStartPos = -scrollOffsetY / mItemHeight - mHalfDrawnItemCount
        var drawnDataPos = drawnDataStartPos + selectedItemPosition
        var drawnOffsetPos = -mHalfDrawnItemCount
        while (drawnDataPos < drawnDataStartPos + selectedItemPosition + mDrawnItemCount) {
            val data = when {
                isCyclic -> {
                    var actualPos = drawnDataPos % dataList.size
                    if (actualPos < 0) actualPos += dataList.size
                    getItem(actualPos)
                }
                drawnDataPos > -1 && drawnDataPos < dataList.size -> {
                    getItem(drawnDataPos)
                }
                else -> ""
            }
            val mDrawnItemCenterY = drawnCenterY + drawnOffsetPos * mItemHeight + scrollOffsetY % mItemHeight
            var distanceToCenter = 0f
            if (isCurved) {
                val ratio = (drawnCenterY - abs(drawnCenterY - mDrawnItemCenterY) - rectDrawn.top) * 1f / (drawnCenterY - rectDrawn.top)
                val unit = when {
                    mDrawnItemCenterY > drawnCenterY -> 1
                    mDrawnItemCenterY < drawnCenterY -> -1
                    else -> 0
                }
                val degree = clamp(-(1 - ratio) * mMaxAngle * unit)
                distanceToCenter = computeYCoordinateAtAngle(degree)
                val transX = when (mItemAlign) {
                    Paint.Align.LEFT -> rectDrawn.left.toFloat()
                    Paint.Align.RIGHT -> rectDrawn.right.toFloat()
                    else -> wheelCenterX.toFloat()
                }
                val transY = wheelCenterY - distanceToCenter
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
            }
            paint.color = mItemTextColor
            if (hasAtmospheric) {
                paint.alpha = max(((drawnCenterY - abs(drawnCenterY - mDrawnItemCenterY)) * 1f / drawnCenterY * 255).toInt(), 0)
            }

            val drawnCenterY = if (isCurved) drawnCenterY - distanceToCenter else mDrawnItemCenterY.toFloat()
            if (mSelectedItemTextColor != -1) {
                canvas.save()
                if (isCurved) canvas.concat(matrixRotate)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canvas.clipOutRect(rectCurrentItem)
                } else {
                    canvas.clipRect(rectCurrentItem, Region.Op.DIFFERENCE)
                }
                canvas.drawText(data, drawnCenterX.toFloat(), drawnCenterY, paint)
                canvas.restore()
                paint.color = mSelectedItemTextColor
                canvas.save()
                if (isCurved) canvas.concat(matrixRotate)
                canvas.clipRect(rectCurrentItem)
                canvas.drawText(data, drawnCenterX.toFloat(), drawnCenterY, paint)
                canvas.restore()
            } else {
                canvas.save()
                canvas.clipRect(rectDrawn)
                if (isCurved) canvas.concat(matrixRotate)
                canvas.drawText(data, drawnCenterX.toFloat(), drawnCenterY, paint)
                canvas.restore()
            }
            drawnDataPos++
            drawnOffsetPos++
        }
    }

    private fun computeYCoordinateAtAngle(degree: Float): Float {
        return sinDegree(degree) / sinDegree(mMaxAngle) * mHalfWheelHeight
    }

    private fun sinDegree(degree: Float): Float {
        return sin(Math.toRadians(degree.toDouble())).toFloat()
    }

    private fun computeDepth(degree: Float): Float {
        return (mHalfWheelHeight - cos(Math.toRadians(degree.toDouble())) * mHalfWheelHeight).toFloat()
    }

    private fun clamp(value: Float): Float {
        return when {
            value < -mMaxAngle -> -mMaxAngle
            value > mMaxAngle -> mMaxAngle
            else -> value
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    if (tracker == null) {
                        tracker = VelocityTracker.obtain()
                    } else {
                        tracker!!.clear()
                    }
                    tracker!!.addMovement(event)
                    if (!scroller.isFinished) {
                        scroller.abortAnimation()
                        isForceFinishScroll = true
                    }
                    lastPointY = event.y.toInt()
                    downPointY = lastPointY
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(downPointY - event.y) < touchSlop && computeDistanceToEndPoint(scroller.finalY % mItemHeight) > 0) {
                        isClick = true
                    } else {
                        isClick = false
                        tracker!!.addMovement(event)
                        val move = event.y - lastPointY
                        if (abs(move) >= 1) {
                            scrollOffsetY += move.toInt()
                            lastPointY = event.y.toInt()
                            invalidate()
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (!isClick) {
                        tracker!!.addMovement(event)
                        tracker!!.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                        isForceFinishScroll = false
                        val velocity = tracker!!.yVelocity.toInt()
                        if (abs(velocity) > minimumVelocity) {
                            scroller.fling(0, scrollOffsetY, 0, velocity, 0, 0, minFlingY, maxFlingY)
                            scroller.finalY = scroller.finalY + computeDistanceToEndPoint(scroller.finalY % mItemHeight)
                        } else {
                            scroller.startScroll(0, scrollOffsetY, 0, computeDistanceToEndPoint(scrollOffsetY % mItemHeight))
                        }
                        if (!isCyclic) {
                            if (scroller.finalY > maxFlingY) {
                                scroller.finalY = maxFlingY
                            } else if (scroller.finalY < minFlingY) {
                                scroller.finalY = minFlingY
                            }
                        }
                        mHandler.post(mRunnable)
                        tracker!!.recycle()
                        tracker = null
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (tracker != null) {
                        tracker!!.recycle()
                        tracker = null
                    }
                }
            }
        }
        return true
    }

    private fun computeDistanceToEndPoint(remainder: Int): Int {
        return if (abs(remainder) > mHalfItemHeight) {
            if (scrollOffsetY < 0) {
                -mItemHeight - remainder
            } else {
                mItemHeight - remainder
            }
        } else {
            -remainder
        }
    }

    fun setOnItemSelectedListener(listener: IOnItemSelectedListener): WheelPicker {
        onItemSelectedListener = listener
        return this
    }

    fun getCurrentItemPosition(): Int {
        return currentItemPosition
    }

    fun setCurrentItemPosition(position: Int): WheelPicker {
        currentItemPosition = max(min(position, dataList.lastIndex), 0)
        selectedItemPosition = currentItemPosition
        scrollOffsetY = 0
        computeFlingLimitY()
        requestLayout()
        invalidate()
        return this
    }

    fun scrollTo(position: Int) {
        if (position != currentItemPosition) {
            val differencesLines = currentItemPosition - position
            val newScrollOffsetY = scrollOffsetY + differencesLines * mItemHeight // % adapter.getItemCount();
            ValueAnimator.ofInt(scrollOffsetY, newScrollOffsetY).apply {
                duration = 300
                addUpdateListener { animation ->
                    scrollOffsetY = animation.animatedValue as Int
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        currentItemPosition = position
                        onItemSelectedListener?.onItemSelected(currentItemPosition)
                    }
                })
                start()
            }
        }
    }

    fun notifyDataSetChanged() {
        if (selectedItemPosition > dataList.size - 1 || currentItemPosition > dataList.size - 1) {
            currentItemPosition = dataList.size - 1
        }
        selectedItemPosition = currentItemPosition
        scrollOffsetY = 0
        computeTextSize()
        computeFlingLimitY()
        requestLayout()
        postInvalidate()
    }

    fun interface IOnItemSelectedListener {
        fun onItemSelected(position: Int)
    }
}