package com.example.paging3likeyoutube.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.recyclerview.widget.RecyclerView
import com.example.paging3likeyoutube.R
import com.example.paging3likeyoutube.databinding.LayoutPostItemBinding

@UnstableApi
class PostViewHolder(
    val binding: LayoutPostItemBinding,
    private val listener: PostAdapter.Listener
) :
    RecyclerView.ViewHolder(binding.root) {

    private lateinit var player: ExoPlayer

    private lateinit var itemRef: PostUIItem

    //state variable for the player
    private var playerInitialized = false
    private var audioOn = true
    private var videoOn = false

    init {
        setupVideoView()

        binding.play.setOnClickListener {
            if (videoOn) {
                onDetach()
            } else {
                onAttach()
            }
        }
    }

    //region public methods
    private fun onAttach() {
        audioOn = true
        videoOn = true
        updatePlayerStatus()
    }

    fun onDetach() {
        audioOn = false
        videoOn = false
        updatePlayerStatus()
    }

    fun bind(item: PostUIItem) {
        itemRef = item

        //set video
        if (playerInitialized.not()) {
            initializePlayer(itemRef.url)
            playerInitialized = true
        }
    }

    private fun setupVideoView() {

        val trackSelector = DefaultTrackSelector(binding.root.context).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player = ExoPlayer.Builder(binding.root.context).setTrackSelector(trackSelector)
            .build().also { exoPlayer ->
                binding.videoView.player = exoPlayer
            }

        player.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                //  startPlayer()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // startPlayer()
            }
        })
    }

    private fun initializePlayer(url: String) {

        val mediaItem =
            MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        player.volume = if (audioOn) 1f else 0f
        player.prepare()
    }

    companion object {
        fun getBinding(parent: ViewGroup): LayoutPostItemBinding {
            val inflater = LayoutInflater.from(parent.context)

            return LayoutPostItemBinding.inflate(
                inflater,
                parent,
                false
            )
        }
    }

    private fun updatePlayerStatus() {
        //update player volume status
        player.volume = if (audioOn) 1f else 0f
        //update player playback status
        if (videoOn) player.play() else player.pause()
        //update volume icon status
        /*binding.discoverVerticalVideoItemAudioIcon.setImageResource(
            if (audioOn)
                R.drawable.ic_baseline_volume_up
            else
                R.drawable.ic_baseline_volume_off
        )*/
    }
}