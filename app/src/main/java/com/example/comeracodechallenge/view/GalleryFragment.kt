package com.example.comeracodechallenge.view

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import com.example.comeracodechallenge.databinding.FragmentGalleryBinding
import com.example.comeracodechallenge.view.adapter.MediaAdapter
import com.example.comeracodechallenge.viewmodel.MediaViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class GalleryFragment: Fragment() {
    private lateinit var binding: FragmentGalleryBinding
    private lateinit var mediaAdapter: MediaAdapter
    private val viewModel by viewModel<MediaViewModel>()

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
        requestForImageVideoPermissions()
        setMediaRecyclerView()
    }

    private fun bindView(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.viewState.collectLatest { list ->
                    if (list.isNotEmpty()){
                        mediaAdapter.submitList(list)
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
                bindView()
            }
        }

    private val requestVideoAndImagePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isGranted = permissions.values.all { it }
            if (isGranted){
                bindView()
            }
        }


}