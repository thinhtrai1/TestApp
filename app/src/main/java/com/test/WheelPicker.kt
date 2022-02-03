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

class WheelPicker(context: Context, attrs: AttributeSet) : View(context, attrs) {
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
    private var isCyclic = true
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
                position = if (position < 0) position + itemCount else position
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
        style = Paint.Style.FILL
    }

    private fun Int.toPx() = this * resources.displayMetrics.density

    init {
        val conf = ViewConfiguration.get(getContext())
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

        // Correct sizes of original content
        var resultWidth = mTextMaxWidth
        var resultHeight = mTextMaxHeight * mVisibleItemCount + mItemSpace * (mVisibleItemCount - 1)

        // Correct view sizes again if curved is enable
        if (isCurved) {
            // The text is written on the circle circumference from -mMaxAngle to mMaxAngle.
            // 2 * sinDegree(mMaxAngle): Height of drawn circle
            // Math.PI: Circumference of half unit circle, `mMaxAngle / 90f`: The ratio of half-circle we draw on
            resultHeight = (2 * sinDegree(mMaxAngle) / (Math.PI * mMaxAngle / 90f) * resultHeight).toInt()
        }

        // Consideration padding influence the view sizes
        resultWidth += paddingLeft + paddingRight
        resultHeight += paddingTop + paddingBottom

        // Consideration sizes of parent can influence the view sizes
        resultWidth = measureSize(modeWidth, sizeWidth, resultWidth)
        resultHeight = measureSize(modeHeight, sizeHeight, resultHeight)
        setMeasuredDimension(resultWidth, resultHeight)
    }

    private fun measureSize(mode: Int, sizeExpect: Int, sizeActual: Int): Int {
        return if (mode == MeasureSpec.EXACTLY) {
            sizeExpect
        } else {
            if (mode == MeasureSpec.AT_MOST) {
                min(sizeActual, sizeExpect)
            } else {
                sizeActual
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        // Set content region
        rectDrawn.set(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)

        // Get the center coordinates of content region
        wheelCenterX = rectDrawn.centerX()
        wheelCenterY = rectDrawn.centerY()

        // Correct item drawn center
        computeDrawnCenter()
        mHalfWheelHeight = rectDrawn.height() / 2
        mItemHeight = rectDrawn.height() / mVisibleItemCount
        mHalfItemHeight = mItemHeight / 2

        // Initialize fling max Y-coordinates
        computeFlingLimitY()

        // Correct region of current select item
        computeCurrentItemRect()
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
        val drawnDataStartPos = -scrollOffsetY / mItemHeight - mHalfDrawnItemCount
        var drawnDataPos = drawnDataStartPos + selectedItemPosition
        var drawnOffsetPos = -mHalfDrawnItemCount
        while (drawnDataPos < drawnDataStartPos + selectedItemPosition + mDrawnItemCount) {
            val data = getItem(drawnDataPos)
            val mDrawnItemCenterY = drawnCenterY + drawnOffsetPos * mItemHeight + scrollOffsetY % mItemHeight
            var distanceToCenter = 0f
            if (isCurved) {
                // Correct ratio of item's drawn center to wheel center
                val ratio = (drawnCenterY - abs(drawnCenterY - mDrawnItemCenterY) - rectDrawn.top) * 1f / (drawnCenterY - rectDrawn.top)

                // Correct unit
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
            // Correct item's drawn centerY base on curved state
            val drawnCenterY = if (isCurved) drawnCenterY - distanceToCenter else mDrawnItemCenterY.toFloat()

            // Judges need to draw different color for current item or not
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
        // Compute y-coordinate for item at degree. mMaxAngle is at mHalfWheelHeight
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

                        // Scroll WheelPicker's content
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

                        // Judges the WheelPicker is scroll or fling base on current velocity
                        isForceFinishScroll = false
                        val velocity = tracker!!.yVelocity.toInt()
                        if (abs(velocity) > minimumVelocity) {
                            scroller.fling(0, scrollOffsetY, 0, velocity, 0, 0, minFlingY, maxFlingY)
                            scroller.finalY = scroller.finalY + computeDistanceToEndPoint(scroller.finalY % mItemHeight)
                        } else {
                            scroller.startScroll(0, scrollOffsetY, 0, computeDistanceToEndPoint(scrollOffsetY % mItemHeight))
                        }
                        // Correct coordinates
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