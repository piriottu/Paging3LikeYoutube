package com.example.paging3likeyoutube.ui.home

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
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

    //state variable for the player
    private var playerInitialized = false
    private var audioOn = true
    private var videoOn = false

    init {
        setupVideoView()

        /* binding.play.setOnClickListener {
             if (videoOn) {
                 onDetach()
             } else {
                 onAttach()
             }
         }*/
    }

     //region public methods
     private fun onAttach() {
         audioOn = true
         videoOn = true
         updatePlayerStatus()
     }

     private fun onDetach() {
         audioOn = false
         videoOn = false
         //player.clearVideoSurface()
         updatePlayerStatus()
         playerInitialized = false
     }

    fun setPlayer() {
        binding.placeholder.setBackgroundColor(Color.BLUE)

        //set video
        if (playerInitialized.not()) {
            initializePlayer(itemRef.url)
            playerInitialized = true
        }

        onAttach()

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

    fun bind(item: PostUIItem, canPlay: Boolean) {
        itemRef = item

        setCoverImage(itemRef.url, canPlay)
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

    private fun setCoverImage(videoUrl: String?, canPlay: Boolean) {

       /* Glide.with(binding.root.context)
            .load(videoUrl)
            .error(R.drawable.ic_dashboard_black_24dp)
            .placeholder(R.drawable.ic_dashboard_black_24dp)
            .centerCrop()
            .addListener(object : RequestListener<Drawable> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (canPlay) {
                        setPlayer()
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (canPlay) {
                        setPlayer()
                    }
                    return false
                }

            })
            .into(binding.placeholder)*/

          if (canPlay) {
              binding.placeholder.setBackgroundColor(Color.BLUE)
          } else {
              onDetach()
              binding.placeholder.setBackgroundColor(Color.RED)
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