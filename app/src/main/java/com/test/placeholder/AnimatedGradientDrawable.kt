package com.test.placeholder

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnDetach
import androidx.core.view.forEach
import com.test.toPx
import java.lang.ref.WeakReference

class AnimatedGradientDrawable(view: View, gradientShape: GradientShape = GradientShape.Rectangle) : ShapeDrawable() {
    private val colors = intArrayOf(
        Color.parseColor("#FFE3E3E3"),
        Color.parseColor("#FFCCCCCC"),
        Color.parseColor("#FFE3E3E3")
    )
    private val view = WeakReference(view)
    private val currentBackground = view.background
    private var shapeWidth = 0
    private var startCoordinate = 0f
    private var valueAnimator: ValueAnimator? = null

    init {
        shape = when (gradientShape) {
            is GradientShape.Oval -> OvalShape()
            is GradientShape.Rectangle -> RectShape()
            is GradientShape.RoundRectangle -> RoundRectShape(FloatArray(8) { gradientShape.radius }, null, null)
        }
    }

    override fun draw(canvas: Canvas) {
        if (view.get()?.background != this) {
            cancelAnimation()
            return
        }
        if (valueAnimator == null) {
            shapeWidth = bounds.width()
            valueAnimator = ValueAnimator.ofInt(-shapeWidth, shapeWidth).apply {
                duration = 1000
                repeatMode = ValueAnimator.RESTART
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    startCoordinate = (it.animatedValue as Int).toFloat()
                    invalidateSelf()
                }
                start()
            }
        } else {
            paint.shader = LinearGradient(
                startCoordinate,
                0f,
                startCoordinate + shapeWidth.toFloat(),
                0f,
                colors,
                null,
                Shader.TileMode.CLAMP
            )
            shape.draw(canvas, paint)
        }
    }

    fun attach() {
        view.get()?.background = this
        view.get()?.doOnDetach {
            cancelAnimation()
        }
        cancelAnimation()
        invalidateSelf()
    }

    fun detach() {
        view.get()?.background = currentBackground
        cancelAnimation()
    }

    private fun cancelAnimation() {
        valueAnimator?.cancel()
        valueAnimator = null
    }

    companion object {
        fun attachViewGroup(view: ViewGroup) {
            AnimatedGradientDrawable(view).attach()
            view.forEach {
                AnimatedGradientDrawable(it).attach()
            }
        }

        fun detachViewGroup(view: ViewGroup) {
            (view.background as? AnimatedGradientDrawable)?.detach()
            view.forEach {
                (it.background as? AnimatedGradientDrawable)?.detach()
            }
        }

        fun isAttached(view: View): Boolean {
            return view.background is AnimatedGradientDrawable
        }

        fun isAttached(view: ViewGroup): Boolean {
            view.forEach {
                if (it.background is AnimatedGradientDrawable) return true
            }
            return false
        }
    }

    sealed class GradientShape {
        object Oval: GradientShape()
        object Rectangle: GradientShape()
        data class RoundRectangle(val radius: Float = 8.toPx()): GradientShape()
    }
}