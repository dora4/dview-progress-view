package dora.widget

import android.animation.Animator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import dora.widget.progressview.R

class DoraProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progressBgRect = RectF()
    private var progressBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressHoverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressWidth = 10f
    private var progressBgColor = 0
    private var progressHoverColor = 0
    private var progressType = PROGRESS_TYPE_LINE
    private var progressOrigin = PROGRESS_ORIGIN_LEFT
    private var percentRate = 0f
    private var angle = 0
    private var animationTime = 0
    private var paintCap: Paint.Cap = Paint.Cap.SQUARE
    private var animator: ValueAnimator? = null
    private var listener: OnProgressCompleteListener? = null

    private fun initAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.DoraProgressView,
            defStyleAttr,
            0
        )
        when (a.getInt(R.styleable.DoraProgressView_dview_progressType, PROGRESS_TYPE_LINE)) {
            0 -> progressType = PROGRESS_TYPE_LINE
            1 -> progressType = PROGRESS_TYPE_SEMICIRCLE
            2 -> progressType = PROGRESS_TYPE_SEMICIRCLE_REVERSE
            3 -> progressType = PROGRESS_TYPE_CIRCLE
            4 -> progressType = PROGRESS_TYPE_CIRCLE_REVERSE
        }
        when (a.getInt(R.styleable.DoraProgressView_dview_progressOrigin, PROGRESS_ORIGIN_LEFT)) {
            0 -> progressOrigin = PROGRESS_ORIGIN_LEFT
            1 -> progressOrigin = PROGRESS_ORIGIN_TOP
            2 -> progressOrigin = PROGRESS_ORIGIN_RIGHT
            3 -> progressOrigin = PROGRESS_ORIGIN_BOTTOM
        }
        when(a.getInt(R.styleable.DoraProgressView_dview_paintCap, 0)) {
            0 -> paintCap = Paint.Cap.SQUARE
            1 -> paintCap = Paint.Cap.ROUND
        }
        progressWidth = a.getDimension(R.styleable.DoraProgressView_dview_progressWidth, 30f)
        progressBgColor =
            a.getColor(R.styleable.DoraProgressView_dview_progressBgColor, Color.GRAY)
        progressHoverColor =
            a.getColor(R.styleable.DoraProgressView_dview_progressHoverColor, Color.BLUE)
        animationTime = a.getInt(R.styleable.DoraProgressView_dview_animationTime, 1000)
        a.recycle()
    }

    private fun initPaints() {
        progressBgPaint.style = Paint.Style.STROKE
        progressBgPaint.strokeCap = paintCap
        progressBgPaint.color = progressBgColor
        progressBgPaint.strokeWidth = progressWidth
        progressHoverPaint.style = Paint.Style.STROKE
        progressHoverPaint.strokeCap = paintCap
        progressHoverPaint.color = progressHoverColor
        progressHoverPaint.strokeWidth = progressWidth
    }

    fun reset() {
        percentRate = 0f
        animator?.cancel()
    }

    /**
     * 两次调用间隔需要大于animationTime。
     *
     * @param rate 0~1的小数
     */
    fun setPercentRate(rate: Float) {
        if (animator == null) {
            animator = ValueAnimator.ofObject(
                AnimationEvaluator(),
                percentRate,
                rate
            )
        }
        animator?.addUpdateListener { animation: ValueAnimator ->
            val value = animation.animatedValue as Float
            angle =
                if (progressType == PROGRESS_TYPE_CIRCLE || progressType == PROGRESS_TYPE_CIRCLE_REVERSE) {
                    (value * 360).toInt()
                } else if (progressType == PROGRESS_TYPE_SEMICIRCLE || progressType == PROGRESS_TYPE_SEMICIRCLE_REVERSE) {
                    (value * 180).toInt()
                } else {
                    0   // 线不需要求角度
                }
            percentRate = value
            invalidate()
        }
        animator?.interpolator = LinearInterpolator()
        animator?.setDuration(animationTime.toLong())?.start()
        animator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                percentRate = rate
                listener?.onComplete()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    interface OnProgressCompleteListener {
        fun onComplete()
    }

    fun setOnProgressCompleteListener(listener: OnProgressCompleteListener) {
        this.listener = listener
    }


    private inner class AnimationEvaluator : TypeEvaluator<Float> {
        override fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
            return if (endValue > startValue) {
                startValue + fraction * (endValue - startValue)
            } else {
                startValue - fraction * (startValue - endValue)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        progressBgPaint.strokeWidth = progressWidth
        progressHoverPaint.strokeWidth = progressWidth
        if (progressType == PROGRESS_TYPE_LINE) {
            // 线
            var left = 0f
            var top = 0f
            var right = measuredWidth.toFloat()
            var bottom = measuredHeight.toFloat()
            val isHorizontal = when(progressOrigin) {
                PROGRESS_ORIGIN_LEFT, PROGRESS_ORIGIN_RIGHT -> true
                else -> false
            }
            if (isHorizontal) {
                top = (measuredHeight - progressWidth) / 2
                bottom = (measuredHeight + progressWidth) / 2
                progressBgRect[left + progressWidth / 2, top, right - progressWidth / 2] = bottom
            } else {
                left = (measuredWidth - progressWidth) / 2
                right = (measuredWidth + progressWidth) / 2
                progressBgRect[left, top + progressWidth / 2, right] = bottom - progressWidth / 2
            }
        } else if (progressType == PROGRESS_TYPE_CIRCLE || progressType == PROGRESS_TYPE_CIRCLE_REVERSE) {
            // 圆
            var left = 0f
            val top = 0f
            var right = measuredWidth
            var bottom = measuredHeight
            progressBgRect[left + progressWidth / 2, top + progressWidth / 2, right - progressWidth / 2] =
                bottom - progressWidth / 2
        } else {
            // 半圆
            val isHorizontal = when(progressOrigin) {
                PROGRESS_ORIGIN_LEFT, PROGRESS_ORIGIN_RIGHT -> true
                else -> false
            }
            val min = measuredWidth.coerceAtMost(measuredHeight)
            var left = 0f
            var top = 0f
            var right = 0f
            var bottom = 0f
            if (isHorizontal) {
                if (measuredWidth >= min) {
                    left = ((measuredWidth - min) / 2).toFloat()
                    right = left + min
                }
                if (measuredHeight >= min) {
                    bottom = top + min
                }
                progressBgRect[left + progressWidth / 2, top + progressWidth / 2, right - progressWidth / 2] =
                    bottom - progressWidth / 2
                setMeasuredDimension(
                    MeasureSpec.makeMeasureSpec(
                        (right - left).toInt(),
                        MeasureSpec.EXACTLY
                    ),
                    MeasureSpec.makeMeasureSpec(
                        (bottom - top + progressWidth).toInt() / 2,
                        MeasureSpec.EXACTLY
                    )
                )
            } else {
                if (measuredWidth >= min) {
                    right = left + min
                }
                if (measuredHeight >= min) {
                    top = ((measuredHeight - min) / 2).toFloat()
                    bottom = top + min
                }
                progressBgRect[left + progressWidth / 2, top + progressWidth / 2, right - progressWidth / 2] =
                    bottom - progressWidth / 2
                setMeasuredDimension(
                    MeasureSpec.makeMeasureSpec(
                        (right - left + progressWidth).toInt() / 2,
                        MeasureSpec.EXACTLY
                    ),
                    MeasureSpec.makeMeasureSpec(
                        (bottom - top).toInt(),
                        MeasureSpec.EXACTLY
                    )
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (progressType == PROGRESS_TYPE_LINE) {
            val isHorizontal = when(progressOrigin) {
                PROGRESS_ORIGIN_LEFT, PROGRESS_ORIGIN_RIGHT -> true
                else -> false
            }
            if (isHorizontal) {
                canvas.drawLine(
                    progressBgRect.left,
                    measuredHeight / 2f,
                    progressBgRect.right,
                    measuredHeight / 2f,
                    progressBgPaint)
            } else {
                canvas.drawLine(measuredWidth / 2f,
                    progressBgRect.top,
                                        measuredWidth / 2f,
                    progressBgRect.bottom, progressBgPaint)
            }
            if (percentRate > 0) {
                when (progressOrigin) {
                    PROGRESS_ORIGIN_LEFT -> {
                        canvas.drawLine(
                            progressBgRect.left,
                            measuredHeight / 2f,
                            (progressBgRect.right) * percentRate,
                            measuredHeight / 2f,
                            progressHoverPaint
                        )
                    }
                    PROGRESS_ORIGIN_TOP -> {
                        canvas.drawLine(measuredWidth / 2f,
                            progressBgRect.top,
                            measuredWidth / 2f,
                            (progressBgRect.bottom) * percentRate,
                            progressHoverPaint)
                    }
                    PROGRESS_ORIGIN_RIGHT -> {
                        canvas.drawLine(
                            progressWidth / 2 + (progressBgRect.right) * (1 - percentRate),
                            measuredHeight / 2f,
                            progressBgRect.right,
                            measuredHeight / 2f,
                            progressHoverPaint
                        )
                    }
                    PROGRESS_ORIGIN_BOTTOM -> {
                        canvas.drawLine(measuredWidth / 2f,
                            progressWidth / 2 + (progressBgRect.bottom) * (1 - percentRate),
                        measuredWidth / 2f,
                            progressBgRect.bottom,
                        progressHoverPaint)
                    }
                }
            }
        } else if (progressType == PROGRESS_TYPE_SEMICIRCLE) {
            if (progressOrigin == PROGRESS_ORIGIN_LEFT) {
                // PI ~ 2PI
                canvas.drawArc(progressBgRect, 180f, 180f, false, progressBgPaint)
                canvas.drawArc(
                    progressBgRect,
                    180f,
                    angle.toFloat(),
                    false,
                    progressHoverPaint
                )
            } else if (progressOrigin == PROGRESS_ORIGIN_TOP) {
                canvas.translate(-progressBgRect.width() / 2, 0f)
                // 3/2PI ~ 2PI, 0 ~ PI/2
                canvas.drawArc(progressBgRect, 270f, 180f, false, progressBgPaint)
                canvas.drawArc(
                    progressBgRect,
                    270f,
                    angle.toFloat(),
                    false,
                    progressHoverPaint
                )
            } else if (progressOrigin == PROGRESS_ORIGIN_RIGHT) {
                canvas.translate(0f, -progressBgRect.height() / 2)
                // 2PI ~ PI
                canvas.drawArc(progressBgRect, 0f, 180f, false, progressBgPaint)
                canvas.drawArc(
                    progressBgRect,
                    0f,
                    angle.toFloat(),
                    false,
                    progressHoverPaint
                )
            } else if (progressOrigin == PROGRESS_ORIGIN_BOTTOM) {
                // PI/2 ~ 3/2PI
                canvas.drawArc(progressBgRect, 90f, 180f, false, progressBgPaint)
                canvas.drawArc(
                    progressBgRect,
                    90f,
                    angle.toFloat(),
                    false,
                    progressHoverPaint
                )
            }
        } else if (progressType == PROGRESS_TYPE_SEMICIRCLE_REVERSE) {
            if (progressOrigin == PROGRESS_ORIGIN_LEFT) {
                canvas.translate(0f, -progressBgRect.height() / 2)
                // PI ~ 2PI
                canvas.drawArc(progressBgRect, 180f, -180f, false, progressBgPaint)
                canvas.drawArc(
                    progressBgRect,
                    180f,
                    -angle.toFloat(),
                    false,
                    progressHoverPaint
                )
            } else if (progressOrigin == PROGRESS_ORIGIN_TOP) {
                // 3/2PI ~ PI/2
                canvas.drawArc(progressBgRect, 270f, -180f, false, progressBgPaint)
                canvas.drawArc(
                    progressBgRect,
                    270f,
                    -angle.toFloat(),
                    false,
                    progressHoverPaint
                )
            } else if (progressOrigin == PROGRESS_ORIGIN_RIGHT) {
                // 2PI ~ PI
                canvas.drawArc(progressBgRect, 0f, -180f, false, progressBgPaint)
                canvas.drawArc(
                    progressBgRect,
                    0f,
                    -angle.toFloat(),
                    false,
                    progressHoverPaint
                )
            } else if (progressOrigin == PROGRESS_ORIGIN_BOTTOM) {
                canvas.translate(-progressBgRect.width() / 2, 0f)
                // PI/2 ~ 2PI, 2PI ~ 3/2PI
                canvas.drawArc(progressBgRect, 90f, -180f, false, progressBgPaint)
                canvas.drawArc(
                    progressBgRect,
                    90f,
                    -angle.toFloat(),
                    false,
                    progressHoverPaint
                )
            }
        } else if (progressType == PROGRESS_TYPE_CIRCLE) {
            val deltaAngle = if (progressOrigin == PROGRESS_ORIGIN_TOP) {
                90f
            } else if (progressOrigin == PROGRESS_ORIGIN_RIGHT) {
                180f
            } else if (progressOrigin == PROGRESS_ORIGIN_BOTTOM) {
                270f
            } else {
                0f
            }
            canvas.drawArc(progressBgRect, 0f, 360f, false, progressBgPaint)
            canvas.drawArc(
                progressBgRect,
                180f + deltaAngle,
                angle.toFloat(),
                false,
                progressHoverPaint
            )
        } else if (progressType == PROGRESS_TYPE_CIRCLE_REVERSE) {
            val deltaAngle = if (progressOrigin == PROGRESS_ORIGIN_TOP) {
                90f
            } else if (progressOrigin == PROGRESS_ORIGIN_RIGHT) {
                180f
            } else if (progressOrigin == PROGRESS_ORIGIN_BOTTOM) {
                270f
            } else {
                0f
            }
            canvas.drawArc(progressBgRect, 0f, 360f, false, progressBgPaint)
            canvas.drawArc(
                progressBgRect,
                180f + deltaAngle,
                -angle.toFloat(),
                false,
                progressHoverPaint
            )
        }
    }

    companion object {

        const val PROGRESS_TYPE_LINE = 0
        const val PROGRESS_TYPE_SEMICIRCLE = 1
        const val PROGRESS_TYPE_SEMICIRCLE_REVERSE = 2
        const val PROGRESS_TYPE_CIRCLE = 3
        const val PROGRESS_TYPE_CIRCLE_REVERSE = 4

        const val PROGRESS_ORIGIN_LEFT = 0
        const val PROGRESS_ORIGIN_TOP = 1
        const val PROGRESS_ORIGIN_RIGHT = 2
        const val PROGRESS_ORIGIN_BOTTOM = 3
    }

    init {
        initAttrs(context, attrs, defStyleAttr)
        initPaints()
    }
}
