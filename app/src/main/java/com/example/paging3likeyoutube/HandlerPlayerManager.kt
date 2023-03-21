package com.example.paging3likeyoutube
import android.net.Uri
import androidx.media3.common.util.UnstableApi

@UnstableApi class HandlerPlayerManager {

    private val playerInstances = HashMap<String, PlayerManager>()

    companion object {
        const val tag = "ExoPlayerViewManager"
        const val extraVideoUri = "video_uri"
        const val fallbackVideoUri = "fallback_video_uri"
        const val videoTitle = "video_title"
        val instance: HandlerPlayerManager by lazy { HandlerPlayerManager() }
    }

    //region - Public methods

    fun getPlayerInstance(tag: String,videoUri: String): PlayerManager {
        var instance: PlayerManager? = playerInstances[tag]
        if (instance == null) {
            instance = PlayerManager(videoUri)
            playerInstances[tag] = instance
        } else {
            instance.videoUri = videoUri
        }
        return instance
    }

    fun getPlayerInstance(videoUri: String): PlayerManager {
        var instance: PlayerManager? = playerInstances[videoUri]
        if (instance == null) {
            instance = PlayerManager(videoUri)
            playerInstances[videoUri] = instance
        }
        return instance
    }


    fun goToBackground() {
        for (playerInstance in playerInstances) {
            if (playerInstance.value.player != null) {
                playerInstance.value.player!!.stop()
            }
        }
    }

    fun goToForeground() {
        for (playerInstance in playerInstances) {
            if (playerInstance.value.player != null) {
                playerInstance.value.player!!.retry()
            }
        }
    }

    fun goToForeground(tag: String) {
        val playerInstance = playerInstances[tag]
        playerInstance?.retry()
    }

    fun releasePlayers() {

        for (playerInstance in playerInstances) {
            if (playerInstance.value.player != null) {
                playerInstance.value.player?.release()
                playerInstance.value.player = null
            }
        }
    }

    // endregion

}