package com.example.paging3likeyoutube.ui.home

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.paging3likeyoutube.HandlerPlayerManager
import com.example.paging3likeyoutube.PlayerManager
import com.example.paging3likeyoutube.R
import com.example.paging3likeyoutube.databinding.LayoutPostItemBinding
import java.lang.Math.ceil
import javax.sql.DataSource

@UnstableApi
class PostViewHolder(
    val binding: LayoutPostItemBinding,
    private val listener: PostAdapter.Listener
) :
    RecyclerView.ViewHolder(binding.root) {

    private lateinit var player: ExoPlayer
    private lateinit var itemRef: PostUIItem

    private val playbackStateListener: Player.Listener = playbackStateListener()

    //state variable for the player
    private var playerInitialized = false
    private var audioOn = false
    private var videoOn = false

    init {
        setupVideoView()

        binding.audio.setOnClickListener {
            audioOn = !audioOn
            player.volume = if (audioOn) 1f else 0f
        }
    }

    fun bind(item: PostUIItem, canPlay: Boolean) {
        itemRef = item
        setCoverImage(itemRef.url, canPlay)
    }

    fun setPlayer() {

        //set video
        if (playerInitialized.not()) {
            initializePlayer(itemRef.url)
            playerInitialized = true
            onAttach()
        }
        Log.d("niko", "id : ${itemRef.id} - canPlay true")
        /*   val url = itemRef.url

            val playerManager =
                HandlerPlayerManager.instance.getPlayerInstance(url)
            val settings = PlayerManager.PlayerSettings(
                loopVideo = true,
                audio = false,
                listener = object : PlayerManager.PlayerListener {
                    override fun endBlock() {}

                    override fun onBlock(block: PlayerManager.PlayerBlock) {}

                    override fun onVideoEnded() {}

                    override fun onBuffering() {}

                    override fun onReady() {
                        binding.videoView.isVisible = true
                        binding.placeholder.isVisible = false
                    }

                    override fun onError() {
                        binding.videoView.visibility = View.INVISIBLE
                        binding.placeholder.isVisible = true
                    }

                })

            playerManager.preparePlayer(
                binding.root.context,
                binding.videoView,
                settings
            )

            val player = playerManager.player
            // Set ExoPlayer to player view
            binding.videoView.player = player*/
    }

    //region public methods
    private fun onAttach() {
        audioOn = true
        videoOn = true
        updatePlayerStatus()
    }

    private fun onDetach() {
        playerInitialized = false
        audioOn = false
        videoOn = false
        updatePlayerStatus()
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
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {}
        })
    }

    private fun initializePlayer(url: String) {

        val mediaItem =
            MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        player.volume = if (audioOn) 1f else 0f
        player.addListener(playbackStateListener)
        player.playWhenReady = true
        player.prepare()
    }

    private fun setCoverImage(videoUrl: String?, canPlay: Boolean) {

        Glide.with(binding.root.context)
            .load(videoUrl)
            .error(R.drawable.ic_dashboard_black_24dp)
            .placeholder(R.drawable.ic_dashboard_black_24dp)
            .centerCrop()
            .into(binding.placeholder)

        Log.d("niko", "id : ${itemRef.id} - canPlay $canPlay")


        if (canPlay) {
            binding.placeholder.setBackgroundColor(Color.BLUE)
        } else {
            onDetach()
            binding.placeholder.isVisible = true
            binding.videoView.isVisible = false
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

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> {
                    binding.placeholder.isVisible = true
                    binding.videoView.isVisible = false
                }
                ExoPlayer.STATE_READY -> {
                    binding.placeholder.isVisible = false
                    binding.videoView.isVisible = true
                }
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
        }
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
}