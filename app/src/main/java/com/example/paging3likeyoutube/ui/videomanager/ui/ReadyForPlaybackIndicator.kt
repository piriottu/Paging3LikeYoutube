package com.example.paging3likeyoutube.ui.videomanager.ui

import android.util.Log
import android.util.Pair


class ReadyForPlaybackIndicator {
    private var mVideoSize: Pair<Int?, Int?>? = null
    private var mSurfaceTextureAvailable = false
    var isFailedToPrepareUiForPlayback = false
    private var mFailedToPrepareUiForPlayback = false
    val isVideoSizeAvailable: Boolean
        get() {
            val isVideoSizeAvailable = mVideoSize!!.first != null && mVideoSize!!.second != null
            if (SHOW_LOGS) Log.d(
                TAG,
                "isVideoSizeAvailable $isVideoSizeAvailable"
            )
            return isVideoSizeAvailable
        }
    var isSurfaceTextureAvailable: Boolean
        get() {
            if (SHOW_LOGS) Log.d(
                TAG,
                "isSurfaceTextureAvailable $mSurfaceTextureAvailable"
            )
            return mSurfaceTextureAvailable
        }
        set(available) {
            mSurfaceTextureAvailable = available
        }
    val isReadyForPlayback: Boolean
        get() {
            val isReadyForPlayback = isVideoSizeAvailable && isSurfaceTextureAvailable
            if (SHOW_LOGS) Log.d(
                TAG,
                "isReadyForPlayback $isReadyForPlayback"
            )
            return isReadyForPlayback
        }

    fun setVideoSize(videoHeight: Int?, videoWidth: Int?) {
        mVideoSize = Pair(videoHeight, videoWidth)
    }

    override fun toString(): String {
        return javaClass.simpleName + isReadyForPlayback
    }

    fun setFailedToPrepareUiForPlayback(failed: Boolean) {
        mFailedToPrepareUiForPlayback = failed
    }
    fun isFailedToPrepareUiForPlayback(): Boolean {
        return mFailedToPrepareUiForPlayback
    }
    fun isSurfaceTextureAvailable(): Boolean {
        if (SHOW_LOGS) Log.v(
            TAG,
            "isSurfaceTextureAvailable $mSurfaceTextureAvailable"
        )
        return mSurfaceTextureAvailable
    }
    companion object {
        private val TAG = ReadyForPlaybackIndicator::class.java.simpleName
        private const val SHOW_LOGS = Config.SHOW_LOGS
    }
}
