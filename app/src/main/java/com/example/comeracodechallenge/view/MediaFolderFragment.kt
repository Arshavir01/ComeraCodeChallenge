package com.example.comeracodechallenge.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.comeracodechallenge.databinding.FragmentMediaFolderBinding
import com.example.comeracodechallenge.view.adapter.MediaFolderAdapter
import com.example.comeracodechallenge.viewmodel.MediaViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MediaFolderFragment: Fragment() {
    private lateinit var binding: FragmentMediaFolderBinding
    private val viewModel: MediaViewModel by activityViewModel()
    private lateinit var folderAdapter: MediaFolderAdapter
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaFolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
        setFolderRecyclerView()
        setBackButton()
        bindView()
    }

    private fun setFolderRecyclerView() {
        folderAdapter = MediaFolderAdapter(::onItemClick)
        val layoutManager = GridLayoutManager(requireContext(), 2)
        binding.mediaFoldersListRv.layoutManager = layoutManager
        binding.mediaFoldersListRv.adapter = folderAdapter
    }

    private fun onItemClick(folderId: Int){
        viewModel.updateMediaListFromFolder(folderId)
        dismissFragment()
    }

    private fun setBackButton(){
        binding.backButton.setOnClickListener {
            dismissFragment()
        }
    }

    private fun bindView(){
        val folders = viewModel.getAllFolderWithData()
        folderAdapter.submitList(folders)
        binding.noMediaLabel.isVisible = folders.isEmpty()
    }

    private fun dismissFragment() {
        navController.popBackStack()
    }
}
