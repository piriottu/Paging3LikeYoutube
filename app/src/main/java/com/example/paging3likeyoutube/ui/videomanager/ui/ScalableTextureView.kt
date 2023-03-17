package com.example.paging3likeyoutube.ui.videomanager.ui

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import com.example.paging3likeyoutube.ui.videomanager.ui.ScalableTextureView.ScaleType.*

/**
 * This extension of [TextureView] is created to isolate scaling of this view.
 */
abstract class ScalableTextureView : TextureView {
    protected var contentWidth: Int? = null
        private set
    protected var contentHeight: Int? = null
        private set
    private var mPivotPointX = 0f
    private var mPivotPointY = 0f
    private var mContentScaleX = 1f
    private var mContentScaleY = 1f
    private var mContentRotation = 0f
    private var mContentScaleMultiplier = 1f
    private var mContentX = 0
    private var mContentY = 0
    private val mTransformMatrix = Matrix()
    private var mScaleType: ScaleType? = null

    enum class ScaleType {
        CENTER_CROP, TOP, BOTTOM, FILL
    }

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context!!, attrs, defStyleAttr, defStyleRes
    ) {
    }

    fun setScaleType(scaleType: ScaleType?) {
        mScaleType = scaleType
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (SHOW_LOGS) Log.v(
            TAG,
            "onMeasure, mContentoWidth " + contentWidth + ", mContentHeight " + contentHeight
        )
        if (contentWidth != null && contentHeight != null) {
            updateTextureViewSize()
        }
    }

    fun updateTextureViewSize() {
        if (SHOW_LOGS) Log.d(TAG, ">> updateTextureViewSize")
        if (contentWidth == null || contentHeight == null) {
            throw RuntimeException("null content size")
        }
        val viewWidth = measuredWidth.toFloat()
        val viewHeight = measuredHeight.toFloat()
        val contentWidth = contentWidth!!.toFloat()
        val contentHeight = contentHeight!!.toFloat()
        if (SHOW_LOGS) {
            Log.v(
                TAG,
                "updateTextureViewSize, mContentWidth " + this.contentWidth + ", mContentHeight " + this.contentHeight + ", mScaleType " + mScaleType
            )
            Log.v(
                TAG,
                "updateTextureViewSize, viewWidth $viewWidth, viewHeight $viewHeight"
            )
        }
        var scaleX = 1.0f
        var scaleY = 1.0f
        when (mScaleType) {
            FILL -> if (viewWidth > viewHeight) {   // device in landscape
                scaleX = viewHeight * contentWidth / (viewWidth * contentHeight)
            } else {
                scaleY = viewWidth * contentHeight / (viewHeight * contentWidth)
            }
            BOTTOM, CENTER_CROP, TOP -> if (contentWidth > viewWidth && contentHeight > viewHeight) {
                scaleX = contentWidth / viewWidth
                scaleY = contentHeight / viewHeight
            } else if (contentWidth < viewWidth && contentHeight < viewHeight) {
                scaleY = viewWidth / contentWidth
                scaleX = viewHeight / contentHeight
            } else if (viewWidth > contentWidth) {
                scaleY = viewWidth / contentWidth / (viewHeight / contentHeight)
            } else if (viewHeight > contentHeight) {
                scaleX = viewHeight / contentHeight / (viewWidth / contentWidth)
            }
            null -> {}
        }
        if (SHOW_LOGS) {
            Log.v(
                TAG,
                "updateTextureViewSize, scaleX $scaleX, scaleY $scaleY"
            )
        }

        // Calculate pivot points, in our case crop from center
        val pivotPointX: Float
        val pivotPointY: Float
        when (mScaleType) {
            TOP -> {
                pivotPointX = 0f
                pivotPointY = 0f
            }
            BOTTOM -> {
                pivotPointX = viewWidth
                pivotPointY = viewHeight
            }
            CENTER_CROP -> {
                pivotPointX = viewWidth / 2
                pivotPointY = viewHeight / 2
            }
            FILL -> {
                pivotPointX = mPivotPointX
                pivotPointY = mPivotPointY
            }
            else -> throw IllegalStateException("pivotPointX, pivotPointY for ScaleType $mScaleType are not defined")
        }
        if (SHOW_LOGS) Log.v(
            TAG,
            "updateTextureViewSize, pivotPointX $pivotPointX, pivotPointY $pivotPointY"
        )
        var fitCoef = 1f
        when (mScaleType) {
            FILL, null -> {}
            BOTTOM, CENTER_CROP, TOP -> fitCoef =
                if (this.contentHeight!! > this.contentWidth!!) { //Portrait video
                    viewWidth / (viewWidth * scaleX)
                } else { //Landscape video
                    viewHeight / (viewHeight * scaleY)
                }
        }
        mContentScaleX = fitCoef * scaleX
        mContentScaleY = fitCoef * scaleY
        mPivotPointX = pivotPointX
        mPivotPointY = pivotPointY
        updateMatrixScaleRotate()
        if (SHOW_LOGS) Log.d(TAG, "<< updateTextureViewSize")
    }

    private fun updateMatrixScaleRotate() {
        if (SHOW_LOGS) Log.d(
            TAG,
            ">> updateMatrixScaleRotate, mContentRotation $mContentRotation, mContentScaleMultiplier $mContentScaleMultiplier, mPivotPointX $mPivotPointX, mPivotPointY $mPivotPointY"
        )
        mTransformMatrix.reset()
        mTransformMatrix.setScale(
            mContentScaleX * mContentScaleMultiplier,
            mContentScaleY * mContentScaleMultiplier,
            mPivotPointX,
            mPivotPointY
        )
        mTransformMatrix.postRotate(mContentRotation, mPivotPointX, mPivotPointY)
        setTransform(mTransformMatrix)
        if (SHOW_LOGS) Log.d(
            TAG,
            "<< updateMatrixScaleRotate, mContentRotation $mContentRotation, mContentScaleMultiplier $mContentScaleMultiplier, mPivotPointX $mPivotPointX, mPivotPointY $mPivotPointY"
        )
    }

    private fun updateMatrixTranslate() {
        if (SHOW_LOGS) {
            Log.d(
                TAG,
                "updateMatrixTranslate, mContentX $mContentX, mContentY $mContentY"
            )
        }
        val scaleX = mContentScaleX * mContentScaleMultiplier
        val scaleY = mContentScaleY * mContentScaleMultiplier
        mTransformMatrix.reset()
        mTransformMatrix.setScale(scaleX, scaleY, mPivotPointX, mPivotPointY)
        mTransformMatrix.postTranslate(mContentX.toFloat(), mContentY.toFloat())
        setTransform(mTransformMatrix)
    }

    override fun setRotation(degrees: Float) {
        if (SHOW_LOGS) Log.d(
            TAG,
            "setRotation, degrees $degrees, mPivotPointX $mPivotPointX, mPivotPointY $mPivotPointY"
        )
        mContentRotation = degrees
        updateMatrixScaleRotate()
    }

    override fun getRotation(): Float {
        return mContentRotation
    }

    override fun setPivotX(pivotX: Float) {
        if (SHOW_LOGS) Log.d(
            TAG,
            "setPivotX, pivotX $pivotX"
        )
        mPivotPointX = pivotX
    }

    override fun setPivotY(pivotY: Float) {
        if (SHOW_LOGS) Log.d(
            TAG,
            "setPivotY, pivotY $pivotY"
        )
        mPivotPointY = pivotY
    }

    override fun getPivotX(): Float {
        return mPivotPointX
    }

    override fun getPivotY(): Float {
        return mPivotPointY
    }

    val contentAspectRatio: Float
        get() = if (contentWidth != null && contentHeight != null) contentWidth!!.toFloat() / contentHeight!!.toFloat() else 0F

    /**
     * Use it to animate TextureView content x position
     * @param x
     */
    protected var contentX: Float
        protected get() = mContentX.toFloat()
        set(x) {
            mContentX = x.toInt() - (measuredWidth - scaledContentWidth) / 2
            updateMatrixTranslate()
        }

    /**
     * Use it to animate TextureView content x position
     * @param y
     */
    protected var contentY: Float
        protected get() = mContentY.toFloat()
        set(y) {
            mContentY = y.toInt() - (measuredHeight - scaledContentHeight) / 2
            updateMatrixTranslate()
        }

    /**
     * Use it to set content of a TextureView in the center of TextureView
     */
    fun centralizeContent() {
        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight
        val scaledContentWidth = scaledContentWidth
        val scaledContentHeight = scaledContentHeight
        if (SHOW_LOGS) Log.d(
            TAG,
            "centralizeContent, measuredWidth $measuredWidth, measuredHeight $measuredHeight, scaledContentWidth $scaledContentWidth, scaledContentHeight $scaledContentHeight"
        )
        mContentX = 0
        mContentY = 0
        if (SHOW_LOGS) Log.d(
            TAG,
            "centerVideo, mContentX $mContentX, mContentY $mContentY"
        )
        updateMatrixScaleRotate()
    }

    val scaledContentWidth: Int
        get() = (mContentScaleX * mContentScaleMultiplier * measuredWidth).toInt()
    val scaledContentHeight: Int
        get() = (mContentScaleY * mContentScaleMultiplier * measuredHeight).toInt()
    var contentScale: Float
        get() = mContentScaleMultiplier
        set(contentScale) {
            if (SHOW_LOGS) Log.d(
                TAG,
                "setContentScale, contentScale $contentScale"
            )
            mContentScaleMultiplier = contentScale
            updateMatrixScaleRotate()
        }

    protected fun setContentHeight(height: Int) {
        contentHeight = height
    }

    protected fun setContentWidth(width: Int) {
        contentWidth = width
    }

    protected fun getContentHeight():Int {
       return contentHeight!!
    }

    protected fun getContentWidth():Int {
       return contentWidth!!
    }

    companion object {
        private const val SHOW_LOGS = false
        private val TAG = ScalableTextureView::class.java.simpleName
    }
}
