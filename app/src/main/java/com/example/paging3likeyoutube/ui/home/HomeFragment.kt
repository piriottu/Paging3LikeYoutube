package com.example.paging3likeyoutube.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.example.paging3likeyoutube.databinding.FragmentHomeBinding

@UnstableApi
class HomeFragment : Fragment(), PostAdapter.Listener {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    /**
     * ViewModel
     */
    private val viewModel: HomeViewModel by viewModels()

    /**
     * Adapter
     * */
    private val adapter: PostAdapter by lazy {
        PostAdapter(this)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.recyclerView.adapter=adapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    //viewModel.pauseVideo()
                }
            }
        })

        viewModel.playVideo(0)
        viewModel.video.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPlayVideo(position: Int) {
        viewModel.playVideo(position)
    }

  /*  fun initPlayPosition() {
        var index = 0
        do {
            if (isVideoItem(index)) {
                playPosition = index
            }
            index += 1
        } while (playPosition == -1 && index >= homeFeed.size - 1)
    }*/
}