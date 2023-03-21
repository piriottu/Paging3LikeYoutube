package com.example.paging3likeyoutube.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    val video = MutableLiveData<MutableList<PostUIItem>>()

    fun showItems() {
        video.value = getVideo()
    }

    private fun getVideo(): MutableList<PostUIItem> {
        return mutableListOf(
            PostUIItem(
                0,
                "https://dzqwwyjh5mag2.cloudfront.net/post_media/cf1b2175-ca7a-4a1c-96d6-da7735c8edaa.mp4",
            ),
            PostUIItem(
                1,
                "https://dzqwwyjh5mag2.cloudfront.net/post_media/cf1b2175-ca7a-4a1c-96d6-da7735c8edaa.mp4",
            ),
            PostUIItem(
                2,
                "https://dzqwwyjh5mag2.cloudfront.net/post_media/fd3d0cdc-af70-4322-89eb-c1d471455f30.mp4",
            ),
            PostUIItem(
                3,
                "https://dzqwwyjh5mag2.cloudfront.net/post_media/b25075a6-328b-4806-82e5-717bb39bac6f.mp4",
            )
        )
    }
}