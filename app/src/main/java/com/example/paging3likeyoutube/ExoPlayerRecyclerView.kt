package com.example.paging3likeyoutube

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.paging3likeyoutube.ui.home.PostAdapter
import com.example.paging3likeyoutube.ui.home.PostViewHolder
import kotlin.math.abs

@UnstableApi class ExoPlayerRecyclerView : RecyclerView {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        addOnScrollListener(object : OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    // There's a special case when the end of the list has been reached.
                    // Need to handle that with this bit of logic
                    if (!recyclerView.canScrollVertically(1)) {
                        tryToPlayVideo(true)
                    } else {
                        tryToPlayVideo(false)
                    }
                }
            }
        })
    }
    private fun tryToPlayVideo(isEndOfList: Boolean) {

        val targetPosition: Int
        val adapter = (this.adapter as PostAdapter)
        if (!isEndOfList) {
            val startPosition = (this.layoutManager  as LinearLayoutManager).findFirstVisibleItemPosition()
            var endPosition =
                (this.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

            // if there is more than 2 list-items on the screen, set the difference to be 1
            if (endPosition - startPosition > 1) {
                endPosition = startPosition + 1
            }

            // something is wrong. return.
            if (startPosition < 0 || endPosition < 0) {
                return
            }

            // if there is more than 1 list-item on the screen
            targetPosition = if (startPosition != endPosition) {
                val startPositionVideoHeight = getVisibleVideoSurfaceHeight(startPosition)
                val endPositionVideoHeight = getVisibleVideoSurfaceHeight(endPosition)

                if (startPositionVideoHeight > endPositionVideoHeight) startPosition else endPosition
            } else {
                startPosition
            }
        } else {
            targetPosition = (this.adapter as PostAdapter).itemCount - 1
        }

        // video is already playing so return
        if (targetPosition == adapter.playPosition) {
            return
        }

        val holder = this.findViewHolderForAdapterPosition(targetPosition)

        if(holder !is PostViewHolder){
            return
        }

        holder.setPlayer()

        if(adapter.playPosition >= 0) {
            adapter.notifyItemChanged(adapter.playPosition)
        }

        adapter.playPosition = targetPosition
    }


    /**
     * Returns the visible region of the video surface on the screen.
     * if some is cut off, it will return less than the @videoSurfaceDefaultHeight
     */
    private fun getVisibleVideoSurfaceHeight(playPosition: Int): Int {
        val at = abs(playPosition - (this.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
        Log.d("exo_fragment", "getVisibleVideoSurfaceHeight: at: $at")

        val child = this.getChildAt(at) ?: return 0

        val location = IntArray(2)
        child.getLocationOnScreen(location)

        val displayMetrics = DisplayMetrics()
        //context.applicationContext.display?.getre
        context.getSystemService(WindowManager::class.java).defaultDisplay.getMetrics(displayMetrics)
        val heightScreen = displayMetrics.heightPixels

        return if (location[1] <= 0) {
            Log.d("exo_fragment", "getVisibleVideoSurfaceHeight: location[1] + (heightScreen/2): ${location[1] + (heightScreen/2)}")
            location[1] + (heightScreen/2)
        } else {
            Log.d("exo_fragment", "getVisibleVideoSurfaceHeight:  heightScreen - location[1]: ${ heightScreen - location[1]}")
            heightScreen - location[1]
        }
    }
}