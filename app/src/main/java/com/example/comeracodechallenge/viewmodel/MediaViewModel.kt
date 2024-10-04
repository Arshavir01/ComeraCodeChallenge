package com.example.comeracodechallenge.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comeracodechallenge.model.repository.MediaRepository
import kotlinx.coroutines.launch

class MediaViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    val viewState = repository.mediaFlow

    init {
        viewModelScope.launch {
            repository.getAllMediaFilesOnDevice()
        }
    }
}