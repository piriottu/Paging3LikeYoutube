package com.example.paging3likeyoutube

import android.content.Context
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
/*import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView*/

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView

@UnstableApi class PlayerManager(videoUrl: String) {

    interface PlayerListener {
        fun onVideoEnded()
        fun onBuffering()
        fun onReady()
        fun onError()
        fun onBlock(block: PlayerBlock)
        fun endBlock()
    }

    var player: ExoPlayer? = null

    var videoUri: String
    private var isPlayerPlaying: Boolean = false
    private var bookmark: Long = 0
    var settings: PlayerSettings = PlayerSettings()


    init {
        this.videoUri = videoUrl
    }

    //region - Public methods

    fun preparePlayer(context: Context?, exoPlayerView: PlayerView?, settings: PlayerSettings) {
        this.settings = settings
        preparePlayer(context, exoPlayerView, settings.loopVideo)
    }

  /*  fun releaseVideoPlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        settings.startOnReady = playWhenReady
        player?.playWhenReady = playWhenReady
        player?.playbackState
    }
*/
    fun retry() {
        player?.pause()
    }

  /*  fun stop() {
        player?.stop()
    }

    fun updateBookmarkVideoPlayer() {
        updateBookmark()
        player?.playWhenReady = false
    }

    fun goToBackground() {
        if (player != null) {
            isPlayerPlaying = player!!.playWhenReady
            player?.stop()
        }
    }

    fun getDuration(): Long {
        return if (player != null) {
            player?.duration ?: 0
        } else {
            0
        }
    }

    fun getCurrentPosition(): Long {
        return if (player != null) {
            player?.currentPosition ?: 0
        } else {
            0
        }
    }

    private var volume = 0f
    fun mute() {
        volume = player?.volume ?: 0f
        player?.volume = 0f
    }

    fun unmute() {
        player?.volume = volume
    }


    fun goToForeground() {
        if (player != null) {
            player!!.pause()
            player!!.playWhenReady = isPlayerPlaying
        }
    }*/
    // endregion

    //region - Private Methods
    private fun preparePlayer(context: Context?, exoPlayerView: PlayerView?, loopVideo: Boolean) {
        if (context == null || exoPlayerView == null) {
            return
        }

        if (player != null) {
            player?.release()
            player = null
        }

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player =
            ExoPlayer.Builder(context)
                /*.setLoadControl(DefaultLoadControl())*/
                /*.setRenderersFactory(DefaultRenderersFactory(context))*/
                .setTrackSelector(trackSelector)
                .build()

        val mediaSource = MediaItem.fromUri(videoUri) /*getMediaSource(context)*/

       /* if (loopVideo) {
            // Looping source
            val loopingSource = LoopingMediaSource(mediaSource)
            // Prepare player
            player?.prepare(loopingSource, true, false)
        } else {
            player?.prepare(mediaSource, true, false)
        }*/

        player?.setMediaItem(mediaSource)

        player?.clearVideoSurface()
        when (exoPlayerView.videoSurfaceView) {
            is TextureView -> {
                player?.setVideoTextureView(exoPlayerView.videoSurfaceView as TextureView)
            }
            else -> {
                player?.setVideoSurfaceView(exoPlayerView.videoSurfaceView as SurfaceView)
            }
        }

        setPlayerSettings()
    }

    /*private fun getMediaSource(context: Context): MediaSource {
        val cacheDSF = CacheDataSourceFactory(
            context,
            100 * 1024 * 1024,
            50 * 1024 * 1024
        )

        return when (Util.inferContentType(videoUri!!)) {
            C.CONTENT_TYPE_DASH -> {
                DashMediaSource
                    .Factory(DefaultDataSourceFactory(context, cacheDSF))
                    .createMediaSource(videoUri!!)
            }
            C. CONTENT_TYPE_SS -> {
                SsMediaSource
                    .Factory(DefaultDataSourceFactory(context, cacheDSF))
                    .createMediaSource(videoUri!!)
            }
            C. CONTENT_TYPE_HLS  -> {
                HlsMediaSource
                    .Factory(DefaultDataSourceFactory(context, cacheDSF))
                    .createMediaSource(videoUri!!)
            }
            else -> {
                ProgressiveMediaSource
                    .Factory(DefaultDataSourceFactory(context, cacheDSF))
                    .createMediaSource(videoUri!!)
            }
        }
    }*/

    private fun setPlayerSettings() {
        setPlayerListener()

        if (!settings.audio) {
            player?.volume = 0f
        }

        player?.videoScalingMode = settings.videoScalingMode
    }

    private fun setPlayerListener() {
        player?.addListener(object : Player.Listener {

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                Log.d("EXO EVENT LISTENER", "onPlayerStateChanged $playbackState")
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        settings.listener?.onVideoEnded()
                        if (settings.lastBlockNotified > -1) {
                            settings.listener?.endBlock()
                        }
                    }

                    Player.STATE_BUFFERING -> {
                        settings.listener?.onBuffering()
                    }
                    Player.STATE_READY -> {
                        if (settings.startOnReady) {
                            player?.playWhenReady = true
                            player?.playbackState
                        }

                        if (player?.playWhenReady == true) {
                            notifyBlocks()
                        }
                        settings.listener?.onReady()
                    }
                    Player.STATE_IDLE -> {
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.d("EXO EVENT LISTENER", "onPlayerError:" + (error.localizedMessage ?: ""))
                settings.listener?.onError()
            }
        })
    }


    /*private fun updateBookmark() {
        bookmark = player?.currentPosition ?: 0
        Log.d("EXO POSITION", bookmark.toString())
    }*/

    private fun notifyBlocks() {
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            tryToSendNotification()
            if (canNotify()) {
                notifyBlocks()
            }
        }, 1000)
    }

    private fun tryToSendNotification() {
        if (canNotify()) {
            val currentPosition = player!!.currentPosition
            val block = settings.blocks!!.firstOrNull {
                it.startBlock <= currentPosition &&
                        it.endBlock >= currentPosition &&
                        it.id != settings.lastBlockNotified
            }
            if (block != null) {
                settings.lastBlockNotified = block.id
                settings.listener!!.onBlock(block)
            } else {
                if (settings.lastBlockNotified > -1) {
                    val lastBlock = settings.blocks!![settings.lastBlockNotified]
                    if (lastBlock.endBlock < currentPosition || lastBlock.startBlock > currentPosition) {
                        settings.lastBlockNotified = -1
                        settings.listener!!.endBlock()
                    }
                }
            }
        }
    }

    private fun canNotify(): Boolean {
        if (player == null) {
            return false
        }

        if (!player!!.playWhenReady) {
            return false
        }

        if (settings.listener == null || settings.blocks.isNullOrEmpty()) {
            return false
        }

        return true
    }

    //endregion

    data class PlayerSettings(
        var loopVideo: Boolean = false,
        var startOnReady: Boolean = true,
        var audio: Boolean = true,
        var videoScalingMode: Int = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING,
        var listener: PlayerListener? = null,
        var blocks: List<PlayerBlock>? = null,
        var lastBlockNotified: Int = -1
    )

    data class PlayerBlock(
        val id: Int,
        var startBlock: Long,
        var endBlock: Long
    )
}









