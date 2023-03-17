package com.example.paging3likeyoutube.ui.videomanager.ui

import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * This class encapsulates [MediaPlayer]
 * and follows this use-case diagram:
 *
 * http://developer.android.com/reference/android/media/MediaPlayer.html
 */
abstract class MediaPlayerWrapper protected constructor(mediaPlayer: MediaPlayer) :
    OnErrorListener, OnBufferingUpdateListener, OnInfoListener,
    OnCompletionListener, OnVideoSizeChangedListener {
    private val TAG: String = ""
    private var mFuture: ScheduledFuture<*>? = null
    private var mSurface: Surface? = null
    private val SHOW_LOGS: Boolean = Config.SHOW_LOGS

    enum class State {
        IDLE, INITIALIZED, PREPARING, PREPARED, STARTED, PAUSED, STOPPED, PLAYBACK_COMPLETED, END, ERROR
    }

    private val mMainThreadHandler = Handler(Looper.getMainLooper())
    private val mMediaPlayer: MediaPlayer
    private val mState = AtomicReference<State>()
    private var mListener: MainThreadMediaPlayerListener? = null
    private var mVideoStateListener: VideoStateListener? = null
    private val mPositionUpdateNotifier = Executors.newScheduledThreadPool(1)
    private val mOnVideoPreparedMessage = Runnable {
        if (SHOW_LOGS) Log.v(TAG, ">> run, onVideoPreparedMainThread")
        mListener!!.onVideoPreparedMainThread()
        if (SHOW_LOGS) Log.v(TAG, "<< run, onVideoPreparedMainThread")
    }

    open fun getCurrentState(): State? {
        synchronized(mState) { return mState.get() }
    }
    fun prepare() {
        if (SHOW_LOGS) Log.v(TAG, ">> prepare, mState $mState")
        synchronized(mState) {
            when (mState.get()) {
                State.STOPPED, State.INITIALIZED -> try {
                    mMediaPlayer.prepare()
                    mState.set(State.PREPARED)
                    if (mListener != null) {
                        mMainThreadHandler.post(mOnVideoPreparedMessage)
                    }
                } catch (ex: IllegalStateException) {
                    /** we should not call [MediaPlayerWrapper.prepare] in wrong state so we fall here */
                    throw RuntimeException(ex)
                } catch (ex: IOException) {
                    onPrepareError(ex)
                }
                State.IDLE, State.PREPARING, State.PREPARED, State.STARTED, State.PAUSED, State.PLAYBACK_COMPLETED, State.END, State.ERROR -> throw IllegalStateException(
                    "prepare, called from illegal state $mState"
                )
            }
        }
        if (SHOW_LOGS) Log.v(TAG, "<< prepare, mState $mState")
    }

    /**
     * This method propagates error when [IOException] is thrown during synchronous [.prepare]
     * @param ex
     */
    private fun onPrepareError(ex: IOException) {
        if (SHOW_LOGS) Log.e(TAG, "catch IO exception [$ex]")
        // might happen because of lost internet connection
//      TODO: if (SHOW_LOGS) Logger.err(TAG, "catch exception, is Network Connected [" + Utils.isNetworkConnected());
        mState.set(State.ERROR)
        if (mListener != null) {
            mListener!!.onErrorMainThread(
                1,
                -1004
            ) //TODO: remove magic numbers. Find a way to get actual error
        }
        if (mListener != null) {
            mMainThreadHandler.post {
                if (SHOW_LOGS) Log.v(
                    TAG,
                    ">> run, onVideoPreparedMainThread"
                )
                mListener!!.onErrorMainThread(
                    1,
                    -1004
                ) //TODO: remove magic numbers. Find a way to get actual error
                if (SHOW_LOGS) Log.v(
                    TAG,
                    "<< run, onVideoPreparedMainThread"
                )
            }
        }
    }

    /**
     * @see MediaPlayer.setDataSource
     */
    @Throws(IOException::class)
    fun setDataSource(filePath: String) {
        synchronized(mState) {
            if (SHOW_LOGS) Log.v(
                TAG,
                "setDataSource, filePath $filePath, mState $mState"
            )
            when (mState.get()) {
                State.IDLE -> {
                    mMediaPlayer.setDataSource(filePath)
                    mState.set(State.INITIALIZED)
                }
                State.INITIALIZED, State.PREPARING, State.PREPARED, State.STARTED, State.PAUSED, State.STOPPED, State.PLAYBACK_COMPLETED, State.END, State.ERROR -> throw IllegalStateException(
                    "setDataSource called in state $mState"
                )
                else -> throw IllegalStateException("setDataSource called in state $mState")
            }
        }
    }

    /**
     * @see MediaPlayer.setDataSource
     */
    @Throws(IOException::class)
    fun setDataSource(assetFileDescriptor: AssetFileDescriptor) {
        synchronized(mState) {
            when (mState.get()) {
                State.IDLE -> {
                    mMediaPlayer.setDataSource(
                        assetFileDescriptor.fileDescriptor,
                        assetFileDescriptor.startOffset,
                        assetFileDescriptor.length
                    )
                    mState.set(State.INITIALIZED)
                }
                State.INITIALIZED, State.PREPARING, State.PREPARED, State.STARTED, State.PAUSED, State.STOPPED, State.PLAYBACK_COMPLETED, State.END, State.ERROR -> throw IllegalStateException(
                    "setDataSource called in state $mState"
                )
                else -> throw IllegalStateException("setDataSource called in state $mState")
            }
        }
    }

    override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
        if (SHOW_LOGS) Log.v(
            TAG,
            "onVideoSizeChanged, width $width, height $height"
        )
        /*  if(!inUiThread()){
            throw new RuntimeException("this should be called in Main Thread");
        }*/if (mListener != null) {
            mListener!!.onVideoSizeChangedMainThread(width, height)
        }
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (SHOW_LOGS) Log.v(
            TAG,
            "onVideoCompletion, mState $mState"
        )
        synchronized(mState) { mState.set(State.PLAYBACK_COMPLETED) }
        if (mListener != null) {
            mListener!!.onVideoCompletionMainThread()
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        if (SHOW_LOGS) Log.v(
            TAG,
            "onErrorMainThread, what $what, extra $extra"
        )
        synchronized(mState) { mState.set(State.ERROR) }
        if (positionUpdaterIsWorking()) {
            stopPositionUpdateNotifier()
        }
        if (SHOW_LOGS) Log.v(
            TAG,
            "onErrorMainThread, mListener $mListener"
        )
        if (mListener != null) {
            mListener!!.onErrorMainThread(what, extra)
        }
        // We always return true, because after Error player stays in this state.
        // See here http://developer.android.com/reference/android/media/MediaPlayer.html
        return true
    }

    private fun positionUpdaterIsWorking(): Boolean {
        return mFuture != null
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
        if (mListener != null) {
            mListener!!.onBufferingUpdateMainThread(percent)
        }
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        if (SHOW_LOGS) Log.v(TAG, "onInfo")
        printInfo(what)
        return false
    }

    private fun printInfo(what: Int) {
        when (what) {
            MEDIA_INFO_UNKNOWN -> if (SHOW_LOGS) Log.d(TAG, "onInfo, MEDIA_INFO_UNKNOWN")
            MEDIA_INFO_VIDEO_TRACK_LAGGING -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_VIDEO_TRACK_LAGGING"
            )
            MEDIA_INFO_VIDEO_RENDERING_START -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_VIDEO_RENDERING_START"
            )
            MEDIA_INFO_BUFFERING_START -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_BUFFERING_START"
            )
            MEDIA_INFO_BUFFERING_END -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_BUFFERING_END"
            )
            MEDIA_INFO_BAD_INTERLEAVING -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_BAD_INTERLEAVING"
            )
            MEDIA_INFO_NOT_SEEKABLE -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_NOT_SEEKABLE"
            )
            MEDIA_INFO_METADATA_UPDATE -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_METADATA_UPDATE"
            )
            MEDIA_INFO_UNSUPPORTED_SUBTITLE -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_UNSUPPORTED_SUBTITLE"
            )
            MEDIA_INFO_SUBTITLE_TIMED_OUT -> if (SHOW_LOGS) Log.d(
                TAG,
                "onInfo, MEDIA_INFO_SUBTITLE_TIMED_OUT"
            )
        }
    }

    /**
     * Listener trigger 'onVideoPreparedMainThread' and `onVideoCompletionMainThread` events
     */
    fun setMainThreadMediaPlayerListener(listener: MainThreadMediaPlayerListener?) {
        mListener = listener
    }

    fun setVideoStateListener(listener: VideoStateListener?) {
        mVideoStateListener = listener
    }

    /**
     * Play or resume video. Video will be played as soon as view is available and media player is
     * prepared.
     *
     *
     * If video is stopped or ended and play() method was called, video will start over.
     */
    fun start() {
        if (SHOW_LOGS) Log.d(TAG, ">> start")
        synchronized(mState) {
            if (SHOW_LOGS) Log.d(TAG, "start, mState $mState")
            when (mState.get()) {
                State.IDLE, State.INITIALIZED, State.PREPARING, State.STARTED -> throw IllegalStateException(
                    "start, called from illegal state $mState"
                )
                State.STOPPED, State.PLAYBACK_COMPLETED, State.PREPARED, State.PAUSED -> {
                    if (SHOW_LOGS) Log.d(
                        TAG,
                        "start, video is $mState, starting playback."
                    )
                    mMediaPlayer.start()
                    startPositionUpdateNotifier()
                    mState.set(State.STARTED)
                }
                State.ERROR, State.END -> throw IllegalStateException(
                    "start, called from illegal state $mState"
                )
            }
        }
        if (SHOW_LOGS) Log.d(TAG, "<< start")
    }

    /**
     * Pause video. If video is already paused, stopped or ended nothing will happen.
     */
    fun pause() {
        if (SHOW_LOGS) Log.d(TAG, ">> pause")
        synchronized(mState) {
            if (SHOW_LOGS) Log.d(TAG, "pause, mState $mState")
            when (mState.get()) {
                State.IDLE, State.INITIALIZED, State.PAUSED, State.PLAYBACK_COMPLETED, State.ERROR, State.PREPARING, State.STOPPED, State.PREPARED, State.END -> throw IllegalStateException(
                    "pause, called from illegal state $mState"
                )
                State.STARTED -> {
                    mMediaPlayer.pause()
                    mState.set(State.PAUSED)
                }
            }
        }
        if (SHOW_LOGS) Log.d(TAG, "<< pause")
    }

    private val mOnVideoStopMessage = Runnable {
        if (SHOW_LOGS) Log.d(TAG, ">> run, onVideoStoppedMainThread")
        mListener!!.onVideoStoppedMainThread()
        if (SHOW_LOGS) Log.d(TAG, "<< run, onVideoStoppedMainThread")
    }

    fun stop() {
        if (SHOW_LOGS) Log.d(TAG, ">> stop")
        synchronized(mState) {
            if (SHOW_LOGS) Log.d(TAG, "stop, mState $mState")
            when (mState.get()) {
                State.STARTED, State.PAUSED -> {
                    stopPositionUpdateNotifier()
                    if (SHOW_LOGS) Log.d(TAG, ">> stop")
                    mMediaPlayer.stop()
                    if (SHOW_LOGS) Log.d(TAG, "<< stop")
                    mState.set(State.STOPPED)
                    if (mListener != null) {
                        mMainThreadHandler.post(mOnVideoStopMessage)
                    }
                }
                State.PLAYBACK_COMPLETED, State.PREPARED, State.PREPARING -> {
                    if (SHOW_LOGS) Log.d(TAG, ">> stop")
                    mMediaPlayer.stop()
                    if (SHOW_LOGS) Log.d(TAG, "<< stop")
                    mState.set(State.STOPPED)
                    if (mListener != null) {
                        mMainThreadHandler.post(mOnVideoStopMessage)
                    }
                }
                State.STOPPED -> throw IllegalStateException("stop, already stopped")
                State.IDLE, State.INITIALIZED, State.END, State.ERROR -> throw IllegalStateException(
                    "cannot stop. Player in mState $mState"
                )
            }
        }
        if (SHOW_LOGS) Log.d(TAG, "<< stop")
    }

    fun reset() {
        if (SHOW_LOGS) Log.d(TAG, ">> reset , mState $mState")
        synchronized(mState) {
            when (mState.get()) {
                State.IDLE, State.INITIALIZED, State.PREPARED, State.STARTED, State.PAUSED, State.STOPPED, State.PLAYBACK_COMPLETED, State.ERROR -> {
                    mMediaPlayer.reset()
                    mState.set(State.IDLE)
                }
                State.PREPARING, State.END -> throw IllegalStateException(
                    "cannot call reset from state " + mState.get()
                )
            }
        }
        if (SHOW_LOGS) Log.d(TAG, "<< reset , mState $mState")
    }

    fun release() {
        if (SHOW_LOGS) Log.d(TAG, ">> release, mState $mState")
        synchronized(mState) {
            mMediaPlayer.release()
            mState.set(State.END)
        }
        if (SHOW_LOGS) Log.d(TAG, "<< release, mState $mState")
    }

    fun clearAll() {
        if (SHOW_LOGS) Log.d(TAG, ">> clearAll, mState $mState")
        synchronized(mState) {
            mMediaPlayer.setOnVideoSizeChangedListener(null)
            mMediaPlayer.setOnCompletionListener(null)
            mMediaPlayer.setOnErrorListener(null)
            mMediaPlayer.setOnBufferingUpdateListener(null)
            mMediaPlayer.setOnInfoListener(null)
        }
        if (SHOW_LOGS) Log.d(TAG, "<< clearAll, mState $mState")
    }

    fun setLooping(looping: Boolean) {
        if (SHOW_LOGS) Log.d(TAG, "setLooping $looping")
        mMediaPlayer.isLooping = looping
    }

    fun setSurfaceTexture(surfaceTexture: SurfaceTexture?) {
        if (SHOW_LOGS) Log.d(
            TAG,
            ">> setSurfaceTexture $surfaceTexture"
        )
        if (SHOW_LOGS) Log.d(
            TAG,
            "setSurfaceTexture mSurface $mSurface"
        )
        if (surfaceTexture != null) {
            mSurface = Surface(surfaceTexture)
            mMediaPlayer.setSurface(mSurface) // TODO fix illegal state exception
        } else {
            mMediaPlayer.setSurface(null)
        }
        if (SHOW_LOGS) Log.d(
            TAG,
            "<< setSurfaceTexture $surfaceTexture"
        )
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        mMediaPlayer.setVolume(leftVolume, rightVolume)
    }

    val videoWidth: Int
        get() = mMediaPlayer.videoWidth
    val videoHeight: Int
        get() = mMediaPlayer.videoHeight
    val currentPosition: Int
        get() = mMediaPlayer.currentPosition
    val isPlaying: Boolean
        get() = mMediaPlayer.isPlaying
    val isReadyForPlayback: Boolean
        get() {
            var isReadyForPlayback = false
            synchronized(mState) {
                if (SHOW_LOGS) Log.d(
                    TAG,
                    "isReadyForPlayback, mState $mState"
                )
                val state = mState.get()
                isReadyForPlayback = when (state) {
                    State.IDLE, State.INITIALIZED, State.ERROR, State.PREPARING, State.STOPPED, State.END -> false
                    State.PREPARED, State.STARTED, State.PAUSED, State.PLAYBACK_COMPLETED -> true
                }
            }
            return isReadyForPlayback
        }
    val duration: Int
        get() {
            var duration = 0
            synchronized(mState) {
                duration = when (mState.get()) {
                    State.END, State.IDLE, State.INITIALIZED, State.PREPARING, State.ERROR -> 0
                    State.PREPARED, State.STARTED, State.PAUSED, State.STOPPED, State.PLAYBACK_COMPLETED -> mMediaPlayer.duration
                }
            }
            return duration
        }

    fun seekToPercent(percent: Int) {
        synchronized(mState) {
            val state = mState.get()
            if (SHOW_LOGS) Log.d(
                TAG,
                "seekToPercent, percent $percent, mState $state"
            )
            when (state) {
                State.IDLE, State.INITIALIZED, State.ERROR, State.PREPARING, State.END, State.STOPPED -> if (SHOW_LOGS) Log.d(
                    TAG,
                    "seekToPercent, illegal state"
                )
                State.PREPARED, State.STARTED, State.PAUSED, State.PLAYBACK_COMPLETED -> {
                    val positionMillis =
                        (percent.toFloat() / 100f * duration).toInt()
                    mMediaPlayer.seekTo(positionMillis)
                    notifyPositionUpdated()
                }
            }
        }
    }

    private val mNotifyPositionUpdateRunnable = Runnable { notifyPositionUpdated() }

    init {
        if (SHOW_LOGS) Log.d(TAG, "constructor of MediaPlayerWrapper")
        if (SHOW_LOGS) Log.d(
            TAG,
            "constructor of MediaPlayerWrapper, main Looper " + Looper.getMainLooper()
        )
        if (SHOW_LOGS) Log.d(
            TAG,
            "constructor of MediaPlayerWrapper, my Looper " + Looper.myLooper()
        )
        if (Looper.myLooper() != null) {
            throw RuntimeException("myLooper not null, a bug in some MediaPlayer implementation cause that listeners are not called at all. Please use a thread without Looper")
        }
        mMediaPlayer = mediaPlayer
        mState.set(State.IDLE)
        mMediaPlayer.setOnVideoSizeChangedListener(this)
        mMediaPlayer.setOnCompletionListener(this)
        mMediaPlayer.setOnErrorListener(this)
        mMediaPlayer.setOnBufferingUpdateListener(this)
        mMediaPlayer.setOnInfoListener(this)
    }

    private fun startPositionUpdateNotifier() {
        if (SHOW_LOGS) Log.d(
            TAG,
            "startPositionUpdateNotifier, mPositionUpdateNotifier $mPositionUpdateNotifier"
        )
        mFuture = mPositionUpdateNotifier.scheduleAtFixedRate(
            mNotifyPositionUpdateRunnable,
            0,
            POSITION_UPDATE_NOTIFYING_PERIOD.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    private fun stopPositionUpdateNotifier() {
        if (SHOW_LOGS) Log.d(
            TAG,
            "stopPositionUpdateNotifier, mPositionUpdateNotifier $mPositionUpdateNotifier"
        )
        mFuture!!.cancel(true)
        mFuture = null
    }

    private fun notifyPositionUpdated() {
        synchronized(mState) { //todo: remove
//            if (SHOW_LOGS) Log.d(TAG, "notifyPositionUpdated, mVideoStateListener " + mVideoStateListener);
            if (mVideoStateListener != null && mState.get() == State.STARTED) {
                mVideoStateListener!!.onVideoPlayTimeChanged(mMediaPlayer.currentPosition)
            }
        }
    }

    val currentState: State
        get() {
            synchronized(mState) { return mState.get() }
        }

    override fun toString(): String {
        return javaClass.simpleName + "@" + hashCode()
    }

    interface MainThreadMediaPlayerListener {
        fun onVideoSizeChangedMainThread(width: Int, height: Int)
        fun onVideoPreparedMainThread()
        fun onVideoCompletionMainThread()
        fun onErrorMainThread(what: Int, extra: Int)
        fun onBufferingUpdateMainThread(percent: Int)
        fun onVideoStoppedMainThread()
    }

    interface VideoStateListener {
        fun onVideoPlayTimeChanged(positionInMilliseconds: Int)
    }

    private fun inUiThread(): Boolean {
        return Thread.currentThread().id == 1L
    }

    companion object {
        private const val SHOW_LOGS: Boolean = Config.SHOW_LOGS
        const val POSITION_UPDATE_NOTIFYING_PERIOD = 1000 // milliseconds
        fun positionToPercent(progressMillis: Int, durationMillis: Int): Int {
            val percentPrecise = progressMillis.toFloat() / durationMillis.toFloat() * 100f
            return Math.round(percentPrecise)
        }
    }
}
