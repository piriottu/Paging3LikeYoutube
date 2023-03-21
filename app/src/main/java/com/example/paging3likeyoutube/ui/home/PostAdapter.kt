package com.example.paging3likeyoutube.ui.home

import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

@UnstableApi
class PostAdapter(private val listener: Listener) :
    ListAdapter<PostUIItem, PostViewHolder>(DIFF_CALLBACK) {

    interface Listener {
        fun onPlayVideo(position: Int)
    }

    var playPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            binding = PostViewHolder.getBinding(
                parent
            ), listener
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position), canPlay = position == playPosition)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PostUIItem>() {
            override fun areItemsTheSame(
                oldItem: PostUIItem,
                newItem: PostUIItem
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: PostUIItem,
                newItem: PostUIItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}