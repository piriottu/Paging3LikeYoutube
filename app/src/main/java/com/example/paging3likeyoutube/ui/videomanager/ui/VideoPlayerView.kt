package com.example.paging3likeyoutube.ui.videomanager.ui

import android.annotation.TargetApi
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.os.Looper
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import java.io.IOException

/**
 * This is player implementation based on [TextureView]
 * It encapsulates [MediaPlayer].
 *
 * It ensures that MediaPlayer methods are called from not main thread.
 * MediaPlayer methods are directly connected with hardware. That's why they should not be called from UI thread
 *
 * @author danylo.volokh
 */
open class VideoPlayerView : ScalableTextureView, SurfaceTextureListener,
    MediaPlayerWrapper.MainThreadMediaPlayerListener, MediaPlayerWrapper.VideoStateListener {
    private var TAG: String? = null

    /**
     * MediaPlayerWrapper instance.
     * If you need to use it you should synchronize in on [VideoPlayerView.mReadyForPlaybackIndicator] in order to have a consistent state.
     * Also you should call it from background thread to avoid ANR
     */
    private var mMediaPlayer: MediaPlayerWrapper? = null
    private var mViewHandlerBackgroundThread: HandlerThreadExtension? = null

    /**
     * A Listener that propagates [MediaPlayer] listeners is background thread.
     * Probably call of this listener should also need to be synchronized with it creation and destroy places.
     */
    private var mMediaPlayerListenerBackgroundThread: BackgroundThreadMediaPlayerListener? = null
    private var mVideoStateListener: MediaPlayerWrapper.VideoStateListener? = null
    private var mLocalSurfaceTextureListener: SurfaceTextureListener? = null
    var assetFileDescriptorDataSource: AssetFileDescriptor? = null
        private set
    var videoUrlDataSource: String? = null
        private set
    private val mReadyForPlaybackIndicator: ReadyForPlaybackIndicator = ReadyForPlaybackIndicator()
    private val mMediaPlayerMainThreadListeners: MutableSet<MediaPlayerWrapper.MainThreadMediaPlayerListener> =
        HashSet<MediaPlayerWrapper.MainThreadMediaPlayerListener>()
    val currentState: MediaPlayerWrapper.State?
        get() {
            synchronized(mReadyForPlaybackIndicator) { return mMediaPlayer?.getCurrentState() }
        }

    interface BackgroundThreadMediaPlayerListener {
        fun onVideoSizeChangedBackgroundThread(width: Int, height: Int)
        fun onVideoPreparedBackgroundThread()
        fun onVideoCompletionBackgroundThread()
        fun onErrorBackgroundThread(what: Int, extra: Int)
    }

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView()
    }

    private fun checkThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw RuntimeException("cannot be in main thread")
        }
    }

    fun reset() {
        checkThread()
        synchronized(mReadyForPlaybackIndicator) { mMediaPlayer!!.reset() }
    }

    fun release() {
        checkThread()
        synchronized(mReadyForPlaybackIndicator) { mMediaPlayer!!.release() }
    }

    fun clearPlayerInstance() {
        if (SHOW_LOGS) Log.d(TAG, ">> clearPlayerInstance")
        checkThread()
        synchronized(mReadyForPlaybackIndicator) {
            mReadyForPlaybackIndicator.setVideoSize(null, null)
            mMediaPlayer!!.clearAll()
            mMediaPlayer = null
        }
        if (SHOW_LOGS) Log.d(TAG, "<< clearPlayerInstance")
    }

    fun createNewPlayerInstance() {
        if (SHOW_LOGS) Log.d(TAG, ">> createNewPlayerInstance")
        if (SHOW_LOGS) Log.d(
            TAG,
            "createNewPlayerInstance main Looper " + Looper.getMainLooper()
        )
        if (SHOW_LOGS) Log.d(TAG, "createNewPlayerInstance my Looper " + Looper.myLooper())
        checkThread()
        synchronized(mReadyForPlaybackIndicator) {
            mMediaPlayer = MediaPlayerWrapperImpl()
            mReadyForPlaybackIndicator.setVideoSize(null, null)
            mReadyForPlaybackIndicator.setFailedToPrepareUiForPlayback(false)
            if (mReadyForPlaybackIndicator.isSurfaceTextureAvailable()) {
                val texture = surfaceTexture
                if (SHOW_LOGS) Log.d(TAG, "texture $texture")
                mMediaPlayer!!.setSurfaceTexture(texture)
            } else {
                if (SHOW_LOGS) Log.d(TAG, "texture not available")
            }
            mMediaPlayer!!.setMainThreadMediaPlayerListener(this)
            mMediaPlayer!!.setVideoStateListener(this)
        }
        if (SHOW_LOGS) Log.d(TAG, "<< createNewPlayerInstance")
    }

    fun prepare() {
        checkThread()
        synchronized(mReadyForPlaybackIndicator) { mMediaPlayer!!.prepare() }
    }

    fun stop() {
        checkThread()
        synchronized(mReadyForPlaybackIndicator) { mMediaPlayer!!.stop() }
    }

    private fun notifyOnVideoStopped() {
        if (SHOW_LOGS) Log.d(TAG, "notifyOnVideoStopped")
        var listCopy: List<MediaPlayerWrapper.MainThreadMediaPlayerListener?>
        synchronized(mMediaPlayerMainThreadListeners) {
            listCopy = ArrayList<MediaPlayerWrapper.MainThreadMediaPlayerListener?>(
                mMediaPlayerMainThreadListeners
            )
        }
        for (listener in listCopy) {
            listener?.onVideoStoppedMainThread()
        }
    }

    private val isVideoSizeAvailable: Boolean
        private get() {
            val isVideoSizeAvailable = getContentHeight() != null && getContentWidth() != null
            if (SHOW_LOGS) Log.d(
                TAG,
                "isVideoSizeAvailable $isVideoSizeAvailable"
            )
            return isVideoSizeAvailable
        }

    fun start() {
        if (SHOW_LOGS) Log.d(TAG, ">> start")
        synchronized(mReadyForPlaybackIndicator) {
            if (mReadyForPlaybackIndicator.isReadyForPlayback) {
                mMediaPlayer!!.start()
            } else {
                if (SHOW_LOGS) Log.d(TAG, "start, >> wait")
                if (!mReadyForPlaybackIndicator.isFailedToPrepareUiForPlayback()) {
                    try {
                        // mReadyForPlaybackIndicator.wait()
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                    if (SHOW_LOGS) Log.d(TAG, "start, << wait")
                    if (mReadyForPlaybackIndicator.isReadyForPlayback) {
                        mMediaPlayer!!.start()
                    } else {
                        if (SHOW_LOGS) Log.d(
                            TAG,
                            "start, movie is not ready, Player become STARTED state, but it will actually don't play"
                        )
                    }
                } else {
                    if (SHOW_LOGS) Log.d(
                        TAG,
                        "start, movie is not ready. Video size will not become available"
                    )
                }
            }
        }
        if (SHOW_LOGS) Log.d(TAG, "<< start")
    }

    private fun initView() {
        if (!isInEditMode) {
            TAG = "" + this
            if (SHOW_LOGS) Log.d(TAG, "initView")
            setScaleType(ScaleType.CENTER_CROP)
            super.setSurfaceTextureListener(this)
        }
    }

    override fun setSurfaceTextureListener(listener: SurfaceTextureListener?) {
        mLocalSurfaceTextureListener = listener
    }

    fun setDataSource(path: String) {
        checkThread()
        synchronized(mReadyForPlaybackIndicator) {
            if (SHOW_LOGS) Log.d(
                TAG,
                "setDataSource, path $path, this $this"
            )
            try {
                mMediaPlayer!!.setDataSource(path)
            } catch (e: IOException) {
                e.message?.let { Log.d(TAG, it) }
                throw RuntimeException(e)
            }
            videoUrlDataSource = path
        }
    }

    fun setDataSource(assetFileDescriptor: AssetFileDescriptor) {
        checkThread()
        synchronized(mReadyForPlaybackIndicator) {
            if (SHOW_LOGS) Log.d(
                TAG,
                "setDataSource, assetFileDescriptor $assetFileDescriptor, this $this"
            )
            try {
                mMediaPlayer!!.setDataSource(assetFileDescriptor)
            } catch (e: IOException) {
                e.message?.let { Log.d(TAG, it) }
                throw RuntimeException(e)
            }
            assetFileDescriptorDataSource = assetFileDescriptor
        }
    }

    fun setOnVideoStateChangedListener(listener: MediaPlayerWrapper.VideoStateListener?) {
        mVideoStateListener = listener
        checkThread()
        synchronized(mReadyForPlaybackIndicator) { mMediaPlayer!!.setVideoStateListener(listener) }
    }

    fun addMediaPlayerListener(listener: MediaPlayerWrapper.MainThreadMediaPlayerListener) {
        synchronized(mMediaPlayerMainThreadListeners) { mMediaPlayerMainThreadListeners.add(listener) }
    }

    fun setBackgroundThreadMediaPlayerListener(listener: BackgroundThreadMediaPlayerListener?) {
        mMediaPlayerListenerBackgroundThread = listener
    }

    override fun onVideoSizeChangedMainThread(width: Int, height: Int) {
        if (SHOW_LOGS) Log.d(
            TAG,
            ">> onVideoSizeChangedMainThread, width $width, height $height"
        )
        if (width != 0 && height != 0) {
            setContentWidth(width)
            setContentHeight(height)
            onVideoSizeAvailable()
        } else {
            if (SHOW_LOGS) Log.d(
                TAG,
                "onVideoSizeChangedMainThread, size 0. Probably will be unable to start video"
            )
            synchronized(mReadyForPlaybackIndicator) {
                mReadyForPlaybackIndicator.setFailedToPrepareUiForPlayback(true)
                //mReadyForPlaybackIndicator.notifyAll()
            }
        }
        notifyOnVideoSizeChangedMainThread(width, height)
        if (SHOW_LOGS) Log.d(
            TAG,
            "<< onVideoSizeChangedMainThread, width $width, height $height"
        )
    }

    private fun notifyOnVideoSizeChangedMainThread(width: Int, height: Int) {
        if (SHOW_LOGS) Log.d(
            TAG,
            "notifyOnVideoSizeChangedMainThread, width $width, height $height"
        )
        var listCopy: List<MediaPlayerWrapper.MainThreadMediaPlayerListener?>
        synchronized(mMediaPlayerMainThreadListeners) {
            listCopy = ArrayList<MediaPlayerWrapper.MainThreadMediaPlayerListener?>(
                mMediaPlayerMainThreadListeners
            )
        }
        for (listener in listCopy) {
            listener?.onVideoSizeChangedMainThread(width, height)
        }
    }

    private val mVideoCompletionBackgroundThreadRunnable = Runnable {
        mMediaPlayerListenerBackgroundThread!!.onVideoSizeChangedBackgroundThread(
            getContentHeight(),
            getContentWidth()
        )
    }

    override fun onVideoCompletionMainThread() {
        notifyOnVideoCompletionMainThread()
        if (mMediaPlayerListenerBackgroundThread != null) {
            mViewHandlerBackgroundThread?.post(mVideoCompletionBackgroundThreadRunnable)
        }
    }

    private fun notifyOnVideoCompletionMainThread() {
        if (SHOW_LOGS) Log.d(TAG, "notifyVideoCompletionMainThread")
        var listCopy: List<MediaPlayerWrapper.MainThreadMediaPlayerListener?>
        synchronized(mMediaPlayerMainThreadListeners) {
            listCopy = ArrayList<MediaPlayerWrapper.MainThreadMediaPlayerListener?>(
                mMediaPlayerMainThreadListeners
            )
        }
        for (listener in listCopy) {
            listener?.onVideoCompletionMainThread()
        }
    }

    private fun notifyOnVideoPreparedMainThread() {
        if (SHOW_LOGS) Log.d(TAG, "notifyOnVideoPreparedMainThread")
        var listCopy: List<MediaPlayerWrapper.MainThreadMediaPlayerListener?>
        synchronized(mMediaPlayerMainThreadListeners) {
            listCopy = ArrayList<MediaPlayerWrapper.MainThreadMediaPlayerListener?>(
                mMediaPlayerMainThreadListeners
            )
        }
        for (listener in listCopy) {
            listener?.onVideoPreparedMainThread()
        }
    }

    private fun notifyOnErrorMainThread(what: Int, extra: Int) {
        if (SHOW_LOGS) Log.d(TAG, "notifyOnErrorMainThread")
        var listCopy: List<MediaPlayerWrapper.MainThreadMediaPlayerListener?>
        synchronized(mMediaPlayerMainThreadListeners) {
            listCopy = ArrayList<MediaPlayerWrapper.MainThreadMediaPlayerListener?>(
                mMediaPlayerMainThreadListeners
            )
        }
        for (listener in listCopy) {
            listener?.onErrorMainThread(what, extra)
        }
    }

    private val mVideoPreparedBackgroundThreadRunnable =
        Runnable { mMediaPlayerListenerBackgroundThread!!.onVideoPreparedBackgroundThread() }

    override fun onVideoPreparedMainThread() {
        notifyOnVideoPreparedMainThread()
        if (mMediaPlayerListenerBackgroundThread != null) {
            mViewHandlerBackgroundThread?.post(mVideoPreparedBackgroundThreadRunnable)
        }
    }

    override fun onErrorMainThread(what: Int, extra: Int) {
        if (SHOW_LOGS) Log.d(TAG, "onErrorMainThread, this " + this@VideoPlayerView)
        when (what) {
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                if (SHOW_LOGS) Log.d(TAG, "onErrorMainThread, what MEDIA_ERROR_SERVER_DIED")
                printErrorExtra(extra)
            }
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                if (SHOW_LOGS) Log.d(TAG, "onErrorMainThread, what MEDIA_ERROR_UNKNOWN")
                printErrorExtra(extra)
            }
        }
        notifyOnErrorMainThread(what, extra)
        if (mMediaPlayerListenerBackgroundThread != null) {
            mViewHandlerBackgroundThread?.post(Runnable {
                mMediaPlayerListenerBackgroundThread!!.onErrorBackgroundThread(
                    what,
                    extra
                )
            })
        }
    }

    override fun onBufferingUpdateMainThread(percent: Int) {}
    override fun onVideoStoppedMainThread() {
        notifyOnVideoStopped()
    }

    private fun printErrorExtra(extra: Int) {
        when (extra) {
            MediaPlayer.MEDIA_ERROR_IO -> if (SHOW_LOGS) Log.d(TAG, "error extra MEDIA_ERROR_IO")
            MediaPlayer.MEDIA_ERROR_MALFORMED -> if (SHOW_LOGS) Log.d(
                TAG,
                "error extra MEDIA_ERROR_MALFORMED"
            )
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> if (SHOW_LOGS) Log.d(
                TAG,
                "error extra MEDIA_ERROR_UNSUPPORTED"
            )
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> if (SHOW_LOGS) Log.d(
                TAG,
                "error extra MEDIA_ERROR_TIMED_OUT"
            )
        }
    }

    private val mVideoSizeAvailableRunnable = Runnable {
        if (SHOW_LOGS) Log.d(TAG, ">> run, onVideoSizeAvailable")
        synchronized(mReadyForPlaybackIndicator) {
            if (SHOW_LOGS) Log.d(
                TAG,
                "onVideoSizeAvailable, mReadyForPlaybackIndicator $mReadyForPlaybackIndicator"
            )
            mReadyForPlaybackIndicator.setVideoSize(getContentHeight(), getContentWidth())
            if (mReadyForPlaybackIndicator.isReadyForPlayback) {
                if (SHOW_LOGS) Log.d(
                    TAG,
                    "run, onVideoSizeAvailable, notifyAll"
                )
                //  mReadyForPlaybackIndicator.notifyAll()
            }
            if (SHOW_LOGS) Log.d(
                TAG,
                "<< run, onVideoSizeAvailable"
            )
        }
        if (mMediaPlayerListenerBackgroundThread != null) {
            mMediaPlayerListenerBackgroundThread!!.onVideoSizeChangedBackgroundThread(
                getContentHeight(),
                getContentWidth()
            )
        }
    }

    private fun onVideoSizeAvailable() {
        if (SHOW_LOGS) Log.d(TAG, ">> onVideoSizeAvailable")
        updateTextureViewSize()
        if (isAttachedToWindow) {
            mViewHandlerBackgroundThread?.post(mVideoSizeAvailableRunnable)
        }
        if (SHOW_LOGS) Log.d(TAG, "<< onVideoSizeAvailable")
    }

    fun muteVideo() {
        synchronized(mReadyForPlaybackIndicator) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(IS_VIDEO_MUTED, true).commit()
            mMediaPlayer!!.setVolume(0F, 0F)
        }
    }

    fun unMuteVideo() {
        synchronized(mReadyForPlaybackIndicator) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(IS_VIDEO_MUTED, false).commit()
            mMediaPlayer!!.setVolume(1F, 1F)
        }
    }

    val isAllVideoMute: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(IS_VIDEO_MUTED, false)

    fun pause() {
        if (SHOW_LOGS) Log.d(TAG, ">> pause ")
        synchronized(mReadyForPlaybackIndicator) { mMediaPlayer!!.pause() }
        if (SHOW_LOGS) Log.d(TAG, "<< pause")
    }

    /**
     * @see MediaPlayer.getDuration
     */
    val duration: Int
        get() {
            synchronized(mReadyForPlaybackIndicator) { return mMediaPlayer?.duration!! }
        }

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        if (SHOW_LOGS) Log.d(
            TAG,
            "onSurfaceTextureAvailable, width $width, height $height, this $this"
        )
        if (mLocalSurfaceTextureListener != null) {
            mLocalSurfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture, width, height)
        }
        notifyTextureAvailable()
    }

    private fun notifyTextureAvailable() {
        if (SHOW_LOGS) Log.d(TAG, ">> notifyTextureAvailable")
        mViewHandlerBackgroundThread?.post(Runnable {
            if (SHOW_LOGS) Log.d(TAG, ">> run notifyTextureAvailable")
            synchronized(mReadyForPlaybackIndicator) {
                if (mMediaPlayer != null) {
                    mMediaPlayer!!.setSurfaceTexture(surfaceTexture)
                } else {
                    mReadyForPlaybackIndicator.setVideoSize(null, null)
                    if (SHOW_LOGS) Log.d(
                        TAG,
                        "mMediaPlayer null, cannot set surface texture"
                    )
                }
                mReadyForPlaybackIndicator.isSurfaceTextureAvailable = true
                if (mReadyForPlaybackIndicator.isReadyForPlayback) {
                    if (SHOW_LOGS) Log.d(
                        TAG,
                        "notify ready for playback"
                    )
                  //TODO  mReadyForPlaybackIndicator.notifyAll()
                }
            }
            if (SHOW_LOGS) Log.d(TAG, "<< run notifyTextureAvailable")
        })
        if (SHOW_LOGS) Log.d(TAG, "<< notifyTextureAvailable")
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        if (mLocalSurfaceTextureListener != null) {
            mLocalSurfaceTextureListener!!.onSurfaceTextureSizeChanged(surface, width, height)
        }
    }

    /**
     * Note : this method might be called after [.onDetachedFromWindow]
     * @param surface
     * @return
     */
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        if (SHOW_LOGS) Log.d(
            TAG,
            "onSurfaceTextureDestroyed, surface $surface"
        )
        if (mLocalSurfaceTextureListener != null) {
            mLocalSurfaceTextureListener!!.onSurfaceTextureDestroyed(surface)
        }
        if (isAttachedToWindow) {
            mViewHandlerBackgroundThread?.post(Runnable {
                synchronized(mReadyForPlaybackIndicator) {
                    mReadyForPlaybackIndicator.isSurfaceTextureAvailable = (false)
                    /** we have to notify a Thread may be in wait() state in [VideoPlayerView.start] method */
                    /** we have to notify a Thread may be in wait() state in [VideoPlayerView.start] method */
                    /** we have to notify a Thread may be in wait() state in [VideoPlayerView.start] method */
                    /** we have to notify a Thread may be in wait() state in [VideoPlayerView.start] method */
                   //TODO mReadyForPlaybackIndicator.notifyAll()
                }
            })
        }

        // We have to release this surface manually for better control.
        // Also we do this because we return false from this method
        surface.release()
        return false
    }

    override fun isAttachedToWindow(): Boolean {
        return mViewHandlerBackgroundThread != null
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//        if (SHOW_LOGS) Log.d(TAG, "onSurfaceTextureUpdated, mIsVideoStartedCalled " + mIsVideoStartedCalled.get() + ", mMediaPlayer.getState() " + mMediaPlayer.getState());
        if (mLocalSurfaceTextureListener != null) {
            mLocalSurfaceTextureListener!!.onSurfaceTextureUpdated(surface)
        }
    }

    interface PlaybackStartedListener {
        fun onPlaybackStarted()
    }

    override fun onVideoPlayTimeChanged(positionInMilliseconds: Int) {}
    override fun toString(): String {
        return javaClass.simpleName + "@" + hashCode()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val isInEditMode = isInEditMode
        if (SHOW_LOGS) Log.d(TAG, ">> onAttachedToWindow $isInEditMode")
        if (!isInEditMode) {
            mViewHandlerBackgroundThread = HandlerThreadExtension(TAG, false)
            mViewHandlerBackgroundThread!!.startThread()
        }
        if (SHOW_LOGS) Log.d(TAG, "<< onAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val isInEditMode = isInEditMode
        if (SHOW_LOGS) Log.d(
            TAG,
            ">> onDetachedFromWindow, isInEditMode $isInEditMode"
        )
        if (!isInEditMode) {
            mViewHandlerBackgroundThread?.postQuit()
            mViewHandlerBackgroundThread = null
        }
        if (SHOW_LOGS) Log.d(TAG, "<< onDetachedFromWindow")
    }

    protected fun onVisibilityChanged(changedView: View?, visibility: Int) {
        super.onVisibilityChanged(changedView!!, visibility)
        val isInEditMode = isInEditMode
        if (SHOW_LOGS) Log.d(
            TAG,
            ">> onVisibilityChanged " + visibilityStr(visibility) + ", isInEditMode " + isInEditMode
        )
        if (!isInEditMode) {
            when (visibility) {
                VISIBLE -> {}
                INVISIBLE, GONE -> synchronized(mReadyForPlaybackIndicator) {
                    // have to notify worker thread in case we exited this screen without getting ready for playback
                  //TODO  mReadyForPlaybackIndicator.notifyAll()
                }
            }
        }
        if (SHOW_LOGS) Log.d(TAG, "<< onVisibilityChanged")
    }

    companion object {
        private const val SHOW_LOGS = Config.SHOW_LOGS
        private const val IS_VIDEO_MUTED = "IS_VIDEO_MUTED"
        private fun visibilityStr(visibility: Int): String {
            return when (visibility) {
                VISIBLE -> "VISIBLE"
                INVISIBLE -> "INVISIBLE"
                GONE -> "GONE"
                else -> throw RuntimeException("unexpected")
            }
        }
    }
}
