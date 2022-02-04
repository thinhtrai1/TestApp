package com.test

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

class SpotlightView(private val activity: Activity) : View(activity) {

    private class Spotlight(val type: SpotlightType, val focusView: View, val tutorialView: View, val isTutorialInTop: Boolean) {
        val rect = RectF()
    }

    sealed class SpotlightType {
        object Circle : SpotlightType()
        class Rectangle(val radius: Int = 0) : SpotlightType()
    }

    var currentPosition = 0
    private val spotlights = ArrayList<Spotlight>()
    private var spotlightAnimator: ValueAnimator? = null
    private var reverseSpotlightAnimator: ValueAnimator? = null
    private val reverseRect = RectF()
    private val statusBarHeight = resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"))
    private var mLeft = 0f
    private var mTop = 0f
    private var mRight = 0f
    private var mBottom = 0f
    private var mPadding = 8.toPx()
    private val paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setOnClickListener {
            currentPosition++
            if (currentPosition < spotlights.size) {
                show()
            } else {
                dismiss()
            }
        }
    }

    fun add(
        type: SpotlightType,
        focusView: View,
        tutorialView: View,
        isTutorialInTop: Boolean = false
    ): SpotlightView {
        spotlights.add(Spotlight(type, focusView, tutorialView, isTutorialInTop))
        return this
    }

    fun start() {
        currentPosition = 0
        activity.addContentView(this, ViewGroup.LayoutParams(-1, -1))
        show()
    }

    fun dismiss() {
        activity.findViewById<ViewGroup>(android.R.id.content).removeView(this)
    }

    fun show() {
        spotlights[currentPosition].apply {
            tutorialView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            tutorialView.layout(0, 0, tutorialView.measuredWidth, tutorialView.measuredHeight)
            val loc = IntArray(2).also { focusView.getLocationOnScreen(it) }[1] - statusBarHeight
            mLeft = focusView.left - mPadding
            mTop = loc - mPadding
            mRight = focusView.right + mPadding
            mBottom = loc + focusView.height + mPadding
            val centerX = (mRight + mLeft) / 2
            val centerY = (mBottom + mTop) / 2
            val targetX = (mRight - mLeft) / 2
            val targetY = (mBottom - mTop) / 2
            spotlightAnimator = ValueAnimator.ofFloat(0f, targetX).apply {
                addUpdateListener {
                    val value = it.animatedValue as Float
                    rect.set(
                        centerX - value,
                        centerY - value * targetY / targetX,
                        centerX + value,
                        centerY + value * targetY / targetX
                    )
                    invalidate()
                }
                start()
            }
            reverseSpotlightAnimator?.start()
            reverseSpotlightAnimator = ValueAnimator.ofFloat(targetX, 0f).apply {
                addUpdateListener {
                    val value = it.animatedValue as Float
                    reverseRect.set(
                        centerX - value,
                        centerY - value * targetY / targetX,
                        centerX + value,
                        centerY + value * targetY / targetX
                    )
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#80000000"))
        spotlights[currentPosition].let { spotlight ->
            when (spotlight.type) {
                is SpotlightType.Circle -> canvas.drawCircle(
                    spotlight.rect.centerX(),
                    spotlight.rect.centerY(),
                    max(spotlight.rect.width(), spotlight.rect.height()) / 2,
                    paint
                )
                is SpotlightType.Rectangle -> canvas.drawRoundRect(
                    spotlight.rect,
                    spotlight.type.radius.toPx(),
                    spotlight.type.radius.toPx(),
                    paint
                )
            }
            spotlight.tutorialView.let { view ->
                canvas.save()
                if (spotlight.isTutorialInTop) {
                    canvas.translate((mRight + mLeft - view.measuredWidth) / 2, mTop - view.measuredHeight - mPadding)
                } else {
                    canvas.translate((mRight + mLeft - view.measuredWidth) / 2, mBottom + mPadding)
                }
                view.draw(canvas)
                canvas.restore()
            }
        }
        if (currentPosition > 0) spotlights[currentPosition - 1].let { spotlight ->
            when (spotlight.type) {
                is SpotlightType.Circle -> canvas.drawCircle(
                    reverseRect.centerX(),
                    reverseRect.centerY(),
                    max(reverseRect.width(), reverseRect.height()) / 2,
                    paint
                )
                is SpotlightType.Rectangle -> canvas.drawRoundRect(
                    reverseRect,
                    spotlight.type.radius.toPx(),
                    spotlight.type.radius.toPx(),
                    paint
                )
            }
        }
    }
}