package com.example.comeracodechallenge.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.comeracodechallenge.databinding.FragmentGalleryBinding
import com.example.comeracodechallenge.viewmodel.MediaViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class GalleryFragment: Fragment() {
    private lateinit var binding: FragmentGalleryBinding

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
    }

}