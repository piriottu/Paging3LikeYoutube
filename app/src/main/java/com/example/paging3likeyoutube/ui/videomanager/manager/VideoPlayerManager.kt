package com.example.paging3likeyoutube.ui.videomanager.manager

import android.content.res.AssetFileDescriptor

interface VideoPlayerManager<T : MetaData?> {
    /**
     * Call it if you have direct url or path to video source
     * @param metaData - optional Meta Data
     * @param videoPlayerView - the actual video player
     * @param videoUrl - the link to the video source
     */
    fun playNewVideo(metaData: T, videoPlayerView: VideoPlayerView?, videoUrl: String?)

    /**
     * Call it if you have video source in assets directory
     * @param metaData - optional Meta Data
     * @param videoPlayerView - the actual video player
     * @param assetFileDescriptor -The asset descriptor of the video file
     */
    fun playNewVideo(
        metaData: T,
        videoPlayerView: VideoPlayerView?,
        assetFileDescriptor: AssetFileDescriptor?
    )

    /**
     * Call it if you need to stop any playback that is currently playing
     */
    fun stopAnyPlayback()

    /**
     * Call it if you no longer need the player
     */
    fun resetMediaPlayer()
}
