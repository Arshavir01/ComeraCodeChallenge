package com.example.comeracodechallenge.view

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.example.comeracodechallenge.databinding.FragmentGalleryBinding
import com.example.comeracodechallenge.utils.UtilMethods.hasMediaPermission
import com.example.comeracodechallenge.view.adapter.MediaAdapter
import com.example.comeracodechallenge.viewmodel.MediaViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class GalleryFragment: Fragment() {
    private lateinit var binding: FragmentGalleryBinding
    private lateinit var mediaAdapter: MediaAdapter
    private val viewModel by viewModel<MediaViewModel>()
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkMediaPermission()
        setMediaRecyclerView()
        bindView()
    }

    private fun bindView(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.viewState.collectLatest { viewState ->
                    val list = viewState.media
                    if (list.isNotEmpty()){
                        mediaAdapter.submitList(list)
                        if (isFirstLoad) {
                            isFirstLoad = false
                            initScrollListener()
                        }
                    }
                }
            }
        }
    }

    private fun setMediaRecyclerView() {
        mediaAdapter = MediaAdapter()
        val layoutManager = GridLayoutManager(requireContext(), 3)
        binding.mediaListRv.layoutManager = layoutManager
        binding.mediaListRv.adapter = mediaAdapter
    }

    private fun checkMediaPermission() {
        if (hasMediaPermission(requireContext())) {
            viewModel.onPermissionGranted()
        } else {
            requestForImageVideoPermissions()
            //put here text permission denied
        }
    }

    private fun initScrollListener() {
        binding.mediaListRv.clearOnScrollListeners()

        binding.mediaListRv.addOnScrollListener(
            object : OnScrollListener() {
                var firstItemPosition = -1
                var lastItemPosition = -1
                val linearLayoutManager = binding.mediaListRv.layoutManager as LinearLayoutManager?

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (linearLayoutManager != null) {
                        firstItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
                        lastItemPosition = linearLayoutManager.findLastVisibleItemPosition()

                        if (dy == 0 && lastItemPosition != -1) {
                            viewModel.generateFirstPartOfVideoThumbs(lastItemPosition, requireContext())
                        }
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    val isScrollingStopped = newState == RecyclerView.SCROLL_STATE_IDLE
                    if (isScrollingStopped && firstItemPosition != -1 && lastItemPosition != -1) {
                        viewModel.onScrollPositionChanged(firstItemPosition, lastItemPosition, requireContext())
                    }
                }
            },
        )
    }


    private fun requestForImageVideoPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestVideoAndImagePermissions.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ),
            )
        } else {
            requestPermissionLauncherForExternalStorage.launch(
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
    }

    private val requestPermissionLauncherForExternalStorage =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted){
                viewModel.onPermissionGranted()
            }
        }

    private val requestVideoAndImagePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isGranted = permissions.values.all { it }
            if (isGranted){
                viewModel.onPermissionGranted()
            }
        }

}