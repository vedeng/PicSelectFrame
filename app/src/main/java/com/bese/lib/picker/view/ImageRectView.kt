package com.bese.lib.picker.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewParent
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bese.lib.picker.R


/**
 * Square ImageView Component
 *      View allowed ImageView show in a square area, which will resolved the image variable.
 *      View can also defined the ratio to show{formula: ratio = height / width}.
 *      Inverse ratio will be compiled to negative number that apply to {Inverse Proportional Function}.
 *
 * @attr squareRatio R.SquareImageView_squareRatio
 * @author Fires 2019.12.28
 */
class ImageRectView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var mRatio = 1f

    private var hasCorner = false

    private var mPath: Path = Path()
    private var mPaint: Paint = Paint()

    private var radius = floatArrayOf()

    init {

        mPath.fillType = Path.FillType.INVERSE_EVEN_ODD
        mPaint.isAntiAlias = true

        scaleType = ScaleType.CENTER_CROP

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ImageRectView)
        val ratio = typedArray.getFloat(R.styleable.ImageRectView_squareRatio, 1f)
        val rad = typedArray.getDimension(R.styleable.ImageRectView_squareRadius, 0f)
        var rad1 = typedArray.getDimension(R.styleable.ImageRectView_squareLeftTopRadius, 0f)
        var rad2 = typedArray.getDimension(R.styleable.ImageRectView_squareRightTopRadius, 0f)
        var rad3 = typedArray.getDimension(R.styleable.ImageRectView_squareRightBottomRadius, 0f)
        var rad4 = typedArray.getDimension(R.styleable.ImageRectView_squareLeftBottomRadius, 0f)

        setRatio(ratio, false)


        if (rad > 0) {
            radius = floatArrayOf(rad, rad, rad, rad, rad, rad, rad, rad)
            hasCorner = true
        } else {
            rad1 = if (rad1 > 0) rad1 else 0f
            rad2 = if (rad2 > 0) rad2 else 0f
            rad3 = if (rad3 > 0) rad3 else 0f
            rad4 = if (rad4 > 0) rad4 else 0f
            radius = floatArrayOf(rad1, rad1, rad2, rad2, rad3, rad3, rad4, rad4)
            hasCorner = rad1 > 0 || rad2 > 0 || rad3 > 0 || rad > 0
        }

        setPadding(0, 0, 0, 0)

        // recycle the attribute asset.
        typedArray.recycle()
    }

    /** set ratio value */
    fun setRatio(ratio: Float, reDraw: Boolean = true) {
        // zero is not support in there, default value is 1.
        if (ratio == 0f) mRatio = 1f
        // negative number will be compile to {Inverse Proportional Number(etc: -2 will be convert to 0.5)}.
        mRatio = if (ratio < 0f) (-1 / ratio) else ratio
        // it needn't requestLayout when drawing this view,but need requestLayout when change this View.
        updateView(reDraw)
        if (reDraw) requestLayout()
    }

    fun getRatio() : Float {
        return mRatio
    }

    /** set universal radius value */
    fun setRadius(rad: Float) {
        setRadius(rad, rad, rad, rad)
    }

    /** set per radius value */
    fun setRadius(leftTop: Float, rightTop: Float, rightBottom: Float, leftBottom: Float) {
        radius = floatArrayOf(leftTop, leftTop, rightTop, rightTop, rightBottom, rightBottom, leftBottom, leftBottom)
        hasCorner = leftTop > 0 || rightTop > 0 || rightBottom > 0 || leftBottom > 0
        updateView()
    }

    /** variable value will request update view to show */
    private fun updateView(reDraw: Boolean = true) {
        /**
         * api < 14 invalidate function will spark onDraw function, api >=14 will not.
         * {class suggest use in static state view.}
         * this function may be not work in trend view.
         */
        if (reDraw) invalidate()
    }

    /**
     * padding value is unsupported in this view. Unpredictable size in paddingBottom.
     *   So there need reset the padding value to disable giving numbers.
     */
    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(0, 0, 0, 0)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
        val heightSize = MeasureSpec.makeMeasureSpec((measuredWidth * mRatio).toInt(), MeasureSpec.EXACTLY)
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (hasCorner) {
            mPath.reset()
            mPath.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radius, Path.Direction.CCW)
            mPaint.color = getCurrentBgColor()

            canvas?.save()
            canvas?.drawPath(mPath, mPaint)
            canvas?.restore()
        }
    }

    private fun getCurrentBgColor() : Int {
        var paintColor: Int = getPaintColor(parent)
        if (Color.TRANSPARENT == paintColor) {
            // get theme background color
            val array = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            paintColor = array.getColor(0, Color.TRANSPARENT)
            array.recycle()
        }
        return paintColor
    }

    private fun getPaintColor(vp: ViewParent?): Int {
        vp?.run {
            if (this is View) {
                val parentView = vp as? View
                val color: Int = getViewBackgroundColor(parentView)
                if (Color.TRANSPARENT != color) {
                    return color
                } else {
                    getPaintColor(parentView?.parent)
                }
            }
        }
        return Color.TRANSPARENT
    }

    private fun getViewBackgroundColor(view: View?): Int {
        view?.run {
            val drawable = background
            background?.run {
                val bgCls = drawable.javaClass as? Class<Drawable>
                try {
                    val field = bgCls?.getDeclaredField("mColorState")
                    field?.isAccessible = true
                    val colorState = field?.get(drawable)
                    val colorStateClass: Class<Any>? = colorState?.javaClass
                    val colorStateField = colorStateClass?.getDeclaredField("mUseColor")
                    colorStateField?.isAccessible = true
                    val viewColor = colorStateField?.get(colorState) as? Int
                    if (Color.TRANSPARENT != viewColor) {
                        return viewColor ?: Color.TRANSPARENT
                    }
                } catch (e: NoSuchFieldException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
        return Color.TRANSPARENT
    }

}